package ru.mcs.q.division;

import java.util.*;

/**
 * Тетраэдральная сетка с пошаговым ростом.
 *
 * Правила:
 *  - Каждый шаг: рост на всех свободных гранях одновременно (snapshot)
 *  - Апекс нового тетраэдра = зеркало противоположной вершины через плоскость грани
 *  - Если апекс уже существует в пуле → только линкуем соседей (без дубликата)
 *  - Грани не пересекаются: два тетраэдра не могут иметь один апекс
 */
public class TetraMesh {

    public static final double STEP = 2.0;
    private static final double SQ3  = Math.sqrt(3.0);

    // Квант снаппинга: две точки считаются одной если ближе SNAP единиц
    // STEP * 0.01 = 0.02 — достаточно, чтобы поймать drift, но не слипнуть разные
    private static final double SNAP = STEP * 0.01;

    // ── Данные ────────────────────────────────────────────────────────────────
    public final List<TetraNode>       nodes       = new ArrayList<>();
    public final List<double[]>        vertices    = new ArrayList<>();  // {x,y,z}
    private final Map<String, Integer> vertexIndex = new HashMap<>();

    // ── Конструктор / сброс ───────────────────────────────────────────────────
    public TetraMesh() { reset(); }

    public void reset() {
        nodes.clear();
        vertices.clear();
        vertexIndex.clear();
        buildSeed();
    }

    private void buildSeed() {
        double a = STEP;
        double r = a / Math.sqrt(3.0);
        double h = a * Math.sqrt(2.0 / 3.0);

        int v0 = vertex( r,             0.0,  0.0);
        int v1 = vertex(-r * 0.5,  r * SQ3 * 0.5, 0.0);
        int v2 = vertex(-r * 0.5, -r * SQ3 * 0.5, 0.0);
        int v3 = vertex( 0.0,          0.0,   h);

        makeTetra(v0, v1, v2, v3);
    }

    // ── Один шаг роста ────────────────────────────────────────────────────────
    public void grow() {
        // Snapshot: собираем все свободные грани ДО того как начнём добавлять
        List<int[]> free = new ArrayList<>();
        for (int i = 0; i < nodes.size(); i++) {
            TetraNode n = nodes.get(i);
            for (int f = 0; f < 4; f++) {
                if (n.neighbors[f] == null) free.add(new int[]{i, f});
            }
        }

        // Для каждой свободной грани — пробуем вырасти
        for (int[] ff : free) {
            TetraNode src = nodes.get(ff[0]);
            int face      = ff[1];
            if (src.neighbors[face] != null) continue; // уже занято в этом шаге
            tryGrow(src, face);
        }
    }

    // ── Попытка роста через грань ─────────────────────────────────────────────
    private void tryGrow(TetraNode src, int faceIdx) {
        // Три вершины грани и вершина напротив
        int[] fi = otherVerts(faceIdx);
        int vA = src.vertexIds[fi[0]];
        int vB = src.vertexIds[fi[1]];
        int vC = src.vertexIds[fi[2]];
        int vO = src.vertexIds[faceIdx];

        double[] pA = vertices.get(vA);
        double[] pB = vertices.get(vB);
        double[] pC = vertices.get(vC);
        double[] pO = vertices.get(vO);

        // Апекс = зеркало vO через плоскость грани ABC
        double[] apex = reflect(pO, pA, pB, pC);
        String   key  = snapKey(apex[0], apex[1], apex[2]);

        if (vertexIndex.containsKey(key)) {
            // ── Апекс уже занят → ищем тетраэдр с этими 4 вершинами и линкуем ──
            int       vApex = vertexIndex.get(key);
            TetraNode nb    = findTetraWith(vA, vB, vC, vApex);
            if (nb != null) {
                int fB = faceContaining(nb, vA, vB, vC);
                if (fB >= 0 && nb.neighbors[fB] == null) {
                    link(src, faceIdx, nb, fB);
                }
            }
        } else {
            // ── Апекс свободен → создаём новый тетраэдр ──────────────────────
            int       vApex = vertex(apex[0], apex[1], apex[2]);
            TetraNode nb    = makeTetra(vA, vB, vC, vApex);
            // Грань nb противоположная vApex (= vertexIds[3]) содержит vA,vB,vC
            int fB = faceContaining(nb, vA, vB, vC);
            if (fB >= 0) link(src, faceIdx, nb, fB);
        }
    }

    // ── Связать двух соседей ──────────────────────────────────────────────────
    private void link(TetraNode a, int fA, TetraNode b, int fB) {
        a.neighbors[fA]    = b;
        a.neighborFace[fA] = fB;
        b.neighbors[fB]    = a;
        b.neighborFace[fB] = fA;
    }

    // ── Создать тетраэдр с 4 вершинами ───────────────────────────────────────
    private TetraNode makeTetra(int v0, int v1, int v2, int v3) {
        TetraNode n = new TetraNode(nodes.size());
        n.vertexIds[0] = v0;
        n.vertexIds[1] = v1;
        n.vertexIds[2] = v2;
        n.vertexIds[3] = v3;
        for (int i = 0; i < 4; i++) {
            double[] p  = vertices.get(n.vertexIds[i]);
            n.verts[i][0] = p[0];
            n.verts[i][1] = p[1];
            n.verts[i][2] = p[2];
        }
        n.updateCenter();
        for (int f = 0; f < 4; f++) n.faceSize[f] = 1f;
        nodes.add(n);
        return n;
    }

    // ── Пул вершин ────────────────────────────────────────────────────────────
    private int vertex(double x, double y, double z) {
        String key = snapKey(x, y, z);
        return vertexIndex.computeIfAbsent(key, k -> {
            int id = vertices.size();
            vertices.add(new double[]{x, y, z});
            return id;
        });
    }

    private String snapKey(double x, double y, double z) {
        long ix = Math.round(x / SNAP);
        long iy = Math.round(y / SNAP);
        long iz = Math.round(z / SNAP);
        return ix + "," + iy + "," + iz;
    }

    // ── Геометрические утилиты ────────────────────────────────────────────────

    // Индексы трёх вершин грани excl (грань напротив вершины excl)
    private static int[] otherVerts(int excl) {
        int[] r = new int[3]; int ri = 0;
        for (int i = 0; i < 4; i++) if (i != excl) r[ri++] = i;
        return r;
    }

    // Отражение точки p через плоскость abc
    private static double[] reflect(double[] p, double[] a, double[] b, double[] c) {
        double[] ab = sub(b, a);
        double[] ac = sub(c, a);
        double[] n  = cross(ab, ac);
        double   nl = len(n);
        if (nl < 1e-12) return p.clone();
        n[0] /= nl; n[1] /= nl; n[2] /= nl;
        double d = dot(sub(p, a), n);
        return new double[]{p[0] - 2*d*n[0], p[1] - 2*d*n[1], p[2] - 2*d*n[2]};
    }

    // Найти тетраэдр со всеми четырьмя заданными вершинами
    private TetraNode findTetraWith(int v0, int v1, int v2, int v3) {
        for (TetraNode n : nodes) {
            int match = 0;
            for (int v : n.vertexIds)
                if (v == v0 || v == v1 || v == v2 || v == v3) match++;
            if (match == 4) return n;
        }
        return null;
    }

    // Найти грань тетраэдра, которая содержит va, vb, vc
    // (грань f = все вершины кроме vertexIds[f])
    private int faceContaining(TetraNode n, int va, int vb, int vc) {
        for (int f = 0; f < 4; f++) {
            int found = 0;
            for (int v = 0; v < 4; v++) {
                if (v == f) continue;
                int vid = n.vertexIds[v];
                if (vid == va || vid == vb || vid == vc) found++;
            }
            if (found == 3) return f;
        }
        return -1;
    }

    // Центр и радиус меша (для камеры)
    public double[] center() {
        if (nodes.isEmpty()) return new double[]{0, 0, 0};
        double sx = 0, sy = 0, sz = 0;
        for (TetraNode n : nodes) { sx += n.rx; sy += n.ry; sz += n.rz; }
        int N = nodes.size();
        return new double[]{sx/N, sy/N, sz/N};
    }

    public double radius() {
        if (nodes.isEmpty()) return 1.0;
        double[] c = center();
        double   r = 0;
        for (TetraNode n : nodes) {
            double dx = n.rx - c[0], dy = n.ry - c[1], dz = n.rz - c[2];
            double d  = Math.sqrt(dx*dx + dy*dy + dz*dz);
            if (d > r) r = d;
        }
        return Math.max(r, 0.001);
    }

    // ── Векторная математика ──────────────────────────────────────────────────
    private static double[] sub(double[] a, double[] b) {
        return new double[]{a[0]-b[0], a[1]-b[1], a[2]-b[2]};
    }
    private static double dot(double[] a, double[] b) {
        return a[0]*b[0] + a[1]*b[1] + a[2]*b[2];
    }
    private static double[] cross(double[] a, double[] b) {
        return new double[]{
                a[1]*b[2]-a[2]*b[1],
                a[2]*b[0]-a[0]*b[2],
                a[0]*b[1]-a[1]*b[0]
        };
    }
    private static double len(double[] v) {
        return Math.sqrt(v[0]*v[0]+v[1]*v[1]+v[2]*v[2]);
    }
}