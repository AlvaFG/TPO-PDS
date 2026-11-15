# 7. Librerías y herramientas

Todo lo que usa el proyecto está declarado en `pom.xml` (el archivo de **Maven**, la herramienta que descarga las librerías, compila y corre las pruebas). Las versiones son las que usa el kit de la cátedra.

## Lenguaje y compilación

| Qué | Versión | Para qué |
|---|---|---|
| **Java (JDK)** | Se ejecuta con **21**; el código se compila con `release 17` | Lenguaje. Se pide JDK 21 para compilar, y el bytecode es compatible con 17 (la versión del material de la cátedra). **No usar Java 25**: la ventana no abre (ver README) |
| **Maven** | 3.9 o superior | Descarga dependencias, compila, corre pruebas y arranca la aplicación (`mvn …`) |
| **Docker + Compose** | cualquiera reciente | Levanta MySQL en un contenedor. Solo para el modo real |

## Dependencias de la aplicación (`<dependencies>`)

| Librería | Versión | Alcance | Para qué sirve | Dónde se nota |
|---|---|---|---|---|
| **hibernate-core** | 7.2.1.Final | compilación | **Implementación de JPA**: convierte objetos en filas y arma las consultas (JPQL) | `infrastructure/persistence`, `persistence.xml` |
| **jakarta.persistence-api** | 3.2.0 | compilación | Las **anotaciones y la API de JPA** (`@Entity`, `EntityManager`, …) | Entidades de `domain` y repositorios |
| **flyway-core** | 12.8.1 | compilación | Ejecuta las **migraciones SQL versionadas** | `PersistenceFactory.migrar` |
| **flyway-mysql** | 12.8.1 | compilación | Soporte específico de MySQL para Flyway | (lo usa Flyway internamente) |
| **mysql-connector-j** | 9.7.0 | runtime | **Driver JDBC**: el "cable" entre Java y MySQL | Solo se necesita al ejecutar, no al compilar |

Tres ideas para explicar esto:

- **JPA es el estándar; Hibernate es la implementación.** El código usa anotaciones de `jakarta.persistence`; Hibernate hace el trabajo. Se podría cambiar Hibernate por otra implementación sin tocar las entidades.
- **JDBC es más bajo nivel.** Hibernate usa JDBC por debajo, a través del driver de MySQL.
- **Flyway ≠ Hibernate.** Flyway crea y versiona las tablas; Hibernate las usa y valida.

## Dependencias de prueba (scope `test`)

| Librería | Versión | Para qué |
|---|---|---|
| **junit-jupiter** | 6.1.0 | Framework de pruebas (`@Test`, `assertEquals`, `assertThrows`…) |
| **mockito-core** | 5.20.0 | Crear **dobles** de interfaces (repositorios falsos) para probar casos de uso sin base |
| **h2** | 2.4.240 | Base **en memoria**, compatible con MySQL, para probar persistencia sin Docker |

## Plugins de Maven (`<build>`)

| Plugin | Qué hace | Cuándo |
|---|---|---|
| **maven-compiler-plugin** | Compila con `release 17` | `mvn compile` |
| **maven-surefire-plugin** | Ejecuta las pruebas | `mvn test` |
| **jacoco-maven-plugin** | Mide la **cobertura** de pruebas y **falla el build** si `domain`+`application` bajan del 80 % | `mvn verify`; reporte en `target/site/jacoco/index.html` |
| **exec-maven-plugin** | Ejecuta `SkillSwapApplication` desde Maven (`mvn exec:java`), igual en cualquier sistema operativo | Arranque |

## Qué NO se usa (y por qué)

| Herramienta | Por qué no |
|---|---|
| **Spring / Spring Boot** | La consigna lo prohíbe. Por eso el armado de objetos es **manual** (`ConfiguracionApp`) |
| **Lombok** | Evita magia; los `record` de Java ya reducen el código repetitivo |
| **MapStruct u otros mappers** | La conversión es poca y está en una clase (`Conversor`) |
| **Liquibase** | La consigna admite Flyway **o** Liquibase; se eligió Flyway |
| **Testcontainers** | Las pruebas usan H2; contra MySQL real se prueban con variables de entorno |

## Herramientas del repositorio (no son librerías Java)

| Herramienta | Para qué |
|---|---|
| **StarUML** | Abrir y editar el diagrama de clases (`skillswap.mdj`). La consigna pide el UML en StarUML |
| **Python 3** | Solo para scripts que generan el diagrama y el PDF; **no se necesita para ejecutar la aplicación** (ver [09](09-codigo-python-y-diagramas.md)) |
| **Git** | Control de versiones. Repositorio público `TPO-PDS` |

## Swing: la ventana

La ventana usa **Swing**, que viene incluido en el JDK (paquete `javax.swing`), por eso no figura en el `pom.xml`. Es una tecnología de escritorio: no hay navegador ni servidor web. El código de la ventana es de la cátedra (`ui/`).

## Cómo se descargan las librerías

La primera vez que se corre `mvn`, Maven las baja de **Maven Central** y las guarda en `~/.m2/repository`. Por eso el primer arranque tarda y necesita internet. Los `.jar` no están en el repositorio.

## Qué hace cada comando

| Comando | Qué hace |
|---|---|
| `mvn -q compile exec:java` | Compila y abre la ventana en **modo mock** |
| `mvn -q compile exec:java "-Dexec.args=--real"` | Abre la ventana en **modo real** (necesita MySQL) |
| `mvn test` | Corre todas las pruebas |
| `mvn verify` | Pruebas + cobertura + chequeo del 80 % |
| `mvn clean` | Borra `target/` |
| `docker compose up -d` / `down -v` | Levanta / baja y borra la base |
