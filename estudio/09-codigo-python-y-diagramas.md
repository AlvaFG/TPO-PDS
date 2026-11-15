# 9. Por qué hay código Python (y qué son los diagramas)

## La respuesta corta

**Los archivos `.py` no son parte de la aplicación.** SkillSwap es 100 % Java. Los scripts de Python están en la carpeta `diagrama/` y sirven para **producir documentación**: el diagrama de clases UML y el informe en PDF.

Se pueden borrar sin que la aplicación deje de compilar, de ejecutarse o de pasar las pruebas. Se dejaron en el repositorio porque son **reproducibles**: si cambia el código, se puede volver a generar el diagrama en lugar de dibujarlo a mano.

> La consigna (§13) pide como entrega un **diagrama de clases completo en StarUML** y un **informe PDF**. Estos scripts son la forma en que se produjeron.

## El problema que resuelven

StarUML guarda sus diagramas en un archivo `.mdj` (JSON). Un diagrama de este tamaño tiene **147 clases y 234 relaciones**. Dibujarlas a mano y mantenerlas al día cuando el código cambia es inviable: cada clase necesita sus compartimentos de atributos y operaciones, y cada relación varias referencias cruzadas por `_id`. Por eso el `.mdj` se **genera** a partir de una descripción.

## Qué hace cada archivo de `diagrama/`

```
construir_spec.py ──► spec.json ──► generar_mdj.py ──► skillswap.mdj ──► (StarUML) ──► img/*.png
   (el modelo)        (descripción)    (+ crear_mdj.py)   (el diagrama)
                                                              │
                                                  revisar_mdj.py  (audita errores)
                                                  leer_mdj.py     (lo muestra como texto/Mermaid)

informe.md ──► generar_pdf.py ──► informe.pdf
```

| Archivo | Qué hace | Entrada → Salida |
|---|---|---|
| `construir_spec.py` | Contiene **el modelo de clases de SkillSwap** escrito de forma compacta (nombre, atributos, operaciones, relaciones, patrón, paquete) | → `spec.json` |
| `spec.json` | La descripción del modelo en JSON. Es el "plano" del diagrama | (generado) |
| `crear_mdj.py` | Convierte una descripción JSON en un `.mdj` válido: crea las clases, compartimentos, relaciones, paquetes y los diagramas | `spec.json` → `.mdj` |
| `generar_mdj.py` | Usa `crear_mdj.py` y le suma el **ordenamiento visual**: coloca las clases por capas para que no se superpongan y calcula el trazado de las líneas (ortogonales, esquivando las cajas) | `spec.json` → `skillswap.mdj` |
| `revisar_mdj.py` | **Audita** un `.mdj`: relaciones duplicadas, atributos sin tipo, clases sin dibujar, etc. Debe dar 0 errores | `skillswap.mdj` → informe de errores |
| `leer_mdj.py` | **Lee** un `.mdj` y lo muestra en texto o en Mermaid. Sirve para inspeccionarlo sin abrir StarUML | `.mdj` → texto |
| `generar_pdf.py` | Convierte `informe.md` a `informe.pdf` (Markdown → HTML → PDF con Chrome sin ventana). Necesita el paquete `markdown` | `informe.md` → `informe.pdf` |

## Qué es el `.mdj` y cómo se ve

`skillswap.mdj` se abre con **StarUML**. Contiene un diagrama `Main` con todo el sistema más **12 vistas adicionales**:

| Vista | Qué muestra |
|---|---|
| `Main` | Todo el diseño, en cuatro paquetes: Presentation, Application, Domain e Infrastructure |
| `Capa Dominio`, `Capa Aplicacion`, `Capa Infraestructura`, `Capa Presentacion` | Una capa por diagrama |
| `Patron State - Publicacion`, `Patron State - Contratacion` | Las dos jerarquías de estados |
| `Patron Strategy` | Política de cancelación y recomendación |
| `Patron Decorator` | Cadena de promociones |
| `Patron Observer` | Publicador, observers y eventos |
| `Patron Adapter y Simple Factory` | Proveedores externos y la factory de canales |
| `Patron Composite` | Catálogo jerárquico |
| `Recorrido principal` | El caso de uso completo de la demo |

Las imágenes ya exportadas están en `img/` (13 PNG), para verlas sin StarUML.

**Convenciones de la cátedra que respeta el diagrama:** diagrama llamado `Main`; atributos `private` y operaciones `public` con tipos Java; asociaciones con nombre verbal y multiplicidad en los dos extremos; interfaces con prefijo `I`; composición si la parte no vive sin el todo y agregación si es independiente; el cliente se asocia a la **abstracción**, no a las clases concretas.

> **Para la defensa:** lo importante del diagrama no es cómo se generó, sino que **coincide con el código**. Los nombres de clases y paquetes del UML son los mismos que los del código Java.

## Cómo regenerar (opcional)

Solo hace falta si se cambia el modelo. Requiere Python 3.

```bash
cd TPO
python3 diagrama/construir_spec.py          # modelo → diagrama/spec.json
python3 diagrama/generar_mdj.py             # spec.json → skillswap.mdj (tarda ~2 minutos)
python3 diagrama/revisar_mdj.py skillswap.mdj    # debe dar 0 errores
staruml image skillswap.mdj -f png -o "img/<%=filenamify(element.name)%>.png"   # exporta las imágenes (necesita StarUML)
python3 diagrama/generar_pdf.py             # informe.md → informe.pdf (necesita el paquete markdown y Chrome)
```

`generar_mdj.py` tarda porque calcula, para cada una de las 234 relaciones, un camino que esquive las cajas (búsqueda A* sobre una grilla).

## Cosas para tener presentes

- El **modelo del diagrama está escrito a mano** en `construir_spec.py`; no se deduce solo del código Java. Si se cambia una clase en Java, hay que actualizar el modelo para que el UML siga coincidiendo.
- Si el diagrama `Capa Presentacion` se ve denso en StarUML, es por la cantidad de relaciones; se puede reacomodar a mano dentro de StarUML.
- `generar_pdf.py` necesita el paquete `markdown` de Python y Google Chrome instalados; no hace falta nada de esto para usar la aplicación.

## En el diagrama no están…

Los scripts, esta guía ni el `pom.xml`: el UML representa **solo** el diseño del sistema (dominio y aplicación, más infraestructura y presentación), como pide la consigna.
