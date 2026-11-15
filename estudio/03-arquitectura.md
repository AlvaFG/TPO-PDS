# 3. Arquitectura

## La idea en una imagen

La consigna pide **Clean Architecture**: las capas de afuera conocen a las de adentro, **nunca al revés**. Lo más importante, las reglas del negocio, no depende de nada: ni de la ventana, ni de la base, ni de ningún proveedor.

```
        ┌──────────────────────────────────────────────────────────────┐
        │  PRESENTATION   (presentation/ + la ventana del kit)         │
        │  traduce acciones de la ventana a casos de uso               │
        │   ┌──────────────────────────────────────────────────────┐   │
        │   │  APPLICATION   (application/)                        │   │
        │   │  casos de uso, puertos, DTOs, eventos                │   │
        │   │   ┌──────────────────────────────────────────────┐   │   │
        │   │   │  DOMAIN   (domain/)                          │   │   │
        │   │   │  entidades, estados, políticas, precios      │   │   │
        │   │   └──────────────────────────────────────────────┘   │   │
        │   └──────────────────────────────────────────────────────┘   │
        │  INFRASTRUCTURE (infrastructure/)                            │
        │  JPA/MySQL y adaptadores: implementa los puertos             │
        └──────────────────────────────────────────────────────────────┘
        Las flechas de dependencia apuntan siempre hacia el DOMAIN.
```

Versión en tabla (es la de la consigna §6, aplicada):

| Capa | Paquete | Responsabilidad | **No debe conocer** |
|---|---|---|---|
| **Dominio** | `domain` | Entidades, value objects, reglas, políticas, estados | Interfaz gráfica, MySQL, casos de uso |
| **Aplicación** | `application` | Casos de uso, coordinación transaccional, puertos de entrada y salida | Controllers concretos, JPA, SDK externos |
| **Infraestructura** | `infrastructure` | JPA, repositorios, adaptadores a proveedores | Decisiones de negocio |
| **Presentación** | `presentation` (y el kit) | Recibir acciones de la ventana, validar entrada, delegar | SQL, reglas internas de las entidades |

## Puertos y adaptadores: cómo se cumple "las dependencias apuntan adentro"

El problema: un caso de uso necesita guardar una contratación, pero **no puede importar JPA**. La solución es la **inversión de dependencias** (la D de SOLID):

1. `application` define una **interfaz** (un *puerto*): `IContratacionRepository`, en `application/port`.
2. `infrastructure` la **implementa**: `ContratacionJpaRepository`.
3. En el arranque, `ConfiguracionApp` le entrega la implementación al caso de uso.

```
application/port/IContratacionRepository   ◄── la usa  ContratacionService   (application)
          ▲
          │ implementa
infrastructure/persistence/ContratacionJpaRepository   (infrastructure)
```

El caso de uso solo conoce la interfaz. Por eso en las pruebas de `application` se reemplaza por un repositorio falso, y para pasar de MySQL a otra base solo se escribiría otra implementación.

Todos los puertos de salida (`application/port`):

| Puerto | Qué necesita la aplicación | Implementación real |
|---|---|---|
| `IPerfilRepository`, `IPublicacionRepository`, `IContratacionRepository`, `IBilleteraRepository`, `ICalificacionRepository`, `INotificacionRepository`, `ICatalogoRepository`, `IPoliticaCancelacionRepository` | Guardar y buscar entidades | `*JpaRepository` en `infrastructure/persistence` |
| `IGestorTransacciones` | Ejecutar una operación dentro de una transacción | `JpaGestorTransacciones` |
| `IServicioIdentidad` | Saber si un `userId` es válido y obtener su email | `IdentidadAdapter` → `IdentityClientMock` |
| `ICanalNotificacion` | Enviar una notificación por un canal | `EmailAdapter`, `PushAdapter`, `CanalInterno` |
| `ICanalNotificacionFactory` | Obtener el canal que el usuario eligió | `CanalNotificacionFactory` |

Los **puertos de entrada** son las interfaces `I…Service` en `application/service` (por ejemplo `IContratacionService`): lo que usan los controllers.

## Cómo se conecta con la ventana de la cátedra

La ventana (`ui/SkillSwapView`) **solo conoce `UiControllers`**, una interfaz con seis controllers (`BusquedaController`, `PublicacionController`, `ContratacionController`, `WalletController`, `NotificacionController`, `PerfilController`). Es el "contrato de integración" de la consigna §10.

```
SkillSwapView  ──usa──►  UiControllers (interfaz del kit)
                              ▲                      ▲
                              │ implementa           │ implementa
        MockSkillSwapControllers (kit)     SkillSwapControllers (nuestro)
        datos en memoria                   → casos de uso → JPA → MySQL
```

Cambiar de "mock" a "real" es **cambiar qué implementación se le pasa a la ventana**. Eso lo decide `SkillSwapApplication.main` según el parámetro `--real`.

## Composition Root: dónde se arma todo

En esta cursada no se usa Spring, así que **alguien tiene que construir los objetos y conectarlos a mano** (inyección por constructor). Ese lugar es el *Composition Root*:

| Clase | Rol |
|---|---|
| `SkillSwapApplication` | `main`. Decide el modo, crea el `EntityManagerFactory`, los repositorios JPA y los adaptadores, y abre la ventana |
| `ConfiguracionApp` | Recibe los puertos ya construidos (`record Puertos`) y arma servicios, observers y controllers |

Es **el único lugar que conoce las clases concretas**. Todo lo demás habla con interfaces. Pasos de `ConfiguracionApp.crear(...)`:

1. Crea el `PublicadorEventos` y le suscribe `NotificacionObserver` y `ReputacionObserver` (**Observer**).
2. Crea los 7 servicios que usa la ventana (publicación, contratación, búsqueda, recomendación, billetera, notificación y perfil), pasándoles repositorios, el gestor de transacciones, el publicador de eventos y el reloj. `CalificacionService` existe y está probado, pero no se cablea acá porque la ventana del kit no tiene un botón para calificar.
3. Crea el `EjecutorDeAcciones` (valida usuario y traduce errores).
4. Crea los 6 controllers y los agrupa en `SkillSwapControllers`.

Como `ConfiguracionApp.crear` recibe los puertos, las pruebas arman la misma aplicación con repositorios en memoria o con H2.

## Qué hace cada pieza intermedia

### Servicio (caso de uso)
Una operación completa. Abre la transacción, busca las entidades, **llama al método del dominio** y guarda. **No contiene reglas de negocio**: las reglas viven en las entidades. Ejemplo, `ContratacionService.pagar`:

1. Busca la contratación y verifica que quien paga es el cliente.
2. Busca las billeteras del cliente y del proveedor.
3. Llama a `c.pagar(...)` (el dominio decide si se puede y mueve los créditos).
4. Guarda billeteras y contratación, **todo en una transacción**.
5. **Después del commit** publica el evento `PAGO_CONFIRMADO`.

El evento se publica después para no notificar operaciones que luego hicieron rollback.

### Controller
Recibe lo que manda la ventana, valida el usuario, llama al caso de uso y devuelve un valor simple. **No tiene SQL ni reglas**. Cada método es una línea gracias a `EjecutorDeAcciones`.

### `EjecutorDeAcciones` (GRASP: Pure Fabrication)
Una clase que no representa nada del negocio: existe para **evitar repetir** el mismo `try/catch` en cada controller. Verifica el `userId` con identidad, ejecuta la acción y convierte `ReglaNegocioException`, `AccesoDenegadoException`, `RecursoNoEncontradoException` y `ConflictoConcurrenciaException` en un `ErrorDeAplicacion` con mensaje legible.

### `JpaGestorTransacciones`
Abre un `EntityManager` y una transacción **por operación**; si hay un error hace rollback. Traduce `OptimisticLockException` (JPA) a `ConflictoConcurrenciaException` (nuestra), para que **JPA no se filtre a las capas de arriba**.

## Una concesión conocida

La consigna (§6) dice que el dominio no debería conocer JPA. En este proyecto las **entidades del dominio llevan anotaciones `@Entity`, `@Id`, etc.** de `jakarta.persistence`. Es lo mismo que hace el material de la cátedra (Magic Café) y evita duplicar clases con un mapeo manual. El dominio **solo** usa esas anotaciones; no hay SQL ni repositorios ahí. Queda registrado como límite conocido en el README y en el informe.

## Reglas de dependencia que se cumplen (se pueden verificar)

| Regla | Cómo comprobarla |
|---|---|
| `domain` no importa `application`, `infrastructure` ni `presentation` | `grep -r "import ar.edu.uade.skillswap.application" src/main/java/ar/edu/uade/skillswap/domain` no devuelve nada |
| `application` no importa `infrastructure` ni `presentation` | Igual, sobre `application/` |
| Solo `domain` (anotaciones de mapeo), `infrastructure` y `SkillSwapApplication` (que crea el `EntityManagerFactory`) importan `jakarta.persistence` | `grep -rl "jakarta.persistence" src/main/java` |
| Solo `ConfiguracionApp` y `SkillSwapApplication` conocen clases concretas de todas las capas | Ver sus `import` |

## SOLID y GRASP: dónde se ven

| Principio | Dónde se ve |
|---|---|
| **S** (una responsabilidad) | `EjecutorDeAcciones` solo traduce errores; `Conversor` solo convierte datos; cada estado solo define lo que permite |
| **O** (abierto/cerrado) | Agregar un estado, una política, un canal o una promoción es una clase nueva, sin tocar las existentes |
| **L** (sustitución) | Cualquier `PoliticaCancelacion` o `EstadoContratacion` reemplaza a otra sin romper al contexto |
| **I** (interfaces chicas) | Un puerto por repositorio; `ICalculoPrecio` tiene 2 métodos |
| **D** (inversión de dependencias) | Los servicios dependen de `I…Repository` y no de JPA |
| **Information Expert** | El saldo lo valida `Billetera`; el cupo lo valida `Publicacion` |
| **Creator** | `Contratacion` crea la `Calificacion`; la `Publicacion` arma la cadena de decoradores |
| **Controller** | Los `*ControllerImpl` reciben las acciones de la ventana |
| **Low Coupling / High Cohesion** | Capas separadas por interfaces; cada clase con una tarea |
| **Polymorphism** | Estados, políticas, promociones y canales se eligen por tipo, sin `if/else` por tipo |
| **Indirection / Protected Variations** | Los adaptadores aíslan al sistema de los proveedores externos |
| **Pure Fabrication** | `EjecutorDeAcciones`, `JpaGestorTransacciones`, las factories |
