#!/usr/bin/env python3
"""Convierte informe.md en informe.pdf (Markdown -> HTML -> PDF con Chrome headless).

Uso (desde TPO/):
    python3 diagrama/generar_pdf.py
Requiere el paquete `markdown` y google-chrome.
"""
import os
import re
import subprocess
import sys
import urllib.parse

import markdown

RAIZ = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))

CSS = """
@page { size: A4; margin: 16mm 14mm; }
body { font-family: "DejaVu Sans", Arial, sans-serif; font-size: 10pt; line-height: 1.4; color: #111; }
h1 { font-size: 20pt; border-bottom: 2px solid #333; padding-bottom: 4px; }
h2 { font-size: 14pt; margin-top: 22px; border-bottom: 1px solid #999; page-break-after: avoid; }
h3 { font-size: 11.5pt; margin-top: 16px; page-break-after: avoid; }
table { border-collapse: collapse; width: 100%; margin: 8px 0; font-size: 8.8pt; }
th, td { border: 1px solid #aaa; padding: 3px 5px; vertical-align: top; text-align: left; }
th { background: #eee; }
tr { page-break-inside: avoid; }
code { font-family: "DejaVu Sans Mono", monospace; font-size: 8.6pt; background: #f3f3f3; padding: 0 2px; }
pre { background: #f3f3f3; padding: 6px; font-size: 8pt; white-space: pre-wrap; }
.figuras p { page-break-inside: avoid; text-align: center; margin: 14px 0; }
.figuras img { max-width: 100%; max-height: 245mm; }
hr { border: 0; border-top: 1px solid #ccc; }
"""


def main():
    with open(os.path.join(RAIZ, "informe.md"), encoding="utf-8") as f:
        texto = f.read()
    # Las rutas de imagen tienen espacios, que Markdown no acepta: se pasan a <img>
    # con la ruta codificada antes de convertir.
    texto = re.sub(r"!\[([^\]]*)\]\(img/([^)]+)\)",
                   lambda m: f'<p><img alt="{m.group(1)}" src="img/{urllib.parse.quote(m.group(2))}">'
                             f'<br><em>{m.group(1)}</em></p>',
                   texto)
    cuerpo = markdown.markdown(texto, extensions=["tables", "fenced_code", "md_in_html"])

    html = (f'<!doctype html><html lang="es"><head><meta charset="utf-8">'
            f'<title>SkillSwap - Informe de diseno</title><style>{CSS}</style></head>'
            f'<body>{cuerpo}</body></html>')
    ruta_html = os.path.join(RAIZ, "informe.html")
    with open(ruta_html, "w", encoding="utf-8") as f:
        f.write(html)

    ruta_pdf = os.path.join(RAIZ, "informe.pdf")
    r = subprocess.run(["google-chrome", "--headless=new", "--disable-gpu", "--no-pdf-header-footer",
                        f"--print-to-pdf={ruta_pdf}", "file://" + ruta_html],
                       capture_output=True, text=True, timeout=180)
    if r.returncode != 0 or not os.path.exists(ruta_pdf):
        print(r.stderr, file=sys.stderr)
        return 1
    os.remove(ruta_html)
    print(f"Generado {ruta_pdf}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
