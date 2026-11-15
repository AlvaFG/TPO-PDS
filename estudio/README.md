# Guía de estudio de SkillSwap

Esta carpeta explica la aplicación para poder **entenderla, ubicarse en el código y defenderla**. No es parte del programa: es material para leer.

Se lee en orden. Cada archivo se entiende solo, pero el orden va de lo general a lo técnico.

| # | Archivo | Qué responde |
|---|---|---|
| 1 | [01-que-es-la-aplicacion.md](01-que-es-la-aplicacion.md) | ¿De qué trata? ¿Para qué sirve? ¿Qué pide la consigna? ¿Cómo se usa? |
| 2 | [02-mapa-de-carpetas.md](02-mapa-de-carpetas.md) | ¿Qué hay en cada carpeta y en cada paquete? ¿Dónde está cada cosa? |
| 3 | [03-arquitectura.md](03-arquitectura.md) | Las 4 capas, quién depende de quién y cómo arranca todo |
| 4 | [04-patrones-de-diseno.md](04-patrones-de-diseno.md) | Los 7 patrones: qué problema resuelven, dónde están y cómo se relacionan con lo visto en clase |
| 5 | [05-recorrido-de-punta-a-punta.md](05-recorrido-de-punta-a-punta.md) | Qué pasa, clase por clase, cuando se aprieta un botón |
| 6 | [06-base-de-datos-y-jpa.md](06-base-de-datos-y-jpa.md) | MySQL, tablas, Flyway, JPA, transacciones y el cupo concurrente |
| 7 | [07-librerias-y-herramientas.md](07-librerias-y-herramientas.md) | Cada librería del `pom.xml` y para qué está |
| 8 | [08-pruebas.md](08-pruebas.md) | Qué se prueba, dónde y cómo se corren |
| 9 | [09-codigo-python-y-diagramas.md](09-codigo-python-y-diagramas.md) | **Por qué hay Python** en el repo y qué es cada archivo |
| 10 | [10-consigna-vs-implementacion.md](10-consigna-vs-implementacion.md) | Cada punto de la consigna y dónde se cumple; lo que quedó afuera |
| 11 | [11-preguntas-de-defensa.md](11-preguntas-de-defensa.md) | Preguntas probables de la defensa, con respuesta corta y dónde mirar |

## Si hay poco tiempo

1. Leer el 1 (qué es) y el 3 (capas).
2. Leer el 5 (recorrido): es el flujo que se muestra en la demo.
3. Leer el 4 (patrones) y el 11 (preguntas).

## Resumen en diez líneas

- SkillSwap es un **marketplace de talento**: profesionales ofrecen mentorías o servicios, otros los contratan, y se paga con **SkillCoins** (créditos ficticios, no hay dinero real).
- Es una aplicación **de escritorio** (ventana Swing) hecha en **Java 17**, con **Hibernate/JPA** y **MySQL**, **sin Spring**.
- La ventana (la interfaz) la dio la cátedra (el "Mock UI Kit"). Lo que se construyó es todo lo que hay detrás: reglas de negocio, casos de uso, base de datos y controllers.
- El código está en **4 capas** (Clean Architecture): `domain`, `application`, `infrastructure` y `presentation`.
- Usa **7 patrones de diseño**: State, Strategy, Decorator, Observer, Adapter, Simple Factory y Composite.
- La base se crea con **Flyway** (migraciones SQL) y el último cupo de una publicación se protege con **optimistic locking** (`@Version`).
- Hay **222 pruebas automáticas** y una cobertura de 99,2 % en `domain` y `application`.
- Se puede ejecutar en **modo mock** (datos en memoria, sin base) o en **modo real** (`--real`, con MySQL en Docker).
- Los archivos `.py` **no son parte de la aplicación**: generan el diagrama de clases para StarUML y el PDF del informe.
- El diseño completo y sus justificaciones están en `informe.md` / `informe.pdf`; el diagrama, en `skillswap.mdj` y `img/`.

> Los números de líneas, clases y pruebas son los del momento de escribir esta guía. Si el código cambia, el comando `mvn verify` da los valores actuales.
