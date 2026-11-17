# 10. La consigna contra lo que se hizo

Punto por punto de `consigna.docx`: qué pide y **dónde se cumple**. Sirve para responder "¿dónde está X?" en la defensa, y para saber con honestidad **qué quedó afuera**.

## §1 Condiciones generales

| La consigna pide | Cómo se cumple |
|---|---|
| Java con Hibernate/JPA y MySQL, **sin Spring Boot** | Java 17, Hibernate 7.2.1, MySQL 8.4; el armado es manual en `ConfiguracionApp` |
| El foco es el backend | Todo el trabajo está en `domain`, `application`, `infrastructure` y los controllers |
| Código fuente + UML en StarUML + informe PDF | `src/`, `skillswap.mdj`, `informe.pdf` |
| Cualquier integrante explica y modifica una parte | Esta guía (`estudio/`) |
| No se evalúa el diseño visual | La ventana es la del kit, sin modificar |

## §4 Alcance funcional

| Requisito | Dónde está | Estado |
|---|---|---|
| **4.1** Perfil con `userId` externo, nombre, bio, zona horaria, habilidades, intereses, disponibilidad | `Perfil`, `PerfilService.crearPerfil`, `IServicioIdentidad` | ✅ (sin pantalla en la ventana: se prueba por servicio) |
| **4.1** Perfil público con reputación, publicaciones activas e historial | `PerfilService.verPerfil`, `PerfilPublicoDto` | ✅ |
| **4.1** Reputación recalculada, no editable | `Perfil.recalcularReputacion`, `ReputacionObserver` | ✅ (no hay setter público) |
| **4.2** Publicación con todos sus datos | `Publicacion` | ✅ |
| **4.2** Estados BORRADOR/PUBLICADA/PAUSADA/CERRADA/CANCELADA | `domain/estado/publicacion` | ✅ |
| **4.2** Publicar exige título, categoría, precio ≥ 0 y una franja | `Publicacion.validarPublicable` | ✅ |
| **4.2** Transiciones inválidas rechazadas por el dominio | `EstadoPublicacionBase` | ✅ |
| **4.3** Buscar por texto, categoría, modalidad, precio, habilidad y reputación | `PublicacionJpaRepository.buscar`, `FiltroBusqueda` | ✅ |
| **4.3** Ordenar por relevancia, reputación, precio y fecha | `CriterioOrden` → `ORDER BY` | ✅ |
| **4.3** Dos estrategias de recomendación | `AfinidadPorHabilidades`, `PopularidadReciente` | ✅ |
| **4.3** Paginar todos los listados | `Pagina<T>` | ✅ |
| **4.4** Solicitar con mensaje, franja y objetivo | `SolicitarCommand`, `ContratacionService.solicitar` | ✅ |
| **4.4** Aceptar/rechazar; aceptar reserva el cupo transaccionalmente | `EstadoPendiente`, `@Version` | ✅ |
| **4.4** Pago con SkillCoins, sin saldos negativos ni doble cobro | `Billetera`, `UNIQUE` en `movimiento_credito` | ✅ |
| **4.4** Reprogramar si ambas partes aceptan | `PropuestaReprogramacion`, `Contratacion.aplicarPropuesta` | ✅ (en servicio; la ventana no lo ofrece) |
| **4.4** Una calificación por parte, nunca a sí mismo | `Contratacion.calificar` | ✅ |
| **4.5** Estados de la contratación y sus acciones | `domain/estado/contratacion` | ✅ |
| **4.6** Notificar los eventos relevantes | `PublicadorEventos`, `NotificacionObserver` | ✅ |
| **4.6** Canal elegido por usuario (email, push, interna) | `Perfil.getCanales`, `CanalNotificacionFactory` | ✅ |
| **4.6** Proveedores simulados e intercambiables | `EmailAdapter`, `PushAdapter`, `*ClientMock` | ✅ |

## §5 Reglas de negocio mínimas

| Regla | Dónde se garantiza |
|---|---|
| El autor no solicita su propia publicación | Constructor de `Contratacion` |
| Una publicación no publicada no admite solicitudes | `Publicacion.admiteSolicitudes` |
| No se reserva una franja ocupada ni se supera el cupo | `Contratacion.validarFranjaDisponible`, `Publicacion.reservarCupo`, `CHECK` y `@Version` |
| Débito y crédito en una única transacción | `ContratacionService.pagar` |
| Cancelación devuelve 100/50/0 % | `PoliticaFlexible`, `PoliticaEstricta` |
| Promociones nunca dejan total negativo | `SkillCoins` + `restarHastaCero` |
| Calificar una vez, solo participantes de una contratación completada | `Contratacion.calificar` + `UNIQUE` |
| Toda transición queda en historial inmutable | `cambiarEstado` → `CambioEstado`; `getHistorial` devuelve lista no modificable |
| Baja lógica del perfil | `Perfil.activo`; `PerfilService` (probado en `actualizarYDarDeBaja`) |
| Controllers validan permisos con el `userId` de identidad | `EjecutorDeAcciones` + chequeos en los servicios |

## §6 Arquitectura

Ver [03-arquitectura.md](03-arquitectura.md). Dependencias hacia el dominio: verificado (`domain` y `application` no importan capas externas). Una concesión: las entidades del dominio llevan anotaciones JPA.

## §7 Patrones

Pide **al menos 4**, cada uno respaldado por pruebas. Hay **7**. Ver [04](04-patrones-de-diseno.md).

## §8 Persistencia

| Requisito | Dónde |
|---|---|
| Persistir perfiles, habilidades, publicaciones, franjas, solicitudes, contrataciones, movimientos, calificaciones, notificaciones e historial | 19 tablas en `V1__esquema.sql` |
| Cardinalidades, claves, unicidad e índices | `V1__esquema.sql` |
| Migraciones versionadas, sin `ddl-auto=create` | Flyway, `hbm2ddl=validate` |
| Límites transaccionales | `JpaGestorTransacciones` y los servicios |
| Evitar N+1 y justificarlo | `JOIN FETCH`, `batch_fetch_size`, proyecciones a DTO |
| Datos de demostración reproducibles | `V2__datos_demo.sql` |
| Optimistic locking contra sobreventa | `@Version` en `Publicacion`, `Contratacion`, `Billetera` |

## §9 Controllers mínimos

La consigna dice que los nombres son **orientativos** y que la cátedra provee las interfaces. Los métodos que pide la tabla de §9 existen, mapeados a las interfaces del kit:

| Pide la consigna | Interfaz del kit | Implementación |
|---|---|---|
| `PerfilController.verPerfil` | `PerfilController.verPerfil` | `PerfilControllerImpl` |
| `PerfilController.crearPerfil` | (no está en el kit) | `PerfilService.crearPerfil` (sin controller de ventana) |
| `PublicacionController.crearBorrador` / `cambiarEstado` | igual | `PublicacionControllerImpl` |
| `BusquedaController.buscar` | `buscar(filtros)` | `BusquedaControllerImpl` |
| `RecomendacionController.recomendar` | `BusquedaController.recomendar` | `BusquedaControllerImpl` |
| `ContratacionController.solicitar` / `aceptar` / `pagar` / `completar` | `solicitar` y **`avanzar`** (un botón que elige aceptar, pagar, iniciar o completar) | `ContratacionControllerImpl` |
| `ContratacionController.cancelar` | `cancelar` | `ContratacionControllerImpl` |
| `CalificacionController.calificar` | (no está en el kit) | `CalificacionService.calificar` |
| `NotificacionController.listar` | `listarNotificaciones` | `NotificacionControllerImpl` |

Los controllers son delgados, sin SQL ni reglas, y manejan los errores de forma uniforme (`ErrorDeAplicacion`).

## §10 Interfaz provista

La ventana y sus modelos están **sin modificar**. Cumple el contrato: *"la UI conocerá interfaces de controllers, nunca repositorios ni entidades JPA"*. El `userId` activo (`user-mateo`) viene de un login simulado.

## §11 Hitos

| Hito | Contenido | Dónde |
|---|---|---|
| 1 · Diseño | Alcance, historias, dominio, UML, decisiones | `informe.md` / `.pdf`, `skillswap.mdj`, `img/` |
| 2 · Núcleo | Dominio y casos de uso con pruebas | `domain/`, `application/`, pruebas unitarias |
| 3 · Infraestructura | JPA/MySQL, repositorios, adaptadores | `infrastructure/`, migraciones |
| 4 · Producto | Interfaz conectada, documentación, pruebas | `presentation/`, `README.md`, esta guía |

## §12–§15 Pruebas, entrega, evaluación y casos de aceptación

- 222 pruebas, 99,2 % de cobertura en `domain` + `application`, casos negativos, y README reproducible. Ver [08](08-pruebas.md).
- Los **10 casos de aceptación** con su prueba: tabla en el [README principal](../README.md).
- Recorrido de la demo (§13): `RecorridoPrincipalTest`.

## §16 Fuera de alcance (no se hizo, a propósito)

Autenticación real, dinero real, chat, videollamadas, despliegue productivo y diseño responsivo.

---

## Límites conocidos (decirlos antes de que pregunten)

| Límite | Detalle |
|---|---|
| **JPA en el dominio** | Las entidades llevan anotaciones `jakarta.persistence`. Es la práctica del material de la cátedra y evita duplicar clases, pero la consigna §6 dice que el dominio no debería conocer JPA |
| **La ventana no expone todo** | No hay pantalla para crear perfil ni darse de baja (la ventana usa un usuario fijo que ya existe). Calificar, rechazar, reprogramar, promociones, política de cancelación, elegir horario y editar perfil y borradores sí están en la ventana |
| **`HIBRIDA`** | La ventana ofrece la modalidad híbrida, pero el dominio solo tiene remota y presencial (la consigna §4.2 dice solo eso); se rechaza con un error legible |
| **Franjas automáticas** | La ventana no carga horarios: el borrador nace con tres franjas diarias desde la semana siguiente. Al solicitar se puede elegir una franja libre (por defecto, la primera) |
| **Promociones sin controller** | El dominio y el cálculo están probados; no hay forma de crearlas desde la ventana |
| **Reputación fuera de la transacción** | Se recalcula por evento, después de guardar la calificación |
| **HTML sin escapar en la vista del kit** | `SkillSwapView` arma etiquetas `<html>` concatenando títulos y descripciones. Es código de la cátedra, sin modificar. Swing no ejecuta scripts, pero un `<img>` con una URL externa produciría una petición de red al mostrarse |
| **Email y push** | Son simulados: escriben en la consola. Solo la bandeja interna se guarda |
| **macOS y Windows** | Los pasos del README están escritos pero **no se probaron** en esos sistemas; se probó en Linux |
| **Pruebas contra MySQL real** | Las de persistencia corren en H2; hay pruebas que además corren contra MySQL si se definen variables de entorno, pero no son automáticas |
