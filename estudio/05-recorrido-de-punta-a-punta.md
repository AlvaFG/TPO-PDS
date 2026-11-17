# 5. Recorrido de punta a punta

Este es el flujo que se muestra en la demo (consigna §13): **publicar → solicitar → aceptar → pagar → completar → calificar**. Acá se sigue cada paso **por las clases reales** que intervienen. Es la mejor forma de entender cómo las capas trabajan juntas.

## Cómo viaja una acción (la regla general)

```
Ventana (SkillSwapView)
   │  llama a una interfaz del kit:  contrataciones().avanzar(userId, id)
   ▼
Controller           presentation/ContratacionControllerImpl
   │  EjecutorDeAcciones valida el userId con IServicioIdentidad y atrapa errores
   ▼
Caso de uso          application/service/ContratacionService
   │  abre una transacción (IGestorTransacciones) y busca entidades por sus repositorios
   ▼
Dominio              domain/model/Contratacion  →  EstadoXxx
   │  la entidad decide si la acción es válida y cambia su estado
   ▼
Repositorio JPA      infrastructure/persistence/ContratacionJpaRepository  →  MySQL
   │  commit de la transacción
   ▼
Evento               application/evento/PublicadorEventos
   │  avisa a los observers (después del commit)
   ▼
Observers            NotificacionObserver → canales (email / push / interna)
                     ReputacionObserver  → recalcula la reputación
```

Lo que **vuelve** a la ventana es un `record` simple (`Conversor` lo arma desde un DTO). Si algo falla, la ventana recibe un `ErrorDeAplicacion` con un mensaje legible y lo muestra en un diálogo.

---

## Paso 0 — El arranque

1. `SkillSwapApplication.main` se ejecuta (`mvn -q compile exec:java`).
2. Con `--real`: `PersistenceFactory.crear()` corre **Flyway** (crea tablas y carga datos de demo en MySQL) y abre el `EntityManagerFactory` de Hibernate, que **valida** el esquema.
3. Se crean los repositorios JPA, `IdentidadAdapter` y `CanalNotificacionFactory`.
4. `ConfiguracionApp.crear(puertos)` arma servicios, observers y controllers.
5. Se abre `SkillSwapView` con esos controllers. El usuario activo es `user-mateo`.

---

## Paso 1 — Publicar una oferta

**En la ventana:** el proveedor crea un borrador y pide pasarlo a PUBLICADA.

| Qué pasa | Clase |
|---|---|
| La ventana llama a `crearBorrador(...)` | `PublicacionControllerImpl.crearBorrador` |
| El controller valida el usuario y delega | `EjecutorDeAcciones` → `PublicacionService` |
| Se crea la entidad en estado **BORRADOR** con tres franjas diarias desde la semana siguiente | `domain/model/Publicacion` + `EstadoBorrador` |
| Se guarda | `PublicacionJpaRepository.guardar` |
| La ventana pide `cambiarEstado(..., PUBLICADA)` | `PublicacionControllerImpl.cambiarEstado` |
| La entidad valida: título, categoría, precio ≥ 0 y al menos una franja (`validarPublicable`) | `Publicacion.publicar` → `EstadoBorrador.publicar` |
| Si falta algo, **lanza `ReglaNegocioException`** y la ventana muestra el mensaje | (caso de aceptación #2) |
| Pasa a **PUBLICADA** y queda en el historial (fecha, actor, motivo) | `Publicacion.cambiarEstado` |

## Paso 2 — Buscar y encontrar la publicación

| Qué pasa | Clase |
|---|---|
| La ventana arma los filtros (texto, categoría, modalidad, precio…) | UI del kit |
| `BusquedaControllerImpl.buscar` los convierte a `FiltroBusqueda` | controller + `Conversor` |
| `BusquedaService` resuelve la categoría **y sus subcategorías** (Composite) | `Categoria.listarSubcategoriaIds` |
| `PublicacionJpaRepository.buscar` arma una consulta con `WHERE`, `ORDER BY` y `LIMIT/OFFSET`, más un `COUNT` aparte | usa los índices de `V1` |
| Devuelve una `Pagina<PublicacionDto>` | los listados siempre van paginados |

Solo se devuelven publicaciones en estado **PUBLICADA**.

## Paso 3 — Solicitar

El cliente (otro usuario) aprieta "solicitar" en el detalle.

| Qué pasa | Clase |
|---|---|
| `ContratacionControllerImpl.solicitar` crea un `SolicitarCommand` (el cliente no elige franja: se toma la primera libre) | controller |
| `ContratacionService.solicitar` busca el perfil (debe estar activo) y la publicación | caso de uso |
| `new Contratacion(...)` valida: la publicación **admite solicitudes**, el cliente **no es el autor**, la franja pertenece a la publicación y **está libre** | `domain/model/Contratacion` (constructor) |
| Calcula el precio con las promociones vigentes y **guarda una foto** (`precioAcordado`) | `Publicacion.cotizar` (Decorator) |
| Nace en estado **PENDIENTE** | `EstadoPendiente` |
| Se guarda y, al terminar, se publica `SOLICITUD_RECIBIDA` | `PublicadorEventos` |
| El proveedor recibe una notificación por los canales que eligió | `NotificacionObserver` → factory → adapters |

## Paso 4 — Aceptar (reserva el cupo)

El proveedor aprieta "avanzar". `ContratacionControllerImpl.avanzar` mira **el estado y el rol** y elige el caso de uso: `PENDIENTE` + proveedor → `aceptar`.

| Qué pasa | Clase |
|---|---|
| Verifica que quien acepta es el proveedor | `ContratacionService.delProveedor` |
| `Contratacion.aceptar` → `estado.aceptar` | `EstadoPendiente.aceptar` |
| `publicacion.reservarCupo(franja)`: verifica que haya cupo, marca la franja ocupada y suma un cupo ocupado | `Publicacion.reservarCupo` |
| Cambia a **ACEPTADA** y registra el historial | `Contratacion.cambiarEstado` |
| Guarda publicación **y** contratación en la misma transacción | `ContratacionService.aceptar` |

**El caso de las dos aceptaciones simultáneas (caso #4):** si dos proveedores aceptan a la vez el último cupo, ambos leen la misma `version` de la publicación. El primer commit sube la versión; el segundo falla con `OptimisticLockException`, que `JpaGestorTransacciones` convierte en `ConflictoConcurrenciaException`, y la ventana muestra *"Cupo agotado o publicación modificada por otra operación, reintente"*. Detalle en [06](06-base-de-datos-y-jpa.md).

## Paso 5 — Pagar

El cliente aprieta "avanzar": `ACEPTADA` + cliente → `pagar`.

| Qué pasa | Clase |
|---|---|
| Verifica que quien paga es el cliente | `ContratacionService.pagar` |
| Busca las billeteras del cliente y del proveedor | `IBilleteraRepository` |
| `EstadoAceptada.pagar`: **debita** al cliente y **acredita** al proveedor | `Billetera.debitar` / `acreditar` |
| Si el saldo no alcanza, `debitar` **lanza** antes de modificar nada: saldos y estado quedan intactos | caso #5 |
| Si ya pagó, la restricción `UNIQUE (billetera_id, contratacion_id, tipo)` impide el doble cobro | caso "doble pago" |
| Cambia a **CONFIRMADA** | `EstadoConfirmada` |
| Las dos billeteras y la contratación se guardan **en una sola transacción** | `ContratacionService.pagar` |
| Se publica `PAGO_CONFIRMADO` y se notifica | observers |

## Paso 6 — Iniciar y completar

| Acción | Estado antes → después | Quién |
|---|---|---|
| `iniciar` | CONFIRMADA → **EN_CURSO** | proveedor |
| `completar` | EN_CURSO → **COMPLETADA** | proveedor |

Cada una es un `avanzar` más. `completar` publica `SESION_COMPLETADA`. En COMPLETADA la contratación **habilita** las calificaciones.

## Paso 7 — Calificar y recalcular la reputación

En la ventana, una contratación COMPLETADA muestra el botón **Calificar** (puntaje de 1 a 5 y comentario). También lo cubren las pruebas (`RecorridoPrincipalTest`, `CalificacionServiceTest`, `SkillSwapControllersTest`).

| Qué pasa | Clase |
|---|---|
| `CalificacionService.calificar` llama a `Contratacion.calificar` | caso de uso |
| La entidad valida: está COMPLETADA, el autor **participa**, **no se califica a sí mismo**, **no calificó antes** | `Contratacion.calificar` |
| Crea la `Calificacion` y marca que ese participante ya calificó | entidad |
| La restricción `UNIQUE (contratacion_id, autor_id)` es el respaldo en la base | `V1__esquema.sql` |
| Se publica `CALIFICACION_REGISTRADA` | `PublicadorEventos` |
| `ReputacionObserver` **recalcula** la reputación de las dos partes (promedio) | `Perfil.recalcularReputacion` |

## Cancelar (camino alternativo)

`ContratacionService.cancelar` corre en **una transacción**:

1. Verifica que quien cancela participa.
2. Llama a `Contratacion.cancelar` → el **estado actual** decide:
   - En **ACEPTADA** (todavía no se pagó): no hay reintegro; solo se libera el cupo.
   - En **CONFIRMADA** (ya se pagó): si cancela el **proveedor** se devuelve el 100 %; si cancela el **cliente** decide la **política de cancelación** de la publicación (`PoliticaFlexible` / `PoliticaEstricta`), que devuelve 100 %, 50 % o 0 % según la anticipación. El reintegro sale de la billetera del proveedor y va a la del cliente.
3. Mueve los créditos entre billeteras, **libera el cupo** y registra el historial.
4. Publica `CANCELACION`.

---

## Resumen: quién decide qué

| Decisión | La toma… | No la toma… |
|---|---|---|
| "¿Se puede pagar en este estado?" | `EstadoAceptada` (dominio) | el controller ni el servicio |
| "¿Alcanza el saldo?" | `Billetera` (dominio) | el servicio |
| "¿Hay cupo?" | `Publicacion` (dominio) | el repositorio |
| "¿Cuánto se devuelve?" | `PoliticaCancelacion` (dominio) | la contratación |
| "¿Quién puede ejecutar esta acción?" | el servicio (`delProveedor`, `esCliente`) | la ventana |
| "¿Qué botón corresponde?" | `ContratacionControllerImpl.avanzar` | el dominio |
| "¿Qué hacer con un error?" | `EjecutorDeAcciones` | los controllers uno por uno |
| "¿A quién se notifica y cómo?" | observers + preferencias del perfil | el servicio de contratación |

Esta es la señal de un buen diseño: **cada pregunta tiene un único lugar donde se responde**.
