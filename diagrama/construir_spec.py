#!/usr/bin/env python3
"""Arma spec.json (formato de crear_mdj.py) con el modelo de clases de SkillSwap.

Uso:
    python3 construir_spec.py            # escribe spec.json al lado de este archivo

Notacion compacta:
    atributo  "nombre: Tipo"
    operacion "nombre(p: Tipo, q: Tipo): Retorno"   (sufijo " {abstract}" o prefijo "static ")
"""
import json
import os
import re

clases, relaciones = [], []


def _params(texto):
    """Separa 'a: Map<K, V>, b: int' respetando los genericos."""
    partes, nivel, actual = [], 0, ""
    for ch in texto:
        if ch == "<":
            nivel += 1
        elif ch == ">":
            nivel -= 1
        if ch == "," and nivel == 0:
            partes.append(actual)
            actual = ""
        else:
            actual += ch
    if actual.strip():
        partes.append(actual)
    salida = []
    for p in partes:
        nombre, tipo = p.split(":", 1)
        salida.append({"nombre": nombre.strip(), "tipo": tipo.strip()})
    return salida


def _op(texto):
    abstracta = texto.endswith(" {abstract}")
    texto = texto.replace(" {abstract}", "")
    estatico = texto.startswith("static ")
    texto = texto.replace("static ", "", 1)
    m = re.match(r"^(\w+)\((.*)\):\s*(.+)$", texto)
    if not m:
        raise ValueError(f"operacion mal escrita: {texto}")
    op = {"nombre": m.group(1), "retorno": m.group(3).strip(),
          "parametros": _params(m.group(2))}
    if abstracta:
        op["abstracta"] = True
    if estatico:
        op["estatico"] = True
    return op


def _attr(texto):
    nombre, tipo = texto.split(":", 1)
    return {"nombre": nombre.strip(), "tipo": tipo.strip()}


def C(nombre, paquete, attrs=(), ops=(), tipo="clase", abstracta=False,
      est=None, doc=None, lits=()):
    c = {"nombre": nombre, "paquete": paquete, "tipo": tipo,
         "atributos": [_attr(a) for a in attrs], "operaciones": [_op(o) for o in ops]}
    if abstracta:
        c["abstracta"] = True
    if est:
        c["estereotipo"] = est
    if doc:
        c["documentacion"] = doc
    if lits:
        c["literales"] = list(lits)
    clases.append(c)


def I(nombre, paquete, ops, doc=None):
    C(nombre, paquete, ops=ops, tipo="interface", doc=doc)


def E(nombre, paquete, lits, doc=None):
    C(nombre, paquete, tipo="enum", lits=lits, doc=doc)


def real(origen, *destinos):
    for d in destinos:
        relaciones.append({"tipo": "realizacion", "origen": origen, "destino": d})


def her(destino, *origenes):
    for o in origenes:
        relaciones.append({"tipo": "herencia", "origen": o, "destino": destino})


def dep(origen, destino, nombre=""):
    relaciones.append({"tipo": "dependencia", "origen": origen, "destino": destino,
                       "nombre": nombre})


def aso(origen, destino, m_origen, m_destino, nombre, rol_destino=""):
    relaciones.append({"tipo": "asociacion", "origen": origen, "destino": destino,
                       "mult_origen": m_origen, "mult_destino": m_destino,
                       "nombre": nombre, "rol_destino": rol_destino})


def agr(todo, parte, m_todo, m_parte, nombre):
    relaciones.append({"tipo": "agregacion", "todo": todo, "parte": parte,
                       "mult_todo": m_todo, "mult_parte": m_parte, "nombre": nombre})


def comp(todo, parte, m_todo, m_parte, nombre):
    relaciones.append({"tipo": "composicion", "todo": todo, "parte": parte,
                       "mult_todo": m_todo, "mult_parte": m_parte, "nombre": nombre})


P, A, IN, D = "Presentation", "Application", "Infrastructure", "Domain"

# =====================================================================
# PRESENTATION: controllers delgados, conocidos por la UI via interfaces
# =====================================================================
C("ErrorDeAplicacion", P, [], [],
  doc="Extiende RuntimeException. Es lo unico que ve la UI: lleva un mensaje legible en "
      "lugar de las excepciones de dominio, de aplicacion o de JPA.")
C("EjecutorDeAcciones", P,
  ["identidad: IServicioIdentidad"],
  ["ejecutar(userId: String, accion: Supplier<T>): T",
   "ejecutar(userId: String, accion: Runnable): void"],
  est="Pure Fabrication",
  doc="Valida el userId contra el servicio de identidad, ejecuta la accion y traduce las "
      "excepciones de dominio, de aplicacion y el conflicto de concurrencia a "
      "ErrorDeAplicacion. Evita repetir try/catch en cada controller.")
C("Models", P, [],
  [],
  doc="Registros de la Mock UI Kit que la UI conoce (Perfil, Publicacion, Contratacion, "
      "Movimiento, Notificacion, FiltrosBusqueda y sus enumeraciones). Se mantienen tal "
      "como los entrego la catedra.")
C("Conversor", P, [],
  ["static id(id: String): Long", "static entero(valor: long): int",
   "static publicacion(d: PublicacionDto): Models.Publicacion",
   "static contratacion(d: ContratacionDto): Models.Contratacion",
   "static notificacion(d: NotificacionDto): Models.Notificacion",
   "static movimientos(billetera: BilleteraDto): List<Models.Movimiento>",
   "static franja(d: FranjaDto): Models.Franja",
   "static perfil(userId: String, d: PerfilPublicoDto): Models.Perfil"],
  est="Pure Fabrication",
  doc="Traduce los DTO de aplicacion a los registros de la UI y los ids String a Long.")
C("ModalidadUi", P, [], ["static aDominio(modalidad: Models.Modalidad): Modalidad"],
  doc="HIBRIDA existe en la UI pero no en el dominio (consigna 4.2): se rechaza con un "
      "error legible.")

CONTROLLERS = {
    "Busqueda": (["buscar(filtros: Models.FiltrosBusqueda): List<Models.Publicacion>",
                  "recomendar(userId: String): List<Models.Publicacion>",
                  "categorias(): List<String>"],
                 ["busqueda: IBusquedaService", "recomendacion: IRecomendacionService"]),
    "Publicacion": (["listarPropias(userId: String): List<Models.Publicacion>",
                     "crearBorrador(userId: String, titulo: String, descripcion: String, categoria: String, modalidad: Models.Modalidad, duracion: int, precio: int, politicaId: String): Models.Publicacion",
                     "cambiarEstado(userId: String, publicacionId: String, estado: Models.EstadoPublicacion): Models.Publicacion",
                     "editar(userId: String, publicacionId: String, titulo: String, descripcion: String, precio: int): Models.Publicacion",
                     "agregarPromocion(userId: String, publicacionId: String, tipo: String, valor: int, diasVigencia: int): void",
                     "listarPoliticas(): List<Models.Politica>"],
                    ["publicaciones: IPublicacionService", "busqueda: IBusquedaService",
                     "reloj: Clock"]),
    "Contratacion": (["listarContrataciones(userId: String): List<Models.Contratacion>",
                      "solicitar(userId: String, publicacionId: String, objetivo: String, franjaId: String): Models.Contratacion",
                      "franjasDePublicacion(publicacionId: String): List<Models.Franja>",
                      "avanzar(userId: String, contratacionId: String): Models.Contratacion",
                      "rechazar(userId: String, contratacionId: String, motivo: String): Models.Contratacion",
                      "franjasParaReprogramar(userId: String, contratacionId: String): List<Models.Franja>",
                      "proponerReprogramacion(userId: String, contratacionId: String, franjaId: String): Models.Contratacion",
                      "aceptarReprogramacion(userId: String, contratacionId: String): Models.Contratacion",
                      "cancelar(userId: String, contratacionId: String): Models.Contratacion",
                      "calificar(userId: String, contratacionId: String, puntaje: int, comentario: String): void",
                      "comprobante(userId: String, contratacionId: String): String"],
                     ["contrataciones: IContratacionService", "calificaciones: ICalificacionService",
                      "publicaciones: IPublicacionService"]),
    "Wallet": (["consultarSaldo(userId: String): int",
                "listarMovimientos(userId: String): List<Models.Movimiento>",
                "acreditarDemo(userId: String, importe: int): int"],
               ["billetera: IBilleteraService"]),
    "Notificacion": (["listarNotificaciones(userId: String): List<Models.Notificacion>",
                      "marcarLeida(userId: String, notificacionId: String): void",
                      "marcarTodasLeidas(userId: String): void"],
                     ["notificaciones: INotificacionService"]),
    "Perfil": (["verPerfil(userId: String): Models.Perfil",
                "verPerfilDe(perfilId: String): Models.Perfil",
                "actualizar(userId: String, nombre: String, biografia: String): Models.Perfil"],
               ["perfiles: IPerfilService"]),
}
for nombre, (ops, servicios) in CONTROLLERS.items():
    I(f"{nombre}Controller", P, ops,
      doc=f"Contrato de la Mock UI Kit para el modulo {nombre}: la UI solo conoce esta interfaz.")
    C(f"{nombre}ControllerImpl", P, servicios + ["ejecutor: EjecutorDeAcciones"], ops,
      est="GRASP Controller",
      doc="Controller delgado: delega en los casos de uso a traves de EjecutorDeAcciones y "
          "convierte el resultado a los registros de la UI. Sin SQL ni reglas de negocio.")
    real(f"{nombre}ControllerImpl", f"{nombre}Controller")
    for a in servicios:
        tipo = a.split(":")[1].strip()
        if tipo.startswith("I"):
            aso(f"{nombre}ControllerImpl", tipo, "0..*", "1", "usa")
    aso(f"{nombre}ControllerImpl", "EjecutorDeAcciones", "0..*", "1", "delega errores en")
    dep(f"{nombre}ControllerImpl", "Conversor", "convierte con")
aso("EjecutorDeAcciones", "IServicioIdentidad", "1", "1", "valida usuario con")
dep(f"PublicacionControllerImpl", "ModalidadUi", "traduce modalidad con")

I("UiControllers", P,
  ["busqueda(): BusquedaController", "publicaciones(): PublicacionController",
   "contrataciones(): ContratacionController", "wallet(): WalletController",
   "notificaciones(): NotificacionController", "perfiles(): PerfilController"],
  doc="Punto unico que conoce la UI: devuelve los seis controllers por interfaz.")
C("SkillSwapControllers", P,
  ["busqueda: BusquedaController", "publicaciones: PublicacionController",
   "contrataciones: ContratacionController", "wallet: WalletController",
   "notificaciones: NotificacionController", "perfiles: PerfilController"],
  ["busqueda(): BusquedaController", "publicaciones(): PublicacionController",
   "contrataciones(): ContratacionController", "wallet(): WalletController",
   "notificaciones(): NotificacionController", "perfiles(): PerfilController"],
  doc="Agrupa los controllers reales. Es la implementacion de UiControllers que arma "
      "ConfiguracionApp.")
C("MockSkillSwapControllers", P, [], [],
  doc="Implementacion de UiControllers con datos en memoria que trae el kit; se usa sin --real.")
C("SkillSwapView", P, ["controllers: UiControllers"], [],
  doc="Ventana Swing del kit. Solo conoce UiControllers, nunca repositorios ni entidades.")
real("SkillSwapControllers", "UiControllers")
real("MockSkillSwapControllers", "UiControllers")
for nombre in CONTROLLERS:
    aso("SkillSwapControllers", f"{nombre}Controller", "1", "1", f"agrupa {nombre}")
aso("SkillSwapView", "UiControllers", "0..*", "1", "invoca")

# =====================================================================
# APPLICATION: casos de uso, puertos de entrada/salida, DTOs y observers
# =====================================================================
# ---- Puertos de entrada (casos de uso) y sus implementaciones
SERVICIOS = {
    "Perfil": (["crearPerfil(userId: String, cmd: PerfilCommand): PerfilPublicoDto",
                "actualizarPerfil(userId: String, cmd: PerfilCommand): void",
                "verPerfil(perfilId: Long): PerfilPublicoDto",
                "verPerfilDeUsuario(userId: String): PerfilPublicoDto",
                "darDeBaja(userId: String): void",
                "actualizarBasico(userId: String, nombreVisible: String, biografia: String): void"],
               ["perfilRepository: IPerfilRepository", "publicacionRepository: IPublicacionRepository",
                "contratacionRepository: IContratacionRepository", "catalogoRepository: ICatalogoRepository",
                "billeteraRepository: IBilleteraRepository", "transacciones: IGestorTransacciones"]),
    "Publicacion": (["crearBorrador(userId: String, cmd: PublicacionCommand): Long",
                     "editar(userId: String, publicacionId: Long, cmd: PublicacionCommand): void",
                     "cambiarEstado(userId: String, cmd: CambiarEstadoCommand): void",
                     "listarPropias(userId: String): List<PublicacionDto>",
                     "listarFranjasLibres(publicacionId: Long): List<FranjaDto>",
                     "listarPoliticas(): List<PoliticaDto>",
                     "agregarPromocion(userId: String, cmd: PromocionCommand): void"],
                    ["publicacionRepository: IPublicacionRepository", "perfilRepository: IPerfilRepository",
                     "catalogoRepository: ICatalogoRepository",
                     "politicaRepository: IPoliticaCancelacionRepository",
                     "transacciones: IGestorTransacciones", "reloj: Clock"]),
    "Busqueda": (["buscar(filtro: FiltroBusqueda): Pagina<PublicacionDto>",
                  "listarCategorias(): List<CategoriaDto>"],
                 ["publicacionRepository: IPublicacionRepository", "catalogoRepository: ICatalogoRepository"]),
    "Recomendacion": (["recomendar(userId: String, pagina: int, tamanio: int): Pagina<PublicacionDto>"],
                      ["estrategia: IEstrategiaRecomendacion", "perfilRepository: IPerfilRepository",
                       "publicacionRepository: IPublicacionRepository"]),
    "Contratacion": (["solicitar(userId: String, cmd: SolicitarCommand): Long",
                      "aceptar(userId: String, contratacionId: Long): void",
                      "rechazar(userId: String, contratacionId: Long, motivo: String): void",
                      "pagar(userId: String, contratacionId: Long): void",
                      "proponerReprogramacion(userId: String, contratacionId: Long, franjaId: Long): void",
                      "aceptarReprogramacion(userId: String, contratacionId: Long): void",
                      "iniciar(userId: String, contratacionId: Long): void",
                      "completar(userId: String, contratacionId: Long): void",
                      "cancelar(userId: String, contratacionId: Long, motivo: String): long",
                      "listar(userId: String, pagina: int, tamanio: int): Pagina<ContratacionDto>",
                      "verComprobante(userId: String, contratacionId: Long): ContratacionDto",
                      "franjasLibres(userId: String, contratacionId: Long): List<FranjaDto>"],
                     ["contratacionRepository: IContratacionRepository",
                      "publicacionRepository: IPublicacionRepository", "perfilRepository: IPerfilRepository",
                      "billeteraRepository: IBilleteraRepository", "transacciones: IGestorTransacciones",
                      "eventos: IPublicadorEventos", "reloj: Clock"]),
    "Calificacion": (["calificar(userId: String, cmd: CalificarCommand): void"],
                     ["contratacionRepository: IContratacionRepository",
                      "calificacionRepository: ICalificacionRepository",
                      "transacciones: IGestorTransacciones", "eventos: IPublicadorEventos"]),
    "Notificacion": (["listar(userId: String, pagina: int, tamanio: int): Pagina<NotificacionDto>",
                      "marcarLeida(userId: String, notificacionId: Long): void"],
                     ["notificacionRepository: INotificacionRepository"]),
    "Billetera": (["consultar(userId: String): BilleteraDto",
                  "acreditarDemo(userId: String, importe: long): BilleteraDto"],
                  ["billeteraRepository: IBilleteraRepository"]),
}
for nombre, (ops, attrs) in SERVICIOS.items():
    I(f"I{nombre}Service", A, ops, doc=f"Puerto de entrada: casos de uso de {nombre}.")
    C(f"{nombre}Service", A, attrs, ops, est="Pure Fabrication",
      doc="Caso de uso: busca en los puertos, delega las reglas en el dominio y "
          "define el limite transaccional.")
    real(f"{nombre}Service", f"I{nombre}Service")
    for a in attrs:
        tipo = a.split(":")[1].strip()
        if tipo.startswith("I"):
            aso(f"{nombre}Service", tipo, "0..*", "1",
                {"IGestorTransacciones": "ejecuta en", "IPublicadorEventos": "publica en",
                 "IEstrategiaRecomendacion": "utiliza"}.get(tipo, "accede a"))

# ---- Comandos, filtros y DTOs (entrada/salida de los casos de uso)
C("PerfilCommand", A, ["tipo: TipoPerfil", "nombreVisible: String", "biografia: String",
                       "zonaHoraria: String", "habilidadIds: List<Long>", "interesIds: List<Long>",
                       "disponibilidad: List<RangoHorario>", "canales: Set<CanalNotificacion>"])
C("PublicacionCommand", A, ["tipo: TipoPublicacion", "titulo: String", "descripcion: String",
                            "categoriaId: Long", "habilidadIds: List<Long>", "modalidad: Modalidad",
                            "duracionMinutos: int", "cupo: int", "precioBase: long",
                            "etiquetas: Set<String>", "inicioFranjas: List<LocalDateTime>",
                            "politicaCancelacionId: Long"])
C("CambiarEstadoCommand", A, ["publicacionId: Long", "accion: AccionPublicacion", "motivo: String"])
C("AccionPublicacion", A, [], ["ejecutar(publicacion: Publicacion, userId: String, motivo: String, ahora: LocalDateTime): void {abstract}"],
  tipo="enum", lits=["PUBLICAR", "PAUSAR", "REANUDAR", "CERRAR", "CANCELAR"],
  doc="Cada literal redefine ejecutar(publicacion, userId, motivo) y llama al metodo de "
      "dominio correspondiente: no hay switch en el caso de uso.")
C("SolicitarCommand", A, ["publicacionId: Long", "franjaId: Long", "mensaje: String",
                          "objetivo: String"])
C("CalificarCommand", A, ["contratacionId: Long", "puntaje: int", "comentario: String"])
C("PromocionCommand", A, ["publicacionId: Long", "tipo: Tipo", "valor: int", "codigo: String",
                          "diasVigencia: int"],
  doc="Tipo es un enum anidado: PORCENTUAL o MONTO_FIJO. Vale desde hoy durante diasVigencia dias.")
C("FranjaDto", A, ["id: Long", "inicio: LocalDateTime"],
  doc="Franja libre de una publicacion, para elegir al solicitar o reprogramar.")
C("PoliticaDto", A, ["id: Long", "nombre: String"],
  doc="Politica de cancelacion disponible al crear una publicacion.")
C("FiltroBusqueda", A, ["texto: String", "categoriaId: Long", "modalidad: Modalidad",
                        "precioMinimo: Long", "precioMaximo: Long", "habilidadId: Long",
                        "reputacionMinima: Double", "orden: CriterioOrden", "pagina: int",
                        "tamanio: int"])
E("CriterioOrden", A, ["RELEVANCIA", "MEJOR_REPUTACION", "MENOR_PRECIO", "MAS_RECIENTE"],
  doc="Se traduce a ORDER BY en la consulta del repositorio (lado query, CQRS).")
C("Pagina<T>", A, ["contenido: List<T>", "numero: int", "tamanio: int", "totalElementos: long"],
  ["static de(todos: List<T>, numero: int, tamanio: int): Pagina<T>", "totalPaginas(): int",
   "map(conversion: Function<T, R>): Pagina<R>"])
C("PublicacionDto", A, ["id: Long", "titulo: String", "categoria: String", "modalidad: String",
                        "precio: long", "reputacionAutor: double", "estado: String",
                        "descripcion: String", "duracionMinutos: int", "autor: String",
                        "etiquetas: List<String>", "autorId: Long"],
  ["static desde(p: Publicacion): PublicacionDto"])
C("CategoriaDto", A, ["id: Long", "nombre: String"],
  ["static desde(c: Categoria): CategoriaDto"])
C("PerfilPublicoDto", A, ["id: Long", "tipo: String", "nombreVisible: String", "biografia: String",
                          "reputacion: double", "cantidadCalificaciones: int",
                          "habilidades: List<String>", "publicacionesActivas: List<PublicacionDto>",
                          "historialResumido: List<String>"])
C("ContratacionDto", A, ["id: Long", "publicacion: String", "estado: String", "precio: long",
                         "inicio: LocalDateTime", "detallePrecio: String", "contraparte: String",
                         "rol: String", "propuestaFecha: LocalDateTime",
                         "puedeAceptarPropuesta: boolean", "CLIENTE: String", "PROVEEDOR: String"],
  ["static desde(c: Contratacion, userId: String): ContratacionDto"])
C("NotificacionDto", A, ["id: Long", "tipo: String", "mensaje: String", "fecha: LocalDateTime",
                         "leida: boolean"],
  ["static desde(n: Notificacion): NotificacionDto"])
C("BilleteraDto", A, ["saldo: long", "movimientos: List<MovimientoDto>"],
  ["static desde(b: Billetera): BilleteraDto"])
C("MovimientoDto", A, ["fecha: LocalDateTime", "tipo: String", "monto: long", "concepto: String"],
  ["static desde(m: MovimientoCredito): MovimientoDto"])
comp("BilleteraDto", "MovimientoDto", "1", "0..*", "detalla")
comp("PerfilPublicoDto", "PublicacionDto", "1", "0..*", "muestra")
comp("Pagina<T>", "PublicacionDto", "0..1", "0..*", "contiene")
aso("FiltroBusqueda", "CriterioOrden", "0..*", "1", "ordena por")
aso("CambiarEstadoCommand", "AccionPublicacion", "0..*", "1", "indica")
dep("AccionPublicacion", "Publicacion", "invoca")

# ---- Puertos de salida: repositorios
REPOS = {
    "Perfil": ["guardar(perfil: Perfil): Perfil", "buscarPorId(id: Long): Optional<Perfil>",
               "buscarPorUserId(userId: String): Optional<Perfil>"],
    "Publicacion": ["guardar(publicacion: Publicacion): Publicacion",
                    "buscarPorId(id: Long): Optional<Publicacion>",
                    "buscar(filtro: FiltroBusqueda, categoriaIds: List<Long>): Pagina<Publicacion>",
                    "listarActivasDe(autorId: Long): List<Publicacion>",
                    "listarDeAutor(autorId: Long): List<Publicacion>",
                    "listarPublicadas(): List<Publicacion>"],
    "Contratacion": ["guardar(contratacion: Contratacion): Contratacion",
                     "buscarPorId(id: Long): Optional<Contratacion>",
                     "listarPorParticipante(userId: String, pagina: int, tamanio: int): Pagina<Contratacion>",
                     "contarRecientesPorPublicacion(desde: LocalDateTime): Map<Long, Long>"],
    "Billetera": ["guardar(billetera: Billetera): Billetera",
                  "buscarPorUserId(userId: String): Optional<Billetera>"],
    "Calificacion": ["guardar(calificacion: Calificacion): Calificacion",
                     "listarRecibidas(perfilId: Long): List<Calificacion>"],
    "Notificacion": ["guardar(notificacion: Notificacion): Notificacion",
                     "buscarPorId(id: Long): Optional<Notificacion>",
                     "listarPorDestinatario(userId: String, pagina: int, tamanio: int): Pagina<Notificacion>"],
    "Catalogo": ["buscarCategoria(id: Long): Optional<Categoria>",
                 "buscarHabilidades(ids: List<Long>): List<Habilidad>",
                 "listarCategorias(): List<Categoria>"],
    "PoliticaCancelacion": ["buscarPorId(id: Long): Optional<PoliticaCancelacion>",
                            "listar(): List<PoliticaCancelacion>"],
}
for nombre, ops in REPOS.items():
    I(f"I{nombre}Repository", A, ops,
      doc="Puerto de salida de persistencia (Protected Variations: cambiar JPA no toca "
          "los casos de uso).")

# ---- Otros puertos de salida
I("IGestorTransacciones", A, ["ejecutar(operacion: Supplier<T>): T"],
  doc="Limite transaccional de aceptar, pagar, cancelar y calificar. Commit o rollback "
      "de todo lo que hace la operacion.")
I("IServicioIdentidad", A, ["validarUsuario(userId: String): boolean",
                            "obtenerEmail(userId: String): String"],
  doc="Target del Adapter de identidad externa.")
I("ICanalNotificacion", A, ["enviar(notificacion: Notificacion): void"],
  doc="Target de los adapters de email y push, y contrato del canal interno.")
I("ICanalNotificacionFactory", A, ["crear(canal: CanalNotificacion): ICanalNotificacion"],
  doc="Puerto de la Simple Factory de canales: Application no conoce los adapters concretos.")

# ---- Observer
I("IPublicadorEventos", A, ["suscribir(observer: IEventoObserver): void",
                            "desuscribir(observer: IEventoObserver): void",
                            "publicar(evento: EventoContratacion): void"],
  doc="Observer: rol Sujeto (Observable).")
I("IEventoObserver", A, ["actualizar(evento: EventoContratacion): void"],
  doc="Observer: rol Observador.")
C("PublicadorEventos", A, ["observers: List<IEventoObserver>"],
  ["suscribir(observer: IEventoObserver): void", "desuscribir(observer: IEventoObserver): void",
   "publicar(evento: EventoContratacion): void"], est="Observer · Sujeto concreto")
C("NotificacionObserver", A, ["perfilRepository: IPerfilRepository",
                              "canalFactory: ICanalNotificacionFactory"],
  ["actualizar(evento: EventoContratacion): void"], est="Observer · Observador concreto",
  doc="Por cada destinatario del evento arma la Notificacion y la envia por cada canal "
      "que el usuario eligio (email, push y/o interna).")
C("ReputacionObserver", A, ["perfilRepository: IPerfilRepository",
                            "calificacionRepository: ICalificacionRepository"],
  ["actualizar(evento: EventoContratacion): void"], est="Observer · Observador concreto",
  doc="Ante CALIFICACION_REGISTRADA recalcula la reputacion del calificado.")
real("PublicadorEventos", "IPublicadorEventos")
real("NotificacionObserver", "IEventoObserver")
real("ReputacionObserver", "IEventoObserver")
agr("PublicadorEventos", "IEventoObserver", "1", "0..*", "notifica a")
aso("NotificacionObserver", "IPerfilRepository", "0..*", "1", "consulta preferencias en")
aso("NotificacionObserver", "ICanalNotificacionFactory", "0..*", "1", "obtiene canales de")
dep("NotificacionObserver", "ICanalNotificacion", "envia por")
aso("ReputacionObserver", "IPerfilRepository", "0..*", "1", "actualiza")
aso("ReputacionObserver", "ICalificacionRepository", "0..*", "1", "lee calificaciones de")
dep("ICanalNotificacionFactory", "ICanalNotificacion", "crea")

# ---- Strategy de recomendacion
I("IEstrategiaRecomendacion", A,
  ["recomendar(perfil: Perfil, candidatas: List<Publicacion>): List<Publicacion>"],
  doc="Strategy: rol Estrategia. Se elige en el composition root; el caso de uso no cambia.")
C("AfinidadPorHabilidades", A, [],
  ["recomendar(perfil: Perfil, candidatas: List<Publicacion>): List<Publicacion>"],
  est="Strategy · Estrategia concreta",
  doc="Puntua cada publicacion por coincidencia entre sus habilidades y los intereses y "
      "habilidades del perfil.")
C("PopularidadReciente", A, ["diasVentana: int", "contratacionRepository: IContratacionRepository",
                             "reloj: Clock"],
  ["recomendar(perfil: Perfil, candidatas: List<Publicacion>): List<Publicacion>"],
  est="Strategy · Estrategia concreta",
  doc="Ordena por cantidad de contrataciones en los ultimos diasVentana dias (default 30).")
real("AfinidadPorHabilidades", "IEstrategiaRecomendacion")
real("PopularidadReciente", "IEstrategiaRecomendacion")
aso("PopularidadReciente", "IContratacionRepository", "0..*", "1", "cuenta contrataciones en")

# ---- Excepciones de aplicacion
C("RecursoNoEncontradoException", A, [], ["getMensaje(): String"],
  doc="Extiende RuntimeException. La traduce EjecutorDeAcciones.")
C("AccesoDenegadoException", A, [], ["getMensaje(): String"],
  doc="Extiende RuntimeException: el userId no existe o no es parte de la operacion.")

C("ConflictoConcurrenciaException", A, [],
  [],
  doc="Extiende RuntimeException. La lanza JpaGestorTransacciones cuando @Version detecta "
      "que otra operacion modifico el mismo dato (por ejemplo el ultimo cupo); la "
      "operacion se deshizo completa y se puede reintentar.")

# =====================================================================
# INFRASTRUCTURE: JPA, adapters de proveedores externos (mock) y factory
# =====================================================================
C("JpaGestorTransacciones", IN, ["emf: EntityManagerFactory",
                                 "actual: ThreadLocal<EntityManager>"],
  ["ejecutar(operacion: Supplier<T>): T", "entityManager(): EntityManager"],
  doc="Abre el EntityManager y la transaccion; commit al terminar, rollback ante "
      "excepcion. Traduce OptimisticLockException a ConflictoConcurrenciaException.")
dep("JpaGestorTransacciones", "ConflictoConcurrenciaException", "lanza")
real("JpaGestorTransacciones", "IGestorTransacciones")
for nombre, ops in REPOS.items():
    C(f"{nombre}JpaRepository", IN, ["gestor: JpaGestorTransacciones"], ops,
      doc="Implementacion JPA del puerto. Consultas con JOIN FETCH o proyecciones para "
          "evitar N+1.")
    real(f"{nombre}JpaRepository", f"I{nombre}Repository")
    aso(f"{nombre}JpaRepository", "JpaGestorTransacciones", "0..*", "1", "usa EntityManager de")

C("PersistenceFactory", IN, ["UNIDAD: String", "URL_MYSQL: String", "USUARIO_MYSQL: String",
                             "CLAVE_MYSQL: String"],
  ["static crear(): EntityManagerFactory",
   "static crear(url: String, usuario: String, clave: String): EntityManagerFactory",
   "static migrar(url: String, usuario: String, clave: String): void",
   "static abrir(url: String, usuario: String, clave: String): EntityManagerFactory"],
  doc="Corre las migraciones de Flyway (V1 esquema, V2 datos de demostracion) y abre la "
      "unidad de persistencia, que solo valida el esquema. Por defecto usa el MySQL de "
      "docker-compose.yml; SKILLSWAP_DB_URL, SKILLSWAP_DB_USER y SKILLSWAP_DB_PASSWORD lo cambian.")
C("ConfiguracionApp", IN, [],
  ["static crear(p: Puertos): UiControllers",
   "static crear(p: Puertos, estrategia: IEstrategiaRecomendacion): UiControllers"],
  est="Composition Root",
  doc="Unico lugar que conoce las clases concretas de servicios, observers y controllers "
      "(sin Spring). Recibe los puertos de salida ya construidos: JPA en la aplicacion, "
      "memoria en las pruebas.")
C("SkillSwapApplication", IN, [], ["static main(args: String[]): void"],
  doc="Punto de entrada. Sin parametros usa MockSkillSwapControllers; con --real arma los "
      "repositorios JPA sobre MySQL y cierra el EntityManagerFactory al salir.")
dep("ConfiguracionApp", "SkillSwapControllers", "crea")
dep("ConfiguracionApp", "PublicadorEventos", "arma")
dep("SkillSwapApplication", "ConfiguracionApp", "usa")
dep("SkillSwapApplication", "PersistenceFactory", "abre la base con")
dep("SkillSwapApplication", "SkillSwapView", "muestra")

# ---- Adapter: identidad externa
C("IdentidadAdapter", IN, ["cliente: IdentityClientMock"],
  ["validarUsuario(userId: String): boolean", "obtenerEmail(userId: String): String"],
  est="Adapter · Adapter")
C("IdentityClientMock", IN, [], ["fetchUser(userId: String): Map<String, String>"],
  est="Adapter · Adaptee", doc="Simula el SDK del servicio de identidad. No se modifica.")
real("IdentidadAdapter", "IServicioIdentidad")
aso("IdentidadAdapter", "IdentityClientMock", "0..*", "1", "traduce a")

# ---- Adapter: canales de notificacion
C("EmailAdapter", IN, ["cliente: EmailClientMock", "identidad: IServicioIdentidad"],
  ["enviar(notificacion: Notificacion): void"], est="Adapter · Adapter")
C("EmailClientMock", IN, [], ["sendMail(to: String, subject: String, body: String): void"],
  est="Adapter · Adaptee")
C("PushAdapter", IN, ["cliente: PushClientMock"],
  ["enviar(notificacion: Notificacion): void"], est="Adapter · Adapter")
C("PushClientMock", IN, [], ["push(deviceId: String, payload: String): int"],
  est="Adapter · Adaptee")
C("CanalInterno", IN, ["notificacionRepository: INotificacionRepository"],
  ["enviar(notificacion: Notificacion): void"],
  doc="No adapta a nadie: guarda la Notificacion en la bandeja interna.")
real("EmailAdapter", "ICanalNotificacion")
real("PushAdapter", "ICanalNotificacion")
real("CanalInterno", "ICanalNotificacion")
aso("EmailAdapter", "EmailClientMock", "0..*", "1", "traduce a")
aso("EmailAdapter", "IServicioIdentidad", "0..*", "1", "obtiene email de")
aso("PushAdapter", "PushClientMock", "0..*", "1", "traduce a")
aso("CanalInterno", "INotificacionRepository", "0..*", "1", "guarda en")

# ---- Simple Factory de canales
C("CanalNotificacionFactory", IN, ["emailClient: EmailClientMock", "pushClient: PushClientMock",
                                   "identidad: IServicioIdentidad",
                                   "notificacionRepository: INotificacionRepository"],
  ["crear(canal: CanalNotificacion): ICanalNotificacion"], est="Simple Factory",
  doc="Un unico switch sobre CanalNotificacion: agregar un canal toca solo esta clase "
      "y el adapter nuevo.")
real("CanalNotificacionFactory", "ICanalNotificacionFactory")
dep("CanalNotificacionFactory", "EmailAdapter", "crea")
dep("CanalNotificacionFactory", "PushAdapter", "crea")
dep("CanalNotificacionFactory", "CanalInterno", "crea")

# =====================================================================
# DOMAIN: entidades, value objects, reglas, politicas y eventos
# =====================================================================
E("TipoPerfil", D, ["PROFESIONAL", "ORGANIZACION"])
E("TipoPublicacion", D, ["MENTORIA", "SERVICIO", "DESAFIO"])
E("Modalidad", D, ["REMOTA", "PRESENCIAL"])
E("CanalNotificacion", D, ["EMAIL", "PUSH", "INTERNA"])
E("TipoMovimiento", D, ["DEBITO", "CREDITO", "REINTEGRO"])
E("TipoEvento", D, ["SOLICITUD_RECIBIDA", "SOLICITUD_ACEPTADA", "PAGO_CONFIRMADO",
                    "REPROGRAMACION", "CANCELACION", "SESION_COMPLETADA",
                    "CALIFICACION_REGISTRADA"])

C("SkillCoins", D, ["cantidad: long", "CERO: SkillCoins"],
  ["sumar(otro: SkillCoins): SkillCoins", "restar(otro: SkillCoins): SkillCoins",
   "restarHastaCero(otro: SkillCoins): SkillCoins", "porcentaje(valor: int): SkillCoins",
   "esMenorQue(otro: SkillCoins): boolean"],
  est="Value Object",
  doc="Inmutable y nunca negativo: restar lanza ReglaNegocioException si el resultado "
      "seria negativo.")
C("RangoHorario", D, ["dia: DayOfWeek", "desde: LocalTime", "hasta: LocalTime"],
  ["incluye(momento: LocalDateTime): boolean"], est="Value Object")
C("CambioEstado", D, ["fecha: LocalDateTime", "actorId: String", "estadoAnterior: String",
                      "estadoNuevo: String", "motivo: String"], [],
  est="Value Object", doc="Registro inmutable del historial: solo se agrega, nunca se edita.")
C("ReglaNegocioException", D, [], ["getMensaje(): String"],
  doc="Extiende RuntimeException. La lanza el dominio ante transiciones invalidas, saldo "
      "insuficiente, cupo agotado, etc.")

# ---- Perfil
C("Perfil", D, ["id: Long", "userId: String", "tipo: TipoPerfil", "nombreVisible: String",
                "biografia: String", "zonaHoraria: ZoneId", "habilidades: List<Habilidad>",
                "intereses: List<Habilidad>", "disponibilidad: List<RangoHorario>",
                "canales: Set<CanalNotificacion>", "reputacion: double",
                "cantidadCalificaciones: int", "activo: boolean", "version: long"],
  ["actualizarDatos(nombreVisible: String, biografia: String, zonaHoraria: ZoneId): void",
   "definirHabilidades(habilidades: List<Habilidad>, intereses: List<Habilidad>): void",
   "definirDisponibilidad(rangos: List<RangoHorario>): void",
   "elegirCanales(canales: Set<CanalNotificacion>): void",
   "recalcularReputacion(calificaciones: List<Calificacion>): void",
   "puedePublicar(tipo: TipoPublicacion): boolean", "darDeBaja(): void"],
  est="Information Expert",
  doc="Referencia el userId del servicio de identidad. La reputacion no tiene setter: solo "
      "se recalcula desde calificaciones verificadas. Baja logica (activo=false).")
aso("Perfil", "TipoPerfil", "0..*", "1", "es de tipo")
agr("Perfil", "Habilidad", "0..*", "0..*", "domina")
aso("Perfil", "Habilidad", "0..*", "0..*", "le interesa", "intereses")
comp("Perfil", "RangoHorario", "1", "0..*", "disponible en")
aso("Perfil", "CanalNotificacion", "0..*", "1..*", "prefiere")
dep("Perfil", "Calificacion", "recalcula con")

# ---- Composite: catalogo de categorias y habilidades
C("ElementoCatalogo", D, ["id: Long", "nombre: String"],
  ["getNombre(): String", "contiene(elemento: ElementoCatalogo): boolean {abstract}",
   "listarHabilidades(): List<Habilidad> {abstract}"],
  abstracta=True, est="Composite · Componente")
C("Categoria", D, ["hijos: List<ElementoCatalogo>"],
  ["agregar(elemento: ElementoCatalogo): void", "quitar(elemento: ElementoCatalogo): void",
   "contiene(elemento: ElementoCatalogo): boolean", "listarHabilidades(): List<Habilidad>",
   "listarSubcategoriaIds(): List<Long>"],
  est="Composite · Compuesto")
C("Habilidad", D, [],
  ["contiene(elemento: ElementoCatalogo): boolean", "listarHabilidades(): List<Habilidad>"],
  est="Composite · Hoja")
her("ElementoCatalogo", "Categoria", "Habilidad")
agr("Categoria", "ElementoCatalogo", "0..1", "0..*", "agrupa")

# ---- Publicacion (contexto de State, componente concreto del Decorator)
C("Publicacion", D, ["id: Long", "autor: Perfil", "tipo: TipoPublicacion", "titulo: String",
                     "descripcion: String", "categoria: Categoria", "habilidades: List<Habilidad>",
                     "modalidad: Modalidad", "duracionMinutos: int", "cupo: int",
                     "cuposOcupados: int", "precioBase: SkillCoins", "etiquetas: Set<String>",
                     "franjas: List<FranjaHoraria>", "estado: EstadoPublicacion",
                     "nombreEstado: String", "politicaCancelacion: PoliticaCancelacion",
                     "promociones: List<Promocion>", "historial: List<CambioEstado>",
                     "fechaPublicacion: LocalDateTime", "version: long"],
  ["editar(titulo: String, descripcion: String, precioBase: SkillCoins): void",
   "publicar(actorId: String, ahora: LocalDateTime): void", "pausar(actorId: String): void",
   "reanudar(actorId: String): void", "cerrar(actorId: String): void",
   "cancelar(actorId: String, motivo: String): void",
   "cambiarEstado(nuevo: EstadoPublicacion, actorId: String, motivo: String): void",
   "validarPublicable(): void", "admiteSolicitudes(): boolean",
   "reservarCupo(franja: FranjaHoraria): void", "liberarCupo(franja: FranjaHoraria): void",
   "agregarFranja(franja: FranjaHoraria): void", "agregarPromocion(promocion: Promocion): void",
   "esVisiblePara(userId: String): boolean",
   "cotizar(fecha: LocalDate): ICalculoPrecio", "calcularPrecio(): SkillCoins",
   "getDescripcion(): String", "esAutor(userId: String): boolean"],
  est="State · Contexto / Decorator · Componente concreto",
  doc="Invariantes: publicar exige titulo, categoria, precio >= 0 y al menos una franja. "
      "@Version evita sobreventa de cupos (optimistic locking). El estado se persiste como "
      "nombreEstado y se reconstruye en @PostLoad con EstadoPublicacionFactory.")
C("FranjaHoraria", D, ["id: Long", "inicio: LocalDateTime", "fin: LocalDateTime",
                       "ocupada: boolean"],
  ["ocupar(): void", "liberar(): void", "seSuperponeCon(otra: FranjaHoraria): boolean",
   "estaLibre(): boolean"])
aso("Publicacion", "Perfil", "0..*", "1", "publicada por")
aso("Publicacion", "TipoPublicacion", "0..*", "1", "es de tipo")
aso("Publicacion", "Categoria", "0..*", "1", "clasificada en")
aso("Publicacion", "Habilidad", "0..*", "0..*", "trabaja")
aso("Publicacion", "Modalidad", "0..*", "1", "se dicta en")
comp("Publicacion", "FranjaHoraria", "1", "1..*", "ofrece")
comp("Publicacion", "CambioEstado", "1", "0..*", "registra")
aso("Publicacion", "PoliticaCancelacion", "0..*", "1", "aplica")
comp("Publicacion", "Promocion", "1", "0..*", "incluye")
aso("Publicacion", "EstadoPublicacion", "0..*", "1", "se encuentra en")
dep("Publicacion", "EstadoPublicacionFactory", "reconstruye estado con")

# ---- State de Publicacion
I("EstadoPublicacion", D, ["getNombre(): String",
                           "publicar(p: Publicacion, actorId: String): void",
                           "pausar(p: Publicacion, actorId: String): void",
                           "reanudar(p: Publicacion, actorId: String): void",
                           "cerrar(p: Publicacion, actorId: String): void",
                           "cancelar(p: Publicacion, actorId: String, motivo: String): void",
                           "esEditable(): boolean", "admiteSolicitudes(): boolean",
                           "esVisiblePara(p: Publicacion, userId: String): boolean"],
  doc="State: rol Estado.")
C("EstadoPublicacionBase", D, [],
  ["publicar(p: Publicacion, actorId: String): void",
   "pausar(p: Publicacion, actorId: String): void",
   "reanudar(p: Publicacion, actorId: String): void",
   "cerrar(p: Publicacion, actorId: String): void",
   "cancelar(p: Publicacion, actorId: String, motivo: String): void",
   "esEditable(): boolean", "admiteSolicitudes(): boolean",
   "esVisiblePara(p: Publicacion, userId: String): boolean"],
  abstracta=True, est="State · Estado base",
  doc="Por defecto toda transicion lanza ReglaNegocioException (transicion invalida); "
      "cada estado sobrescribe solo lo que permite.")
real("EstadoPublicacionBase", "EstadoPublicacion")
ESTADOS_PUB = {
    "EstadoBorrador": ["publicar(p: Publicacion, actorId: String): void",
                       "cancelar(p: Publicacion, actorId: String, motivo: String): void",
                       "esEditable(): boolean"],
    "EstadoPublicada": ["pausar(p: Publicacion, actorId: String): void",
                        "cerrar(p: Publicacion, actorId: String): void",
                        "cancelar(p: Publicacion, actorId: String, motivo: String): void",
                        "admiteSolicitudes(): boolean"],
    "EstadoPausada": ["reanudar(p: Publicacion, actorId: String): void",
                      "cerrar(p: Publicacion, actorId: String): void",
                      "cancelar(p: Publicacion, actorId: String, motivo: String): void"],
    "EstadoCerrada": [],
    "EstadoPublicacionCancelada": [],
}
for nombre, ops in ESTADOS_PUB.items():
    C(nombre, D, [], ["getNombre(): String"] + ops +
      ["esVisiblePara(p: Publicacion, userId: String): boolean"],
      est="State · Estado concreto")
    her("EstadoPublicacionBase", nombre)
C("EstadoPublicacionFactory", D, [], ["static desdeNombre(nombre: String): EstadoPublicacion"],
  est="Simple Factory", doc="Reconstruye el estado desde la columna nombreEstado (@PostLoad).")
dep("EstadoPublicacionFactory", "EstadoPublicacion", "crea")

# ---- Strategy: politica de cancelacion (persistida SINGLE_TABLE, como DescuentoStrategy)
C("PoliticaCancelacion", D, ["id: Long", "nombre: String"],
  ["calcularReintegro(pagado: SkillCoins, inicio: LocalDateTime, cancelacion: LocalDateTime): SkillCoins {abstract}"],
  abstracta=True, est="Strategy · Estrategia",
  doc="Entidad abstracta con @Inheritance(SINGLE_TABLE): cada publicacion referencia la "
      "politica elegida por su autor.")
C("PoliticaFlexible", D, ["horasReintegroTotal: int", "horasReintegroParcial: int"],
  ["calcularReintegro(pagado: SkillCoins, inicio: LocalDateTime, cancelacion: LocalDateTime): SkillCoins"],
  est="Strategy · Estrategia concreta",
  doc="100 % con anticipacion >= horasReintegroTotal (24), 50 % con >= horasReintegroParcial "
      "(2), si no 0 %.")
C("PoliticaEstricta", D, ["horasReintegroParcial: int"],
  ["calcularReintegro(pagado: SkillCoins, inicio: LocalDateTime, cancelacion: LocalDateTime): SkillCoins"],
  est="Strategy · Estrategia concreta",
  doc="Nunca devuelve el 100 %: 50 % con anticipacion >= horasReintegroParcial (72), si no 0 %.")
her("PoliticaCancelacion", "PoliticaFlexible", "PoliticaEstricta")

# ---- Decorator: promociones acumulables sobre el precio
I("ICalculoPrecio", D, ["calcularPrecio(): SkillCoins", "getDescripcion(): String"],
  doc="Decorator: rol Componente.")
C("PromocionDecorator", D, ["envuelto: ICalculoPrecio"],
  ["calcularPrecio(): SkillCoins", "getDescripcion(): String",
   "aplicar(precio: SkillCoins): SkillCoins {abstract}"],
  abstracta=True, est="Decorator · Decorador",
  doc="calcularPrecio() = aplicar(envuelto.calcularPrecio()). Como SkillCoins nunca es "
      "negativo, ninguna combinacion de promociones produce un total negativo.")
C("DescuentoPorcentual", D, ["porcentaje: int"],
  ["aplicar(precio: SkillCoins): SkillCoins", "getDescripcion(): String"],
  est="Decorator · Decorador concreto")
C("DescuentoMontoFijo", D, ["monto: SkillCoins"],
  ["aplicar(precio: SkillCoins): SkillCoins", "getDescripcion(): String"],
  est="Decorator · Decorador concreto")
real("Publicacion", "ICalculoPrecio")
real("PromocionDecorator", "ICalculoPrecio")
her("PromocionDecorator", "DescuentoPorcentual", "DescuentoMontoFijo")
aso("PromocionDecorator", "ICalculoPrecio", "0..*", "1", "envuelve")
C("Promocion", D, ["id: Long", "codigo: String", "vigenteDesde: LocalDate",
                   "vigenteHasta: LocalDate"],
  ["estaVigente(fecha: LocalDate): boolean",
   "decorar(precio: ICalculoPrecio): ICalculoPrecio {abstract}"],
  abstracta=True, est="Creator",
  doc="Definicion persistida de la promocion. Cada subclase crea su decorador porque tiene "
      "los datos para inicializarlo (Creator); los decoradores no se persisten.")
C("PromocionPorcentual", D, ["porcentaje: int"],
  ["decorar(precio: ICalculoPrecio): ICalculoPrecio"])
C("PromocionMontoFijo", D, ["monto: SkillCoins"],
  ["decorar(precio: ICalculoPrecio): ICalculoPrecio"])
her("Promocion", "PromocionPorcentual", "PromocionMontoFijo")
dep("PromocionPorcentual", "DescuentoPorcentual", "crea")
dep("PromocionMontoFijo", "DescuentoMontoFijo", "crea")

# ---- Contratacion (contexto de State)
C("Contratacion", D, ["id: Long", "publicacion: Publicacion", "cliente: Perfil",
                      "proveedor: Perfil", "mensaje: String", "objetivo: String",
                      "franja: FranjaHoraria", "precioAcordado: SkillCoins",
                      "detallePrecio: String", "estado: EstadoContratacion",
                      "nombreEstado: String", "propuesta: PropuestaReprogramacion",
                      "historial: List<CambioEstado>", "calificadaPorCliente: boolean",
                      "calificadaPorProveedor: boolean", "version: long"],
  ["aceptar(actorId: String): void", "rechazar(actorId: String, motivo: String): void",
   "pagar(actorId: String, cliente: Billetera, proveedor: Billetera): void",
   "proponerReprogramacion(actorId: String, nuevaFranja: FranjaHoraria): void",
   "aceptarReprogramacion(actorId: String): void", "iniciar(actorId: String): void",
   "completar(actorId: String): void",
   "cancelar(actorId: String, motivo: String, ahora: LocalDateTime, cliente: Billetera, proveedor: Billetera): SkillCoins",
   "calificar(autorId: String, puntaje: int, comentario: String): Calificacion",
   "cambiarEstado(nuevo: EstadoContratacion, actorId: String, motivo: String): void",
   "registrarPropuesta(actorId: String, nuevaFranja: FranjaHoraria): void",
   "aplicarPropuesta(actorId: String): void",
   "esCliente(userId: String): boolean", "esProveedor(userId: String): boolean",
   "esParticipante(userId: String): boolean", "contraparteDe(userId: String): Perfil"],
  est="State · Contexto",
  doc="Se crea con Publicacion.cotizar(): guarda precioAcordado y detallePrecio como foto. "
      "El cliente no puede ser el autor. Si cancela el proveedor se reintegra el 100 %; si "
      "cancela el cliente decide la PoliticaCancelacion.")
C("PropuestaReprogramacion", D, ["nuevaFranja: FranjaHoraria", "propuestaPor: String",
                                 "fecha: LocalDateTime"],
  ["puedeAceptarla(userId: String): boolean"], est="Value Object",
  doc="La acepta solo la contraparte de quien la propuso.")
aso("Contratacion", "Publicacion", "0..*", "1", "contrata")
aso("Contratacion", "Perfil", "0..*", "1", "solicitada por", "cliente")
aso("Contratacion", "Perfil", "0..*", "1", "prestada por", "proveedor")
aso("Contratacion", "FranjaHoraria", "0..1", "1", "reserva")
comp("Contratacion", "PropuestaReprogramacion", "1", "0..1", "tiene pendiente")
aso("PropuestaReprogramacion", "FranjaHoraria", "0..1", "1", "propone")
comp("Contratacion", "CambioEstado", "1", "1..*", "registra")
aso("Contratacion", "EstadoContratacion", "0..*", "1", "se encuentra en")
dep("Contratacion", "Calificacion", "crea")
dep("Contratacion", "Billetera", "debita y acredita")
dep("Contratacion", "EstadoContratacionFactory", "reconstruye estado con")

# ---- State de Contratacion
OPS_EC = ["aceptar(c: Contratacion, actorId: String): void",
          "rechazar(c: Contratacion, actorId: String, motivo: String): void",
          "pagar(c: Contratacion, actorId: String, cliente: Billetera, proveedor: Billetera): void",
          "proponerReprogramacion(c: Contratacion, actorId: String, franja: FranjaHoraria): void",
          "aceptarReprogramacion(c: Contratacion, actorId: String): void",
          "iniciar(c: Contratacion, actorId: String): void",
          "completar(c: Contratacion, actorId: String): void",
          "cancelar(c: Contratacion, actorId: String, motivo: String, ahora: LocalDateTime, cliente: Billetera, proveedor: Billetera): SkillCoins",
          "permiteCalificar(): boolean"]
_op_ec = {o.split("(")[0]: o for o in OPS_EC}
I("EstadoContratacion", D, ["getNombre(): String"] + OPS_EC, doc="State: rol Estado.")
C("EstadoContratacionBase", D, [], OPS_EC,
  abstracta=True, est="State · Estado base",
  doc="Por defecto toda accion lanza ReglaNegocioException (transicion invalida).")
real("EstadoContratacionBase", "EstadoContratacion")
ESTADOS_CON = {
    "EstadoPendiente": ["aceptar", "rechazar"],
    "EstadoAceptada": ["pagar", "proponerReprogramacion", "aceptarReprogramacion", "cancelar"],
    "EstadoConfirmada": ["iniciar", "proponerReprogramacion", "aceptarReprogramacion", "cancelar"],
    "EstadoEnCurso": ["completar"],
    "EstadoCompletada": ["permiteCalificar"],
    "EstadoRechazada": [],
    "EstadoContratacionCancelada": [],
}
for nombre, acciones in ESTADOS_CON.items():
    C(nombre, D, [], ["getNombre(): String"] + [_op_ec[a] for a in acciones],
      est="State · Estado concreto")
    her("EstadoContratacionBase", nombre)
C("EstadoContratacionFactory", D, [], ["static desdeNombre(nombre: String): EstadoContratacion"],
  est="Simple Factory")
dep("EstadoContratacionFactory", "EstadoContratacion", "crea")

# ---- Billetera de SkillCoins
C("Billetera", D, ["id: Long", "titular: Perfil", "saldo: SkillCoins",
                   "movimientos: List<MovimientoCredito>", "version: long"],
  ["debitar(monto: SkillCoins, concepto: String, contratacionId: Long): void",
   "acreditar(monto: SkillCoins, tipo: TipoMovimiento, concepto: String, contratacionId: Long): void",
   "tieneSaldo(monto: SkillCoins): boolean", "yaPago(contratacionId: Long): boolean"],
  est="Information Expert",
  doc="No permite saldo negativo ni un segundo debito para la misma contratacion "
      "(ademas de UNIQUE(billetera_id, contratacion_id, tipo) en la base).")
C("MovimientoCredito", D, ["id: Long", "fecha: LocalDateTime", "tipo: TipoMovimiento",
                           "monto: SkillCoins", "concepto: String", "contratacionId: Long"], [])
aso("Billetera", "Perfil", "0..1", "1", "pertenece a")
comp("Billetera", "MovimientoCredito", "1", "0..*", "registra")
aso("MovimientoCredito", "TipoMovimiento", "0..*", "1", "es de tipo")

# ---- Calificacion, notificacion y eventos
C("Calificacion", D, ["id: Long", "contratacion: Contratacion", "autor: Perfil",
                      "calificado: Perfil", "puntaje: int", "comentario: String", "PUNTAJE_MINIMO: int",
                      "PUNTAJE_MAXIMO: int",
                      "fecha: LocalDateTime"],
  ["validarPuntaje(): void"],
  doc="Verificada: solo la crea una Contratacion COMPLETADA, una por participante, y nunca "
      "con autor = calificado.")
aso("Calificacion", "Contratacion", "0..2", "1", "califica")
aso("Calificacion", "Perfil", "0..*", "1", "escrita por", "autor")
aso("Calificacion", "Perfil", "0..*", "1", "dirigida a", "calificado")
C("Notificacion", D, ["id: Long", "destinatarioId: String", "canal: CanalNotificacion",
                      "tipoEvento: TipoEvento", "mensaje: String", "fecha: LocalDateTime",
                      "leida: boolean"],
  ["marcarLeida(userId: String): void"])
aso("Notificacion", "TipoEvento", "0..*", "1", "informa")
aso("Notificacion", "CanalNotificacion", "0..*", "1", "se envia por")
C("EventoContratacion", D, ["tipo: TipoEvento", "contratacionId: Long", "clienteId: String",
                            "proveedorId: String", "fecha: LocalDateTime"],
  ["static de(contratacion: Contratacion, tipo: TipoEvento): EventoContratacion",
   "destinatarios(): List<String>"],
  est="Evento de dominio",
  doc="Sabe a quien le interesa cada tipo de evento (Information Expert): solicitud "
      "recibida -> proveedor, aceptacion -> cliente, etc.")
aso("EventoContratacion", "TipoEvento", "0..*", "1", "es de tipo")
dep("EventoContratacion", "Contratacion", "se crea desde")
dep("NotificacionObserver", "Notificacion", "crea")

# ---- Value object SkillCoins: embebido (@Embeddable) en las entidades que manejan montos
comp("Publicacion", "SkillCoins", "1", "1", "precio base")
comp("Contratacion", "SkillCoins", "1", "1", "precio acordado")
comp("Billetera", "SkillCoins", "1", "1", "saldo")
comp("MovimientoCredito", "SkillCoins", "1", "1", "monto")

# ---- Errores: el dominio y la aplicacion lanzan, el ejecutor traduce a ErrorDeAplicacion
for origen in ("EstadoPublicacionBase", "EstadoContratacionBase", "SkillCoins", "Billetera",
               "Publicacion", "Contratacion"):
    dep(origen, "ReglaNegocioException", "lanza")
for origen in ("ContratacionService", "PublicacionService", "CalificacionService"):
    dep(origen, "RecursoNoEncontradoException", "lanza")
    dep(origen, "AccesoDenegadoException", "lanza")
for exc in ("ReglaNegocioException", "RecursoNoEncontradoException", "AccesoDenegadoException"):
    dep("EjecutorDeAcciones", exc, "traduce")
dep("EjecutorDeAcciones", "ErrorDeAplicacion", "lanza")
dep("EjecutorDeAcciones", "ConflictoConcurrenciaException", "traduce")

# ---- Entradas y salidas de los casos de uso (dependencias punteadas, como en la catedra)
for serv, dtos in {"PerfilService": ["PerfilCommand", "PerfilPublicoDto"],
                   "PublicacionService": ["PublicacionCommand", "CambiarEstadoCommand", "PromocionCommand",
                                          "FranjaDto", "PoliticaDto"],
                   "BusquedaService": ["FiltroBusqueda", "Pagina<T>"],
                   "RecomendacionService": ["Pagina<T>"],
                   "ContratacionService": ["SolicitarCommand", "ContratacionDto", "FranjaDto"],
                   "CalificacionService": ["CalificarCommand"],
                   "NotificacionService": ["NotificacionDto"],
                   "BilleteraService": ["BilleteraDto"]}.items():
    for dto in dtos:
        dep(serv, dto, "recibe" if ("Command" in dto or "Filtro" in dto) else "devuelve")

# ---- Dependencias de la aplicacion hacia el dominio (las mas significativas)
dep("ContratacionService", "EventoContratacion", "crea")
dep("ContratacionService", "Contratacion", "crea")
dep("PublicacionService", "Publicacion", "crea")
dep("IEventoObserver", "EventoContratacion", "recibe")

# =====================================================================
# Vistas parciales: una por capa y una por patron
# =====================================================================
diagramas = [
    {"nombre": "Capa Dominio", "paquetes": [D]},
    {"nombre": "Capa Aplicacion", "paquetes": [A]},
    {"nombre": "Capa Infraestructura",
     "paquetes": [IN],
     "clases": [f"I{n}Repository" for n in REPOS] + ["IGestorTransacciones",
                "IServicioIdentidad", "ICanalNotificacion", "ICanalNotificacionFactory"]},
    {"nombre": "Capa Presentacion", "paquetes": [P],
     "clases": [f"I{n}Service" for n in SERVICIOS] + ["IServicioIdentidad"]},
    {"nombre": "Patron State - Publicacion",
     "clases": ["Publicacion", "EstadoPublicacion", *ESTADOS_PUB, "CambioEstado"]},
    {"nombre": "Patron State - Contratacion",
     "clases": ["Contratacion", "EstadoContratacion", *ESTADOS_CON, "CambioEstado",
                "PropuestaReprogramacion"]},
    {"nombre": "Patron Strategy",
     "clases": ["RecomendacionService", "IRecomendacionService", "IEstrategiaRecomendacion",
                "AfinidadPorHabilidades", "PopularidadReciente", "IContratacionRepository",
                "Publicacion", "PoliticaCancelacion", "PoliticaFlexible", "PoliticaEstricta"]},
    {"nombre": "Patron Decorator",
     "clases": ["ICalculoPrecio", "Publicacion", "PromocionDecorator", "DescuentoPorcentual",
                "DescuentoMontoFijo", "Promocion", "PromocionPorcentual",
                "PromocionMontoFijo", "SkillCoins"]},
    {"nombre": "Patron Observer",
     "clases": ["ContratacionService", "CalificacionService", "IPublicadorEventos",
                "PublicadorEventos", "IEventoObserver", "NotificacionObserver",
                "ReputacionObserver", "EventoContratacion", "TipoEvento",
                "ICanalNotificacionFactory", "ICanalNotificacion", "Notificacion"]},
    {"nombre": "Patron Adapter y Simple Factory",
     "clases": ["IServicioIdentidad", "IdentidadAdapter", "IdentityClientMock",
                "ICanalNotificacion", "EmailAdapter", "EmailClientMock", "PushAdapter",
                "PushClientMock", "CanalInterno", "ICanalNotificacionFactory",
                "CanalNotificacionFactory", "INotificacionRepository", "EjecutorDeAcciones"]},
    {"nombre": "Patron Composite",
     "clases": ["ElementoCatalogo", "Categoria", "Habilidad", "Publicacion", "Perfil",
                "BusquedaService", "ICatalogoRepository"]},
    {"nombre": "Recorrido principal",
     "clases": ["ContratacionControllerImpl", "ContratacionController", "IContratacionService",
                "ContratacionService", "IContratacionRepository", "IBilleteraRepository",
                "IGestorTransacciones", "IPublicadorEventos", "Contratacion", "Publicacion",
                "Billetera", "EstadoContratacion", "EventoContratacion", "Calificacion"]},
]

# Main resumido: dominio + servicios y patrones principales. Quedan fuera (siguen en el modelo y en
# sus vistas por capa/patron): enums, Base y Factory de los State, repositorios, DTOs, adaptadores, UI.
_fuera = {"EstadoPublicacionBase", "EstadoPublicacionFactory", "EstadoContratacionBase",
          "EstadoContratacionFactory", "ReglaNegocioException"}
_enums = {"TipoPerfil", "TipoPublicacion", "Modalidad", "CanalNotificacion", "TipoMovimiento",
          "TipoEvento"}
principal = [c["nombre"] for c in clases
             if (c.get("paquete") == D and c["nombre"] not in _fuera | _enums)] + [
    "IPerfilService", "IPublicacionService", "IBusquedaService", "IContratacionService",
    "ICalificacionService", "INotificacionService", "IBilleteraService",
    "IEventoObserver", "IPublicadorEventos", "ICanalNotificacion", "IServicioIdentidad",
    "IEstrategiaRecomendacion"]

spec = {"proyecto": "SkillSwap", "diagrama": "Main", "clases": clases, "principal": principal,
        "relaciones": relaciones, "diagramas": diagramas}

if __name__ == "__main__":
    salida = os.path.join(os.path.dirname(os.path.abspath(__file__)), "spec.json")
    with open(salida, "w", encoding="utf-8") as f:
        json.dump(spec, f, indent=1, ensure_ascii=False)
    print(f"{salida}: {len(clases)} clases, {len(relaciones)} relaciones, "
          f"{len(diagramas) + 1} diagramas")
