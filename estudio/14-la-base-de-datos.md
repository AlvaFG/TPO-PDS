# 14. La base de datos: cómo y dónde está conectada, y por qué es persistente

## 1. Qué base usa y dónde está

SkillSwap usa **MySQL 8.4**, que corre en un contenedor **Docker** en la misma máquina. Se define en `docker-compose.yml` (raíz del proyecto):

| Dato | Valor |
|---|---|
| Imagen | `mysql:8.4` (contenedor `skillswap-mysql`) |
| Host y puerto | `localhost:3306` |
| Base | `skillswap` |
| Usuario / clave | `skillswap` / `skillswap` (root: `root`) |
| URL JDBC | `jdbc:mysql://localhost:3306/skillswap?serverTimezone=UTC` |
| Volumen | `skillswap-datos`, montado en `/var/lib/mysql` |

Dos aclaraciones:

- `localhost:3306` **no es una página web**: es el puerto de MySQL. La aplicación es de escritorio (Swing) y abre su propia ventana.
- Se puede apuntar a otra base con las variables de entorno `SKILLSWAP_DB_URL`, `SKILLSWAP_DB_USER` y `SKILLSWAP_DB_PASSWORD`.

## 2. Cómo se conecta (de afuera hacia adentro)

```
SkillSwapApplication --real
   └─▶ PersistenceFactory.crear()
          ├─ 1. migrar(...)  ──▶ Flyway aplica db/migration (V1 esquema, V2 datos demo)
          └─ 2. abrir(...)   ──▶ Persistence.createEntityManagerFactory("skillswap")
                                   └─ Hibernate (hbm2ddl = validate) ──▶ driver JDBC ──▶ MySQL
```

Paso a paso, con rutas relativas a `src/main/java/ar/edu/uade/skillswap/`:

1. **Arranque.** `SkillSwapApplication.java` (línea 51): con el parámetro `--real` llama a `PersistenceFactory.crear()`. Sin `--real` usa datos en memoria (mock) y no toca ninguna base.
2. **Configuración.** `infrastructure/persistence/PersistenceFactory.java` toma la URL, el usuario y la clave de las variables de entorno y, si no están, usa los valores por defecto de docker-compose (líneas 19 a 21 y 27 a 31).
3. **Migración.** `migrar(...)` (líneas 40 a 46) corre **Flyway** sobre `classpath:db/migration`. Crea las tablas si no existen (`V1__esquema.sql`) y carga los datos de demo (`V2__datos_demo.sql`). Si ya se aplicaron, no las repite.
4. **Persistencia.** `abrir(...)` (líneas 49 a 56) crea el `EntityManagerFactory` de la unidad `skillswap`, definida en `src/main/resources/META-INF/persistence.xml`. Ahí están las 21 entidades listadas, el driver `com.mysql.cj.jdbc.Driver` y `hibernate.hbm2ddl.auto=validate`: Hibernate **solo verifica** que las tablas coincidan con las entidades y no crea ni modifica nada.
5. **Transacciones.** `infrastructure/persistence/JpaGestorTransacciones.java` (líneas 31 a 59): por cada operación de un caso de uso abre un `EntityManager` y una transacción. Si todo sale bien hace `commit`; si falla, `rollback` y relanza la excepción.
6. **Repositorios.** Clases como `PublicacionJpaRepository` usan ese `EntityManager` para consultar y guardar. La capa de aplicación solo conoce las interfaces (`IPublicacionRepository`, `IGestorTransacciones`), nunca JPA: las dependencias apuntan hacia el dominio.
7. **Cierre.** Al cerrar la ventana se llama a `emf.close()` y se libera la conexión.

## 3. Por qué es persistente

Los datos **sobreviven al cierre de la aplicación y de los contenedores** por tres razones encadenadas:

1. **Se guardan en MySQL, no en la memoria de Java.** Cada operación de escritura termina en un `commit` (`JpaGestorTransacciones`). Una vez confirmado, el dato está en el disco de MySQL; que se cierre la ventana no lo afecta.
2. **MySQL escribe en un volumen de Docker.** `skillswap-datos` está montado en `/var/lib/mysql`, la carpeta donde MySQL guarda sus archivos. Aunque el contenedor se pare (`docker compose down`) o se recree, el volumen queda y los datos siguen ahí.
3. **Flyway no pisa lo que ya existe.** Registra qué migraciones aplicó (tabla `flyway_schema_history`). En el segundo arranque ve que V1 y V2 ya están aplicadas y no hace nada, así que lo que se creó desde la UI no se pierde.

Lo único que borra los datos es pedirlo explícitamente:

```bash
docker compose down -v     # -v borra el volumen: la próxima vez Flyway recarga esquema + demo
```

### Qué NO es persistente

- **El modo mock** (sin `--real`): los datos viven en memoria y se pierden al cerrar.
- **Las pruebas** (`mvn verify`): usan **H2 en memoria en modo MySQL**, con las mismas migraciones. Cada corrida arranca de cero. Por eso las pruebas no necesitan Docker.

## 4. Qué garantiza la base además de guardar

- **Esquema versionado:** cualquier cambio futuro es una migración nueva (V3, V4…) y no una edición a mano.
- **Integridad:** claves foráneas, `CHECK` (cupo, saldo ≥ 0, puntaje 1 a 5) y `UNIQUE` (una billetera por titular, sin doble cobro, una calificación por participante).
- **Concurrencia:** columna `version` (`@Version`) en `perfil`, `publicacion`, `contratacion` y `billetera`. Si dos operaciones modifican lo mismo, la segunda falla y se traduce a `ConflictoConcurrenciaException`.
- **Auditoría:** `publicacion_historial` y `contratacion_historial` solo reciben INSERT; guardan cada cambio de estado (ver `13-mis-publicaciones.md`).

## 5. Cómo se levanta (resumen para la demo)

```bash
docker compose up -d                          # MySQL en localhost:3306
docker compose ps                             # esperar "healthy"
mvn -q compile exec:java "-Dexec.args=--real" # Flyway + Hibernate validate + ventana
```

## 6. Frases para la defensa

- *"La aplicación no guarda nada en memoria: cada caso de uso corre en una transacción y termina en commit sobre MySQL."*
- *"MySQL corre en Docker con un volumen, por eso los datos sobreviven aunque se baje el contenedor."*
- *"Flyway crea el esquema; Hibernate solo lo valida. Así el esquema está versionado y no depende de lo que genere Hibernate."*
- *"Las pruebas usan H2 en memoria con las mismas migraciones, así que son rápidas y no dependen de Docker."*
