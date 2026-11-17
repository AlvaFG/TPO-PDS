# SkillSwap

Marketplace de talento (mentorías, servicios y desafíos) que se paga con créditos ficticios, los SkillCoins. TPO de Proceso de Desarrollo de Software (UADE). Java + Hibernate/JPA + MySQL, sin Spring. El enunciado está en `consigna.docx`.

La interfaz Swing es la del Mock UI Kit de la cátedra. Este proyecto implementa los controllers reales detrás de las interfaces del kit y los conecta con los casos de uso y la persistencia.

## Guía de estudio

La carpeta [`estudio/`](estudio/README.md) explica la aplicación para entenderla y defenderla: de qué trata, qué hay en cada carpeta, las capas, los 7 patrones (y cómo se relacionan con lo visto en clase), el recorrido de una contratación clase por clase, la base de datos y JPA, las librerías, las pruebas, por qué hay código Python, la consigna punto por punto y preguntas de defensa.

| Para saber… | Leer |
|---|---|
| De qué trata y para qué sirve | [`estudio/01-que-es-la-aplicacion.md`](estudio/01-que-es-la-aplicacion.md) |
| Dónde está cada cosa | [`estudio/02-mapa-de-carpetas.md`](estudio/02-mapa-de-carpetas.md) |
| Cómo funciona por dentro | [`estudio/03-arquitectura.md`](estudio/03-arquitectura.md), [`estudio/05-recorrido-de-punta-a-punta.md`](estudio/05-recorrido-de-punta-a-punta.md) |
| Qué patrones se usan | [`estudio/04-patrones-de-diseno.md`](estudio/04-patrones-de-diseno.md) |
| Cómo se conecta con MySQL | [`estudio/06-base-de-datos-y-jpa.md`](estudio/06-base-de-datos-y-jpa.md) |
| Para qué es el código Python | [`estudio/09-codigo-python-y-diagramas.md`](estudio/09-codigo-python-y-diagramas.md) |
| Qué preguntarán en la defensa | [`estudio/11-preguntas-de-defensa.md`](estudio/11-preguntas-de-defensa.md) |

## Requisitos

| Herramienta | Versión | Para qué |
|---|---|---|
| JDK | **21** (el código compila con `release 17`) | Compilar y ejecutar. **No usar Java 25**: con ese JDK la ventana no llega a abrirse (el hilo de la interfaz queda en un bucle) |
| Maven | 3.9 o superior | Compilar, ejecutar y correr las pruebas |
| Docker + Docker Compose | cualquiera reciente | Solo para el modo real con MySQL. El modo mock y las pruebas (H2 en memoria) no lo necesitan |

La aplicación es de escritorio (Swing): al arrancar abre su propia ventana, no hay nada para abrir en el navegador. `localhost:3306` es la base MySQL, no una página.

## Instalación

### Linux (Debian / Ubuntu)

```bash
sudo apt install -y openjdk-21-jdk maven
export JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64
```

Docker: https://docs.docker.com/engine/install/ (Docker Engine con el plugin `compose`).

### macOS

```bash
brew install openjdk@21 maven
export JAVA_HOME=$(/usr/libexec/java_home -v 21)
```

Si `java_home` no encuentra el JDK de Homebrew: `export JAVA_HOME="$(brew --prefix openjdk@21)/libexec/openjdk.jdk/Contents/Home"`. Docker: Docker Desktop.

### Windows (PowerShell)

```powershell
winget install EclipseAdoptium.Temurin.21.JDK
# Maven: descargarlo de https://maven.apache.org/download.cgi, descomprimirlo y agregar su carpeta bin al PATH
# (o, si se usa Chocolatey: choco install maven)
$env:JAVA_HOME = "C:\Program Files\Eclipse Adoptium\jdk-21.0.x-hotspot"   # ajustar a la carpeta instalada
```

Docker: Docker Desktop (con WSL 2). Para dejar `JAVA_HOME` fijo en Windows: `setx JAVA_HOME "<carpeta del JDK 21>"` y abrir una terminal nueva.

### Verificar

```bash
mvn -v        # la línea "Java version" tiene que decir 21 (no 25) y "runtime" apuntar al JDK 21
docker --version
```

Maven usa el JDK de `JAVA_HOME`. Si `mvn -v` muestra otra versión, corregir `JAVA_HOME` antes de seguir.

## Ejecutar la aplicación

Todos los comandos se ejecutan **desde la carpeta del proyecto** (la que tiene `pom.xml`) y son iguales en Linux, macOS y Windows.

### Modo mock (sin base de datos)

```bash
mvn -q compile exec:java
```

Abre la ventana "SkillSwap · Mock UI Kit" con los datos en memoria del kit de la cátedra.

### Modo real (casos de uso + JPA + MySQL)

```bash
docker compose up -d          # MySQL 8.4 en localhost:3306 (base, usuario y clave: skillswap)
docker compose ps             # esperar a que figure "healthy"
mvn -q compile exec:java "-Dexec.args=--real"
```

Al arrancar, Flyway aplica el esquema (`V1__esquema.sql`) y los datos de demostración (`V2__datos_demo.sql`) y Hibernate valida el esquema. El usuario activo de la UI es `user-mateo`. Al cerrar la ventana se cierra la conexión.

Para bajar la base y borrar sus datos (la próxima vez Flyway vuelve a cargar todo):

```bash
docker compose down -v
```

Para usar otra base: variables de entorno `SKILLSWAP_DB_URL`, `SKILLSWAP_DB_USER` y `SKILLSWAP_DB_PASSWORD` (por defecto `jdbc:mysql://localhost:3306/skillswap?serverTimezone=UTC`, `skillswap`, `skillswap`).

### Sin pantalla (captura de prueba)

```bash
mvn -q compile exec:java -Djava.awt.headless=true "-Dexec.args=--screenshot captura.png"
mvn -q compile exec:java -Djava.awt.headless=true "-Dexec.args=--real --screenshot captura.png"
```

Guarda una captura de la ventana y sale. Sirve para probar en un servidor o sin entorno gráfico.

### Problemas frecuentes

| Síntoma | Causa y solución |
|---|---|
| `no configuration file provided: not found` | `docker compose` se corrió fuera de la carpeta del proyecto. Entrar a la carpeta que tiene `docker-compose.yml` |
| No se abre ninguna ventana y la CPU queda al 100 % | Se está usando Java 25. Fijar `JAVA_HOME` al JDK 21 y verificar con `mvn -v` |
| `localhost:3306` en el navegador da `ERR_INVALID_HTTP_RESPONSE` | Es normal: ese puerto es MySQL, no HTTP. La interfaz es la ventana Swing |
| `Communications link failure` o conexión rechazada en modo real | La base no está lista: esperar a `healthy` en `docker compose ps`. Si el puerto 3306 está ocupado por otro MySQL, pararlo o cambiar el puerto en `docker-compose.yml` |
| `release version 17 not supported` o `javac` no encontrado | Falta el JDK completo (hay solo un JRE) o `JAVA_HOME` apunta a otro: instalar el JDK 21 |

## Correr las pruebas

```bash
mvn verify          # H2 en memoria (modo MySQL) + JaCoCo; falla si la cobertura de domain+application < 80 %
```

Para correr además contra un MySQL real (con `docker compose up -d`), definir las variables de entorno:

```bash
# Linux / macOS
export SKILLSWAP_DB_URL='jdbc:mysql://localhost:3306/skillswap?serverTimezone=UTC'
mvn verify          # suma MySqlArranqueTest (Flyway + validate + lectura de la demo)
```

```powershell
# Windows (PowerShell)
$env:SKILLSWAP_DB_URL = 'jdbc:mysql://localhost:3306/skillswap?serverTimezone=UTC'
mvn verify
```

Las pruebas de persistencia que crean sus propios datos también pueden correr contra MySQL con `SKILLSWAP_TEST_DB_URL`, `SKILLSWAP_TEST_DB_USER` y `SKILLSWAP_TEST_DB_PASSWORD` y `mvn test -Dtest="ConcurrenciaCupoTest,ContratacionJpaTest,RecorridoPrincipalTest"`. No apuntarlas a la base de la demo: escriben datos.

Sin `SKILLSWAP_DB_URL`, `MySqlArranqueTest` se omite. El reporte de cobertura queda en `target/site/jacoco/index.html`.

## Arquitectura

Cuatro capas con las dependencias hacia el dominio (`src/main/java/ar/edu/uade/skillswap`):

| Capa | Paquete | Contenido |
|---|---|---|
| Dominio | `domain` | Entidades, value objects (`SkillCoins`, `FranjaHoraria`, `RangoHorario`), estados, políticas, promociones y catálogo |
| Aplicación | `application` | Casos de uso (`service`), puertos de entrada (`I...Service`) y de salida (`port`), DTO y comandos, eventos |
| Infraestructura | `infrastructure` | Repositorios JPA, `JpaGestorTransacciones`, adaptadores mock de identidad, email y push |
| Presentación | `presentation` | Implementación de los controllers del kit (finos) y `EjecutorDeAcciones`, que valida el `userId` y traduce errores |

`ConfiguracionApp` es el Composition Root (sin Spring) y `SkillSwapApplication` el punto de entrada. El kit de la cátedra (`controllers`, `model`, `mock`, `ui`) se mantiene tal cual vino. `application` no importa `infrastructure` ni `presentation`, y `domain` no importa `application`; el dominio solo usa las anotaciones de mapeo de `jakarta.persistence`.

### Patrones (7)

| Patrón | Variación que resuelve | Dónde está |
|---|---|---|
| State | Transiciones de publicación y de contratación | `domain/estado/publicacion`, `domain/estado/contratacion` |
| Strategy | Recomendación (afinidad / popularidad) y política de cancelación (flexible / estricta) | `application/recomendacion`, `domain/politica` |
| Decorator | Promociones acumulables sin total negativo | `domain/precio` |
| Observer | Un evento notifica a varias partes y recalcula reputación | `application/evento` |
| Adapter | Identidad, email y push externos simulados | `infrastructure/adapter` |
| Simple Factory | Canal de notificación según la preferencia del usuario; reconstrucción de los estados persistidos | `CanalNotificacionFactory`, `EstadoContratacionFactory`, `EstadoPublicacionFactory` |
| Composite | Catálogo jerárquico de categorías y habilidades | `domain/catalogo` |

La concurrencia sobre el último cupo se resuelve con optimistic locking (`@Version`): el perdedor recibe `ConflictoConcurrenciaException`, que la UI muestra como mensaje legible. El detalle de cada decisión está en `informe.md` / `informe.pdf`, y el diagrama de clases en `skillswap.mdj` (imágenes en `img/`).

## Casos de aceptación (consigna, sección 15) y sus pruebas

Rutas bajo `src/test/java/ar/edu/uade/skillswap/`.

| # | Caso | Prueba |
|---|---|---|
| 1 | Publicar una mentoría válida y encontrarla con búsqueda filtrada | `application/BusquedaServiceTest.encuentraLaMentoriaConFiltrosCombinados`, `application/PublicacionServiceTest.publicarUnaMentoriaValida`, `infrastructure/ServiciosConJpaTest.publicarUnaMentoriaYEncontrarlaPorCategoriaPadre` |
| 2 | Rechazar la publicación de una oferta incompleta | `domain/PublicacionTest.rechazaPublicarUnaOfertaIncompleta`, `application/PublicacionServiceTest.rechazaPublicarUnaOfertaIncompleta`, `infrastructure/ServiciosConJpaTest.unaOfertaIncompletaNoSePublica` |
| 3 | Solicitar, aceptar y pagar con saldo suficiente | `application/ContratacionServiceTest.solicitarAceptarPagarYCompletar`, `infrastructure/ContratacionJpaTest.solicitarAceptarPagarCompletarYCalificarQuedaPersistido`, `infrastructure/RecorridoPrincipalTest` |
| 4 | Dos aceptaciones no consumen el último cupo | `application/ContratacionServiceTest.evitaQueDosAceptacionesConsumanElUltimoCupo`, `infrastructure/ConcurrenciaCupoTest` (dos hilos sobre JPA, probado también en MySQL) |
| 5 | Pago con saldo insuficiente sin tocar saldos ni estado | `domain/BilleteraTest.saldoInsuficienteNoModificaNada`, `application/ContratacionServiceTest.pagoConSaldoInsuficienteNoModificaSaldosNiEstado`, `infrastructure/ContratacionJpaTest.unPagoConSaldoInsuficienteNoModificaSaldosNiEstado` |
| 6 | Cancelar con dos políticas y calcular el reintegro | `domain/PoliticaCancelacionTest` (flexible y estricta), `application/ContratacionServiceTest.cancelarConDosPoliticasDistintasReintegraDistinto`, `infrastructure/ContratacionJpaTest.cancelarConAnticipacionReintegraLiberaElCupoYQuedaEnElHistorial` |
| 7 | Cambiar la estrategia de recomendación sin tocar el caso de uso | `application/RecomendacionServiceTest.cualquierEstrategiaSinTocarElServicio`, `infrastructure/ServiciosConJpaTest.laRecomendacionPorAfinidadUsaLosInteresesPersistidos` |
| 8 | Dos tipos de notificación ante un mismo evento (adaptadores mock) | `application/ObserverTest.unMismoEventoSaleporDosCanalesMock`, `infrastructure/ServiciosConJpaTest.unEventoSeNotificaPorDosCanales` |
| 9 | Promociones acumulables sin precio negativo | `domain/PromocionDecoratorTest.promocionesAcumulables`, `domain/PromocionDecoratorTest.elTotalNuncaEsNegativo` |
| 10 | Completar, calificar una sola vez y recalcular reputación | `application/CalificacionServiceTest.calificarRecalculaLaReputacionDelCalificado`, `application/CalificacionServiceTest.seCalificaUnaSolaVez`, `infrastructure/RecorridoPrincipalTest` |

El recorrido de la sección 13 (publicar, solicitar, aceptar, pagar, completar, calificar) lo ejecuta de punta a punta `infrastructure/RecorridoPrincipalTest`: controllers de la UI, casos de uso y JPA sobre H2 con las migraciones. Casos negativos adicionales: transición inválida (`domain/PublicacionTest`), doble pago (`ContratacionJpaTest.elDoblePagoLoRechazanElDominioYLaRestriccionUnique`), calificación no autorizada (`CalificacionServiceTest.calificacionNoAutorizada`) y conflicto de concurrencia traducido (`presentation/EjecutorDeAccionesTest`).

## Decisiones, supuestos y límites conocidos

- **UI del kit.** Se amplió el kit (`UiControllers`, `Models`, `SkillSwapView`) para exponer: rechazar, reprogramar (proponer y aceptar), calificar, elegir horario al solicitar, ver el comprobante con el detalle del precio, ver el perfil del proveedor, editar el perfil (nombre y biografía), editar un borrador, agregar promociones y elegir la política de cancelación. Sigue sin exponer crear perfil ni darse de baja (`PerfilService.crearPerfil`, `darDeBaja`): la ventana trabaja con un usuario fijo que ya existe. El botón principal de cada contratación se llama según el estado y el rol (Aceptar, Pagar, Iniciar, Completar) y muestra "Esperando a la contraparte" cuando no le toca actuar.
- **Modalidad HIBRIDA.** La UI la ofrece pero el dominio solo tiene remota y presencial (consigna 4.2); se rechaza con un error legible.
- **Franjas.** La UI no carga horarios: el borrador nace con tres franjas diarias desde la semana siguiente. Al solicitar se puede elegir una franja libre (por defecto, la primera).
- **Controllers.** Son los del kit, que devuelven valores y lanzan `ErrorDeAplicacion`; no hay un tipo `Resultado<T>`. Los comandos y DTO son `record`.
- **Mapeo JPA en el dominio.** Las entidades de dominio llevan anotaciones de `jakarta.persistence` (como en el material de la cátedra); no hay otro código de infraestructura en el dominio.
- **Reputación.** Se recalcula por evento (`ReputacionObserver`) fuera de la transacción de calificar, y no puede editarse manualmente.
- **Promociones.** El autor las agrega desde "Mis publicaciones" (`PublicacionService.agregarPromocion`: porcentual o monto fijo, vigente desde hoy durante N días) y se aplican al precio de las nuevas solicitudes; el comprobante muestra el detalle.
- **Notificaciones.** Los proveedores de email y push son mocks que escriben en consola; la bandeja interna sí se persiste.
- **HTML sin escapar en la vista del kit.** `ui/SkillSwapView.java` (código de la cátedra, sin modificar) arma etiquetas `<html>…` concatenando `titulo`, `descripcion` y `bio` sin escapar. Un texto con HTML se interpreta en el `JLabel` (por ejemplo, un `<img src="http://…">` haría un pedido de red al mostrarse) o rompe el formato de la pantalla. Swing no ejecuta scripts. No se corrigió para mantener el kit tal cual vino; la corrección sería escapar `& < >` en `Conversor` o en la vista.
- **Identidad.** `IdentityClientMock` conoce solo a los usuarios de la demostración; no hay login real (fuera de alcance).
- **Datos de demostración.** Son reproducibles (ids y fechas fijos, `V2__datos_demo.sql`). Algunos textos fijos de la ventana del kit (por ejemplo el recuento de proyectos del encabezado) no vienen de la base.
