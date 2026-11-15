# 12. Guion de demo (unos 8 minutos)

La consigna (§13) pide una **demostración en vivo del recorrido principal**: publicar → solicitar → aceptar → pagar → completar → calificar.

## Lo que hay que saber antes (limitación de la ventana)

La ventana del kit **solo actúa como `user-mateo`** (está fijo en el código). Por eso:

| Se puede mostrar en la ventana | Solo se puede mostrar con la prueba |
|---|---|
| Buscar, filtrar, ver detalle y solicitar | Aceptar y pagar **entre dos personas distintas** (hace falta cambiar de usuario) |
| Aceptar una solicitud recibida (Mateo es proveedor de la #3) | Calificar (la ventana no tiene botón) |
| Cancelar con reintegro, errores legibles, billetera, notificaciones | Dos políticas de cancelación, último cupo con dos hilos |
| Crear un borrador y publicarlo | Oferta incompleta (la ventana siempre completa los datos) |

Por eso la demo tiene **dos partes**: la ventana (partes A a E) y el recorrido completo con `RecorridoPrincipalTest` (parte F). Decirlo en voz alta al empezar es mejor que que lo descubra el jurado.

## Preparación (10 minutos antes)

```bash
cd TPO
mvn -v                                  # tiene que decir Java 21 (no 25)
docker compose down -v                  # borra datos de una demo anterior
docker compose up -d && docker compose ps   # esperar "healthy"
mvn -q compile exec:java "-Dexec.args=--real"   # abre la ventana; dejar la terminal visible
```

- **Dejar la terminal a la vista:** ahí salen los emails y pushes simulados (`[email] …`, `[push] …`).
- Tener abiertos `skillswap.mdj` (o `img/Main.png`) e `informe.pdf`.
- Si algo sale mal a mitad de la demo: `docker compose down -v`, volver a levantar y reabrir. Los datos vuelven a su estado inicial.

## Datos de partida (de `V2__datos_demo.sql`)

| Dato | Valor |
|---|---|
| Usuario de la ventana | **Mateo Silva** (`user-mateo`), saldo **2160** SkillCoins |
| Contratación #1 | Mateo (cliente) con Tomás: **CONFIRMADA**, pagó 120 |
| Contratación #2 | Mateo con Laura: **COMPLETADA** y ya calificada |
| Contratación #3 | **Sofía** pidió la mentoría de Mateo: **PENDIENTE** (Mateo la puede aceptar) |
| Notificaciones | 2 sin leer (el contador dice "2 NUEVAS") |
| Promoción | `LANZAMIENTO10`: 10 % de descuento en "Preparación para entrevistas técnicas" (180 → 162) |
| Políticas | Flexible (100 % con ≥24 h, 50 % con ≥2 h) y Estricta (nunca 100 %, 50 % con ≥72 h) |

---

## Parte A — Qué es y cómo está armado (1 min)

1. Mostrar `img/Main.png` (o el `.mdj`): *"Cuatro capas: Presentation, Application, Domain e Infrastructure. Las dependencias apuntan al dominio."*
2. Decir: *"La ventana es del kit de la cátedra; nosotros implementamos los controllers reales, los casos de uso, las reglas y la persistencia en MySQL."*

## Parte B — Buscar y encontrar (1 min) · caso de aceptación #1

1. En **Inicio**, mostrar "Recomendado para vos" (estrategia de recomendación) y las categorías.
2. Ir a **Explorar**, escribir `Java` y apretar **Buscar**.
3. Cambiar la categoría a *Tecnología* (incluye las subcategorías: **Composite**) y el orden a *Menor precio*.
4. Decir: *"La búsqueda filtra, ordena y pagina en la consulta SQL, con índices; no en memoria."*

## Parte C — Solicitar y ver el precio con promoción (1 min)

1. En Explorar, abrir **Preparación para entrevistas técnicas** → **Ver detalle**.
2. Apretar **Solicitar mentoría**. Aparece "¡Solicitud enviada!" y se abre **Contrataciones** con la nueva fila en **PENDIENTE**.
3. Decir: *"La contratación guarda una foto del precio con la promoción aplicada: 180 con 10 % es 162 (Decorator). Si la promoción cambia después, no afecta lo ya contratado."*

**Error legible (caso negativo):** en esa fila apretar **Avanzar estado**. Mateo es el cliente y la solicitud está pendiente, así que sale un diálogo: *"No hay una acción disponible para el cliente con la contratación en estado PENDIENTE"*. Decir: *"El dominio rechaza la transición inválida; la ventana solo muestra el mensaje."*

> Opcional: en **Explorar**, abrir una publicación propia de Mateo (por ejemplo "Mentoría de Java inicial") y solicitarla: *"El autor no puede solicitar su propia publicación."*

## Parte D — Aceptar y notificar (2 min) · casos #3 y #8

1. En **Contrataciones**, buscar la solicitud de **Sofía** sobre "Mentoría de Java inicial" (estado PENDIENTE).
2. Apretar **Avanzar estado**. Pasa a **ACEPTADA**.
3. Mirar la **terminal**: aparece la notificación de Sofía por **push** (`[push] …`) — su canal es *interna + push*.
4. Decir: *"Aceptar reservó la franja y descontó un cupo en la misma transacción. El evento se publicó después del commit y llegó a dos observers."*
5. Explicar con `img/Patron Observer.png` y `img/Patron Adapter y Simple Factory.png`: *"La factory elige el canal según la preferencia del usuario; cada canal es un Adapter sobre un proveedor simulado y reemplazable."*

## Parte E — Cancelar con reintegro y mirar la billetera (1,5 min) · caso #6

1. Ir a **Contrataciones** y cancelar la contratación **#1 (Tomás, CONFIRMADA)** con **Cancelar**.
2. Ir a **SkillCoins** (botón de arriba a la derecha): el saldo pasó de **2160 a 2280**, con un movimiento de **+120 "Reintegro"**.
3. En la terminal aparece `[email] para …`: Mateo eligió **email e interna**.
4. Ir a **Notificaciones**: llegó el aviso de cancelación. Probar **Marcar todas como leídas**.
5. Decir: *"Como cancela el cliente con mucha anticipación, la política Flexible devuelve el 100 %. El reintegro sale de la billetera del proveedor y entra a la del cliente en una sola transacción."*

(Opcional, para el 5.º punto: **+ Acreditar demo** suma 500 créditos.)

## Parte F — El recorrido completo con la prueba (2 min) · casos #3, #4, #5, #10

Cerrar la ventana y, en la terminal, ejecutar:

```bash
mvn test -Dtest=RecorridoPrincipalTest
```

Decir mientras corre: *"Esta prueba hace el recorrido completo de la consigna con dos usuarios distintos, pasando por los controllers de la ventana, los casos de uso y JPA con las migraciones: crear perfiles, publicar, buscar, solicitar, aceptar, pagar, iniciar, completar y calificar. Calificar la hace por servicio porque la ventana del kit no tiene ese botón."*

Mostrar `Tests run: 1, Failures: 0`. Si hay tiempo, mostrar las otras pruebas clave:

| Caso | Comando | Qué demuestra |
|---|---|---|
| #4 último cupo | `mvn test -Dtest=ConcurrenciaCupoTest` | Dos hilos aceptan a la vez: solo uno gana (optimistic locking `@Version`) |
| #5 saldo insuficiente | `mvn test -Dtest=BilleteraTest` | No cambia ni saldo ni estado |
| #6 dos políticas | `mvn test -Dtest=PoliticaCancelacionTest` | Mismas horas, distinto reintegro |
| #2 oferta incompleta | `mvn test -Dtest=PublicacionTest` | El dominio rechaza publicar sin datos |
| Todo | `mvn verify` | 222 pruebas, cobertura ≥ 80 % (obtenida: 99,2 %) |

---

## Cierre (30 segundos)

*"Usamos siete patrones, cada uno por una variación real: State para los ciclos de vida, Strategy para reintegro y recomendación, Decorator para promociones, Observer para eventos, Adapter para los proveedores, Simple Factory para los canales y Composite para el catálogo. Las reglas viven en el dominio, la base se versiona con Flyway y el último cupo se protege con optimistic locking."*

## Cómo cubrir los 10 casos de aceptación

| # | Caso | Dónde se muestra |
|---|---|---|
| 1 | Publicar y encontrar con filtros | Parte B (ventana) |
| 2 | Rechazar oferta incompleta | `PublicacionTest` (parte F) |
| 3 | Solicitar, aceptar y pagar | Partes C y D (ventana) + `RecorridoPrincipalTest` para el pago |
| 4 | Dos aceptaciones, un cupo | `ConcurrenciaCupoTest` |
| 5 | Saldo insuficiente | `BilleteraTest` |
| 6 | Cancelar con dos políticas | Parte E (ventana, una política) + `PoliticaCancelacionTest` (las dos) |
| 7 | Cambiar la estrategia de recomendación | `RecomendacionServiceTest` (el servicio no cambia) |
| 8 | Dos tipos de notificación | Partes D y E (consola) |
| 9 | Promociones acumulables sin negativo | Parte C (precio) + `PromocionDecoratorTest` |
| 10 | Completar, calificar una vez y reputación | `RecorridoPrincipalTest` |

## Problemas posibles durante la demo

| Pasa… | Hacer |
|---|---|
| La ventana no abre y la CPU sube al 100 % | Se está usando Java 25: `export JAVA_HOME=…21…` y reabrir |
| Error de conexión al abrir en modo real | La base no está lista: esperar "healthy" en `docker compose ps` |
| Los datos ya no coinciden con este guion | `docker compose down -v`, levantar y reabrir |
| Un diálogo de error aparece donde no se esperaba | Es el manejador de errores de la ventana; la terminal tiene el detalle |
| Preguntan por qué no se puede pagar en la ventana | Responder con la limitación de la sección inicial (la ventana tiene un usuario fijo) |

## Si preguntan lo incómodo

Ver [11-preguntas-de-defensa.md](11-preguntas-de-defensa.md) y, en [10](10-consigna-vs-implementacion.md), la tabla de límites conocidos.
