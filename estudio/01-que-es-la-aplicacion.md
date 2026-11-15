# 1. Qué es la aplicación

## En una frase

SkillSwap es un **marketplace de talento**: una plataforma donde unas personas ofrecen una mentoría o un servicio breve, otras lo contratan, y todo se paga con **SkillCoins**, créditos ficticios que solo existen dentro del sistema.

## Para qué sirve (el problema del negocio)

La consigna lo plantea así: una empresa quiere **conectar personas que necesitan resolver un problema concreto con profesionales dispuestos a enseñar o prestar un servicio**.

Ejemplos que da la consigna: una mentoría para preparar entrevistas, una revisión de portfolio, una asesoría de arquitectura, una sesión de fotografía o el armado de un prototipo.

Un mismo usuario puede ser **cliente** (contrata), **proveedor** (ofrece) o las dos cosas. Una **organización** puede publicar un *desafío* en lugar de una oferta.

> **No hay dinero real.** Los SkillCoins son créditos de juguete. Tampoco hay login real, ni API REST, ni endpoints. Todo eso está explícitamente fuera de alcance (consigna §16).

## Por qué existe este trabajo

Es el **TPO (Trabajo Práctico Obligatorio) de Proceso de Desarrollo de Software**, UADE. No se evalúa que la aplicación sea linda, sino **cómo está diseñada**. La consigna (§3) dice qué se mira:

| Competencia | Qué tiene que mostrar el trabajo |
|---|---|
| Clean Architecture | Las dependencias apuntan al dominio; los casos de uso no dependen de la interfaz, de JPA ni de proveedores externos |
| SOLID y GRASP | Clases con una responsabilidad, bajo acoplamiento, polimorfismo, controllers delgados |
| Patrones de diseño | Patrones justificados por una variación real del problema (no por cantidad). Mínimo 4 |
| Persistencia | Modelo relacional consistente, transacciones, relaciones JPA, consultas eficientes |
| Calidad | Pruebas automáticas de reglas, casos de uso y persistencia |

Por eso el proyecto tiene más documentación (informe, diagramas, esta guía) que lo habitual: **el diseño es lo que se defiende**.

## Qué se puede hacer con la aplicación (funcionalidad)

La consigna (§4) define seis módulos. Así se ven en la aplicación:

### Perfiles y reputación
- Un perfil profesional se asocia a un `userId` que da un servicio de identidad externo (simulado).
- Tiene nombre visible, biografía, zona horaria, habilidades, intereses y disponibilidad.
- La **reputación** es un promedio de calificaciones. **No se puede editar a mano**: se recalcula sola cuando alguien califica.
- Un perfil no se borra: se hace **baja lógica** (el historial de operaciones se conserva).

### Publicaciones
Una publicación tiene título, descripción, categoría, modalidad (remota o presencial), duración, cupo, precio base en SkillCoins y etiquetas. Pasa por estos **estados**:

```
BORRADOR ──publicar──► PUBLICADA ──pausar──► PAUSADA
                          │  ▲                  │
                          │  └───reanudar───────┘
                          ├──cerrar──► CERRADA   (no se reabre)
                          └──cancelar (con motivo)──► CANCELADA
```

- **BORRADOR**: editable, no se ve.
- **PUBLICADA**: visible y acepta solicitudes.
- **PAUSADA**: la ve solo el autor y no acepta solicitudes nuevas.
- **CERRADA**: terminada, no puede reabrirse.
- **CANCELADA**: retirada, el motivo es obligatorio.

Para **publicar** hace falta título, categoría, precio no negativo y al menos una franja horaria. Una transición inválida (por ejemplo, reabrir una CERRADA) la **rechaza el dominio** con un error.

### Búsqueda y recomendación
- Se busca por texto, categoría, modalidad, rango de precio, habilidad y reputación mínima.
- Se ordena por relevancia, mejor reputación, menor precio o más reciente.
- Todos los listados están **paginados**.
- Hay **dos estrategias de recomendación intercambiables**: *afinidad por habilidades* y *popularidad reciente*.

### Solicitud, reserva y contratación
Es el corazón del sistema. Una **contratación** nace cuando un cliente solicita una publicación:

```
PENDIENTE ──aceptar──► ACEPTADA ──pagar──► CONFIRMADA ──iniciar──► EN_CURSO ──completar──► COMPLETADA
    │                     │                    │
    └──rechazar──► RECHAZADA   └──cancelar───────┴──► CANCELADA
```

| Estado | Qué se puede hacer |
|---|---|
| PENDIENTE | El proveedor acepta o rechaza |
| ACEPTADA | El cliente paga; también cancelar o proponer reprogramación |
| CONFIRMADA | Iniciar, reprogramar o cancelar (según la política) |
| EN_CURSO | Completar |
| COMPLETADA | Calificar y ver el comprobante |
| RECHAZADA / CANCELADA | Solo consultar el historial |

Al **aceptar**, se reserva la franja y se descuenta un cupo de la publicación (en una transacción). Al **pagar**, se debita al cliente y se acredita al proveedor **en una única transacción**, sin saldos negativos ni doble cobro. Al **cancelar**, se devuelve **100 %, 50 % o 0 %** según la anticipación y la política elegida.

### Notificaciones
Cuando pasa algo relevante (solicitud recibida, aceptada, pago confirmado, reprogramación, cancelación, sesión completada) se notifica a las partes. **Cada usuario elige** si quiere email, push o notificación interna. Email y push son simulados (escriben en la consola); la bandeja interna se guarda en la base.

### Calificaciones
Al completarse la sesión, **cada parte puede calificar a la otra una sola vez**. Nadie puede calificarse a sí mismo. Al calificar se recalcula la reputación.

## Reglas de negocio importantes (consigna §5)

- El autor no puede solicitar su propia publicación.
- Una publicación no publicada no admite solicitudes.
- No se reserva una franja ocupada ni se supera el cupo.
- Débito y crédito de SkillCoins en una única transacción.
- Las promociones bajan el precio pero **nunca** dejan un total negativo.
- Solo participantes de una contratación completada pueden calificarla, una vez.
- Toda transición de estado queda en un **historial inmutable** (fecha, actor y motivo).
- Los controllers que modifican datos validan el `userId` contra el servicio de identidad.

## Cómo se usa

La aplicación abre una **ventana de escritorio** (Swing). **No hay página web**: `localhost:3306` es el puerto de MySQL, no una web.

Pantallas de la ventana (las dio la cátedra):

| Pantalla | Para qué |
|---|---|
| Inicio y buscador | Buscar y filtrar publicaciones, ver recomendaciones |
| Detalle de publicación | Solicitar la contratación |
| Panel del proveedor | Cambiar el estado de las publicaciones, aceptar solicitudes |
| Contrataciones | Avanzar el estado (aceptar, pagar, iniciar, completar) o cancelar |
| Wallet | Ver saldo y movimientos; acreditar créditos de demostración |
| Notificaciones | Ver la bandeja y marcar como leídas |
| Perfil | Ver el perfil y la reputación |

El usuario activo de la demo es `user-mateo`. Cómo instalar y ejecutar: ver el [README principal](../README.md).

### Dos modos de ejecución

| Modo | Comando | Qué usa |
|---|---|---|
| **Mock** | `mvn -q compile exec:java` | Datos en memoria del kit de la cátedra. No usa nuestro código ni la base: sirve para ver la ventana sola |
| **Real** | `docker compose up -d` y `mvn -q compile exec:java "-Dexec.args=--real"` | Toda nuestra implementación: casos de uso, reglas, JPA y MySQL |

> Para la defensa se muestra el **modo real**: es donde está el trabajo del equipo.

## Quién hizo qué: lo de la cátedra y lo nuestro

Esto es lo que más conviene tener claro:

| Origen | Qué es | Dónde |
|---|---|---|
| **Cátedra** (no se toca) | La ventana Swing, el tema visual, los modelos de la vista y los datos mock | `ui/`, `model/`, `mock/`, `controllers/` |
| **Equipo** | Todo lo demás: dominio, casos de uso, persistencia, adaptadores, controllers reales, pruebas, base de datos | `domain/`, `application/`, `infrastructure/`, `presentation/`, `ConfiguracionApp`, `SkillSwapApplication` |

La cátedra define **interfaces de controllers** (`UiControllers`). La ventana solo conoce esas interfaces. Nuestro trabajo fue **implementarlas** de verdad, en lugar de usar los datos mock (ver [03-arquitectura.md](03-arquitectura.md)).

## Glosario rápido

| Término | Significado |
|---|---|
| **SkillCoins** | Moneda ficticia del sistema |
| **Publicación** | Una oferta (mentoría o servicio) o un desafío que alguien publica |
| **Contratación** | La solicitud de un cliente sobre una publicación, con su ciclo de estados |
| **Franja horaria** | Un horario concreto en el que se puede dar la sesión |
| **Cupo** | Cuántas contrataciones admite una publicación |
| **Billetera / wallet** | El saldo de SkillCoins de un perfil y sus movimientos |
| **Política de cancelación** | La regla que decide cuánto se devuelve al cancelar |
| **Promoción** | Descuento que baja el precio (porcentual o monto fijo) |
| **Caso de uso** | Una operación completa del sistema (por ejemplo, "pagar una contratación") |
| **Puerto** | Una interfaz que define qué necesita una capa de otra |
| **Adapter** | Clase que traduce una interfaz externa a la que espera nuestro código |
