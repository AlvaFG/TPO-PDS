# Relevamiento del material de la cátedra

Resumen de lo que enseña y cómo resuelve la cátedra cada tema que pide el TP. Se usó para diseñar SkillSwap con el mismo estilo.

## 1. Arquitectura

Fuente: `magiccafecleanstrategyadapter/4. PDS Clase 4 Arquitecturas de Software.pptx`.

- **Capas (S5):**
  - PRESENTATION: pantallas, controllers, DTOs de entrada.
  - APPLICATION: casos de uso, orquestación, **transacciones**.
  - DOMAIN: reglas, entidades, políticas.
  - DATA/INFRA: repositorios, SQL, APIs.
- **Clean / Hexagonal (S6):** las dependencias apuntan hacia adentro, los puertos son contratos y los adaptadores son DB, API y UI. La fortaleza es la testabilidad.
- **CQRS (S9):**
  - El *command side* valida reglas, cambia el agregado y persiste.
  - El *query side* no cambia estado y devuelve DTOs o proyecciones optimizadas.
- **Frases de la cátedra:**
  - "Application no importa clases de infrastructure".
  - "La aplicación define el contrato. Infraestructura resuelve la traducción" (Adapter S14).
  - "El dominio define contratos; la infraestructura implementa integraciones" (Clase 12 S12).
  - "Composite es una decisión de dominio. No nace por Hibernate ni por Swing" (Clase 10 S28).
- **Composition Root:** `Main` cablea todo por constructor, porque no se usa Spring (Adapter S17). En Netflix lo hace `AppFactory`.
- **Controller vs Facade** (Clase 13 S23–S24):
  - Los dos ocultan complejidad.
  - Facade simplifica un subsistema y no está atado a la UI.
  - El Controller recibe eventos externos y coordina casos de uso.

## 2. Cómo dibuja la cátedra (resolución del final 15 y `.mdj`)

- **Diagrama `Main` con 4 paquetes:** `Presentation`, `Application`, `Infrastructure` y `Domain`.
- **Nombres:**
  - Clases y miembros en español sin tildes.
  - Interfaces con prefijo `I` en los diseños por capas (`ISuscripcionService`, `ISuscripcionRepository`, `INotificador`, `IPaymentGatewayProAdapter`).
- **Inyección:** el colaborador se modela como atributo de tipo interfaz **y** como asociación, con nombre verbal y multiplicidad (`+usa`, `+accede a`, `+envia por`).
- **Colecciones de estrategias:** agregación `1 ◇ 1..*` desde el servicio (`+posee`, `+contiene`).
- **Casos de uso:**
  - `I<Entidad>Service` (puerto de entrada) con su implementación `<Entidad>Service`.
  - DTOs `<Accion>Dto`.
  - El controller depende de los DTO con líneas punteadas.
- **Repositorio:** `I<Entidad>Repository` en Application y la implementación en Infrastructure.
- **Anti-ejemplos de la resolución del 15 (no repetir):**
  - Clases dibujadas dentro de un paquete pero que en el explorador pertenecen a otro.
  - `IMedioPago` duplicada.
  - Faltan multiplicidades.
  - **El dominio depende de interfaces de Application**: las estrategias quedaron en Application y `Suscripcion` las referencia.
  - No hay State: el estado queda como String.
- **Garbarisi y Exportador:**
  - Las constantes de las políticas se modelan como atributos con valor por defecto.
  - El Adapter se usa **solo** donde el proveedor puede cambiar (PDFBox sí, POI no).
  - Simple Factory con un `enum` de selección.

## 3. Cómo programa la cátedra cada patrón (Magic Café)

Versión más completa: `13. PDS Clase 13 magiccafefactory/`.

| Patrón | Estructura de la cátedra | Persistencia |
|---|---|---|
| **Strategy** | `DescuentoStrategy` abstracta con `aplicar(subtotal)` (SinDescuento, Estudiante, Hechizo). `Pedido` es el contexto y la recibe por constructor. "La selección ocurre fuera de Pedido; la ejecución, dentro." | Entidad `SINGLE_TABLE` + `@DiscriminatorColumn`, con `@ManyToOne` desde Pedido |
| **Adapter** | Target `ProcesadorPago` en `application/port`, Adapter `PasarelaPagoAdapter` y Adaptee `PasarelaPagoLegacy` en `infrastructure/payment/...`. Es un adapter de objeto, con el Adaptee inyectado | — |
| **Composite** | `Vendible` (componente abstracto), `Producto` (hoja), `Combo` (compuesto) y `ComboItem` con cantidad | `SINGLE_TABLE`; `@OneToMany(mappedBy, cascade=ALL, orphanRemoval)` |
| **Decorator** | `ItemVendible` (interfaz); `VendibleDecorator` abstracto guarda el componente envuelto; `ConLeche`/`ConCrema` suman al precio. `Vendible` implementa `ItemVendible` | **No se persiste**: `ItemPedido` guarda una foto (precioUnitario y extras) |
| **State** | Interfaz `EstadoPedido`; `EstadoPedidoBase` abstracta lanza `IllegalStateException` en cada acción y cada estado sobrescribe solo lo válido; `pedido.cambiarEstado(new EstadoPagado())` | Se persiste un `String` más un `@Transient EstadoPedido`, que se reconstruye en `@PostLoad` con `EstadoPedidoFactory.desdeNombre()` (Simple Factory) |
| **Observer** | `PedidoSubject` (agregar, quitar y notificar), `PedidoObserver.actualizar(evento)` y `PedidoEstadoCambiadoEvent`. Los observers concretos están en `infrastructure/notification`. En Netflix el publisher (`PlaybackEventPublisher`) está en application | Lista de observers `@Transient`, que se reengancha al cargar |
| **Simple Factory** | `NotificationFactory.crearNotificacion(tipo)` con switch; `EstadoPedidoFactory` | — |
| **Abstract Factory** | `MenuMagicoFactory` y sus familias | — |
| **Facade** | Solo slides (`HomeTheaterFacade`) | — |

- **Repositorios:** `JpaRepository<T,ID>` como puerto y `SimpleJpaRepository` en infraestructura, que abre un EntityManager por operación y hace commit o rollback.
- **Stack:** Hibernate 6.5.2, mysql-connector-j 9.0.0, Java 17, `persistence.xml` RESOURCE_LOCAL. **Sin tests** en ningún proyecto.
- **Errores de dominio:** `IllegalArgumentException` / `IllegalStateException`.

## 4. JPA

Fuente: Clase 9 Herencia.

| Estrategia | Ventajas | Desventajas |
|---|---|---|
| `SINGLE_TABLE` | Rápida | Columnas nulas |
| `JOINED` | Normalizada | Más joins |
| `TABLE_PER_CLASS` | Sin joins | Columnas duplicadas |

- **Error frecuente:** `LazyInitializationException`. La cátedra lo resuelve con EAGER. En el TP se piden fetch plans, así que conviene `JOIN FETCH` o proyecciones.
- **Transacciones y locking:** no tienen material propio. El TP pide optimistic locking (`@Version`) y Flyway o Liquibase.

## 5. GRASP y SOLID

No tienen presentación propia; aparecen como justificación de los patrones:

- **OCP:** Strategy, State, Observer, Factory y Composite.
- **DIP:** Observer y Factory.
- **Polymorphism y Protected Variations:** State.
- **Low Coupling y High Cohesion:** Observer y Factory.

La resolución del final 15 nombra Controller, Information Expert, Polymorphism, Low Coupling, High Cohesion, Indirection, Protected Variations, Pure Fabrication (servicios y repositorios) y Creator.

## 6. Errores del material

- Clase 13 S4: Simple Factory y Factory Method aparecen cruzados.
- Clase 12 S5: repite los beneficios de State.
- Clase 2 S21: dice que una interfaz "solo tiene métodos abstractos", lo que ya no es cierto desde Java 8.
- La numeración de las clases no coincide con el cronograma.
