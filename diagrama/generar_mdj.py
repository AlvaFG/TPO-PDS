"""Genera skillswap.mdj con un layout ordenado: clases sin solaparse y lineas ortogonales
que esquivan las cajas. Reutiliza crear_mdj.py y solo reemplaza posicionar() y los puntos
de cada relacion.

Uso:  python3 diagrama/generar_mdj.py [--ancho 2600] [--orden P,A,D,IN]
"""
import argparse
import heapq
import importlib.util
import json
import os

RAIZ = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
SKILL = os.path.join(os.path.dirname(os.path.abspath(__file__)), "crear_mdj.py")
spec_mod = importlib.util.spec_from_file_location("crear_mdj", SKILL)
cm = importlib.util.module_from_spec(spec_mod)
spec_mod.loader.exec_module(cm)

CELDA = 10
SEP_X, SEP_Y = 80, 130
ORDEN_PAQUETES = ["Presentation", "Application", "Domain", "Infrastructure"]
RUTAS = {}      # id(relacion) -> puntos "x:y;x:y;..."


def vecinos(spec, nombres):
    v = {n: set() for n in nombres}
    for r in spec["relaciones"]:
        a, b = ((r["parte"], r["todo"]) if r["tipo"] in ("agregacion", "composicion")
                else (r["origen"], r["destino"]))
        if a in v and b in v and a != b:
            v[a].add(b)
            v[b].add(a)
    return v


def posicionar(spec, nombres, ancho_lienzo):
    dims = {c["nombre"]: (cm.ancho_clase(c), cm.alto_clase(c))
            for c in spec["clases"] if c["nombre"] in nombres}
    paquete_de = {c["nombre"]: c.get("paquete") for c in spec["clases"]}
    grupos = {}
    for c in spec["clases"]:
        if c["nombre"] in nombres:
            grupos.setdefault(paquete_de[c["nombre"]], []).append(c["nombre"])
    orden = sorted(grupos, key=lambda p: ORDEN_PAQUETES.index(p) if p in ORDEN_PAQUETES else 99)
    vec = vecinos(spec, nombres)
    capas = {p: calcular(spec, grupos[p]) for p in orden}
    # Orden inicial por nombre dentro de cada capa; despues se refina por baricentro.
    orden_capa = {(p, d): sorted(ns) for p in orden for d, ns in capas[p].items()}
    xs = {}

    def colocar():
        pos, marcos, y = {}, {}, cm.MARGEN
        for p in orden:
            y0 = y
            if p:
                y += cm.ALTO_NOMBRE + cm.PAD_PAQUETE
            for d in sorted(capas[p]):
                for fila in partir(orden_capa[(p, d)], dims, ancho_lienzo):
                    total = sum(dims[n][0] for n in fila) + SEP_X * (len(fila) - 1)
                    x = cm.MARGEN + cm.PAD_PAQUETE + max(0, (ancho_lienzo - total) // 2)
                    alto = 0
                    for n in fila:
                        w, h = dims[n]
                        pos[n] = (x, y, w, h)
                        xs[n] = x + w / 2
                        x += w + SEP_X
                        alto = max(alto, h)
                    y += alto + SEP_Y
            if p:
                y += cm.PAD_PAQUETE - SEP_Y
                miembros = grupos[p]
                x2 = max(pos[n][0] + pos[n][2] for n in miembros)
                x1 = min(pos[n][0] for n in miembros) - cm.PAD_PAQUETE
                marcos[p] = (x1, y0, x2 + cm.PAD_PAQUETE - x1, y - y0)
                y += cm.SEP_PAQUETES
        return pos, marcos

    pos, marcos = colocar()
    for barrido in range(10):
        for clave in orden_capa:
            def bari(n):
                vs = [xs[m] for m in vec[n] if m in xs]
                return sum(vs) / len(vs) if vs else xs[n]
            orden_capa[clave] = sorted(orden_capa[clave], key=bari)
        pos, marcos = colocar()
    enrutar(spec, nombres, pos)
    return pos, marcos


def calcular(spec, miembros):
    return cm.calcular_capas(spec, miembros)


def partir(lista, dims, ancho):
    filas, fila, w_fila = [], [], 0
    for n in lista:
        w = dims[n][0]
        if fila and w_fila + w + SEP_X > ancho:
            filas.append(fila)
            fila, w_fila = [], 0
        fila.append(n)
        w_fila += w + SEP_X
    if fila:
        filas.append(fila)
    return filas


# ------------------------------------------------------------------ enrutado

def enrutar(spec, nombres, pos):
    """Camino ortogonal por una grilla, esquivando las cajas (A* con penalidad por giros)."""
    W = max(x + w for x, y, w, h in pos.values()) + 120
    H = max(y + h for x, y, w, h in pos.values()) + 120
    nx, ny = W // CELDA + 2, H // CELDA + 2
    libre = [[True] * ny for _ in range(nx)]
    infl = 6
    for x, y, w, h in pos.values():
        for i in range(max(0, (x - infl) // CELDA), min(nx, (x + w + infl) // CELDA + 1)):
            for j in range(max(0, (y - infl) // CELDA), min(ny, (y + h + infl) // CELDA + 1)):
                if x - infl <= i * CELDA + CELDA // 2 <= x + w + infl and \
                   y - infl <= j * CELDA + CELDA // 2 <= y + h + infl:
                    libre[i][j] = False
    uso = {}

    def puertos(g):
        x, y, w, h = g
        res = []
        r = infl + CELDA
        for i in range(max(0, (x - r) // CELDA), min(nx, (x + w + r) // CELDA + 1)):
            for j in range(max(0, (y - r) // CELDA), min(ny, (y + h + r) // CELDA + 1)):
                if not libre[i][j]:
                    continue
                cx, cy = i * CELDA + CELDA // 2, j * CELDA + CELDA // 2
                if x <= cx <= x + w and (cy < y or cy > y + h):
                    res.append((i, j))
                elif y <= cy <= y + h and (cx < x or cx > x + w):
                    res.append((i, j))
        return res

    def borde(g, c):
        x, y, w, h = g
        cx, cy = c[0] * CELDA + CELDA // 2, c[1] * CELDA + CELDA // 2
        if cx < x:
            return x, cy
        if cx > x + w:
            return x + w, cy
        return (cx, y) if cy < y else (cx, y + h)

    pares = []
    for r in spec["relaciones"]:
        a, b = ((r["parte"], r["todo"]) if r["tipo"] in ("agregacion", "composicion")
                else (r["origen"], r["destino"]))
        if a in pos and b in pos and a != b:
            ca, cb = cm.centro(pos[a]), cm.centro(pos[b])
            pares.append((abs(ca[0] - cb[0]) + abs(ca[1] - cb[1]), r, a, b))
    pares.sort(key=lambda t: t[0])

    for _, r, a, b in pares:
        ini, fin = puertos(pos[a]), set(puertos(pos[b]))
        if not ini or not fin:
            continue
        cb = cm.centro(pos[b])
        tx, ty = cb[0] // CELDA, cb[1] // CELDA
        abiertos, previo, costo = [], {}, {}
        for p in ini:
            for d in range(4):
                costo[(p, d)] = 0
                heapq.heappush(abiertos, (abs(p[0] - tx) + abs(p[1] - ty), 0, p, d))
                previo[(p, d)] = None
        meta = None
        pasos = 0
        while abiertos:
            _, c, p, d = heapq.heappop(abiertos)
            if c > costo.get((p, d), 1 << 30):
                continue
            if p in fin:
                meta = (p, d)
                break
            pasos += 1
            if pasos > 3000000:
                break
            for nd, (dx, dy) in enumerate(((1, 0), (-1, 0), (0, 1), (0, -1))):
                q = (p[0] + dx, p[1] + dy)
                if not (0 <= q[0] < nx and 0 <= q[1] < ny) or not libre[q[0]][q[1]]:
                    continue
                paso = 1
                if previo[(p, d)] is not None and nd != d:
                    paso += 6
                orient = nd // 2
                paso += 2 * uso.get((q, orient), 0) + (1 if uso.get((q, 1 - orient)) else 0)
                nc = c + paso
                if nc < costo.get((q, nd), 1 << 30):
                    costo[(q, nd)] = nc
                    previo[(q, nd)] = (p, d)
                    heapq.heappush(abiertos, (nc + abs(q[0] - tx) + abs(q[1] - ty), nc, q, nd))
        if meta is None:
            continue
        camino, s = [], meta
        while s is not None:
            camino.append(s[0])
            s = previo[s]
        camino.reverse()
        for k in range(1, len(camino)):
            o = 0 if camino[k][1] == camino[k - 1][1] else 1
            uso[(camino[k], o)] = uso.get((camino[k], o), 0) + 1
        pts = [borde(pos[a], camino[0])]
        c0 = camino[0]
        pts.append((c0[0] * CELDA + CELDA // 2, c0[1] * CELDA + CELDA // 2))
        for k in range(1, len(camino) - 1):
            d1 = (camino[k][0] - camino[k - 1][0], camino[k][1] - camino[k - 1][1])
            d2 = (camino[k + 1][0] - camino[k][0], camino[k + 1][1] - camino[k][1])
            if d1 != d2:
                pts.append((camino[k][0] * CELDA + CELDA // 2, camino[k][1] * CELDA + CELDA // 2))
        cl = camino[-1]
        pts.append((cl[0] * CELDA + CELDA // 2, cl[1] * CELDA + CELDA // 2))
        pts.append(borde(pos[b], camino[-1]))
        RUTAS[id(r)] = ";".join(f"{x}:{y}" for x, y in pts)


_vista_original = cm.vista_relacion


def vista_relacion(ids, r, rel, diagrama_id, vistas, pos, nombre_a, nombre_b):
    v = _vista_original(ids, r, rel, diagrama_id, vistas, pos, nombre_a, nombre_b)
    if id(r) in RUTAS:
        v["points"] = RUTAS[id(r)]
    return v


cm.posicionar = posicionar
cm.vista_relacion = vista_relacion


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--ancho", type=int, default=2600)
    ap.add_argument("--orden", default="")
    ap.add_argument("--spec", default=os.path.join(RAIZ, "diagrama", "spec.json"))
    ap.add_argument("-o", "--salida", default=os.path.join(RAIZ, "skillswap.mdj"))
    args = ap.parse_args()
    if args.orden:
        ORDEN_PAQUETES[:] = [{"P": "Presentation", "A": "Application", "D": "Domain",
                              "IN": "Infrastructure"}[k] for k in args.orden.split(",")]
    with open(args.spec, encoding="utf-8") as f:
        spec = json.load(f)
    cm.validar(spec)
    proyecto = cm.generar(spec, args.ancho)
    with open(args.salida, "w", encoding="utf-8") as f:
        json.dump(proyecto, f, indent=2, ensure_ascii=False)
    print(f"{args.salida} escrito")


if __name__ == "__main__":
    main()
