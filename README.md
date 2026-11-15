# SkillSwap

Marketplace de talento (mentorías, servicios y desafíos) que se paga con créditos ficticios, los SkillCoins. TPO de Proceso de Desarrollo de Software (UADE). Java + Hibernate/JPA + MySQL, sin Spring. El enunciado está en `consigna.docx`.

La interfaz Swing es la del Mock UI Kit de la cátedra. Este proyecto implementa los controllers reales detrás de las interfaces del kit y los conecta con los casos de uso y la persistencia.

## Requisitos

- JDK 21 y Maven 3.9 (el código compila con `release 17`).
- Docker y Docker Compose, solo para correr contra MySQL. Las pruebas por defecto usan H2 en memoria y no lo necesitan.

## Levantar MySQL

```bash
docker compose up -d          # MySQL 8.4 en localhost:3306 (base, usuario y clave: skillswap)
docker compose ps             # esperar a que figure "healthy"
docker compose down -v        # bajar y borrar el volumen
```

El esquema (`V1__esquema.sql`) y los datos de demostración (`V2__datos_demo.sql`) los aplica Flyway al iniciar la aplicación o las pruebas. Para apuntar a otra base: `SKILLSWAP_DB_URL`, `SKILLSWAP_DB_USER` y `SKILLSWAP_DB_PASSWORD` (por defecto `jdbc:mysql://localhost:3306/skillswap?serverTimezone=UTC`, `skillswap`, `skillswap`).

## Correr la aplicación

```bash
export JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64
mvn -q compile dependency:build-classpath -Dmdep.outputFile=target/cp.txt -Dmdep.includeScope=runtime
CP="target/classes:$(cat target/cp.txt)"

java -cp "$CP" ar.edu.uade.skillswap.SkillSwapApplication          # modo mock: datos en memoria del kit
java -cp "$CP" ar.edu.uade.skillswap.SkillSwapApplication --real   # modo real: casos de uso + JPA + MySQL (docker compose up -d antes)
```

Con `--screenshot archivo.png` en lugar de abrir la ventana guarda una captura y sale (sirve para probar sin pantalla, con `-Djava.awt.headless=true`). El usuario activo de la UI es `user-mateo`. Al cerrar la ventana, el modo real cierra el `EntityManagerFactory`.

## Correr las pruebas

```bash
mvn verify                                   # H2 en memoria (modo MySQL) + JaCoCo (falla si domain+application < 80 %)

# además, contra un MySQL real (docker compose up -d):
export SKILLSWAP_DB_URL='jdbc:mysql://localhost:3306/skillswap?serverTimezone=UTC'
mvn verify                                   # suma MySqlArranqueTest (Flyway + validate + lectura de la demo)

# las pruebas de persistencia que crean sus propios datos también pueden correr contra MySQL:
SKILLSWAP_TEST_DB_URL="$SKILLSWAP_DB_URL" SKILLSWAP_TEST_DB_USER=skillswap SKILLSWAP_TEST_DB_PASSWORD=skillswap \
  mvn test -Dtest='ConcurrenciaCupoTest,ContratacionJpaTest,RecorridoPrincipalTest'
```

Sin `SKILLSWAP_DB_URL`, `MySqlArranqueTest` se omite. El reporte de cobertura queda en `target/site/jacoco/index.html`. Cobertura de líneas en la última corrida: domain 99,2 %, application 99,2 %.

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

- **UI del kit.** No expone crear perfil ni calificar: esas operaciones existen en los servicios (`PerfilService.crearPerfil`, `CalificacionService.calificar`) y se prueban sin la UI. El botón "avanzar" de la UI elige el caso de uso según el estado y el rol (aceptar, pagar, iniciar, completar).
- **Modalidad HIBRIDA.** La UI la ofrece pero el dominio solo tiene remota y presencial (consigna 4.2); se rechaza con un error legible.
- **Franjas.** La UI no carga horarios: el borrador nace con tres franjas diarias desde la semana siguiente, y al solicitar se toma la primera libre.
- **Controllers.** Son los del kit, que devuelven valores y lanzan `ErrorDeAplicacion`; no hay un tipo `Resultado<T>`. Los comandos y DTO son `record`.
- **Mapeo JPA en el dominio.** Las entidades de dominio llevan anotaciones de `jakarta.persistence` (como en el material de la cátedra); no hay otro código de infraestructura en el dominio.
- **Reputación.** Se recalcula por evento (`ReputacionObserver`) fuera de la transacción de calificar, y no puede editarse manualmente.
- **Promociones.** El dominio y el cálculo de precio están probados (`PromocionDecoratorTest`), pero ningún controller crea promociones: no hay pantalla ni caso de uso que las cargue.
- **Notificaciones.** Los proveedores de email y push son mocks que escriben en consola; la bandeja interna sí se persiste.
- **Identidad.** `IdentityClientMock` conoce solo a los usuarios de la demostración; no hay login real (fuera de alcance).
- **Datos de demostración.** Son reproducibles (ids y fechas fijos, `V2__datos_demo.sql`). Algunos textos fijos de la ventana del kit (por ejemplo el recuento de proyectos del encabezado) no vienen de la base.
- **Sin repositorio git.** El proyecto todavía no está versionado.
