#!/usr/bin/env python3
"""Revisa un .mdj de StarUML y reporta errores tipicos de modelado.

Busca las cosas que no se ven mirando el lienzo: relaciones duplicadas, clases
sueltas del ejemplo que quedaron olvidadas, atributos sin tipo, nombre y tipo
invertidos en un parametro, clases que existen en el modelo pero no estan
dibujadas en ningun diagrama.

Uso:
    python3 revisar_mdj.py archivo.mdj

Codigo de salida: 1 si hay hallazgos de severidad ERROR, 0 si no.
"""
import argparse
import json
import re
import sys

sys.path.insert(0, str(__import__("pathlib").Path(__file__).parent))
from leer_mdj import CLASIFICADORES, buscar, cargar, indexar  # noqa: E402

# Nombres que StarUML pone solo al crear un elemento; si sobreviven, quedo basura.
NOMBRE_POR_DEFECTO = re.compile(
    r"^(Class|Interface|Enumeration|DataType|Package|Model|Signal|Actor)\d+$")


def revisar(ruta):
    modelo = cargar(ruta)
    with open(ruta, encoding="utf-8") as f:
        crudo = json.load(f)
    indice = indexar(crudo, {})

    hallazgos = []

    def add(sev, categoria, mensaje, sugerencia=""):
        hallazgos.append({"severidad": sev, "categoria": categoria,
                          "mensaje": mensaje, "sugerencia": sugerencia})

    nombres = {c["nombre"] for c in modelo["clasificadores"]}

    # --- clases vacias o con nombre por defecto -----------------------------
    for c in modelo["clasificadores"]:
        vacia = not (c["atributos"] or c["operaciones"] or c["literales"])
        defecto = bool(NOMBRE_POR_DEFECTO.match(c["nombre"]))
        if defecto:
            add("ERROR", "nombre por defecto",
                f"'{c['nombre']}' conserva el nombre automatico de StarUML",
                "renombrala segun el dominio o borrala si sobro")
        elif vacia and c["tipo"] == "Class":
            add("AVISO", "clase vacia",
                f"'{c['nombre']}' no tiene atributos ni operaciones",
                "toda clase del dominio suele tener al menos un atributo")

    # --- atributos sin tipo -------------------------------------------------
    for c in modelo["clasificadores"]:
        for a in c["atributos"]:
            if not a["tipo"]:
                add("AVISO", "atributo sin tipo",
                    f"{c['nombre']}.{a['nombre']} no declara tipo",
                    "la catedra suele pedir el tipo en cada atributo")

    # --- nombre y tipo invertidos en parametros -----------------------------
    # Sintoma: el *nombre* del parametro coincide con una clase del modelo y su
    # tipo es una palabra comun. Pasa al tipear rapido en StarUML.
    for c in modelo["clasificadores"]:
        for o in c["operaciones"]:
            for p in o["parametros"]:
                if p["nombre"] in nombres and p["tipo"] not in nombres and p["tipo"]:
                    add("ERROR", "parametro invertido",
                        f"{c['nombre']}.{o['nombre']}({p['nombre']}: {p['tipo']}) "
                        f"tiene el nombre y el tipo al reves",
                        f"deberia ser ({p['tipo']}: {p['nombre']})")

    # --- relaciones duplicadas ---------------------------------------------
    # Dos asociaciones entre el mismo par son validas si expresan roles
    # distintos (cliente/proveedor); sin nombre ni rol que las distinga, es
    # casi seguro un redibujo encima de la otra.
    pares = {}
    for r in modelo["relaciones"]:
        if r["clase"] == "asociacion":
            pares.setdefault(tuple(sorted((r["origen"], r["destino"]))), []).append(r)
    for (a, b), rs in pares.items():
        if len(rs) < 2:
            continue
        firmas = [(r.get("nombre") or "", r.get("rol_origen") or "", r.get("rol_destino") or "")
                  for r in rs]
        if all(any(f) for f in firmas) and len(set(firmas)) == len(firmas):
            add("AVISO", "asociaciones multiples",
                f"hay {len(rs)} asociaciones entre {a} y {b} con nombres o roles distintos",
                "está bien si son roles diferentes (p. ej. cliente y proveedor)")
        else:
            add("ERROR", "asociacion duplicada",
                f"hay {len(rs)} asociaciones entre {a} y {b}",
                "dejá una sola; suele pasar al redibujar encima de otra")

    # --- autoasociaciones sin sentido declarado -----------------------------
    for r in modelo["relaciones"]:
        if r["clase"] == "asociacion" and r["origen"] == r["destino"]:
            if not (r.get("nombre") or r.get("rol_origen") or r.get("rol_destino")):
                add("ERROR", "autoasociacion vacia",
                    f"{r['origen']} esta asociada consigo misma sin nombre ni roles",
                    "casi siempre es un arrastre accidental del mouse: borrala")

    # --- asociaciones sin multiplicidad ------------------------------------
    for r in modelo["relaciones"]:
        if r["clase"] == "asociacion" and not (r["mult_origen"] or r["mult_destino"]):
            add("AVISO", "sin multiplicidad",
                f"la asociacion {r['origen']} - {r['destino']} no declara multiplicidad",
                "la catedra pide multiplicidad en ambos extremos (1, 0..*, 1..*)")

    # --- clases aisladas ----------------------------------------------------
    conectadas = set()
    for r in modelo["relaciones"]:
        conectadas.add(r["origen"])
        conectadas.add(r["destino"])
    for c in modelo["clasificadores"]:
        if c["nombre"] not in conectadas and not NOMBRE_POR_DEFECTO.match(c["nombre"]):
            add("AVISO", "clase aislada",
                f"'{c['nombre']}' no participa de ninguna relacion",
                "revisá si le falta una asociacion o si sobra")

    # --- clases del modelo que no estan dibujadas ---------------------------
    dibujadas = set()
    for v in buscar(crudo, ("UMLClassView", "UMLInterfaceView", "UMLEnumerationView")):
        ref = (v.get("model") or {}).get("$ref")
        destino = indice.get(ref) if ref else None
        if destino:
            dibujadas.add(destino.get("name"))
    for c in modelo["clasificadores"]:
        if c["nombre"] not in dibujadas:
            add("AVISO", "no dibujada",
                f"'{c['nombre']}' existe en el modelo pero no aparece en ningun diagrama",
                "arrastrala al lienzo o eliminala del explorador")

    # --- clase abstracta sin operaciones abstractas -------------------------
    for c in modelo["clasificadores"]:
        if c["abstracta"] and c["operaciones"]:
            if not any(o["abstracta"] for o in c["operaciones"]):
                add("AVISO", "abstracta sin metodo abstracto",
                    f"'{c['nombre']}' es abstracta pero ninguna operacion lo es",
                    "si nada obliga a redefinir, quizas no necesite ser abstracta")

    return modelo, hallazgos


ORDEN = {"ERROR": 0, "AVISO": 1}


def main():
    ap = argparse.ArgumentParser(description="Revisa errores de modelado en un .mdj")
    ap.add_argument("archivo")
    args = ap.parse_args()

    try:
        modelo, hallazgos = revisar(args.archivo)
    except FileNotFoundError:
        print(f"No existe el archivo: {args.archivo}", file=sys.stderr)
        return 2
    except json.JSONDecodeError as e:
        print(f"No es un .mdj valido (JSON mal formado): {e}", file=sys.stderr)
        return 2

    errores = sum(1 for h in hallazgos if h["severidad"] == "ERROR")
    avisos = len(hallazgos) - errores

    print(f"REVISION DE {args.archivo}")
    print(f"{len(modelo['clasificadores'])} clasificadores, "
          f"{len(modelo['relaciones'])} relaciones")
    print(f"{errores} errores, {avisos} avisos")
    print()

    if not hallazgos:
        print("Sin hallazgos: el modelo esta prolijo.")
        return 0

    for h in sorted(hallazgos, key=lambda x: (ORDEN[x["severidad"]], x["categoria"])):
        print(f"[{h['severidad']}] {h['categoria']}: {h['mensaje']}")
        if h["sugerencia"]:
            print(f"          -> {h['sugerencia']}")

    return 1 if errores else 0


if __name__ == "__main__":
    sys.exit(main())
