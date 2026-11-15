# 6. Base de datos, JPA y transacciones

## El panorama

| Pieza | Qué es | Dónde está |
|---|---|---|
| **MySQL 8.4** | La base de datos | Contenedor Docker (`docker-compose.yml`), puerto `3306` |
| **JDBC driver** (`mysql-connector-j`) | Permite que Java hable con MySQL | Dependencia en el `pom.xml` |
| **Hibernate** | La implementación de JPA: convierte objetos Java en filas y viceversa | Dependencia `hibernate-core` |
| **JPA** | El estándar (anotaciones `@Entity`, `@Id`, `@OneToMany`…) | Anotaciones en las entidades de `domain` |
| **Flyway** | Crea y versiona el esquema con scripts SQL | `src/main/resources/db/migration/` |
| **H2** | Base en memoria, solo para pruebas | Dependencia de scope `test` |

**Flujo al arrancar el modo real:**

```
docker compose up -d   →  MySQL vacío con la base "skillswap" (usuario y clave: skillswap)
        │
mvn exec:java --real   →  PersistenceFactory.crear()
        │                      1) Flyway migra: ejecuta V1 (esquema) y V2 (datos de demo)
        │                      2) Hibernate abre la unidad "skillswap" y VALIDA que las
        │                         tablas coincidan con las entidades (hbm2ddl=validate)
        ▼
   la aplicación usa la base a través de los repositorios JPA
```

**Por qué `validate` y no `create`:** la consigna prohíbe depender de `ddl-auto=create`. El esquema lo manda **Flyway** (versionado, reproducible); Hibernate solo comprueba que no haya diferencias. Si una entidad y una tabla no coinciden, la aplicación **no arranca** y dice qué falta.

### Conexión

| Dato | Valor por defecto | Cómo cambiarlo |
|---|---|---|
| URL | `jdbc:mysql://localhost:3306/skillswap?serverTimezone=UTC` | `SKILLSWAP_DB_URL` |
| Usuario | `skillswap` | `SKILLSWAP_DB_USER` |
| Clave | `skillswap` | `SKILLSWAP_DB_PASSWORD` |

Está definida en `META-INF/persistence.xml` y en `PersistenceFactory`. El usuario `root` del contenedor tiene clave `root`.

## Las tablas (`V1__esquema.sql`)

19 tablas. Agrupadas por tema:

| Tema | Tablas |
|---|---|
| **Catálogo** | `elemento_catalogo` (categorías y habilidades, con `padre_id` y `tipo`) |
| **Políticas** | `politica_cancelacion` (flexible y estricta, con `tipo`) |
| **Perfiles** | `perfil`, `perfil_habilidad`, `perfil_interes`, `perfil_disponibilidad`, `perfil_canal` |
| **Publicaciones** | `publicacion`, `publicacion_habilidad`, `publicacion_etiqueta`, `franja_horaria`, `promocion`, `publicacion_historial` |
| **Contrataciones** | `contratacion`, `contratacion_historial` |
| **Dinero** | `billetera`, `movimiento_credito` |
| **Reputación** | `calificacion` |
| **Avisos** | `notificacion` |

### Relaciones principales

```
perfil 1──N publicacion 1──N franja_horaria
                  │  ╲── N──1 elemento_catalogo (categoría)
                  │  ╲── N──1 politica_cancelacion
                  1
                  │
                  N
perfil (cliente) N──1 contratacion N──1 perfil (proveedor)
                       │ N──1 franja_horaria
                       │ 1──N contratacion_historial
                       1
                       │
                       N  calificacion (autor, calificado)

perfil 1──1 billetera 1──N movimiento_credito
perfil 1──N notificacion
```

## Restricciones e índices (lo que la consigna §8 pide)

| Tipo | Qué | Para qué |
|---|---|---|
| `UNIQUE` | `perfil(user_id)` | Un perfil por usuario |
| `UNIQUE` | `billetera(titular_id)` | Una billetera por perfil |
| `UNIQUE` | `movimiento_credito(billetera_id, contratacion_id, tipo)` | **Doble cobro imposible**: ni con un bug se pueden registrar dos débitos de la misma contratación |
| `UNIQUE` | `calificacion(contratacion_id, autor_id)` | **Una calificación por participante** |
| Índice | `publicacion(nombre_estado, categoria_id)` | La búsqueda filtra por estado y categoría |
| Índice | `publicacion(precio_base)`, `publicacion(fecha_publicacion)` | Orden por precio y por fecha |
| Índice | `publicacion_habilidad(habilidad_id)` | Búsqueda por habilidad |
| Índice | `contratacion(cliente_id)`, `contratacion(proveedor_id)` | "Mis contrataciones" |
| Índice | `calificacion(calificado_id)` | Calcular reputación |
| Índice | `notificacion(destinatario_id, leida)` | Bandeja de entrada |

Además, `publicacion` tiene un `CHECK (cupos_ocupados >= 0 AND cupos_ocupados <= cupo)`: la base tampoco deja sobrepasar el cupo.

Las restricciones `UNIQUE` y el `CHECK` son un **segundo seguro**: la regla ya la valida el dominio, pero la base la garantiza aunque algo falle.

## Datos de demostración (`V2__datos_demo.sql`)

Reproducibles: ids y fechas fijos. Incluye el catálogo (categorías en tres niveles y habilidades), las dos políticas de cancelación, perfiles (`user-mateo`, `user-sofia`, `user-tomas`, `user-camila`, `user-nicolas`…), publicaciones en distintos estados, billeteras con saldo y movimientos coherentes. Los `userId` coinciden con los que conoce `IdentityClientMock`.

## JPA: cómo se mapean las ideas

### Herencia: `SINGLE_TABLE`

Tres jerarquías (como en la clase de herencia JPA): **todas las clases de la jerarquía van en una sola tabla**, con una columna discriminadora `tipo`.

| Jerarquía | Tabla | Valores de `tipo` |
|---|---|---|
| `ElementoCatalogo` → `Categoria`, `Habilidad` | `elemento_catalogo` | `CATEGORIA`, `HABILIDAD` |
| `PoliticaCancelacion` → `PoliticaFlexible`, `PoliticaEstricta` | `politica_cancelacion` | `FLEXIBLE`, `ESTRICTA` |
| `Promocion` → `PromocionPorcentual`, `PromocionMontoFijo` | `promocion` | `PORCENTUAL`, `MONTO_FIJO` |

| Estrategia | Ventaja | Desventaja |
|---|---|---|
| **`SINGLE_TABLE`** (la usada) | Rápida, sin joins | Columnas nulas |
| `JOINED` | Normalizada | Más joins |
| `TABLE_PER_CLASS` | Sin joins | Columnas duplicadas |

Se eligió `SINGLE_TABLE` porque las jerarquías son chicas y con pocas columnas propias.

### Embebidos y colecciones

| Anotación | Dónde | Qué hace |
|---|---|---|
| `@Embeddable` / `@Embedded` | `SkillCoins`, `RangoHorario`, `PropuestaReprogramacion` | El objeto se guarda **en las columnas de la tabla dueña**, sin tabla propia |
| `@ElementCollection` | Historiales (`CambioEstado`), habilidades, etiquetas | Colección de valores en una tabla auxiliar |
| `@OrderColumn` | `contratacion_historial` | Mantiene el orden; cada cambio nuevo es solo un `INSERT` |
| `@OneToMany(cascade = ALL, orphanRemoval = true)` | `Publicacion` → franjas y promociones | Las partes viven y mueren con el todo |
| `@ManyToOne(fetch = LAZY)` | `Contratacion` → publicación, cliente, proveedor, franja | Se carga solo cuando se usa |
| `@ManyToMany` | `Perfil` ↔ `Habilidad` (habilidades e intereses) | Tabla intermedia |

### El estado (State) en JPA

`Contratacion` guarda `nombreEstado` (texto) y tiene el objeto `estado` como `@Transient`. Al cargar, `@PostLoad` lo reconstruye con la factory. Ver [04](04-patrones-de-diseno.md#1-state--publicación-y-contratación).

### Nombres de columnas

`hibernate.physical_naming_strategy = CamelCaseToUnderscoresNamingStrategy`: el atributo `nombreEstado` es la columna `nombre_estado`.

## Transacciones

### `JpaGestorTransacciones`

Es el "límite transaccional" de cada caso de uso. Para cada operación:

1. Abre un `EntityManager` y una transacción.
2. Lo deja en un `ThreadLocal` para que **todos los repositorios que se usen dentro compartan el mismo**.
3. Ejecuta la operación y hace `flush()`.
4. **Commit** si todo salió bien; **rollback** y relanza el error si falló.
5. Cierra el `EntityManager`.

Si ya hay una transacción en el hilo, la operación **se une a ella** (no abre otra).

| Caso de uso | ¿Qué queda en una sola transacción? |
|---|---|
| `aceptar` | Reservar franja y cupo + cambiar el estado |
| `pagar` | Débito del cliente + crédito del proveedor + cambio de estado |
| `cancelar` | Reintegro + movimiento de billeteras + liberación del cupo + estado |
| `calificar` | Crear la calificación (la reputación se recalcula por evento) |

Si el débito falla (saldo insuficiente), **no se acredita nada**: la transacción hace rollback. El dominio además valida antes de modificar.

## Concurrencia: el último cupo (optimistic locking)

**El problema.** Una publicación tiene un solo cupo libre y dos solicitudes pendientes. Si las dos aceptaciones llegan casi a la vez, sin protección las dos leerían "cupos ocupados = 0" y las dos reservarían: **sobreventa**.

**La solución: `@Version`.** `Publicacion` (y `Contratacion` y `Billetera`) tienen un campo `version`. Hibernate agrega esa condición al `UPDATE`:

```sql
UPDATE publicacion SET cupos_ocupados = 1, version = 1 WHERE id = 7 AND version = 0
```

```
Operación A: lee publicación (version 0) ─┐
Operación B: lee publicación (version 0) ─┤
A: reserva, UPDATE ... WHERE version = 0  → 1 fila actualizada ✔ (version pasa a 1)
B: reserva, UPDATE ... WHERE version = 0  → 0 filas ✘ → OptimisticLockException
```

El perdedor falla. `JpaGestorTransacciones` hace rollback y traduce la excepción a `ConflictoConcurrenciaException`. `EjecutorDeAcciones` la muestra como *"Cupo agotado o publicación modificada por otra operación, reintente"*. Al reintentar, el dominio responde "cupo agotado".

*"Optimista"* porque no bloquea nada de antemano: asume que los conflictos son raros y los detecta al guardar.

**Prueba:** `ConcurrenciaCupoTest` lanza dos hilos sobre JPA (probado también contra MySQL real).

## Evitar el problema N+1

El **N+1** es: una consulta trae N filas y después se hace **una consulta más por cada fila** para traer sus datos relacionados.

Cómo se evita:

| Técnica | Dónde |
|---|---|
| `JOIN FETCH` para relaciones "a uno" (autor, categoría, cliente, proveedor, franja) | Consultas de `PublicacionJpaRepository` y `ContratacionJpaRepository` |
| `hibernate.default_batch_fetch_size = 50` para colecciones (etiquetas, habilidades) | `persistence.xml`: se cargan de a 50 con un `IN`, una consulta por colección para toda la página |
| Paginación en la consulta (`setFirstResult` / `setMaxResults`) + `COUNT` aparte | `buscar` |
| Colecciones `LAZY` por defecto | Todas las entidades (Magic Café usa `EAGER` para evitar `LazyInitializationException`; acá se resuelve con fetch plans) |
| DTOs armados con lo que trajo el fetch plan | Los servicios devuelven `record`, no entidades |

## Repositorios

Un repositorio por entidad en `infrastructure/persistence`, cada uno implementa su puerto de `application/port`. Todos usan `gestor.ejecutar(...)` para obtener el `EntityManager` de la transacción en curso. Ejemplo (`PublicacionJpaRepository.guardar`): si la publicación no tiene id hace `persist`; si ya tiene, `merge`.

## Cómo mirar la base mientras funciona

```bash
docker compose up -d
docker exec -it skillswap-mysql mysql -uskillswap -pskillswap skillswap
```
```sql
SHOW TABLES;
SELECT id, titulo, nombre_estado, cupo, cupos_ocupados, version FROM publicacion;
SELECT id, nombre_estado, precio_acordado FROM contratacion;
SELECT * FROM billetera;
SELECT * FROM movimiento_credito ORDER BY id DESC LIMIT 10;
SELECT * FROM flyway_schema_history;   -- qué migraciones se aplicaron
```

Para empezar de cero: `docker compose down -v` (borra el volumen) y volver a levantar: Flyway recarga todo.
