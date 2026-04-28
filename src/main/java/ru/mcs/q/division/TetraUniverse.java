package ru.mcs.q.division;

import java.util.*;

public class TetraUniverse {

    public enum Mode { BIG_BANG, GROWTH }

    public static final double STEP = 2.0;

    // ── Физические параметры ──────────────────────────────────────────────────
    private double damping  = 0.999;
    private double lambda   = 0.000002;
    private double V        = 300.0;
    private double faceRate = 0.001;

    private static final float  MIN_FACE  = 0.05f;
    private static final double CLAMP_PHI = 6000.0;
    private static final double SQ3       = Math.sqrt(3.0);

    // ── Состояние ─────────────────────────────────────────────────────────────
    private final int W, H;
    private final List<TetraNode> nodes;
    private final BondStrength bondStrength;
    private final Mode mode;

    private int     tick            = 0;
    private boolean dynamicsEnabled = false;
    private double  minPhi, maxPhi;

    private double[][][] baseVerts;

    // ── Пул вершин ────────────────────────────────────────────────────────────
    private final List<Vertex>          vertexPool  = new ArrayList<>();
    private final Map<String, Integer>  vertexIndex = new HashMap<>();
    private final Map<EdgeKey, Integer> edgeCount   = new HashMap<>();
    private static final int MAX_EDGE = 5;

    // ── Геометрия меша ────────────────────────────────────────────────────────
    private double[] meshCenter = {0, 0, 0};
    private double   meshRadius = 1.0;

    // ── Конструктор ───────────────────────────────────────────────────────────
    public TetraUniverse(int W, int H, Mode mode, BondStrength bs) {
        this.W            = W;
        this.H            = H;
        this.mode         = mode;
        this.bondStrength = bs;
        this.nodes        = new ArrayList<>();
        buildSeed();
        initPhase();
    }

    // ── Начальный тетраэдр ────────────────────────────────────────────────────
    private void buildSeed() {
        mesh.reset();
        nodes.clear();
        nodes.addAll(mesh.nodes);
        saveBaseVerts();
        computeMeshBounds();
    }

    // ── Начальные условия поля ────────────────────────────────────────────────
    private void initPhase() {
        if (mode == Mode.BIG_BANG) {
            double e = 1.0 / Math.max(1, nodes.size());
            for (TetraNode n : nodes) { n.phase = e; n.velocity = 0; }
        } else {
            for (TetraNode n : nodes) { n.phase = 0; n.velocity = 0; }
        }
        for (TetraNode n : nodes)
            for (int f = 0; f < 4; f++) n.faceSize[f] = 1f;
    }

    // ── Возбуждение ───────────────────────────────────────────────────────────
    public void excite(int x, int y, double amount) {
        int idx = y * W + x;
        if (idx >= 0 && idx < nodes.size())
            nodes.get(idx).phase += amount;
    }

    public void exciteCenter(double amount) {
        if (!nodes.isEmpty()) nodes.get(0).phase += amount;
    }

    public void exciteById(int id, double amount) {
        if (id >= 0 && id < nodes.size())
            nodes.get(id).phase += amount;
    }

    // ── Главный тик ───────────────────────────────────────────────────────────
    public void step() {
        if (!dynamicsEnabled) return;
        tick++;

        final int      N     = nodes.size();
        final double[] delta = new double[N];

        // Фаза 1: Лапласиан
        for (int i = 0; i < N; i++) {
            TetraNode n   = nodes.get(i);
            double    lap = 0.0;
            for (int face = 0; face < 4; face++) {
                TetraNode nb = n.neighbors[face];
                if (nb == null) continue;
                lap += bondStrength.laplacianContrib(
                        face, n.neighborFace[face],
                        n.faceSize[face], nb.faceSize[n.neighborFace[face]],
                        n.phase, nb.phase);
            }
            double pot = -lambda * (n.phase * n.phase - V * V) * n.phase;
            delta[i] = lap + pot;
        }

        // Фаза 2: применить velocity + phase
        minPhi = Double.MAX_VALUE;
        maxPhi = -Double.MAX_VALUE;
        for (int i = 0; i < N; i++) {
            TetraNode n = nodes.get(i);
            n.velocity = n.velocity * damping + delta[i];
            n.phase   += n.velocity;
            if (n.phase >  CLAMP_PHI) n.phase =  CLAMP_PHI;
            if (n.phase < -CLAMP_PHI) n.phase = -CLAMP_PHI;
            if (n.phase < minPhi) minPhi = n.phase;
            if (n.phase > maxPhi) maxPhi = n.phase;
        }

        // Фаза 3: метрика + деформация вершин
        updateMetricAndVerts();

        // Фаза 4: рост там где горячо
        if (nodes.size() < 100) {
            mesh.grow();
            // синхронизировать nodes с mesh.nodes
            nodes.clear();
            nodes.addAll(mesh.nodes);
            saveBaseVerts();
        }


        // Фаза 5: пересчитать границы меша
        computeMeshBounds();
    }

    // ── Метрика + пересчёт вершин ─────────────────────────────────────────────
    // faceSize меняется от энергии.
    // Каждая вершина тетраэдра масштабируется относительно центра на faceSize
    // примыкающих граней — геометрия "дышит" вместе с полем.
    private void updateMetricAndVerts() {
        int N = nodes.size();
        for (int i = 0; i < N; i++) {
            TetraNode n      = nodes.get(i);
            double    localE = n.velocity * n.velocity;

            for (int face = 0; face < 4; face++) {
                TetraNode nb = n.neighbors[face];
                if (nb == null) continue;
                double sharedE = (localE + nb.velocity * nb.velocity) * 0.5;
                float  target  = (float) Math.min(1.0, MIN_FACE + sharedE * faceRate);
                float  cur     = n.faceSize[face];
                n.faceSize[face] = Math.max(MIN_FACE,
                        Math.min(1f, cur + (target - cur) * 0.01f));
            }

            double cx = n.rx, cy = n.ry, cz = n.rz;
            float  s0 = n.faceSize[0];
            float  s1 = n.faceSize[1];
            float  s2 = n.faceSize[2];
            float[] vertScale = {
                    (s1 + s2) / 2f,
                    (s0 + s2) / 2f,
                    (s0 + s1) / 2f,
                    (s0 + s1 + s2) / 3f
            };

            if (i < baseVerts.length) {
                for (int v = 0; v < 4; v++) {
                    double bx = baseVerts[i][v][0] - cx;
                    double by = baseVerts[i][v][1] - cy;
                    double bz = baseVerts[i][v][2] - cz;
                    float  sc = Math.max(MIN_FACE, vertScale[v]);
                    n.verts[v][0] = cx + bx * sc;
                    n.verts[v][1] = cy + by * sc;
                    n.verts[v][2] = cz + bz * sc;
                }
            }
        }
    }

    // ── Рост: растём там где горячо ───────────────────────────────────────────
    private void growHotRegions() {
        int sizeBefore = nodes.size();          // ← запомнить ДО

        List<TetraNode> snapshot = new ArrayList<>(nodes);
        for (TetraNode n : snapshot) {
            double localAvg = 0;
            int    cnt      = 0;
            for (int f = 0; f < 4; f++) {
                if (n.neighbors[f] != null) {
                    localAvg += Math.abs(n.neighbors[f].velocity);
                    cnt++;
                }
            }
            if (cnt > 0) localAvg /= cnt;

            double threshold = (cnt > 0) ? localAvg * 1.2 + 0.5 : 0.05;
            if (Math.abs(n.velocity) > threshold) {
                for (int f = 0; f < 4; f++) {
                    if (n.neighbors[f] == null) {
                        tryGrow(n, f);
                    }
                }
            }
        }

        if (nodes.size() > sizeBefore) saveBaseVerts();  // ← добавить
    }

    // ── Попытка роста через одну свободную грань ──────────────────────────────
    private void tryGrow(TetraNode src, int faceIdx) {
        int[] fi  = except(faceIdx);
        int   vA  = src.vertexIds[fi[0]];
        int   vB  = src.vertexIds[fi[1]];
        int   vC  = src.vertexIds[fi[2]];

        Vertex pA  = vertexPool.get(vA);
        Vertex pB  = vertexPool.get(vB);
        Vertex pC  = vertexPool.get(vC);
        Vertex opp = vertexPool.get(src.vertexIds[faceIdx]);

        double[] apex = reflectThrough(
                new double[]{opp.x, opp.y, opp.z},
                new double[]{pA.x,  pA.y,  pA.z},
                new double[]{pB.x,  pB.y,  pB.z},
                new double[]{pC.x,  pC.y,  pC.z}
        );

        String apexKey = snapKey(apex[0], apex[1], apex[2]);

        if (vertexIndex.containsKey(apexKey)) {
            // ── Апекс уже есть → там стоит тетраэдр, просто связываем соседей ──
            int       vApex    = vertexIndex.get(apexKey);
            TetraNode existing = findTetraWith(vA, vB, vC, vApex);
            if (existing != null && src.neighbors[faceIdx] == null) {
                int existFace = faceContaining(existing, vA, vB, vC);
                if (existFace >= 0 && existing.neighbors[existFace] == null) {
                    src.neighbors[faceIdx]           = existing;
                    src.neighborFace[faceIdx]        = existFace;
                    existing.neighbors[existFace]    = src;
                    existing.neighborFace[existFace] = faceIdx;
                }
            }
            return;   // ← не создаём дубликат
        }

        // ── Апекса нет → создаём новый тетраэдр ─────────────────────────────────
        int       vApex = findOrCreateVertex(apex[0], apex[1], apex[2]);
        TetraNode nb    = new TetraNode(nodes.size());

        int   faceB = 0;
        int[] ni    = except(faceB);
        nb.vertexIds[ni[0]] = vA;
        nb.vertexIds[ni[1]] = vB;
        nb.vertexIds[ni[2]] = vC;
        nb.vertexIds[faceB] = vApex;

        nb.updateCenter(vertexPool);
        for (int f = 0; f < 4; f++) nb.faceSize[f] = 1f;

        src.neighbors[faceIdx]    = nb;
        src.neighborFace[faceIdx] = faceB;
        nb.neighbors[faceB]       = src;
        nb.neighborFace[faceB]    = faceIdx;

        for (int[] e : new int[][]{{vA,vB},{vB,vC},{vA,vC},{vA,vApex},{vB,vApex},{vC,vApex}})
            edgeCount.merge(EdgeKey.of(e[0], e[1]), 1, Integer::sum);

        nodes.add(nb);
    }

    // Найти тетраэдр, который содержит ровно эти 4 вершины
    private TetraNode findTetraWith(int v0, int v1, int v2, int v3) {
        for (TetraNode n : nodes) {
            int match = 0;
            for (int v : n.vertexIds)
                if (v == v0 || v == v1 || v == v2 || v == v3) match++;
            if (match == 4) return n;
        }
        return null;
    }

    // Найти грань тетраэдра, которая содержит все три вершины va,vb,vc
// (грань i = все вершины кроме вершины i)
    private int faceContaining(TetraNode n, int va, int vb, int vc) {
        for (int f = 0; f < 4; f++) {
            int found = 0;
            for (int v = 0; v < 4; v++) {
                if (v == f) continue;
                if (n.vertexIds[v] == va || n.vertexIds[v] == vb || n.vertexIds[v] == vc)
                    found++;
            }
            if (found == 3) return f;
        }
        return -1;
    }

    // ── Сохранение базовых вершин ─────────────────────────────────────────────
    private void saveBaseVerts() {
        int N = nodes.size();
        baseVerts = new double[N][4][3];
        for (int i = 0; i < N; i++) {
            TetraNode n = nodes.get(i);
            for (int v = 0; v < 4; v++) {
                baseVerts[i][v][0] = n.verts[v][0];
                baseVerts[i][v][1] = n.verts[v][1];
                baseVerts[i][v][2] = n.verts[v][2];
            }
        }
    }

    // ── Пул вершин ────────────────────────────────────────────────────────────
    private static final double SNAP = 1e-6;

    private int findOrCreateVertex(double x, double y, double z) {
        String key = snapKey(x, y, z);
        return vertexIndex.computeIfAbsent(key, k -> {
            int id = vertexPool.size();
            vertexPool.add(new Vertex(id, x, y, z));
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
    private static int[] except(int excl) {
        int[] r = new int[3]; int ri = 0;
        for (int i = 0; i < 4; i++) if (i != excl) r[ri++] = i;
        return r;
    }

    private static double[] reflectThrough(double[] p,
                                           double[] a,
                                           double[] b,
                                           double[] c) {
        double[] n  = cross3(sub3(b, a), sub3(c, a));
        double   nl = Math.sqrt(n[0]*n[0] + n[1]*n[1] + n[2]*n[2]);
        if (nl < 1e-12) return new double[]{p[0], p[1], p[2]};
        double inv = 1.0 / nl;
        n[0] *= inv; n[1] *= inv; n[2] *= inv;
        double d = dot3(sub3(p, a), n);
        return new double[]{p[0]-2*d*n[0], p[1]-2*d*n[1], p[2]-2*d*n[2]};
    }

    private static double[] sub3(double[] a, double[] b) {
        return new double[]{a[0]-b[0], a[1]-b[1], a[2]-b[2]};
    }
    private static double dot3(double[] a, double[] b) {
        return a[0]*b[0] + a[1]*b[1] + a[2]*b[2];
    }
    private static double[] cross3(double[] a, double[] b) {
        return new double[]{
                a[1]*b[2] - a[2]*b[1],
                a[2]*b[0] - a[0]*b[2],
                a[0]*b[1] - a[1]*b[0]
        };
    }

    // ── Границы меша ─────────────────────────────────────────────────────────
    private void computeMeshBounds() {
        if (nodes.isEmpty()) return;
        double sx = 0, sy = 0, sz = 0;
        for (TetraNode n : nodes) { sx += n.rx; sy += n.ry; sz += n.rz; }
        int N = nodes.size();
        meshCenter = new double[]{sx / N, sy / N, sz / N};
        double maxR = 0;
        for (TetraNode n : nodes) {
            double dx = n.rx - meshCenter[0];
            double dy = n.ry - meshCenter[1];
            double dz = n.rz - meshCenter[2];
            double r  = Math.sqrt(dx*dx + dy*dy + dz*dz);
            if (r > maxR) maxR = r;
        }
        meshRadius = Math.max(maxR, 0.001);
    }

    // ── Сброс ─────────────────────────────────────────────────────────────────
    public void reset() {
        tick = 0;
        dynamicsEnabled = false;
        buildSeed();
        initPhase();
    }

    // ── normalizedPhase ───────────────────────────────────────────────────────
    public double normalizedPhase(TetraNode n) {
        double range = maxPhi - minPhi;
        if (range < 1e-9) return 0.5;
        return (n.phase - minPhi) / range;
    }

    // ── Геттеры/сеттеры ───────────────────────────────────────────────────────
    public List<TetraNode> getNodes()           { return Collections.unmodifiableList(nodes); }
    public int     getW()                        { return W; }
    public int     getH()                        { return H; }
    public int     getTick()                     { return tick; }
    public Mode    getMode()                     { return mode; }
    public boolean isDynamicsEnabled()           { return dynamicsEnabled; }
    public void    setDynamicsEnabled(boolean v) { dynamicsEnabled = v; }
    public void    setDamping(double v)          { damping  = v; }
    public void    setLambda(double v)           { lambda   = v; }
    public void    setV(double v)                { this.V   = v; }
    public void    setFaceRate(double v)         { faceRate = v; }
    public BondStrength getBondStrength()        { return bondStrength; }
    public double[] getMeshCenter()              { return meshCenter; }
    public double   getMeshRadius()              { return meshRadius; }

    public double getTotalEnergy() {
        double sum = 0;
        for (TetraNode n : nodes) sum += n.velocity * n.velocity;
        return sum;
    }
    public double getMinPhi() { return minPhi; }
    public double getMaxPhi() { return maxPhi; }

    private final TetraMesh mesh = new TetraMesh();
}
