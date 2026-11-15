# 2. Mapa de carpetas

Cómo leer este archivo: primero la vista de la raíz, después cada paquete Java. La regla para ubicarse es: **"¿qué pregunta responde esta carpeta?"**.

## Raíz del proyecto (`TPO/`)

```
TPO/
├── README.md              Cómo instalar y ejecutar (Linux, macOS, Windows)
├── estudio/               Esta guía de estudio
├── PLAN.md                Plan de trabajo por fases y decisiones de modelos/esfuerzo
├── informe.md / .pdf      Informe de decisiones de diseño (lo que pide la consigna)
├── consigna.docx          El enunciado original de la cátedra
├── consigna/              Imagen extraída de la consigna
├── skillswap.mdj          Diagrama de clases (se abre con StarUML)
├── img/                   13 imágenes PNG de ese diagrama (completo, por capa y por patrón)
├── notas/relevamiento.md  Apuntes de cómo enseña y programa la cátedra cada tema
├── diagrama/              Scripts Python que generan el diagrama y el PDF (no son la app)
├── pom.xml                Configuración de Maven: librerías, versiones y plugins
├── docker-compose.yml     Levanta una base MySQL local en un contenedor
├── .gitignore             Qué no se sube a Git (target/, .idea/, etc.)
└── src/
    ├── main/java/…        El código de la aplicación
    ├── main/resources/    Configuración de JPA y scripts SQL (migraciones)
    └── test/              Las pruebas automáticas
```

| Carpeta o archivo | Se corre o se lee | Notas |
|---|---|---|
| `src/` | Se compila y se ejecuta | Es **la aplicación** |
| `pom.xml` | Lo lee Maven | Define las dependencias |
| `docker-compose.yml` | Lo lee Docker | Solo para el modo real |
| `diagrama/` | Se ejecuta a mano, solo para regenerar documentos | Ver [09](09-codigo-python-y-diagramas.md) |
| `informe.*`, `skillswap.mdj`, `img/` | Se leen o se entregan | Documentación del diseño |
| `target/` | Lo crea Maven al compilar | Está ignorada por Git; se puede borrar |

## El código Java: `src/main/java/ar/edu/uade/skillswap/`

El paquete raíz `ar.edu.uade.skillswap` es el mismo que usa el kit de la cátedra. Adentro hay **cuatro capas nuestras** y **cuatro carpetas del kit**.

```
skillswap/
├── SkillSwapApplication.java     ← main(): arranca la aplicación
├── ConfiguracionApp.java         ← Composition Root: arma todos los objetos
│
├── domain/                       CAPA 1 · las reglas del negocio
├── application/                  CAPA 2 · los casos de uso
├── infrastructure/               CAPA 3 · base de datos y proveedores externos
├── presentation/                 CAPA 4 · puente entre la ventana y los casos de uso
│
├── controllers/UiControllers.java    ┐
├── model/Models.java                 │ Del kit de la cátedra.
├── mock/MockSkillSwapControllers.java│ No se modifican.
└── ui/SkillSwapView.java, Theme.java ┘
```

### `domain/` — las reglas (no sabe nada de la interfaz ni de la base)

| Paquete | Qué contiene | Clases principales |
|---|---|---|
| `domain/model` | Las entidades y valores del negocio | `Perfil`, `Publicacion`, `Contratacion`, `Billetera`, `Calificacion`, `Notificacion`, `FranjaHoraria`, `SkillCoins` |
| `domain/estado/publicacion` | **State** de la publicación | `EstadoPublicacion` (interfaz), `EstadoPublicacionBase`, `EstadoBorrador`, `EstadoPublicada`, `EstadoPausada`, `EstadoCerrada`, `EstadoPublicacionCancelada`, `EstadoPublicacionFactory` |
| `domain/estado/contratacion` | **State** de la contratación | `EstadoContratacion`, `EstadoContratacionBase`, `EstadoPendiente`, `EstadoAceptada`, `EstadoConfirmada`, `EstadoEnCurso`, `EstadoCompletada`, `EstadoRechazada`, `EstadoContratacionCancelada`, `EstadoContratacionFactory` |
| `domain/politica` | **Strategy** de cancelación | `PoliticaCancelacion` (abstracta), `PoliticaFlexible`, `PoliticaEstricta` |
| `domain/precio` | **Decorator** de promociones | `ICalculoPrecio`, `PromocionDecorator`, `DescuentoPorcentual`, `DescuentoMontoFijo`, `Promocion`, `PromocionPorcentual`, `PromocionMontoFijo` |
| `domain/catalogo` | **Composite** de categorías y habilidades | `ElementoCatalogo`, `Categoria`, `Habilidad` |
| `domain/exception` | Error del negocio | `ReglaNegocioException` |

Dentro de `domain/model` además hay enumerados (`Modalidad`, `TipoPublicacion`, `TipoPerfil`, `TipoEvento`, `TipoMovimiento`, `CanalNotificacion`), valores de apoyo (`CambioEstado`, `RangoHorario`, `PropuestaReprogramacion`, `MovimientoCredito`) y el evento `EventoContratacion`.

### `application/` — los casos de uso

| Paquete | Qué contiene | Clases principales |
|---|---|---|
| `application/service` | **Casos de uso**: una interfaz `I…Service` (lo que el controller usa) y su implementación | `ContratacionService`, `PublicacionService`, `BusquedaService`, `RecomendacionService`, `PerfilService`, `BilleteraService`, `CalificacionService`, `NotificacionService` |
| `application/port` | **Puertos de salida**: lo que la aplicación necesita del mundo exterior, como interfaces | `IContratacionRepository`, `IPublicacionRepository`, `IPerfilRepository`, `IBilleteraRepository`, `IGestorTransacciones`, `IServicioIdentidad`, `ICanalNotificacion`, `ICanalNotificacionFactory` y otros |
| `application/dto` | Datos de entrada y salida (son `record`) | Comandos: `SolicitarCommand`, `PublicacionCommand`, `CalificarCommand`. Salidas: `ContratacionDto`, `PublicacionDto`, `Pagina` |
| `application/evento` | **Observer**: publicador y observadores | `PublicadorEventos`, `NotificacionObserver`, `ReputacionObserver` |
| `application/recomendacion` | **Strategy** de recomendación | `IEstrategiaRecomendacion`, `AfinidadPorHabilidades`, `PopularidadReciente` |
| `application/exception` | Errores de aplicación | `AccesoDenegadoException`, `RecursoNoEncontradoException`, `ConflictoConcurrenciaException` |

### `infrastructure/` — la conexión con el mundo real

| Paquete | Qué contiene | Clases principales |
|---|---|---|
| `infrastructure/persistence` | Todo lo que habla con la base **vía JPA** | `PersistenceFactory`, `JpaGestorTransacciones` y un repositorio por entidad: `PublicacionJpaRepository`, `ContratacionJpaRepository`, `PerfilJpaRepository`, `BilleteraJpaRepository`… |
| `infrastructure/adapter` | **Adapter** y **Simple Factory** de canales | `IdentidadAdapter`, `EmailAdapter`, `PushAdapter`, `CanalInterno`, `CanalNotificacionFactory` y los clientes simulados `IdentityClientMock`, `EmailClientMock`, `PushClientMock` |

### `presentation/` — el traductor de la ventana

| Clase | Qué hace |
|---|---|
| `SkillSwapControllers` | Agrupa los 6 controllers y es lo que se le entrega a la ventana |
| `BusquedaControllerImpl`, `PublicacionControllerImpl`, `ContratacionControllerImpl`, `WalletControllerImpl`, `NotificacionControllerImpl`, `PerfilControllerImpl` | Implementan las interfaces del kit; cada método llama a un caso de uso |
| `EjecutorDeAcciones` | Valida el `userId` con identidad y convierte los errores de negocio en `ErrorDeAplicacion` |
| `Conversor` | Convierte DTOs de la aplicación a los `record` que entiende la ventana (`ids String↔Long`) |
| `ErrorDeAplicacion`, `ModalidadUi` | Error que muestra la ventana y traducción de la modalidad |

### Las carpetas del kit (no modificar)

| Carpeta | Contenido |
|---|---|
| `controllers/UiControllers.java` | **Las interfaces** que la ventana usa (`BusquedaController`, `ContratacionController`, …). Es el contrato |
| `model/Models.java` | Los `record` que la ventana muestra (`Publicacion`, `Contratacion`, `Perfil`, …) |
| `mock/MockSkillSwapControllers.java` | Una implementación falsa con datos en memoria (el "modo mock") |
| `ui/SkillSwapView.java`, `ui/Theme.java` | La ventana y su estilo visual |

> **Ojo con los nombres repetidos.** `model.Models.Publicacion` (la vista, un `record` simple) y `domain.model.Publicacion` (la entidad del negocio) **no son lo mismo**. `Conversor` pasa de una a la otra.

## Recursos: `src/main/resources/`

| Archivo | Para qué |
|---|---|
| `META-INF/persistence.xml` | Configura JPA/Hibernate: qué clases se persisten, el driver, `hbm2ddl=validate`, `batch_fetch_size=50` |
| `db/migration/V1__esquema.sql` | Crea todas las tablas, claves, restricciones únicas e índices |
| `db/migration/V2__datos_demo.sql` | Datos de demostración fijos (usuarios, publicaciones, saldos) |

Flyway ejecuta estos scripts **en orden de versión** al arrancar el modo real.

## Pruebas: `src/test/java/ar/edu/uade/skillswap/`

| Carpeta | Qué prueba | Usa base de datos |
|---|---|---|
| `domain/` | Reglas, estados, políticas, promociones, value objects | No |
| `application/` | Casos de uso con repositorios simulados (Mockito) | No |
| `infrastructure/` | Repositorios JPA, transacciones, concurrencia, recorrido completo | **H2** en memoria (y MySQL real si se configura) |
| `presentation/` | Los controllers y el traductor de errores | No |
| `soporte/` | Clases auxiliares para armar datos de prueba | — |

## Cómo encontrar algo rápido

| Quiero encontrar… | Ir a |
|---|---|
| Dónde se valida que el saldo alcance | `domain/model/Billetera.java` (`debitar`) |
| Dónde se decide cuánto se devuelve al cancelar | `domain/politica/PoliticaFlexible.java` y `PoliticaEstricta.java` |
| Dónde se evita vender dos veces el último cupo | `domain/model/Publicacion.java` (`@Version`, `reservarCupo`) y `infrastructure/persistence/JpaGestorTransacciones.java` |
| Qué pasa al apretar "avanzar" | `presentation/ContratacionControllerImpl.java` (`avanzar`) |
| Dónde se envía un email simulado | `infrastructure/adapter/EmailAdapter.java` y `EmailClientMock.java` |
| Dónde se conecta con MySQL | `infrastructure/persistence/PersistenceFactory.java` y `META-INF/persistence.xml` |
| Dónde se arma todo | `ConfiguracionApp.java` y `SkillSwapApplication.java` |
| Cómo se busca y se pagina | `infrastructure/persistence/PublicacionJpaRepository.java` (`buscar`) |
| Qué tablas hay | `src/main/resources/db/migration/V1__esquema.sql` |
