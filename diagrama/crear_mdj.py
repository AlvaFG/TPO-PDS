#!/usr/bin/env python3
"""Genera un archivo .mdj de StarUML a partir de una especificacion JSON simple.

Escribir un .mdj a mano es inviable: cada clase necesita cinco compartimentos,
cada compartimento sus etiquetas, y cada relacion una decena de referencias
cruzadas por _id. Este script se encarga de todo eso; vos solo describis el
modelo en JSON.

Uso:
    python3 crear_mdj.py spec.json -o salida.mdj
    python3 crear_mdj.py spec.json -o salida.mdj --ancho 1400

El formato del spec esta documentado en references/formato-spec.md.
Sale con codigo 1 si el spec tiene errores (clases repetidas, relaciones que
apuntan a clases inexistentes, etc.) y los lista por stderr.
"""
import argparse
import json
import sys

FUENTE = "Arial;13;0"
FUENTE_NEGRITA = "Arial;13;1"
FUENTE_ITALICA = "Arial;13;2"

ALTO_LINEA = 13
ALTO_NOMBRE = 25
PAD_COMPARTIMENTO = 10
ANCHO_MIN = 130
SEP_X = 60          # espacio horizontal entre clases
SEP_Y = 90          # espacio vertical entre capas de herencia
MARGEN = 40

VIS = {"public": "+", "private": "-", "protected": "#", "package": "~"}

# Tipo de relacion -> como se representa internamente.
JERARQUICAS = {"herencia": "UMLGeneralization", "realizacion": "UMLInterfaceRealization"}


class Ids:
    """StarUML solo necesita que los _id sean unicos y estables dentro del archivo."""

    def __init__(self):
        self.n = 0

    def nuevo(self):
        self.n += 1
        return f"AAAAAAGmdj{self.n:08d}="


def ref(id_):
    return {"$ref": id_}


# ------------------------------------------------------------------ validacion

def validar(spec):
    errores = []
    clases = spec.get("clases", [])
    if not clases:
        errores.append("el spec no tiene ninguna clase en 'clases'")

    nombres = [c.get("nombre") for c in clases]
    for n in nombres:
        if not n:
            errores.append("hay una clase sin 'nombre'")
    repetidos = {n for n in nombres if n and nombres.count(n) > 1}
    for n in sorted(repetidos):
        errores.append(f"la clase '{n}' esta definida mas de una vez")

    conocidos = {n for n in nombres if n}
    for r in spec.get("relaciones", []):
        tipo = r.get("tipo")
        if tipo in ("agregacion", "composicion"):
            extremos = [("todo", r.get("todo")), ("parte", r.get("parte"))]
        else:
            extremos = [("origen", r.get("origen")), ("destino", r.get("destino"))]
        for campo, valor in extremos:
            if not valor:
                errores.append(f"relacion {tipo!r} sin '{campo}'")
            elif valor not in conocidos:
                errores.append(
                    f"la relacion {tipo!r} apunta a '{valor}', que no esta en 'clases'")
        if tipo not in ("herencia", "realizacion", "dependencia",
                        "asociacion", "agregacion", "composicion"):
            errores.append(f"tipo de relacion desconocido: {tipo!r}")

    paquetes = {c.get("paquete") for c in clases}
    for d in spec.get("diagramas", []):
        if not d.get("nombre"):
            errores.append("hay un diagrama sin 'nombre'")
        for n in d.get("clases", []):
            if n not in conocidos:
                errores.append(f"el diagrama {d.get('nombre')!r} incluye '{n}', "
                               "que no esta en 'clases'")
        for p in d.get("paquetes", []):
            if p not in paquetes:
                errores.append(f"el diagrama {d.get('nombre')!r} pide el paquete "
                               f"'{p}', que ninguna clase usa")
    return errores


# --------------------------------------------------------------------- modelo

def texto_atributo(a):
    s = f"{VIS.get(a.get('visibilidad', 'private'), '-')}{a['nombre']}"
    if a.get("tipo"):
        s += f": {a['tipo']}"
    if a.get("multiplicidad"):
        s += f"[{a['multiplicidad']}]"
    return s


def texto_operacion(o):
    params = ", ".join(f"{p['nombre']}: {p.get('tipo', '')}".rstrip(": ")
                       for p in o.get("parametros", []))
    return (f"{VIS.get(o.get('visibilidad', 'public'), '+')}{o['nombre']}"
            f"({params}): {o.get('retorno', 'void')}")


def ancho_clase(c):
    """Aproxima el ancho al texto mas largo; StarUML reajusta al abrir."""
    lineas = [c["nombre"], f"«{c.get('estereotipo', '')}»"]
    lineas += [texto_atributo(a) for a in c.get("atributos", [])]
    lineas += [texto_operacion(o) for o in c.get("operaciones", [])]
    lineas += list(c.get("literales", []))
    return max(ANCHO_MIN, int(max(len(l) for l in lineas) * 7.2) + 30)


def alto_clase(c):
    n_arriba = len(c.get("atributos", [])) + len(c.get("literales", []))
    if c.get("estereotipo"):
        n_arriba += 1   # la linea del estereotipo agranda el compartimento del nombre
    n_abajo = len(c.get("operaciones", []))
    # StarUML dibuja el enum con dos compartimentos vacios extra al pie.
    extra = 2 * PAD_COMPARTIMENTO if tipo_uml(c) == "UMLEnumeration" else 0
    return (ALTO_NOMBRE + extra
            + PAD_COMPARTIMENTO + ALTO_LINEA * n_arriba
            + PAD_COMPARTIMENTO + ALTO_LINEA * n_abajo)


def tipo_uml(c):
    return {"interface": "UMLInterface", "interfaz": "UMLInterface",
            "enum": "UMLEnumeration", "enumeracion": "UMLEnumeration",
            "clase": "UMLClass", None: "UMLClass"}.get(c.get("tipo"), "UMLClass")


def construir_modelo(spec, ids, id_modelo):
    """Crea los elementos del modelo (la parte semantica, sin geometria).

    Las clases con 'paquete' quedan adentro de un UMLPackage en el explorador,
    como en la resolucion del final 15 (Presentation, Application, ...).
    """
    elementos, por_nombre, paquetes = [], {}, {}

    for c in spec["clases"]:
        padre_id, contenedor = id_modelo, elementos
        if c.get("paquete"):
            pk = paquetes.get(c["paquete"])
            if pk is None:
                pk = {"_type": "UMLPackage", "_id": ids.nuevo(), "_parent": ref(id_modelo),
                      "name": c["paquete"], "ownedElements": []}
                paquetes[c["paquete"]] = pk
                elementos.append(pk)
            padre_id, contenedor = pk["_id"], pk["ownedElements"]

        cid = ids.nuevo()
        elem = {"_type": tipo_uml(c), "_id": cid, "_parent": ref(padre_id),
                "name": c["nombre"]}
        if c.get("abstracta"):
            elem["isAbstract"] = True
        if c.get("documentacion"):
            elem["documentation"] = c["documentacion"]
        if c.get("estereotipo"):
            elem["stereotype"] = c["estereotipo"]

        attrs = []
        for a in c.get("atributos", []):
            aid = ids.nuevo()
            at = {"_type": "UMLAttribute", "_id": aid, "_parent": ref(cid),
                  "name": a["nombre"], "visibility": a.get("visibilidad", "private")}
            if a.get("tipo"):
                at["type"] = a["tipo"]
            if a.get("multiplicidad"):
                at["multiplicity"] = a["multiplicidad"]
            if a.get("estatico"):
                at["isStatic"] = True
            attrs.append(at)
        if attrs:
            elem["attributes"] = attrs

        lits = []
        for l in c.get("literales", []):
            lits.append({"_type": "UMLEnumerationLiteral", "_id": ids.nuevo(),
                         "_parent": ref(cid), "name": l})
        if lits:
            elem["literals"] = lits

        ops = []
        for o in c.get("operaciones", []):
            oid = ids.nuevo()
            op = {"_type": "UMLOperation", "_id": oid, "_parent": ref(cid),
                  "name": o["nombre"], "visibility": o.get("visibilidad", "public")}
            if o.get("abstracta"):
                op["isAbstract"] = True
            if o.get("estatico"):
                op["isStatic"] = True
            params = []
            for p in o.get("parametros", []):
                pd = {"_type": "UMLParameter", "_id": ids.nuevo(), "_parent": ref(oid),
                      "name": p["nombre"]}
                if p.get("tipo"):
                    pd["type"] = p["tipo"]
                params.append(pd)
            # El tipo de retorno se modela como un parametro con direction "return".
            params.append({"_type": "UMLParameter", "_id": ids.nuevo(),
                           "_parent": ref(oid), "type": o.get("retorno", "void"),
                           "direction": "return"})
            op["parameters"] = params
            ops.append(op)
        if ops:
            elem["operations"] = ops

        contenedor.append(elem)
        por_nombre[c["nombre"]] = elem

    # Relaciones. Cada una cuelga de la clase origen, como hace StarUML.
    relaciones = []
    for r in spec.get("relaciones", []):
        tipo = r["tipo"]
        rid = ids.nuevo()

        if tipo in JERARQUICAS:
            origen = por_nombre[r["origen"]]
            destino = por_nombre[r["destino"]]
            rel = {"_type": JERARQUICAS[tipo], "_id": rid, "_parent": ref(origen["_id"]),
                   "source": ref(origen["_id"]), "target": ref(destino["_id"])}
        elif tipo == "dependencia":
            origen = por_nombre[r["origen"]]
            destino = por_nombre[r["destino"]]
            rel = {"_type": "UMLDependency", "_id": rid, "_parent": ref(origen["_id"]),
                   "source": ref(origen["_id"]), "target": ref(destino["_id"])}
            if r.get("nombre"):
                rel["name"] = r["nombre"]
        else:
            # Asociacion, agregacion y composicion comparten estructura: cambia
            # solo el campo aggregation del extremo que hace de "todo" (end2).
            if tipo in ("agregacion", "composicion"):
                a_nombre, b_nombre = r["parte"], r["todo"]
                mult_a, mult_b = r.get("mult_parte", ""), r.get("mult_todo", "")
                rol_a, rol_b = r.get("rol_parte", ""), r.get("rol_todo", "")
                agg = "shared" if tipo == "agregacion" else "composite"
            else:
                a_nombre, b_nombre = r["origen"], r["destino"]
                mult_a, mult_b = r.get("mult_origen", ""), r.get("mult_destino", "")
                rol_a, rol_b = r.get("rol_origen", ""), r.get("rol_destino", "")
                agg = None

            ca, cb = por_nombre[a_nombre], por_nombre[b_nombre]
            e1id, e2id = ids.nuevo(), ids.nuevo()
            end1 = {"_type": "UMLAssociationEnd", "_id": e1id, "_parent": ref(rid),
                    "reference": ref(ca["_id"])}
            end2 = {"_type": "UMLAssociationEnd", "_id": e2id, "_parent": ref(rid),
                    "reference": ref(cb["_id"])}
            if mult_a:
                end1["multiplicity"] = mult_a
            if mult_b:
                end2["multiplicity"] = mult_b
            if rol_a:
                end1["name"] = rol_a
            if rol_b:
                end2["name"] = rol_b
            if agg:
                end2["aggregation"] = agg
            rel = {"_type": "UMLAssociation", "_id": rid, "_parent": ref(ca["_id"]),
                   "end1": end1, "end2": end2}
            if r.get("nombre"):
                rel["name"] = r["nombre"]
            ca.setdefault("ownedElements", []).append(rel)
            relaciones.append((r, rel, ca, cb))
            continue

        origen.setdefault("ownedElements", []).append(rel)
        relaciones.append((r, rel, origen, destino))

    return elementos, por_nombre, relaciones


# --------------------------------------------------------------------- layout

def calcular_capas(spec, nombres):
    """Ubica las superclases e interfaces arriba y los hijos debajo.

    Un diagrama de clases se lee de arriba hacia abajo siguiendo la herencia,
    asi que ordenar por profundidad produce algo parecido a lo que dibujaria
    una persona, en vez de una grilla arbitraria. Solo cuentan los padres que
    estan en el mismo grupo (`nombres`).
    """
    padres = {n: set() for n in nombres}
    for r in spec.get("relaciones", []):
        if (r["tipo"] in ("herencia", "realizacion")
                and r["origen"] in padres and r["destino"] in padres):
            padres[r["origen"]].add(r["destino"])

    profundidad, visitando = {}, set()

    def calc(n):
        if n in profundidad:
            return profundidad[n]
        if n in visitando:      # herencia circular: cortamos y avisamos despues
            return 0
        visitando.add(n)
        d = 0 if not padres[n] else 1 + max(calc(p) for p in padres[n])
        visitando.discard(n)
        profundidad[n] = d
        return d

    for n in nombres:
        calc(n)

    capas = {}
    for n, d in profundidad.items():
        capas.setdefault(d, []).append(n)
    return capas


PAD_PAQUETE = 25    # margen interno del marco de un paquete
SEP_PAQUETES = 70   # espacio vertical entre paquetes


def posicionar(spec, nombres, ancho_lienzo):
    """Devuelve la geometria de cada clase y la del marco de cada paquete.

    Las clases se agrupan por paquete (en el orden en que aparecen en el spec)
    y cada grupo se acomoda por capas de herencia, uno debajo del otro.
    """
    dims = {c["nombre"]: (ancho_clase(c), alto_clase(c))
            for c in spec["clases"] if c["nombre"] in nombres}
    paquete_de = {c["nombre"]: c.get("paquete") for c in spec["clases"]}
    grupos = {}
    for c in spec["clases"]:
        if c["nombre"] in nombres:
            grupos.setdefault(paquete_de[c["nombre"]], []).append(c["nombre"])

    pos, marcos, y = {}, {}, MARGEN
    for paquete, miembros in grupos.items():
        y0 = y
        if paquete:
            y += ALTO_NOMBRE + PAD_PAQUETE
        capas = calcular_capas(spec, miembros)
        for d in sorted(capas):
            # Repartimos cada capa en filas que entren en el ancho pedido.
            fila, ancho_fila, filas = [], 0, []
            for nombre in sorted(capas[d]):
                w = dims[nombre][0]
                if fila and ancho_fila + w + SEP_X > ancho_lienzo:
                    filas.append(fila)
                    fila, ancho_fila = [], 0
                fila.append(nombre)
                ancho_fila += w + SEP_X
            if fila:
                filas.append(fila)

            for f in filas:
                ancho_total = sum(dims[n][0] for n in f) + SEP_X * (len(f) - 1)
                x = MARGEN + PAD_PAQUETE + max(0, (ancho_lienzo - ancho_total) // 2)
                alto_max = 0
                for nombre in f:
                    w, h = dims[nombre]
                    pos[nombre] = (x, y, w, h)
                    x += w + SEP_X
                    alto_max = max(alto_max, h)
                y += alto_max + SEP_Y
        if paquete:
            y += PAD_PAQUETE - SEP_Y
            xs = [pos[n][0] for n in miembros]
            x2 = max(pos[n][0] + pos[n][2] for n in miembros)
            x1 = min(min(xs), MARGEN + PAD_PAQUETE) - PAD_PAQUETE
            marcos[paquete] = (x1, y0, x2 + PAD_PAQUETE - x1, y - y0)
            y += SEP_PAQUETES
    return pos, marcos


# --------------------------------------------------------------------- vistas

def etiqueta(ids, parent_id, texto="", visible=False, x=-160, y=-240,
             fuente=FUENTE, alineacion=None):
    lv = {"_type": "LabelView", "_id": ids.nuevo(), "_parent": ref(parent_id),
          "font": fuente, "left": x, "top": y, "height": ALTO_LINEA}
    if not visible:
        lv["visible"] = False
    if texto:
        lv["text"] = texto
    if alineacion is not None:
        lv["horizontalAlignment"] = alineacion
    return lv


def vista_clase(ids, c, elem, diagrama_id, geom):
    """Una vista de clase = caja + cinco compartimentos, aunque esten vacios.

    StarUML espera los cinco (nombre, atributos, operaciones, recepciones y
    parametros de template); si falta alguno, abre el diagrama roto.
    """
    x, y, w, h = geom
    vid = ids.nuevo()
    mid = elem["_id"]

    # --- compartimento del nombre
    ncid = ids.nuevo()
    if c.get("estereotipo"):
        est = etiqueta(ids, ncid, f"«{c['estereotipo']}»", visible=True, x=x + 5,
                       y=y + 7, alineacion=2)
        est["width"] = w - 10
    else:
        est = etiqueta(ids, ncid)
    fuente_nombre = FUENTE_ITALICA if c.get("abstracta") else FUENTE_NEGRITA
    nom = etiqueta(ids, ncid, c["nombre"], visible=True, x=x + 5, y=y + 7,
                   fuente=fuente_nombre)
    nom["width"] = w - 10
    nsp = etiqueta(ids, ncid, "(from Model)")
    prp = etiqueta(ids, ncid, alineacion=1)
    nombre_comp = {
        "_type": "UMLNameCompartmentView", "_id": ncid, "_parent": ref(vid),
        "model": ref(mid), "subViews": [est, nom, nsp, prp], "font": FUENTE,
        "left": x, "top": y, "width": w, "height": ALTO_NOMBRE,
        "stereotypeLabel": ref(est["_id"]), "nameLabel": ref(nom["_id"]),
        "namespaceLabel": ref(nsp["_id"]), "propertyLabel": ref(prp["_id"]),
    }

    cursor = y + ALTO_NOMBRE

    # --- compartimento de atributos (o literales, si es un enum)
    acid = ids.nuevo()
    sub_attr, fila = [], cursor + 5
    fuentes_attr = elem.get("attributes", []) or elem.get("literals", [])
    for i, at in enumerate(fuentes_attr):
        if at["_type"] == "UMLEnumerationLiteral":
            texto = at["name"]
        else:
            texto = texto_atributo(c["atributos"][i])
        sub_attr.append({
            "_type": "UMLAttributeView", "_id": ids.nuevo(), "_parent": ref(acid),
            "model": ref(at["_id"]), "font": FUENTE, "left": x + 5, "top": fila,
            "width": w - 10, "height": ALTO_LINEA, "text": texto,
            "horizontalAlignment": 0})
        fila += ALTO_LINEA
    alto_attr = PAD_COMPARTIMENTO + ALTO_LINEA * len(sub_attr)
    attr_comp = {"_type": "UMLAttributeCompartmentView", "_id": acid, "_parent": ref(vid),
                 "model": ref(mid), "font": FUENTE, "left": x, "top": cursor,
                 "width": w, "height": alto_attr}
    if sub_attr:
        attr_comp["subViews"] = sub_attr
    cursor += alto_attr

    # --- compartimento de operaciones
    ocid = ids.nuevo()
    sub_ops, fila = [], cursor + 5
    for i, op in enumerate(elem.get("operations", [])):
        sub_ops.append({
            "_type": "UMLOperationView", "_id": ids.nuevo(), "_parent": ref(ocid),
            "model": ref(op["_id"]), "font": FUENTE, "left": x + 5, "top": fila,
            "width": w - 10, "height": ALTO_LINEA,
            "text": texto_operacion(c["operaciones"][i]), "horizontalAlignment": 0})
        fila += ALTO_LINEA
    alto_ops = PAD_COMPARTIMENTO + ALTO_LINEA * len(sub_ops)
    op_comp = {"_type": "UMLOperationCompartmentView", "_id": ocid, "_parent": ref(vid),
               "model": ref(mid), "font": FUENTE, "left": x, "top": cursor,
               "width": w, "height": alto_ops}
    if sub_ops:
        op_comp["subViews"] = sub_ops
    cursor += alto_ops

    # --- compartimentos que deben existir pero no se dibujan
    # Van con visible=false y fuera del lienzo, igual que los genera StarUML:
    # si quedaran visibles agregarian una franja vacia al pie de cada caja.
    rcid, tcid = ids.nuevo(), ids.nuevo()
    rec_comp = {"_type": "UMLReceptionCompartmentView", "_id": rcid, "_parent": ref(vid),
                "model": ref(mid), "visible": False, "font": FUENTE,
                "left": -216, "top": -184, "width": 10, "height": 10}
    tpl_comp = {"_type": "UMLTemplateParameterCompartmentView", "_id": tcid,
                "_parent": ref(vid), "model": ref(mid), "visible": False,
                "font": FUENTE, "left": -216, "top": -184, "width": 10, "height": 10}

    tipo_vista = {"UMLInterface": "UMLInterfaceView",
                  "UMLEnumeration": "UMLEnumerationView"}.get(elem["_type"],
                                                              "UMLClassView")
    return {
        "_type": tipo_vista, "_id": vid, "_parent": ref(diagrama_id),
        "model": ref(mid),
        "subViews": [nombre_comp, attr_comp, op_comp, rec_comp, tpl_comp],
        "font": FUENTE, "containerChangeable": True,
        "left": x, "top": y, "width": w, "height": max(h, cursor - y),
        "nameCompartment": ref(ncid), "attributeCompartment": ref(acid),
        "operationCompartment": ref(ocid), "receptionCompartment": ref(rcid),
        "templateParameterCompartment": ref(tcid),
        "suppressAttributes": False, "suppressOperations": False,
        "showOperationSignature": True, "showVisibility": True,
    }


def centro(geom):
    x, y, w, h = geom
    return x + w // 2, y + h // 2


def punto_borde(geom, destino):
    """Donde el segmento hacia `destino` corta el borde de la caja.

    Sin esto las lineas nacen en el centro de cada caja y se dibujan por encima
    del texto, atravesando las clases que quedan en el medio.
    """
    cx, cy = centro(geom)
    dx, dy = destino[0] - cx, destino[1] - cy
    if dx == 0 and dy == 0:
        return cx, cy
    _, _, w, h = geom
    escalas = []
    if dx:
        escalas.append((w / 2) / abs(dx))
    if dy:
        escalas.append((h / 2) / abs(dy))
    s = min(escalas)
    return int(cx + dx * s), int(cy + dy * s)


def etiqueta_edge(ids, edge_id, modelo_id, texto, alpha, distancia, posicion):
    """Etiqueta anclada a una linea.

    StarUML no las ubica por coordenadas absolutas sino en polar respecto de la
    linea: `alpha` es el angulo y `distance` la separacion. Asi la etiqueta
    sigue a la linea cuando se mueven las cajas, en vez de quedar flotando.
    """
    lv = {"_type": "EdgeLabelView", "_id": ids.nuevo(), "_parent": ref(edge_id),
          "font": FUENTE, "left": 0, "top": 0, "height": ALTO_LINEA,
          "alpha": alpha, "distance": distancia,
          "hostEdge": ref(edge_id), "edgePosition": posicion}
    if modelo_id:
        lv["model"] = ref(modelo_id)
    if texto:
        lv["text"] = texto
    else:
        lv["visible"] = False
    return lv


# Angulos y distancias con los que StarUML separa cada etiqueta de la linea.
A_MEDIO, A_CABEZA, A_COLA = 1.5707963267948966, 0.5235987755982988, -0.5235987755982988
POS_CABEZA, POS_MEDIO, POS_COLA = 0, 1, 2


def vista_relacion(ids, r, rel, diagrama_id, vistas, pos, nombre_a, nombre_b):
    vid = ids.nuevo()
    tail_v, head_v = vistas[nombre_a], vistas[nombre_b]
    ca, cb = centro(pos[nombre_a]), centro(pos[nombre_b])
    cx1, cy1 = punto_borde(pos[nombre_a], cb)
    cx2, cy2 = punto_borde(pos[nombre_b], ca)
    puntos = f"{cx1}:{cy1};{cx2}:{cy2}"

    tipo_vista = {
        "UMLGeneralization": "UMLGeneralizationView",
        "UMLInterfaceRealization": "UMLInterfaceRealizationView",
        "UMLDependency": "UMLDependencyView",
        "UMLAssociation": "UMLAssociationView",
    }[rel["_type"]]

    nom = etiqueta_edge(ids, vid, rel["_id"], r.get("nombre", ""),
                        A_MEDIO, 15, POS_MEDIO)
    est = etiqueta_edge(ids, vid, rel["_id"], "", A_MEDIO, 30, POS_MEDIO)
    prp = etiqueta_edge(ids, vid, rel["_id"], "", A_MEDIO, 45, POS_MEDIO)
    sub = [nom, est, prp]

    v = {"_type": tipo_vista, "_id": vid, "_parent": ref(diagrama_id),
         "model": ref(rel["_id"]), "subViews": sub, "font": FUENTE,
         "head": ref(head_v["_id"]), "tail": ref(tail_v["_id"]),
         "lineStyle": 1, "points": puntos, "showVisibility": True,
         "nameLabel": ref(nom["_id"]), "stereotypeLabel": ref(est["_id"]),
         "propertyLabel": ref(prp["_id"])}

    if rel["_type"] == "UMLAssociation":
        # Cada extremo lleva tres etiquetas (rol, propiedades, multiplicidad)
        # mas un compartimento de calificadores, aunque esten vacios.
        extras = {}
        for lado, extremo in (("tail", rel["end1"]), ("head", rel["end2"])):
            alpha = A_COLA if lado == "tail" else A_CABEZA
            posicion = POS_COLA if lado == "tail" else POS_CABEZA
            rol = etiqueta_edge(ids, vid, extremo["_id"], extremo.get("name", ""),
                                -alpha, 15, posicion)
            props = etiqueta_edge(ids, vid, extremo["_id"], "", -alpha, 40, posicion)
            mult = etiqueta_edge(ids, vid, extremo["_id"],
                                 extremo.get("multiplicity", ""),
                                 alpha, 25, posicion)
            qcid = ids.nuevo()
            qcomp = {"_type": "UMLQualifierCompartmentView", "_id": qcid,
                     "_parent": ref(vid), "model": ref(extremo["_id"]),
                     "visible": False, "font": FUENTE, "left": -216, "top": -184,
                     "width": 10, "height": 10}
            sub.extend([rol, props, mult, qcomp])
            extras[f"{lado}RoleNameLabel"] = ref(rol["_id"])
            extras[f"{lado}PropertyLabel"] = ref(props["_id"])
            extras[f"{lado}MultiplicityLabel"] = ref(mult["_id"])
            extras[f"{lado}QualifiersCompartment"] = ref(qcid)
        v["showEndOrder"] = "hide"
        v.update(extras)

    return v


# ------------------------------------------------------------------ ensamblado

def vista_paquete(ids, paquete_elem, diagrama_id, geom):
    """Marco de un paquete, con la misma estructura que guarda StarUML."""
    x, y, w, h = geom
    vid, ncid = ids.nuevo(), ids.nuevo()
    est = etiqueta(ids, ncid)
    nom = etiqueta(ids, ncid, paquete_elem["name"], visible=True, x=x + 5, y=y + 7,
                   fuente=FUENTE_NEGRITA)
    nom["width"] = w - 10
    nsp = etiqueta(ids, ncid, "(from Model)")
    prp = etiqueta(ids, ncid, alineacion=1)
    nombre_comp = {
        "_type": "UMLNameCompartmentView", "_id": ncid, "_parent": ref(vid),
        "model": ref(paquete_elem["_id"]), "subViews": [est, nom, nsp, prp],
        "font": FUENTE, "left": x, "top": y, "width": w, "height": ALTO_NOMBRE,
        "stereotypeLabel": ref(est["_id"]), "nameLabel": ref(nom["_id"]),
        "namespaceLabel": ref(nsp["_id"]), "propertyLabel": ref(prp["_id"]),
    }
    return {"_type": "UMLPackageView", "_id": vid, "_parent": ref(diagrama_id),
            "model": ref(paquete_elem["_id"]), "subViews": [nombre_comp],
            "containedViews": [], "font": FUENTE, "containerChangeable": True,
            "left": x, "top": y, "width": w, "height": h,
            "nameCompartment": ref(ncid)}


def diagramas_del_spec(spec):
    """El diagrama principal lleva todas las clases (o las de 'principal' si se define); 'diagramas' agrega vistas parciales."""
    todas = [c["nombre"] for c in spec["clases"]]
    paquete_de = {c["nombre"]: c.get("paquete") for c in spec["clases"]}
    salida = [(spec.get("diagrama", "Main"), list(spec.get("principal", todas)))]
    for d in spec.get("diagramas", []):
        clases = list(d.get("clases", []))
        clases += [n for n in todas
                   if paquete_de[n] in d.get("paquetes", []) and n not in clases]
        salida.append((d["nombre"], clases))
    return salida


def generar(spec, ancho_lienzo=1200):
    ids = Ids()
    id_proyecto, id_modelo = ids.nuevo(), ids.nuevo()

    elementos, por_nombre, relaciones = construir_modelo(spec, ids, id_modelo)
    paquetes = {e["name"]: e for e in elementos if e["_type"] == "UMLPackage"}
    por_spec = {c["nombre"]: c for c in spec["clases"]}

    diagramas = []
    for i, (nombre_diag, nombres) in enumerate(diagramas_del_spec(spec)):
        id_diagrama = ids.nuevo()
        incluidas = set(nombres)
        pos, marcos = posicionar(spec, incluidas, ancho_lienzo)

        # Los marcos van primero en ownedViews para quedar detras de las clases.
        vistas_paq = {p: vista_paquete(ids, paquetes[p], id_diagrama, g)
                      for p, g in marcos.items()}
        vistas, por_clase = list(vistas_paq.values()), {}
        for c in spec["clases"]:
            if c["nombre"] not in incluidas:
                continue
            v = vista_clase(ids, c, por_nombre[c["nombre"]], id_diagrama, pos[c["nombre"]])
            if c.get("paquete") in vistas_paq:
                vp = vistas_paq[c["paquete"]]
                v["containerView"] = ref(vp["_id"])
                vp["containedViews"].append(ref(v["_id"]))
            vistas.append(v)
            por_clase[c["nombre"]] = v

        for r, rel, a, b in relaciones:
            if r["tipo"] in ("agregacion", "composicion"):
                na, nb = r["parte"], r["todo"]
            else:
                na, nb = r["origen"], r["destino"]
            if na in incluidas and nb in incluidas:
                vistas.append(vista_relacion(ids, r, rel, id_diagrama, por_clase,
                                             pos, na, nb))

        diagrama = {"_type": "UMLClassDiagram", "_id": id_diagrama,
                    "_parent": ref(id_modelo), "name": nombre_diag, "ownedViews": vistas}
        if i == 0:
            diagrama["defaultDiagram"] = True
        diagramas.append(diagrama)

    modelo = {"_type": "UMLModel", "_id": id_modelo, "_parent": ref(id_proyecto),
              "name": "Model", "ownedElements": elementos + diagramas}

    return {"_type": "Project", "_id": id_proyecto,
            "name": spec.get("proyecto", "Untitled"), "ownedElements": [modelo]}


def main():
    ap = argparse.ArgumentParser(description="Genera un .mdj de StarUML desde un spec JSON")
    ap.add_argument("spec", help="archivo JSON con la descripcion del modelo")
    ap.add_argument("-o", "--salida", required=True, help="archivo .mdj a escribir")
    ap.add_argument("--ancho", type=int, default=1200,
                    help="ancho del lienzo para acomodar las clases (default 1200)")
    args = ap.parse_args()

    try:
        with open(args.spec, encoding="utf-8") as f:
            spec = json.load(f)
    except FileNotFoundError:
        print(f"No existe el spec: {args.spec}", file=sys.stderr)
        return 1
    except json.JSONDecodeError as e:
        print(f"El spec no es JSON valido: {e}", file=sys.stderr)
        return 1

    errores = validar(spec)
    if errores:
        print("El spec tiene problemas:", file=sys.stderr)
        for e in errores:
            print(f"  - {e}", file=sys.stderr)
        return 1

    proyecto = generar(spec, args.ancho)
    with open(args.salida, "w", encoding="utf-8") as f:
        json.dump(proyecto, f, indent=4, ensure_ascii=False)

    n_rel = len(spec.get("relaciones", []))
    n_diag = 1 + len(spec.get("diagramas", []))
    print(f"Generado {args.salida}: {len(spec['clases'])} clases, {n_rel} relaciones, "
          f"{n_diag} diagrama(s)")
    return 0


if __name__ == "__main__":
    sys.exit(main())
