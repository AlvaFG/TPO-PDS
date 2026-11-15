# 8. Pruebas

La consigna (§12) pide pruebas automáticas de las reglas, los casos de uso, la persistencia y la integración, con **casos negativos**, y una cobertura sugerida del **80 %** en dominio y aplicación.

## Cómo se corren

```bash
mvn test       # corre todas las pruebas
mvn verify     # pruebas + cobertura JaCoCo; falla si domain+application < 80 %
```

Reporte de cobertura: `target/site/jacoco/index.html` (se abre en el navegador). Resultado de la última corrida completa: **222 pruebas, todas verdes, 99,2 % de cobertura** en `domain` y `application`.

## Los cuatro niveles

| Nivel | Carpeta | Qué prueba | Cómo aísla | Base de datos |
|---|---|---|---|---|
| **Dominio** | `test/.../domain` | Reglas, estados, políticas, promociones, value objects | Objetos reales, sin nada más | No |
| **Casos de uso** | `test/.../application` | Servicios con sus puertos | **Repositorios falsos** (`soporte/EnMemoria`, `Repositorios`) y Mockito | No |
| **Persistencia e integración** | `test/.../infrastructure` | Repositorios JPA, transacciones, concurrencia, el recorrido completo | **H2 en memoria** con las mismas migraciones de Flyway | Sí (H2; MySQL opcional) |
| **Presentación** | `test/.../presentation` | Los controllers del kit y la traducción de errores | Casos de uso reales sobre repositorios en memoria | No |

La carpeta `soporte/` tiene datos y repositorios en memoria reutilizados (`Datos`, `Ids`, `EnMemoria`, `Repositorios`).

> **Por qué importa que los casos de uso se prueben sin base de datos:** es la prueba de que la capa de aplicación **no depende de JPA**. Si dependiera, no se podría reemplazar el repositorio por uno falso.

## Mapa de pruebas por tema

### Dominio
| Archivo | Qué verifica |
|---|---|
| `ContratacionTest` | El ciclo de estados completo y las transiciones inválidas, el historial, reprogramación, calificar |
| `PublicacionTest` | Estados de la publicación, publicar solo si está completa, reservar y liberar cupo |
| `BilleteraTest` | Débito y crédito, saldo insuficiente que **no modifica nada**, doble pago |
| `SkillCoinsTest` | Los SkillCoins nunca son negativos; sumar, restar, porcentaje |
| `PoliticaCancelacionTest` | Reintegro 100/50/0 % de las políticas flexible y estricta |
| `PromocionDecoratorTest` | Promociones acumulables y total nunca negativo |
| `CatalogoCompositeTest` | Árbol de categorías, subcategorías, ciclos |
| `EstadoFactoryTest` | Reconstrucción de estados desde el nombre |
| `PerfilTest`, `ValueObjectsTest`, `EventoYNotificacionTest` | Perfil, reputación, rangos y franjas, eventos |

### Casos de uso
| Archivo | Qué verifica |
|---|---|
| `ContratacionServiceTest` | Solicitar, aceptar, pagar, completar, cancelar; último cupo; saldo insuficiente; permisos |
| `PublicacionServiceTest` | Crear borrador, publicar, cambios de estado, oferta incompleta |
| `BusquedaServiceTest` | Filtros combinados, orden, paginación |
| `RecomendacionServiceTest` | Cambiar de estrategia sin tocar el servicio |
| `ObserverTest` | Un evento notifica por dos canales |
| `CalificacionServiceTest` | Calificar una sola vez, no autorizado, reputación |
| `PerfilServiceTest`, `ConsultasServiceTest` | Perfil público, billetera, notificaciones |

### Infraestructura
| Archivo | Qué verifica |
|---|---|
| `ArranquePersistenciaTest` | Que Flyway migre y Hibernate **valide** el esquema |
| `PublicacionJpaRepositoryTest`, `PerfilCatalogoJpaTest` | Guardar, buscar, filtros y paginación sobre la base |
| `ContratacionJpaTest` | Flujo persistido, doble cobro frenado por la restricción `UNIQUE`, cancelar con reintegro |
| `ConcurrenciaCupoTest` | **Dos hilos aceptan a la vez el último cupo: solo uno gana** |
| `JpaGestorTransaccionesTest` | Commit, rollback y traducción de `OptimisticLockException` |
| `ServiciosConJpaTest`, `ComposicionJpaTest` | Servicios completos sobre JPA |
| `RecorridoPrincipalTest` | **Publicar → solicitar → aceptar → pagar → completar → calificar** de punta a punta, pasando por los controllers de la ventana |
| `AdaptadoresTest` | Adapters de identidad, email, push y la factory de canales |
| `MySqlArranqueTest` | Contra MySQL real; **se omite** si no se define `SKILLSWAP_DB_URL` |

### Presentación
| Archivo | Qué verifica |
|---|---|
| `SkillSwapControllersTest` | Los 6 controllers: `avanzar` según estado y rol, errores legibles |
| `EjecutorDeAccionesTest` | Usuario inválido, traducción de errores, conflicto de concurrencia |

## Los casos negativos que pide la consigna

| Caso | Dónde se prueba |
|---|---|
| Transición inválida | `PublicacionTest`, `ContratacionTest` |
| Saldo insuficiente | `BilleteraTest`, `ContratacionServiceTest`, `ContratacionJpaTest` |
| Cupo agotado | `ContratacionServiceTest`, `ConcurrenciaCupoTest` |
| Doble pago | `ContratacionJpaTest` (dominio + restricción `UNIQUE`) |
| Calificación no autorizada | `CalificacionServiceTest.calificacionNoAutorizada` |

La tabla de los **10 casos de aceptación** con el nombre exacto de cada prueba está en el [README principal](../README.md#casos-de-aceptación-consigna-sección-15-y-sus-pruebas).

## Cómo leer una prueba

Todas siguen la estructura **preparar → ejecutar → verificar**. Ejemplo ilustrativo (no es copia literal):

```java
@Test
void pagoConSaldoInsuficienteNoModificaSaldosNiEstado() {
    // preparar: un cliente con menos saldo que el precio, y una contratación ACEPTADA
    // ejecutar: intentar pagar
    // verificar: lanza ReglaNegocioException y NADA cambió (ni saldos ni estado)
    assertThrows(ReglaNegocioException.class, () -> servicio.pagar("user-cliente", id));
}
```

## Probar contra MySQL real

Por defecto las pruebas de persistencia usan H2. Para correr algunas contra el MySQL de Docker, ver la sección *Correr las pruebas* del README principal (variables `SKILLSWAP_DB_URL` y `SKILLSWAP_TEST_DB_*`). **No apuntar las pruebas de escritura a la base de la demo**: crean y borran datos.

## Qué significa "99,2 % de cobertura"

JaCoCo cuenta qué porcentaje de las líneas y ramas del código de `domain` y `application` se ejecutó al correr las pruebas. **Una cobertura alta no prueba que el código sea correcto** (lo dice la propia consigna), pero una baja sí indica código sin probar. Lo valioso acá son los casos negativos y los de concurrencia, no el número.
