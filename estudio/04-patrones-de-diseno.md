# 4. Patrones de diseño

La consigna pide **al menos 4**, y dice que cada uno tiene que resolver **una variación real del problema**, no sumarse por cantidad. Acá hay **7**. Cada uno responde a una pregunta: *¿qué es lo que cambia en este problema y cómo lo aislamos?*

| Patrón | Tipo | Lo que varía | Dónde |
|---|---|---|---|
| **State** (×2) | Comportamiento | El comportamiento depende del estado de la publicación o de la contratación | `domain/estado/…` |
| **Strategy** (×2) | Comportamiento | El algoritmo de reintegro y el de recomendación | `domain/politica`, `application/recomendacion` |
| **Decorator** | Estructural | Las promociones que se acumulan sobre el precio | `domain/precio` |
| **Observer** | Comportamiento | Quién se entera cuando ocurre un evento | `application/evento` |
| **Adapter** | Estructural | El proveedor externo (identidad, email, push) | `infrastructure/adapter` |
| **Simple Factory** | Creacional | Qué canal o qué estado crear | `CanalNotificacionFactory`, `Estado…Factory` |
| **Composite** | Estructural | Un árbol de categorías y habilidades | `domain/catalogo` |

Los diagramas de cada uno están en `img/` (por ejemplo `Patron State - Contratacion.png`) y en `skillswap.mdj`.

## Cómo se relaciona con lo visto en clase

El proyecto de referencia de la cátedra es **Magic Café** (`com.uade.magiccafe`, también en Clean Architecture con JPA). Evoluciona patrón por patrón: Strategy + Adapter → Composite → Decorator → State → Observer → Factory. SkillSwap **copia el estilo de implementación** de cada etapa.

| Patrón | Cómo lo hace Magic Café | Cómo lo hace SkillSwap |
|---|---|---|
| **Strategy** | `DescuentoStrategy` abstracta; el contexto la recibe; entidad `SINGLE_TABLE` con `@DiscriminatorColumn` | `PoliticaCancelacion` abstracta, `SINGLE_TABLE` con discriminador `tipo`, y la contratación la ejecuta |
| **Adapter** | Target en `application/port`, adapter y adaptee en `infrastructure` | `IServicioIdentidad` (target), `IdentidadAdapter`, `IdentityClientMock` (adaptee) |
| **Composite** | `Vendible` (componente), `Producto` (hoja), `Combo` (compuesto), `SINGLE_TABLE` | `ElementoCatalogo`, `Habilidad` (hoja), `Categoria` (compuesto), `SINGLE_TABLE` |
| **Decorator** | `ItemVendible`, `VendibleDecorator`; no se persiste, se guarda una "foto" del precio | `ICalculoPrecio`, `PromocionDecorator`; la contratación guarda una foto (`precioAcordado`) |
| **State** | Interfaz + estado base que tira `IllegalStateException`; se guarda un `String` + `@Transient` rearmado en `@PostLoad` con una factory | Igual: `nombreEstado` + `@Transient estado` + `@PostLoad` + `EstadoContratacionFactory` |
| **Observer** | Sujeto, observers y un evento; observers concretos en infrastructure | `PublicadorEventos`, `IEventoObserver`, `EventoContratacion` |
| **Simple Factory** | `NotificationFactory` con `switch`; `EstadoPedidoFactory` | `CanalNotificacionFactory` y las dos `Estado…Factory` |

Las diferencias respecto de Magic Café son pocas y vienen de la consigna: el estado base lanza `ReglaNegocioException` (en vez de `IllegalStateException`) para que la ventana muestre un mensaje, y los observers viven en `application` (como en el ejemplo Netflix de la cátedra).

---

## 1. State — publicación y contratación

**Problema.** Una publicación y una contratación se comportan distinto según su estado. Sin patrón habría `if (estado == "PENDIENTE") … else if …` en cada método. La consigna pide además que las transiciones inválidas las rechace el dominio.

**Solución.** Cada estado es una **clase** que decide qué acciones permite.

| Rol | Clase (contratación) | Clase (publicación) |
|---|---|---|
| Interfaz de estado | `EstadoContratacion` | `EstadoPublicacion` |
| Estado base | `EstadoContratacionBase` (toda acción lanza error por defecto) | `EstadoPublicacionBase` |
| Estados concretos | `EstadoPendiente`, `EstadoAceptada`, `EstadoConfirmada`, `EstadoEnCurso`, `EstadoCompletada`, `EstadoRechazada`, `EstadoContratacionCancelada` | `EstadoBorrador`, `EstadoPublicada`, `EstadoPausada`, `EstadoCerrada`, `EstadoPublicacionCancelada` |
| **Contexto** | `Contratacion` | `Publicacion` |

**Cómo funciona.** `Contratacion.pagar(...)` hace `estado.pagar(this, ...)`. Si el estado es `EstadoAceptada`, ejecuta el pago y pasa a `Confirmada`. Si es cualquier otro, hereda el método de la base y **lanza** "Transición inválida: no se puede pagar una contratación en estado X". El método `cambiarEstado` es el único que cambia el estado y siempre agrega una entrada al **historial inmutable** (fecha, actor, motivo).

Ejemplo concreto (`EstadoPendiente`): sobrescribe solo `aceptar` (reserva el cupo y pasa a `Aceptada`) y `rechazar`. Todo lo demás queda prohibido sin escribir nada.

**Cómo se guarda en la base.** Un objeto de estado no se puede guardar. Se guarda su **nombre** (`nombre_estado`, una columna de texto) y, al cargar la entidad, `@PostLoad` llama a `EstadoContratacionFactory.desdeNombre(...)` para reconstruir el objeto. Por eso el estado es `@Transient`.

**Principios:** Polymorphism (GRASP), Open/Closed (agregar un estado = una clase nueva), Single Responsibility.

**Prueba:** `ContratacionTest`, `PublicacionTest`, `EstadoFactoryTest`.

---

## 2. Strategy — política de cancelación

**Problema.** Al cancelar se devuelve 100 %, 50 % o 0 % según cuánto falta para la sesión, y la regla **cambia según la publicación** (consigna §5 y caso de aceptación #6, con dos políticas).

**Solución.**

```
PoliticaCancelacion (abstracta)  ── calcularReintegro(pagado, inicio, cancelacion): SkillCoins
        ▲                    ▲
PoliticaFlexible        PoliticaEstricta
100 % si ≥ 24 h         nunca el 100 %
 50 % si ≥ 2 h          50 % si ≥ 72 h
  0 % si no              0 % si no
```

La política la **elige el autor** de la publicación y la **ejecuta** la contratación al cancelar. Los umbrales de horas son configurables (valores por defecto en las clases). Se persisten con `SINGLE_TABLE` y un discriminador (`FLEXIBLE` / `ESTRICTA`), igual que en clase.

**Prueba:** `PoliticaCancelacionTest` (ambas políticas, mismas horas, distinto reintegro).

---

## 3. Strategy — recomendación

**Problema.** Hay dos formas de recomendar y se tiene que poder **cambiar sin tocar el caso de uso** (caso de aceptación #7).

```
IEstrategiaRecomendacion ── recomendar(perfil, candidatas): List<Publicacion>
        ▲                                   ▲
AfinidadPorHabilidades              PopularidadReciente
puntúa por habilidades en común     ordena por contrataciones de los
con los intereses del perfil        últimos 30 días
```

`RecomendacionService` recibe la estrategia **por constructor**. `ConfiguracionApp.crear(puertos)` usa `PopularidadReciente` por defecto, y existe `crear(puertos, estrategia)` para pasar otra. El servicio no cambia. *"La selección ocurre fuera del contexto; la ejecución, dentro"* (frase de la cátedra).

**Prueba:** `RecomendacionServiceTest.cualquierEstrategiaSinTocarElServicio`.

---

## 4. Decorator — promociones acumulables

**Problema.** Se pueden aplicar varias promociones al mismo precio, en cualquier combinación, y **el total nunca puede ser negativo** (consigna §5, caso #9).

**Solución.** Cada promoción **envuelve** al cálculo anterior y le aplica su ajuste.

```
ICalculoPrecio  ── calcularPrecio(), getDescripcion()
   ▲                         ▲
Publicacion               PromocionDecorator (abstracto, guarda el "envuelto")
(componente concreto:        ▲                    ▲
 devuelve el precio base)  DescuentoPorcentual   DescuentoMontoFijo
```

`Publicacion.cotizar(fecha)` arma la cadena: parte de la propia publicación y, por cada promoción **vigente** en esa fecha, la envuelve en un decorador. Resultado: `DescuentoMontoFijo( DescuentoPorcentual( Publicacion ) )`. Al llamar `calcularPrecio()`, cada capa aplica su descuento sobre lo que devolvió la anterior.

**Por qué nunca es negativo:** `SkillCoins` no admite valores negativos, y los descuentos usan `restarHastaCero`, que se frena en 0.

**Detalle importante:** el decorador **no se guarda**. Lo que se persiste son las promociones (entidad `Promocion`, `SINGLE_TABLE`), que saben crear su decorador (`decorar(precio)`). La contratación guarda una **foto** del precio (`precioAcordado`) y un texto con el detalle, así que cambiar una promoción después **no altera** lo ya contratado.

**Limitación conocida:** el dominio y el cálculo están probados, pero ningún controller crea promociones (la ventana no tiene pantalla para eso).

**Prueba:** `PromocionDecoratorTest.promocionesAcumulables` y `elTotalNuncaEsNegativo`.

---

## 5. Observer — eventos y notificaciones

**Problema.** Cuando se acepta, se paga o se completa una contratación, **varias cosas** tienen que enterarse (notificar por distintos canales, recalcular reputación). Si el servicio llamara a cada una directamente, quedaría acoplado a todas y cada nueva reacción obligaría a modificarlo.

**Solución.**

```
PublicadorEventos (Sujeto) ── publicar(evento) → avisa a cada observer suscripto
        │
        ├── NotificacionObserver  → arma la notificación y la manda por cada canal que eligió el usuario
        └── ReputacionObserver    → ante CALIFICACION_REGISTRADA recalcula la reputación de las dos partes
```

- El **evento** es `EventoContratacion` (con un `TipoEvento`: `SOLICITUD_RECIBIDA`, `SOLICITUD_ACEPTADA`, `PAGO_CONFIRMADO`, `REPROGRAMACION`, `CANCELACION`, `SESION_COMPLETADA`, `CALIFICACION_REGISTRADA`).
- Los observers se **suscriben** en `ConfiguracionApp`. Para sumar una reacción nueva alcanza con otra clase y una línea de suscripción.
- El evento se publica **después del commit** de la transacción.

**Prueba:** `ObserverTest.unMismoEventoSaleporDosCanalesMock` (caso #8).

---

## 6. Adapter — identidad, email y push

**Problema.** El sistema depende de proveedores externos que **no controlamos** y cuyas interfaces no coinciden con las nuestras (un servicio de identidad, uno de email, uno de push). La consigna pide que sean reemplazables.

**Solución.** Un **Target** que define lo que *nuestra aplicación* necesita, y un **Adapter** que lo traduce a la API del proveedor (el **Adaptee**).

| Rol | Identidad | Email | Push |
|---|---|---|---|
| **Target** (en `application/port`) | `IServicioIdentidad` | `ICanalNotificacion` | `ICanalNotificacion` |
| **Adapter** (en `infrastructure/adapter`) | `IdentidadAdapter` | `EmailAdapter` | `PushAdapter` |
| **Adaptee** (el proveedor simulado) | `IdentityClientMock.fetchUser(...)` | `EmailClientMock.sendMail(to, subject, body)` | `PushClientMock` |

Ejemplo (`EmailAdapter`): nuestro código dice `enviar(notificacion)`; el adapter busca el email del destinatario en identidad y llama a `sendMail(email, asunto, cuerpo)`, que es lo que entiende el proveedor. Para usar un email real habría que escribir **otro adapter**, sin tocar `application`.

*"La aplicación define el contrato. Infraestructura resuelve la traducción"* (frase de la cátedra).

**Prueba:** `AdaptadoresTest`.

---

## 7. Simple Factory

**Problema.** Hay que **crear** el objeto correcto según un dato, sin que el cliente conozca las clases concretas.

Se usa en tres lugares:

| Factory | Entrada | Salida | Para qué |
|---|---|---|---|
| `CanalNotificacionFactory` | `CanalNotificacion` (EMAIL, PUSH, INTERNA) | `EmailAdapter`, `PushAdapter` o `CanalInterno` | Elegir el canal según la preferencia del usuario. `application` solo conoce la interfaz `ICanalNotificacionFactory` |
| `EstadoContratacionFactory` | nombre del estado (texto) | objeto `Estado…` | Reconstruir el estado al cargar de la base (`@PostLoad`) |
| `EstadoPublicacionFactory` | ídem | ídem | ídem |

Es un `switch` sobre un enum o texto. Agregar un canal toca **solo** la factory y el adapter nuevo.

**Pregunta frecuente:** *¿por qué es Simple Factory y no Factory Method o Abstract Factory?* Porque hay **una sola clase** que decide con un `switch` y no hay familias de objetos relacionados ni subclases creadoras. Sería excesivo.

---

## 8. Composite — catálogo de categorías y habilidades

**Problema.** Las categorías tienen subcategorías y habilidades (Tecnología → Desarrollo backend → Java). Buscar por "Tecnología" tiene que incluir todo lo que cuelga de ella.

```
ElementoCatalogo (componente)
   ▲                    ▲
Habilidad (hoja)     Categoria (compuesto, guarda una lista de hijos)
```

El cliente trata igual a una categoría que a una habilidad. `Categoria` implementa `contiene(elemento)` y `listarHabilidades()` **delegando en sus hijos**, y `listarSubcategoriaIds()` devuelve todos los ids del subárbol, que usa la búsqueda. `agregar` rechaza ciclos (una categoría no puede contenerse a sí misma).

En la base es una sola tabla `elemento_catalogo` con una columna discriminadora `tipo` y una **autorreferencia** `padre_id` (`SINGLE_TABLE`).

*"Composite es una decisión de dominio. No nace por Hibernate ni por Swing."* (cátedra)

**Prueba:** `CatalogoCompositeTest`.

---

## Patrones que se evaluaron y se descartaron

La consigna dice que sumar patrones sin necesidad resta. Se descartaron:

| Patrón | Por qué no |
|---|---|
| **Facade** | El perfil público y el pago ya los coordina un caso de uso (`PerfilService.verPerfil`, `ContratacionService.pagar`). Una Facade encima duplicaría al servicio sin ocultar otro subsistema |
| **Singleton** | No hay ninguna instancia única obligatoria: el Composition Root crea una sola instancia de cada servicio sin forzarlo, y un Singleton es estado global que complica las pruebas |
| **Strategy de orden** | Los cuatro órdenes (`CriterioOrden`) son un `ORDER BY` de la consulta: más simple y más eficiente que ordenar en memoria |
| **Abstract Factory / Factory Method** | No hay familias de objetos ni subclases creadoras; alcanza una Simple Factory |
| **Builder** | Los comandos son `record` con pocos campos; no hay construcción compleja por pasos |
| **Adapter de pasarela de SkillCoins** | Los SkillCoins son créditos internos y no hay proveedor de pagos externo que adaptar |

## Cómo relacionar un patrón con un caso de aceptación

| Caso de aceptación (consigna §15) | Patrón que lo sostiene |
|---|---|
| #4 Dos aceptaciones, un solo cupo | (No es patrón) optimistic locking con `@Version` |
| #6 Cancelar con dos políticas | Strategy (cancelación) |
| #7 Cambiar la recomendación sin tocar el caso de uso | Strategy (recomendación) |
| #8 Dos tipos de notificación ante un evento | Observer + Adapter + Simple Factory |
| #9 Promociones acumulables sin precio negativo | Decorator |
| #3, #5, #10 Flujo de contratación | State |
