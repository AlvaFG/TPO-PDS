#!/usr/bin/env python3
"""Lee un archivo .mdj de StarUML y lo muestra como texto legible o Mermaid.

Uso:
    python3 leer_mdj.py archivo.mdj              # resumen legible (por defecto)
    python3 leer_mdj.py archivo.mdj --mermaid    # diagrama de clases Mermaid
    python3 leer_mdj.py archivo.mdj --json       # modelo normalizado (para procesar)

Sale con codigo 1 si el archivo no existe o no es un .mdj valido.
"""
import argparse
import json
import sys

VIS = {"public": "+", "private": "-", "protected": "#", "package": "~"}
VIS_MERMAID = {"public": "+", "private": "-", "protected": "#", "package": "~"}

# Tipos que representan clasificadores dibujables en un diagrama de clases.
CLASIFICADORES = ("UMLClass", "UMLInterface", "UMLEnumeration", "UMLDataType")


def indexar(nodo, indice):
    """Mapea cada _id -> elemento, para poder resolver los {"$ref": ...}."""
    if isinstance(nodo, dict):
        if "_id" in nodo:
            indice[nodo["_id"]] = nodo
        for v in nodo.values():
            indexar(v, indice)
    elif isinstance(nodo, list):
        for v in nodo:
            indexar(v, indice)
    return indice


def buscar(nodo, tipos, salida=None):
    """Junta todos los elementos cuyo _type este en `tipos`."""
    salida = [] if salida is None else salida
    if isinstance(nodo, dict):
        if nodo.get("_type") in tipos:
            salida.append(nodo)
        for v in nodo.values():
            buscar(v, tipos, salida)
    elif isinstance(nodo, list):
        for v in nodo:
            buscar(v, tipos, salida)
    return salida


def nombre_tipo(valor, indice):
    """El campo `type` puede ser texto libre ("List<Seccion>") o un $ref a otra clase."""
    if valor is None:
        return ""
    if isinstance(valor, str):
        return valor
    if isinstance(valor, dict):
        if "$ref" in valor:
            destino = indice.get(valor["$ref"])
            return destino.get("name", "?") if destino else "?"
        return valor.get("name", "")
    return str(valor)


def multiplicidad(elem):
    m = elem.get("multiplicity")
    return m if m else ""


def leer_atributo(a, indice):
    return {
        "nombre": a.get("name", ""),
        "tipo": nombre_tipo(a.get("type"), indice),
        "visibilidad": a.get("visibility", "private"),
        "estatico": bool(a.get("isStatic")),
        "multiplicidad": multiplicidad(a),
    }


def leer_operacion(op, indice):
    params, retorno = [], "void"
    for p in op.get("parameters", []):
        tipo = nombre_tipo(p.get("type"), indice)
        if p.get("direction") == "return":
            retorno = tipo or "void"
        else:
            params.append({"nombre": p.get("name", ""), "tipo": tipo})
    return {
        "nombre": op.get("name", ""),
        "parametros": params,
        "retorno": retorno,
        "visibilidad": op.get("visibility", "public"),
        "abstracta": bool(op.get("isAbstract")),
        "estatico": bool(op.get("isStatic")),
    }


def leer_clasificador(c, indice):
    return {
        "id": c.get("_id"),
        "tipo": c.get("_type", "").replace("UML", ""),
        "nombre": c.get("name", "(sin nombre)"),
        "abstracta": bool(c.get("isAbstract")),
        "documentacion": (c.get("documentation") or "").strip(),
        "atributos": [leer_atributo(a, indice) for a in c.get("attributes", [])],
        "operaciones": [leer_operacion(o, indice) for o in c.get("operations", [])],
        "literales": [l.get("name", "") for l in c.get("literals", [])],
    }


def ref_nombre(valor, indice):
    if isinstance(valor, dict) and "$ref" in valor:
        destino = indice.get(valor["$ref"])
        return destino.get("name", "?") if destino else "?"
    return "?"


def leer_relaciones(raiz, indice):
    rels = []
    for a in buscar(raiz, ("UMLAssociation",)):
        e1, e2 = a.get("end1", {}), a.get("end2", {})
        # El rombo de agregacion/composicion se guarda en el extremo que hace de
        # "todo". StarUML suele ponerlo en end2, asi que damos vuelta los extremos
        # para que el origen sea siempre el todo y se lea "Todo agrega Parte".
        agg1 = e1.get("aggregation", "none") or "none"
        agg2 = e2.get("aggregation", "none") or "none"
        if agg2 != "none":
            todo, parte, agregacion = e2, e1, agg2
        elif agg1 != "none":
            todo, parte, agregacion = e1, e2, agg1
        else:
            todo, parte, agregacion = e1, e2, "none"
        rels.append({
            "clase": "asociacion",
            "origen": ref_nombre(todo.get("reference"), indice),
            "destino": ref_nombre(parte.get("reference"), indice),
            "mult_origen": multiplicidad(todo),
            "mult_destino": multiplicidad(parte),
            "rol_origen": todo.get("name", ""),
            "rol_destino": parte.get("name", ""),
            # "shared" = agregacion (rombo blanco), "composite" = composicion (rombo negro)
            "agregacion": agregacion,
            "nombre": a.get("name", ""),
        })
    for g in buscar(raiz, ("UMLGeneralization",)):
        rels.append({
            "clase": "herencia",
            "origen": ref_nombre(g.get("source"), indice),
            "destino": ref_nombre(g.get("target"), indice),
        })
    for r in buscar(raiz, ("UMLInterfaceRealization",)):
        rels.append({
            "clase": "realizacion",
            "origen": ref_nombre(r.get("source"), indice),
            "destino": ref_nombre(r.get("target"), indice),
        })
    for d in buscar(raiz, ("UMLDependency",)):
        rels.append({
            "clase": "dependencia",
            "origen": ref_nombre(d.get("source"), indice),
            "destino": ref_nombre(d.get("target"), indice),
            "nombre": d.get("name", ""),
        })
    return rels


def cargar(ruta):
    with open(ruta, encoding="utf-8") as f:
        datos = json.load(f)
    indice = indexar(datos, {})
    clasificadores = [leer_clasificador(c, indice)
                      for c in buscar(datos, CLASIFICADORES)]
    diagramas = [{"nombre": d.get("name", "(sin nombre)"),
                  "tipo": d.get("_type", "").replace("UML", ""),
                  "vistas": len(d.get("ownedViews", []))}
                 for d in buscar(datos, ("UMLClassDiagram", "UMLSequenceDiagram",
                                         "UMLUseCaseDiagram", "UMLStatechartDiagram",
                                         "UMLActivityDiagram", "UMLComponentDiagram",
                                         "UMLDeploymentDiagram", "UMLObjectDiagram"))]
    return {
        "proyecto": datos.get("name", "Untitled"),
        "diagramas": diagramas,
        "clasificadores": clasificadores,
        "relaciones": leer_relaciones(datos, indice),
    }


# ---------------------------------------------------------------- salida texto

def firma_operacion(op):
    params = ", ".join(f"{p['nombre']}: {p['tipo']}" if p["tipo"] else p["nombre"]
                       for p in op["parametros"])
    s = f"{VIS.get(op['visibilidad'], '+')} {op['nombre']}({params}): {op['retorno']}"
    if op["abstracta"]:
        s += "  [abstracto]"
    if op["estatico"]:
        s += "  [static]"
    return s


def firma_atributo(a):
    s = f"{VIS.get(a['visibilidad'], '-')} {a['nombre']}"
    if a["tipo"]:
        s += f": {a['tipo']}"
    if a["multiplicidad"]:
        s += f" [{a['multiplicidad']}]"
    if a["estatico"]:
        s += "  [static]"
    return s


FLECHAS = {
    "herencia": "--|>  hereda de",
    "realizacion": "..|>  implementa",
    "dependencia": "-->   usa",
}


def render_texto(m):
    out = [f"PROYECTO: {m['proyecto']}"]
    if m["diagramas"]:
        out.append("Diagramas: " + ", ".join(
            f"{d['nombre']} ({d['tipo']}, {d['vistas']} vistas)" for d in m["diagramas"]))
    out.append(f"{len(m['clasificadores'])} clasificadores, {len(m['relaciones'])} relaciones")
    out.append("")

    for c in m["clasificadores"]:
        cab = c["nombre"]
        etiquetas = []
        if c["tipo"] != "Class":
            etiquetas.append(f"<<{c['tipo'].lower()}>>")
        if c["abstracta"]:
            etiquetas.append("abstracta")
        if etiquetas:
            cab += "  " + " ".join(etiquetas)
        out.append(cab)
        out.append("-" * max(len(cab), 20))
        if c["documentacion"]:
            out.append(f"  // {c['documentacion']}")
        for l in c["literales"]:
            out.append(f"  * {l}")
        for a in c["atributos"]:
            out.append(f"  {firma_atributo(a)}")
        if c["atributos"] and c["operaciones"]:
            out.append("  " + "." * 18)
        for o in c["operaciones"]:
            out.append(f"  {firma_operacion(o)}")
        if not (c["atributos"] or c["operaciones"] or c["literales"]):
            out.append("  (sin miembros)")
        out.append("")

    if m["relaciones"]:
        out.append("RELACIONES")
        out.append("=" * 40)
        for r in m["relaciones"]:
            if r["clase"] == "asociacion":
                simbolo = {"shared": "<>--  agrega", "composite": "<#>-- compone"}.get(
                    r["agregacion"], "----  asociado a")
                izq = f" \"{r['mult_origen']}\"" if r["mult_origen"] else ""
                der = f" \"{r['mult_destino']}\"" if r["mult_destino"] else ""
                linea = f"  {r['origen']}{izq}  {simbolo}  {r['destino']}{der}"
                extras = [x for x in (r.get("nombre"), r.get("rol_destino")) if x]
                if extras:
                    linea += f"   ({', '.join(extras)})"
                out.append(linea)
            else:
                out.append(f"  {r['origen']}  {FLECHAS[r['clase']]}  {r['destino']}")
    return "\n".join(out)


# -------------------------------------------------------------- salida mermaid

def mermaid_id(nombre):
    """Mermaid no tolera espacios ni acentos en los identificadores de clase."""
    limpio = "".join(ch if ch.isalnum() or ch == "_" else "_" for ch in nombre)
    return limpio or "Clase"


def mermaid_tipo(tipo):
    """Mermaid escribe los genericos con ~ en vez de <>: List<X> -> List~X~."""
    return tipo.replace("<", "~").replace(">", "~") if tipo else tipo


def render_mermaid(m):
    out = ["classDiagram"]
    for c in m["clasificadores"]:
        cid = mermaid_id(c["nombre"])
        cuerpo = []
        for l in c["literales"]:
            cuerpo.append(f"    {l}")
        for a in c["atributos"]:
            tipo = mermaid_tipo(a["tipo"] or "")
            cuerpo.append(f"    {VIS_MERMAID.get(a['visibilidad'], '-')}{tipo} {a['nombre']}".rstrip())
        for o in c["operaciones"]:
            params = ", ".join(f"{p['nombre']}: {mermaid_tipo(p['tipo'])}" if p["tipo"] else p["nombre"]
                               for p in o["parametros"])
            marca = "*" if o["abstracta"] else ("$" if o["estatico"] else "")
            cuerpo.append(f"    {VIS_MERMAID.get(o['visibilidad'], '+')}{o['nombre']}({params}){marca} {mermaid_tipo(o['retorno'])}")
        if cuerpo:
            out.append(f"  class {cid} {{")
            out.extend("  " + l for l in cuerpo)
            out.append("  }")
        else:
            out.append(f"  class {cid}")
        if c["tipo"] == "Interface":
            out.append(f"  <<interface>> {cid}")
        elif c["tipo"] == "Enumeration":
            out.append(f"  <<enumeration>> {cid}")
        elif c["abstracta"]:
            out.append(f"  <<abstract>> {cid}")

    for r in m["relaciones"]:
        o, d = mermaid_id(r["origen"]), mermaid_id(r["destino"])
        if r["clase"] == "herencia":
            out.append(f"  {d} <|-- {o}")
        elif r["clase"] == "realizacion":
            out.append(f"  {d} <|.. {o}")
        elif r["clase"] == "dependencia":
            out.append(f"  {o} ..> {d}")
        else:
            flecha = {"shared": "o--", "composite": "*--"}.get(r["agregacion"], "--")
            izq = f'"{r["mult_origen"]}" ' if r["mult_origen"] else ""
            der = f' "{r["mult_destino"]}"' if r["mult_destino"] else ""
            etiqueta = r.get("nombre") or r.get("rol_destino") or ""
            sufijo = f" : {etiqueta}" if etiqueta else ""
            out.append(f"  {o} {izq}{flecha}{der} {d}{sufijo}")
    return "\n".join(out)


def main():
    ap = argparse.ArgumentParser(description="Lee un .mdj de StarUML")
    ap.add_argument("archivo")
    grupo = ap.add_mutually_exclusive_group()
    grupo.add_argument("--mermaid", action="store_true", help="salida Mermaid")
    grupo.add_argument("--json", action="store_true", help="modelo normalizado en JSON")
    args = ap.parse_args()

    try:
        modelo = cargar(args.archivo)
    except FileNotFoundError:
        print(f"No existe el archivo: {args.archivo}", file=sys.stderr)
        return 1
    except json.JSONDecodeError as e:
        print(f"No es un .mdj valido (JSON mal formado): {e}", file=sys.stderr)
        return 1

    if args.json:
        print(json.dumps(modelo, indent=2, ensure_ascii=False))
    elif args.mermaid:
        print(render_mermaid(modelo))
    else:
        print(render_texto(modelo))
    return 0


if __name__ == "__main__":
    sys.exit(main())
