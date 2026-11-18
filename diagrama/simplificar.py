#!/usr/bin/env python3
"""Deriva spec_simplificado.json desde spec.json: version reducida SOLO del diagrama (el codigo no cambia).

Uso:  python3 diagrama/simplificar.py && python3 diagrama/generar_mdj.py --spec diagrama/spec_simplificado.json -o skillswap_simplificado.mdj

Simplificaciones:
  - sin Base/Factory de los State, enums, DTOs, comandos, repositorios, JPA, mocks ni UI
  - los estados que solo devuelven su nombre se dibujan como un unico "EstadoFinal" por jerarquia
"""
import copy
import json
import os

AQUI = os.path.dirname(os.path.abspath(__file__))
spec = json.load(open(os.path.join(AQUI, "spec.json"), encoding="utf-8"))

FINALES = {"EstadoPublicacionFinal": ("EstadoPublicacion", ["EstadoCerrada", "EstadoPublicacionCancelada"]),
           "EstadoContratacionFinal": ("EstadoContratacion", ["EstadoRechazada", "EstadoContratacionCancelada"])}
BASES = {"EstadoPublicacionBase": "EstadoPublicacion", "EstadoContratacionBase": "EstadoContratacion"}

MANTENER = """Perfil Publicacion Contratacion Billetera MovimientoCredito Calificacion Notificacion
FranjaHoraria SkillCoins CambioEstado PropuestaReprogramacion EventoContratacion
ElementoCatalogo Categoria Habilidad
EstadoPublicacion EstadoBorrador EstadoPublicada EstadoPausada
EstadoContratacion EstadoPendiente EstadoAceptada EstadoConfirmada EstadoEnCurso EstadoCompletada
PoliticaCancelacion PoliticaFlexible PoliticaEstricta
ICalculoPrecio PromocionDecorator Promocion PromocionPorcentual PromocionMontoFijo
IEstrategiaRecomendacion AfinidadPorHabilidades PopularidadReciente
IEventoObserver IPublicadorEventos PublicadorEventos NotificacionObserver ReputacionObserver
ICanalNotificacion EmailAdapter PushAdapter CanalInterno CanalNotificacionFactory
IServicioIdentidad IdentidadAdapter IContratacionService ContratacionService""".split()

por_nombre = {c["nombre"]: c for c in spec["clases"]}
clases = [copy.deepcopy(por_nombre[n]) for n in MANTENER]
for nuevo, (iface, viejos) in FINALES.items():
    clases.append({"nombre": nuevo, "paquete": "Domain", "tipo": "clase", "atributos": [],
                   "operaciones": [{"nombre": "getNombre", "retorno": "String", "parametros": []}],
                   "estereotipo": "State · Estado concreto (final)",
                   "documentacion": "Agrupa en el diagrama: " + ", ".join(v[6:] for v in viejos)
                                    + ". Solo informan su nombre; no permiten ninguna accion."})
nombres = {c["nombre"] for c in clases}

alias = {v: k for k, (_, vs) in FINALES.items() for v in vs}
vistas = set()
relaciones = []
for r in spec["relaciones"]:
    r = copy.deepcopy(r)
    for k in ("origen", "destino", "todo", "parte"):
        if k in r:
            r[k] = alias.get(r[k], r[k])
    if r["tipo"] == "herencia" and r.get("destino") in BASES:   # estado -> Base pasa a estado -> interfaz
        r["tipo"], r["destino"] = "realizacion", BASES[r["destino"]]
    extremos = [r[k] for k in ("origen", "destino", "todo", "parte") if k in r]
    if not all(e in nombres for e in extremos) or r["tipo"] == "dependencia" and r.get("origen") in BASES:
        continue
    clave = json.dumps(r, sort_keys=True)
    if clave not in vistas:
        vistas.add(clave)
        relaciones.append(r)

salida = {"proyecto": "SkillSwap (simplificado)", "diagrama": "Main", "clases": clases,
          "relaciones": relaciones,
          "diagramas": [
              {"nombre": "Patron State - Publicacion",
               "clases": ["Publicacion", "EstadoPublicacion", "EstadoBorrador", "EstadoPublicada",
                          "EstadoPausada", "EstadoPublicacionFinal"]},
              {"nombre": "Patron State - Contratacion",
               "clases": ["Contratacion", "EstadoContratacion", "EstadoPendiente", "EstadoAceptada",
                          "EstadoConfirmada", "EstadoEnCurso", "EstadoCompletada",
                          "EstadoContratacionFinal"]}]}
destino = os.path.join(AQUI, "spec_simplificado.json")
json.dump(salida, open(destino, "w", encoding="utf-8"), indent=1, ensure_ascii=False)
print(f"{destino}: {len(clases)} clases, {len(relaciones)} relaciones")
