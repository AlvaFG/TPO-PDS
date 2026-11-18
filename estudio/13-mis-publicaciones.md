# 13. "Mis publicaciones": en StarUML, en el código y cómo funciona

## 1. Qué es

Es la pantalla donde un proveedor gestiona sus propias publicaciones. Es la opción **"▣ Mis publicaciones"** del menú lateral. Desde ahí se puede:

- listar las publicaciones propias con su estado;
- crear un borrador;
- editarlo (solo mientras es borrador);
- publicar o pausar;
- agregar promociones.

## 2. Dónde está en StarUML

Mostrá los diagramas en este orden, que sigue el recorrido de afuera hacia adentro:

| Diagrama | Qué mostrar ahí |
|---|---|
| **Capa Presentacion** | `SkillSwapView` → `UiControllers` → `PublicacionController` ← `PublicacionControllerImpl`, más `EjecutorDeAcciones`, `Conversor` y `ModalidadUi` |
| **Capa Aplicacion** | `IPublicacionService` ← `PublicacionService`, que usa `IPublicacionRepository`, `IPerfilRepository`, `ICatalogoRepository`, `IPoliticaCancelacionRepository` e `IGestorTransacciones`. También `PublicacionCommand`, `CambiarEstadoCommand`, `AccionPublicacion`, `PromocionCommand` y `PublicacionDto` |
| **Capa Dominio** | `Publicacion` con `FranjaHoraria`, `CambioEstado`, `Promocion` y `PoliticaCancelacion` |
| **Patron State - Publicacion** | `Publicacion` (contexto) → `EstadoPublicacion` ← `EstadoPublicacionBase` ← `EstadoBorrador`, `EstadoPublicada`, `EstadoPausada`, `EstadoCerrada` y `EstadoPublicacionCancelada`. También `EstadoPublicacionFactory` |
| **Patron Decorator** | `Promocion` (`PromocionPorcentual` y `PromocionMontoFijo`), `PromocionDecorator` e `ICalculoPrecio` |
| **Capa Infraestructura** | `PublicacionJpaRepository` implementa `IPublicacionRepository` y usa `JpaGestorTransacciones` |
| **Main** | Todo junto, con las 4 capas |

## 3. Cómo funciona en el código

Todas las rutas son relativas a `src/main/java/ar/edu/uade/skillswap/`. Seguí el caso **"abrir Mis publicaciones"**.

**① La ventana.** En `ui/SkillSwapView.java`, línea 30, el botón del menú llama a `showListings()`. Esa función (líneas 55 a 70) ejecuta `controllers.publicaciones().listarPropias(USER_ID)` y dibuja una fila por publicación, con su estado y los botones **Editar**, **Promoción** y **Pausar/Publicar**. La vista solo conoce `UiControllers`: nunca toca servicios, repositorios ni entidades.

**② El controller.** `presentation/PublicacionControllerImpl.listarPropias()` hace dos cosas:

- Se ejecuta dentro de `EjecutorDeAcciones.ejecutar(...)`, que valida el usuario con `IServicioIdentidad` y traduce cualquier excepción de dominio, de aplicación o de concurrencia a `ErrorDeAplicacion`, que es lo único que ve la UI.
- Llama a `IPublicacionService.listarPropias` y convierte cada `PublicacionDto` a los registros de la UI con `Conversor.publicacion(...)`.

**③ El caso de uso.** `application/service/PublicacionService.listarPropias()`:

1. Busca el `Perfil` activo del usuario en `IPerfilRepository`. Si no hay uno, lanza `AccesoDenegadoException`.
2. Llama a `publicacionRepository.listarDeAutor(autor.getId())`.
3. Mapea cada `Publicacion` a un `PublicacionDto` con `PublicacionDto.desde(...)`.

**④ La base.** `infrastructure/persistence/PublicacionJpaRepository.listarDeAutor()` ejecuta esta consulta JPQL:

```
select p from Publicacion p join fetch p.autor a left join fetch p.categoria
where a.id = :autor order by p.id
```

Corre dentro de `JpaGestorTransacciones`. La tabla es `publicacion` (esquema en `V1__esquema.sql`, datos de demo en `V2__datos_demo.sql`). La entidad `Publicacion` es `@Entity`. El estado se guarda como texto (`nombreEstado`) y el objeto estado se reconstruye al cargar (`@PostLoad`) con `EstadoPublicacionFactory`.

El camino de vuelta es el mismo: entidad → DTO → registro de la UI → pantalla.

```
SkillSwapView ──▶ UiControllers ──▶ PublicacionControllerImpl ──▶ IPublicacionService
 (ui/)             (interfaz)         (presentation/)               │
                                                                     ▼
                                                              PublicacionService
                                                              (application/service)
                                                                     │
                                  ┌──────────────────────────────────┤
                                  ▼                                  ▼
                       IPublicacionRepository               Publicacion (dominio)
                                  ▲                          + estado (State)
                                  │ implementa
                       PublicacionJpaRepository ──▶ MySQL (tabla publicacion)
```

## 4. Las acciones de la pantalla

| Botón | Llamada en el controller | Caso de uso | Dominio |
|---|---|---|---|
| **+ Nueva publicación** | `crearBorrador` | `PublicacionService.crearBorrador` | `new Publicacion(...)` y `agregarFranja(...)` |
| **Editar** (solo en BORRADOR) | `editar` | `PublicacionService.editar` | `Publicacion.editar` → `validarEditable()` |
| **Publicar / Pausar** | `cambiarEstado` | `PublicacionService.cambiarEstado` | `publicar()` / `pausar()` → `estado.xxx(...)` |
| **Promoción** | `agregarPromocion` | `PublicacionService.agregarPromocion` | `Publicacion.agregarPromocion` |

## 5. Los patrones que aparecen

- **State (el principal):**
  - `Publicacion` es el contexto y delega cada acción en su `estado` (`domain/model/Publicacion.java`: atributo `estado` en la línea 89, acciones `publicar`, `pausar`, `reanudar`, `cerrar` y `cancelar` en las líneas 166 a 189).
  - `EstadoPublicacion` es la interfaz del estado (`domain/estado/publicacion/EstadoPublicacion.java`, línea 8). `EstadoPublicacionBase` rechaza toda transición con `ReglaNegocioException` (`EstadoPublicacionBase.java`, clase en la línea 10, métodos por defecto desde la línea 13), y cada estado concreto redefine solo lo que permite. Los estados concretos son `EstadoBorrador`, `EstadoPublicada`, `EstadoPausada`, `EstadoCerrada` y `EstadoPublicacionCancelada`, todos en la misma carpeta.
  - Ejemplo: `EstadoBorrador.publicar()` (`EstadoBorrador.java`, líneas 15 a 18) llama a `validarPublicable()` y luego a `cambiarEstado(new EstadoPublicada(), ...)`. `EstadoBorrador` es el único estado editable (`esEditable()`, línea 26).
  - `cambiarEstado` (`Publicacion.java`, líneas 191 a 195) es el único punto que cambia el estado y deja una entrada en el historial (`CambioEstado`).
  - Reconstrucción al cargar de la base: `@PostLoad` en `Publicacion.java` (línea 112) llama a `EstadoPublicacionFactory.desdeNombre(...)` (`EstadoPublicacionFactory.java`, clase en la línea 6).
- **Decorator:** `Publicacion.cotizar(fecha)` arma la cadena de promociones vigentes sobre el precio base. `Promocion.decorar(precio)` crea el decorador (GRASP Creator).
- **Strategy:** la `PoliticaCancelacion` (flexible o estricta) se elige al crear la publicación.
- **Enum con comportamiento:** `AccionPublicacion` (PUBLICAR, PAUSAR, REANUDAR, CERRAR, CANCELAR). Cada literal llama al método de dominio que le corresponde, así el caso de uso no necesita un `switch`.
- **Controller delgado / Pure Fabrication:** `PublicacionControllerImpl`, `EjecutorDeAcciones` y `Conversor`.

## 6. Reglas de negocio que se pueden mencionar

- **Solo el autor modifica:** `publicacionDelAutor(...)` lanza `AccesoDenegadoException` si el usuario no es el autor.
- **Para publicar:** `validarPublicable()` exige título, categoría, precio y al menos una franja.
- **Editar:** solo en BORRADOR.
- **Cancelar:** exige un motivo.
- **Un borrador:** solo lo ve su autor (`esVisiblePara`).
- **Transacciones:** cada operación de escritura corre en `IGestorTransacciones.ejecutar(...)`, con commit o rollback.

## 7. Detalles de la UI para tener presentes

- **Estado destino:** la UI pide un estado destino (PUBLICADA, PAUSADA…). El controller lo traduce a una acción: PUBLICADA es REANUDAR desde PAUSADA y PUBLICAR desde BORRADOR.
- **Borrador nuevo:** como la UI no carga horarios, el controller crea el borrador como MENTORIA, con 3 franjas (una por día, desde la semana siguiente) para que se pueda publicar.
- **Modalidad HÍBRIDA:** la UI la ofrece, pero el dominio no la tiene (`ModalidadUi` la rechaza con un error legible).

## 8. Pruebas que lo respaldan

- `PublicacionTest` (dominio).
- `PublicacionServiceTest` (caso de uso).
- `PublicacionJpaRepositoryTest` (consulta y base).
- `SkillSwapControllersTest` y `RecorridoPrincipalTest` (de punta a punta).

## Guion corto para exponer (2 a 3 minutos)

1. **Qué hace la pantalla.** Mostrar `Mis publicaciones` en la app.
2. **Capa Presentacion.** Explicar que la vista solo conoce `UiControllers`.
3. **Capa Aplicacion.** Mostrar el caso de uso `PublicacionService` y los puertos.
4. **Patron State - Publicacion.** Explicar el ciclo de vida: BORRADOR → PUBLICADA ⇄ PAUSADA → CERRADA/CANCELADA.
5. **Capa Infraestructura.** Mostrar cómo se guarda en MySQL.
6. **Cierre.** Las dependencias apuntan hacia el dominio.
