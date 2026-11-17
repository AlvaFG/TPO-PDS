# 11. Preguntas de defensa

Preguntas probables, con **respuesta corta** y **dónde mirar**. La consigna dice que cualquier integrante tiene que poder explicar y modificar una parte, así que conviene poder responder todas, no solo las propias.

Las respuestas están pensadas para decirse en 20 a 40 segundos.

---

## Sobre qué es y cómo está armado

**¿De qué trata la aplicación?**
Un marketplace donde profesionales ofrecen mentorías o servicios y otros los contratan, pagando con créditos ficticios (SkillCoins). Hay publicaciones, contrataciones con estados, una billetera, notificaciones y calificaciones que forman la reputación. → [01](01-que-es-la-aplicacion.md)

**¿Qué hicieron ustedes y qué dio la cátedra?**
La cátedra dio la ventana Swing y las interfaces de los controllers (`UiControllers`). Nosotros hicimos todo lo de atrás: dominio, casos de uso, base de datos, adaptadores, los controllers reales y las pruebas. La ventana se conecta a nuestros controllers en lugar de a los datos mock. → [02](02-mapa-de-carpetas.md), [03](03-arquitectura.md)

**¿Cómo se organiza el código?**
En cuatro capas: dominio, aplicación, infraestructura y presentación. Las dependencias apuntan hacia el dominio, que no conoce a nadie. → [03](03-arquitectura.md)

**¿Qué es Clean Architecture y cómo se ve en su código?**
Que lo importante (las reglas del negocio) no dependa de lo cambiante (la base, la ventana, los proveedores). Se ve en que `domain` y `application` no importan nada de `infrastructure` ni de `presentation`: la aplicación define interfaces (puertos) y la infraestructura las implementa.

**¿Qué es un puerto y un adaptador?**
Un puerto es una interfaz que define lo que una capa necesita de afuera (por ejemplo `IContratacionRepository`). Un adaptador es la clase que lo implementa (`ContratacionJpaRepository`). → [03](03-arquitectura.md)

**¿Dónde se arma todo y por qué no usan Spring?**
En `ConfiguracionApp` (Composition Root), por constructor. La consigna prohíbe Spring Boot, así que la inyección de dependencias es manual: ese es el único lugar que conoce las clases concretas.

**¿Por qué las entidades del dominio tienen anotaciones JPA?**
Es lo que hace el material de la cátedra (Magic Café) y evita mantener clases duplicadas con un mapeo manual. Es una concesión conocida respecto de la consigna §6; el dominio solo usa las anotaciones, no hay SQL ni repositorios ahí.

---

## Sobre los patrones

**¿Qué patrones usaron y por qué?**
Siete: State (estados de publicación y contratación), Strategy (política de cancelación y recomendación), Decorator (promociones), Observer (eventos), Adapter (identidad, email, push), Simple Factory (canales y estados) y Composite (catálogo). Cada uno resuelve una variación concreta de la consigna. → [04](04-patrones-de-diseno.md)

**¿Cómo funciona State y por qué no un `if` por estado?**
Cada estado es una clase que define qué acciones permite. `Contratacion.pagar` hace `estado.pagar(...)`: si es `EstadoAceptada` paga, y si no, la clase base lanza "transición inválida". Con `if` habría que tocar cada método al agregar un estado; acá es una clase nueva (Open/Closed).

**¿Cómo se guarda un State en la base?**
Se guarda el nombre del estado en una columna (`nombre_estado`) y el objeto es `@Transient`. Al cargar la entidad, `@PostLoad` lo reconstruye con `EstadoContratacionFactory.desdeNombre(...)`.

**¿Por qué Strategy y no State para la política de cancelación?**
Porque no depende del estado del objeto: es un algoritmo **intercambiable** que elige el autor de la publicación. State cambia el comportamiento según el estado interno; Strategy elige entre algoritmos desde afuera.

**¿Cómo evitan que una promoción deje el precio negativo?**
`SkillCoins` no admite negativos y los descuentos usan `restarHastaCero`, que se frena en 0. Probado en `PromocionDecoratorTest.elTotalNuncaEsNegativo`.

**¿Por qué Decorator y no una lista de promociones que se recorre?**
Porque las promociones se **componen** y cada una se aplica sobre el resultado de la anterior en cualquier combinación; el decorador encadena eso sin que el cliente sepa cuántas hay. Se persisten las promociones (no los decoradores) y la contratación guarda una foto del precio.

**¿Cómo se agrega un tercer canal de notificación (por ejemplo SMS)?**
Una clase `SmsAdapter` que implemente `ICanalNotificacion`, un valor nuevo en el enum `CanalNotificacion` y un `case` en `CanalNotificacionFactory`. `application` no se toca.

**¿Y una nueva estrategia de recomendación?**
Una clase que implemente `IEstrategiaRecomendacion` y se pasa en `ConfiguracionApp.crear(puertos, estrategia)`. El servicio no cambia.

**¿Qué diferencia hay entre Adapter y Strategy?**
Adapter **traduce** una interfaz ajena a la que espera nuestro código (el proveedor no se puede modificar). Strategy **elige** entre algoritmos propios que cumplen la misma interfaz.

**¿Qué diferencia hay entre Observer y llamar directamente a los servicios?**
Con Observer el servicio de contratación no conoce a quién le interesa el evento; agregar una reacción nueva es una clase más y una línea de suscripción, sin modificarlo (Low Coupling, Open/Closed).

**¿Qué patrones descartaron?**
Facade (el caso de uso ya coordina), Singleton (no hay instancia única necesaria), Abstract Factory y Factory Method (no hay familias), Builder (los comandos son `record`) y una Strategy para el orden (alcanza un `ORDER BY`). → [04](04-patrones-de-diseno.md)

---

## Sobre la base de datos y las transacciones

**¿Cómo evitan vender dos veces el último cupo?**
Con optimistic locking: `Publicacion` tiene un campo `@Version`. Si dos aceptaciones leen la misma versión, la segunda falla al guardar con `OptimisticLockException`; se traduce a `ConflictoConcurrenciaException` y la ventana muestra "cupo agotado, reintente". Está probado con dos hilos en `ConcurrenciaCupoTest`. → [06](06-base-de-datos-y-jpa.md)

**¿Por qué optimista y no pesimista?**
Porque los conflictos son raros y no queremos bloquear filas por adelantado. Se detecta el conflicto al guardar y se reintenta.

**¿Cómo garantizan que el pago es atómico?**
`ContratacionService.pagar` corre dentro de `IGestorTransacciones.ejecutar`: el débito del cliente, el crédito del proveedor y el cambio de estado van en una sola transacción. Si falla algo, hay rollback y no cambia nada.

**¿Y el doble cobro?**
Lo frena el dominio (`Billetera.yaPago` y el estado: solo se paga en ACEPTADA) y, como segundo seguro, una restricción `UNIQUE (billetera_id, contratacion_id, tipo)` en la base.

**¿Por qué usan Flyway y `hbm2ddl=validate`?**
La consigna prohíbe depender de `ddl-auto=create`. Flyway versiona el esquema con scripts SQL reproducibles y Hibernate solo valida que las entidades coincidan con las tablas.

**¿Qué es el problema N+1 y cómo lo evitan?**
Una consulta que trae N filas y luego una consulta más por cada una para sus datos relacionados. Se evita con `JOIN FETCH` para las relaciones a uno y `batch_fetch_size = 50` para las colecciones; las consultas paginan en la propia consulta.

**¿Por qué `SINGLE_TABLE` para la herencia?**
Las jerarquías son chicas y con pocas columnas propias; evita joins. El costo son algunas columnas nulas. Es la estrategia que usa el material de la cátedra.

**¿Cómo se carga la base la primera vez?**
Al arrancar con `--real`, Flyway ejecuta `V1__esquema.sql` (tablas) y `V2__datos_demo.sql` (datos fijos de demostración).

**¿Qué pasa si cambio una entidad y no actualizo la base?**
La aplicación no arranca: con `hbm2ddl=validate` Hibernate detecta la diferencia y lo informa. Hay que agregar una migración nueva (`V3__…`).

---

## Sobre las reglas y los casos de uso

**¿Dónde está la regla de que el autor no puede solicitar su propia publicación?**
En el constructor de `Contratacion`: es una invariante del dominio, no del controller.

**¿Por qué las reglas están en las entidades y no en los servicios?**
Porque la entidad es la experta en sus datos (Information Expert): `Billetera` sabe si alcanza el saldo, `Publicacion` si hay cupo. Los servicios solo coordinan: buscar, llamar al dominio, guardar y publicar el evento.

**¿Por qué se publica el evento después del commit?**
Para no notificar operaciones que luego hicieron rollback.

**¿Cómo se calcula el reintegro al cancelar?**
Depende del estado: en ACEPTADA no hubo pago, solo se libera el cupo. En CONFIRMADA, si cancela el proveedor se devuelve el 100 %; si cancela el cliente, la política de la publicación decide 100/50/0 % según las horas de anticipación.

**¿Cómo se recalcula la reputación y por qué no se puede editar?**
Al registrarse una calificación se publica `CALIFICACION_REGISTRADA`; `ReputacionObserver` recalcula el promedio de las calificaciones recibidas. `Perfil` no tiene un setter público de la reputación.

**¿Qué hace el botón "avanzar" de la ventana?**
`ContratacionControllerImpl.avanzar` mira el estado y el rol y elige el caso de uso: PENDIENTE + proveedor → aceptar; ACEPTADA + cliente → pagar; CONFIRMADA + proveedor → iniciar; EN_CURSO + proveedor → completar. Cualquier otra combinación da un error legible.

**¿Cómo se validan los permisos?**
`EjecutorDeAcciones` valida que el `userId` exista y esté activo en el servicio de identidad; los servicios verifican el rol (por ejemplo, `delProveedor`, `esCliente`).

---

## Sobre las pruebas

**¿Cómo prueban sin base de datos?**
Los casos de uso se prueban con repositorios en memoria y Mockito, porque dependen de interfaces. Eso demuestra que la capa de aplicación no depende de JPA. La persistencia se prueba aparte, con H2.

**¿Qué cobertura tienen y alcanza?**
99,2 % en `domain` y `application` (el mínimo sugerido es 80 %). Pero lo importante son los casos negativos: transición inválida, saldo insuficiente, cupo agotado, doble pago y calificación no autorizada. → [08](08-pruebas.md)

**¿Cómo prueban la concurrencia?**
`ConcurrenciaCupoTest` lanza dos hilos que aceptan a la vez el último cupo y verifica que solo uno gana.

**¿Cómo se demuestra el recorrido principal?**
`RecorridoPrincipalTest` ejecuta publicar → solicitar → aceptar → pagar → completar → calificar usando los controllers de la ventana, los casos de uso y JPA sobre H2.

---

## Sobre lo que falta o puede mejorarse (preguntas incómodas)

**¿Qué no cumple la consigna?**
Dos cosas conocidas: las entidades del dominio llevan anotaciones JPA, y la ventana del kit no expone crear perfil ni darse de baja (están implementadas y probadas en los servicios). → [10](10-consigna-vs-implementacion.md)

**¿Por qué hay código Python?**
Son scripts de documentación que generan el diagrama de clases de StarUML y el PDF del informe. No son parte de la aplicación. → [09](09-codigo-python-y-diagramas.md)

**¿Qué haría si hubiera más tiempo?**
Escapar el HTML en la vista, agregar pantallas para calificar y reprogramar, sacar las anotaciones JPA del dominio con un mapeo `orm.xml` o entidades de persistencia separadas, y pruebas automáticas contra MySQL con Testcontainers.

**¿Es seguro el proyecto?**
El alcance excluye autenticación real y dinero real. Los accesos se validan contra un servicio de identidad simulado. La capa de persistencia usa JPA con parámetros (no concatena SQL). Un límite conocido: la vista del kit arma etiquetas HTML sin escapar el texto del usuario.

---

## Cómo defender el diseño en una frase

> "Separamos lo que cambia de lo que no: las reglas del negocio viven en el dominio sin depender de nada; cada variación real del problema (estados, políticas, promociones, canales, proveedores) tiene su patrón; y la base de datos y la ventana se conectan a través de interfaces, así que se pueden reemplazar sin tocar las reglas."
