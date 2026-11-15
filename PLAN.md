# TPO SkillSwap — Plan de trabajo

Consigna: `consigna.docx` (copia de `~/Descargas/0. TP_PDS_SkillSwap.docx`; el texto coincide con el de la raíz).
Presentación del TP: 27/10 y 3/11.

## Fase 0 — Preparación ✅
- [x] Crear `TPO/` y copiar la consigna.
- [x] Leer la consigna completa: secciones 1 a 16.

## Fase 1 — Relevar el material de la cátedra ✅
Objetivo: saber cómo dibuja y cómo programa la cátedra cada cosa que pide el TP, para copiar su estilo y no inventar.

| Bloque | Material | Qué sacar |
|---|---|---|
| Teoría | pptx de las clases 1, 2, 10, 11, 12 y 13, y los de `magiccafecleanstrategyadapter/` (2, 4 Arquitecturas, 9 Herencia JPA y Strategy, 10 Adapter) | Definiciones y estructura de cada patrón, capas y JPA |
| Diagramas | Todos los `.mdj` (ejercicios, clase 13, resolución del final 15) | Convenciones reales: paquetes, nombres, relaciones y estereotipos. Anti-ejemplos conocidos |
| Código | Magic Café en cada etapa (Strategy+Adapter → Composite → Decorator → State → Observer → Factory), Factory, netflix y Youtube | Cómo implementa la cátedra cada patrón en Clean Architecture con JPA |
| Exámenes | Finales modelo 14, 15 y 16, la resolución del 15 y los ejercicios con resolución (Garbarisi, Módulo Exportador) | Qué se espera al justificar los patrones, SOLID y GRASP |

Salida: `notas/relevamiento.md`, con una ficha por patrón (estructura de la cátedra, archivo de referencia y cómo se traslada a SkillSwap).

## Fase 2 — Análisis del dominio (Hito 1: alcance e historias) ✅
- Historias de usuario por módulo: perfiles, publicaciones, búsqueda, contratación, wallet, notificaciones y calificaciones.
- Modelo de dominio: entidades, value objects (`SkillCoins`, `FranjaHoraria`, `Etiqueta`…), invariantes de la sección 5 y estados de la 4.2 y la 4.5.
- Tabla **variación → patrón → GRASP/SOLID**: el TP pide que cada patrón resuelva una variación real, no que se acumulen.

Borrador inicial de la tabla (la versión final está en `informe.md` §6: se agregó Composite, se descartó Facade y la Strategy de orden pasó a ser un `ORDER BY`):

| Variación del enunciado | Patrón | GRASP / SOLID |
|---|---|---|
| Transiciones de Publicación (§4.2) y de Contratación (§4.5) | **State** (dos jerarquías) | Polymorphism, OCP |
| Recomendación intercambiable (§4.3), política de cancelación 100/50/0 % (§5) y orden de resultados | **Strategy** | Polymorphism, Protected Variations, OCP |
| Promociones acumulables sin total negativo (§5) | **Decorator** | OCP, SRP |
| Eventos que notifican a varias partes (§4.6) | **Observer** | Low Coupling, Indirection |
| Identidad externa y canales email/push/interno simulados (§4.6) | **Adapter** | Indirection, Protected Variations, DIP |
| Elegir el canal según la preferencia del usuario | **Simple Factory** | Creator, Pure Fabrication |
| Perfil público con reputación, publicaciones e historial (§4.1) | **Facade** (opcional) | Low Coupling |
| Categorías y habilidades jerárquicas | **Composite** (opcional, solo si se justifica) | Polymorphism |
| Persistencia intercambiable | Puertos de repositorio (Clean Architecture) | Pure Fabrication, DIP |

Singleton queda afuera: no hay una instancia única que lo justifique.

## Fase 3 — Diagrama de clases en StarUML ✅
Skill `staruml-mdj`, con un `spec.json` versionado junto al `.mdj`.
- Paquetes `Domain`, `Application` (casos de uso y puertos de entrada/salida), `Infrastructure` (repositorios JPA y adaptadores mock) y `Presentation` (controllers con interfaz `I…`).
- Convenciones de la cátedra: atributos `private`, operaciones `public` con tipos, asociaciones con verbo y multiplicidad en los dos extremos, y el cliente siempre asociado a la abstracción.
- Por el tamaño, se propone un diagrama `Main` completo y además vistas por módulo o patrón, para poder leerlo y defenderlo.
- Verificar con `revisar_mdj.py` sin errores y mostrar el Mermaid.

## Fase 4 — Documentación del Hito 1 ✅
- `respuestas.md` / `informe.md`: problema, alternativa elegida, consecuencias y evidencia, por decisión (§13). Supuestos explícitos.
- Recorrido de un caso de uso sobre el UML: publicar → solicitar → aceptar → pagar → completar → calificar.
- Informe en PDF (skill `pdf`).

## Fase 5 — Hitos 2 a 4: código Java ✅

Se parte de la consigna (§4 a §12 y los 10 casos de aceptación de §15) y del diseño ya cerrado en `informe.md` y `skillswap.mdj`. El código sigue lo visto en clase (Magic Café); lo único que se agrega es lo que la consigna exige y la cátedra no trae.

### Qué se usa y qué no
| Tema | Se usa | Se descarta |
|---|---|---|
| Patrones (7) | State (publicación y contratación), Strategy (recomendación y política de cancelación), Decorator (promociones), Observer (eventos), Adapter (identidad, email, push), Simple Factory (canal y reconstrucción de estados), Composite (categorías y habilidades) | Facade, Singleton, Abstract Factory, Factory Method y **Builder** (los comandos son clases simples o `record`) |
| Stack de la cátedra | Java 17, Hibernate 7.2.1, JUnit 6.1.0, Flyway 12.8.1, MySQL 9.7.0 (versiones del Mock UI Kit), `persistence.xml` RESOURCE_LOCAL, `EntityManager` por operación, `Main` como Composition Root, `IllegalStateException`/`IllegalArgumentException` en el dominio | Spring, Lombok, mappers, frameworks de inyección |
| Extras que pide la consigna | JUnit 5 y Mockito, H2 para tests de persistencia, MySQL para la demo (docker), Flyway, `@Version` (optimistic locking), JaCoCo | Liquibase, Testcontainers |
| Presentación | Implementar los contratos `UiControllers` del Mock UI Kit (`OneDrive_2_15-11-2026/0. TP_PDS_SkillSwap/skillswap`) como adaptadores finos sobre los casos de uso, y conectar su ventana Swing | Inventar controllers propios distintos de los del kit |
| Estructura | Un solo módulo Maven, `src/main/java/ar/edu/uade/skillswap/{domain,application,infrastructure,presentation}`, mismos nombres que el UML | Multi-módulo |

### Agentes: modelo y esfuerzo
| Paso | Quién | Modelo | Esfuerzo | Por qué |
|---|---|---|---|---|
| 0 | Usuario | — | — | Instalar `openjdk-21-jdk` y `maven` (hoy solo hay un JRE 25, sin `javac` ni `mvn`) |
| 1 | Hito 2: `pom.xml`, dominio, casos de uso, puertos y pruebas unitarias | opus | high | Es el núcleo y define los contratos de los que dependen los otros |
| 2a | Hito 3: entidades JPA, repositorios, migraciones Flyway (`V1` esquema con `UNIQUE` e índices, `V2` datos de demostración), `docker-compose.yml` con MySQL local, `@Version`, adaptadores mock, tests de persistencia con H2 y el caso de dos aceptaciones sobre el último cupo | opus | high | Mapeo del State con Hibernate 7, transacciones y concurrencia: es lo más fácil de hacer mal |
| 2b | Hito 4: adaptadores de `UiControllers` (traduce `avanzar` según el estado, ids `String`↔`Long`, `record` de la UI), Swing conectado y `Main` como Composition Root; pruebas con repositorios en memoria | sonnet | medium | Código delgado con contratos ya fijos. No toca la base, por eso no depende de 2a para probarse |
| 3 | Revisión final: integración sobre H2 y sobre MySQL real (docker), los 10 casos de aceptación, cobertura JaCoCo, README, UML e informe contra el código | sonnet | high | Verifica y corrige lo que 2a y 2b no encajen; sube a high porque es donde aparecen los errores de integración |

2a y 2b corren en paralelo una vez cerrado el paso 1 (usan carpetas distintas). Sin commits hasta que se pidan.

## Cómo regenerar
```bash
cd TPO
python3 diagrama/construir_spec.py                                   # modelo -> diagrama/spec.json
python3 diagrama/generar_mdj.py                                     # spec.json -> skillswap.mdj (layout ordenado)
python3 diagrama/revisar_mdj.py skillswap.mdj
staruml image skillswap.mdj -f png -o "img/<%=filenamify(element.name)%>.png"
python3 diagrama/generar_pdf.py                                      # informe.md -> informe.pdf (necesita el paquete markdown)
```

## Estructura de `TPO/`
```
TPO/
├── PLAN.md
├── consigna.docx
├── consigna/          imágenes de la consigna
├── notas/             relevamiento del material
├── diagrama/          construir_spec.py, spec.json, generar_pdf.py
├── img/               PNG de cada diagrama
├── skillswap.mdj
├── informe.md         → informe.pdf
└── src/               (hitos 2–4)
```
