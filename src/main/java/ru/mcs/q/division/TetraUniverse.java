package ru.mcs.q.division;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class TetraUniverse {

    public enum Mode { BIG_BANG, GROWTH }

    // ── Мировой шаг — расстояние между центрами тетраэдров ───────────────────
    public static final double STEP = 2.0;

    // ── Физические параметры ──────────────────────────────────────────────────
    private double damping  = 0.999;
    private double lambda   = 0.000002;
    private double V        = 300.0;
    private double faceRate = 0.001;

    private static final float  MIN_FACE  = 0.05f;
    private static final double CLAMP_PHI = 6000.0;

    // ── Состояние ─────────────────────────────────────────────────────────────
    private final int W, H;
    private final List<TetraNode> nodes;
    private final BondStrength bondStrength;
    private final Mode mode;

    private int     tick            = 0;
    private boolean dynamicsEnabled = false;
    private double  minPhi, maxPhi;

    // ── Базовые вершины (без деформации) — храним для пересчёта ──────────────
    // baseVerts[nodeIndex][vertexIndex][xyz]
    private double[][][] baseVerts;

    // ── Конструктор ───────────────────────────────────────────────────────────
    public TetraUniverse(int W, int H, Mode mode, BondStrength bs) {
        this.W            = W;
        this.H            = H;
        this.mode         = mode;
        this.bondStrength = bs;
        this.nodes        = new ArrayList<>(W * H);
        buildTorus();
        initPhase();
    }

    // ── Построение тора ───────────────────────────────────────────────────────
    private void buildTorus() {
        for (int i = 0; i < W * H; i++) nodes.add(new TetraNode(i));

        for (int y = 0; y < H; y++) {
            for (int x = 0; x < W; x++) {
                TetraNode cur   = node(x, y);
                TetraNode right = node((x + 1) % W, y);
                TetraNode up    = node(x, (y + 1) % H);
                cur.bond(0, right, 2);
                cur.bond(1, up,    3);
            }
        }

        // Вычисляем 3D-вершины и запоминаем базовые
        computeVerts3D();
        saveBaseVerts();
    }

    // ── 3D-геометрия тетраэдров ───────────────────────────────────────────────
    //
    //  "Вверх" (x+y)%2==0 : основание внизу (z = -h/4), вершина вверху (z = +3h/4)
    //  "Вниз"  (x+y)%2==1 : основание вверху (z = +h/4), вершина вниз  (z = -3h/4)
    //  Основание — правильный треугольник, вращается на 60° для Down
    //  чтобы соседи делили рёбра визуально.
    //
    //  Face индексы:
    //    Face 3 TRANSPARENT = основание v0,v1,v2
    //    Face 0 RED         = боковая   v1,v2,v3
    //    Face 1 BLUE        = боковая   v0,v2,v3
    //    Face 2 GREEN       = боковая   v0,v1,v3
    private void computeVerts3D() {
        for (int y = 0; y < H; y++) {
            for (int x = 0; x < W; x++) {
                setVerts(node(x, y), x, y, 1.0f);
            }
        }
    }

    private void setVerts(TetraNode n, int x, int y, float scale) {
        double cx = x * STEP;
        double cy = y * STEP;

        double r = STEP * 0.55 * scale;           // радиус описанной окружности основания
        double h = r * Math.sqrt(8.0 / 3.0);      // высота правильного тетраэдра

        boolean up       = (x + y) % 2 == 0;
        double  zBase    = up ? -h * 0.25 :  h * 0.25;
        double  zApex    = up ?  h * 0.75 : -h * 0.75;
        // Down-тетраэдр поворачивает основание на 60°, чтобы рёбра соседей совпадали
        double  baseRot  = up
                ? Math.PI / 2.0
                : Math.PI / 2.0 + Math.PI / 3.0;

        // v0, v1, v2 — вершины основания (face TRANSPARENT)
        for (int k = 0; k < 3; k++) {
            double angle = baseRot + k * 2.0 * Math.PI / 3.0;
            n.verts[k][0] = cx + r * Math.cos(angle);
            n.verts[k][1] = cy + r * Math.sin(angle);
            n.verts[k][2] = zBase;
        }
        // v3 — apex
        n.verts[3][0] = cx;
        n.verts[3][1] = cy;
        n.verts[3][2] = zApex;

        n.updateCenter();
    }

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

    // ── Начальные условия ─────────────────────────────────────────────────────
    private void initPhase() {
        if (mode == Mode.BIG_BANG) {
            double e = 1.0 / nodes.size();
            for (TetraNode n : nodes) { n.phase = e; n.velocity = 0; }
        } else {
            for (TetraNode n : nodes) { n.phase = 0; n.velocity = 0; }
        }
        for (TetraNode n : nodes)
            for (int f = 0; f < 4; f++) n.faceSize[f] = 1f;
    }

    // ── Возбуждение ───────────────────────────────────────────────────────────
    public void excite(int x, int y, double amount) {
        if (x < 0 || x >= W || y < 0 || y >= H) return;
        node(x, y).phase += amount;
    }

    public void exciteCenter(double amount) { excite(W / 2, H / 2, amount); }

    // ── Главный тик ───────────────────────────────────────────────────────────
    public void step() {
        if (!dynamicsEnabled) return;
        tick++;

        final int      N     = nodes.size();
        final double[] delta = new double[N];

        // Фаза 1: Лапласиан для всех
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

        // Фаза 2: применить
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

            // Обновить faceSize
            for (int face = 0; face < 4; face++) {
                TetraNode nb = n.neighbors[face];
                if (nb == null) continue;
                double sharedE = (localE + nb.velocity * nb.velocity) * 0.5;
                float  target  = (float) Math.min(1.0, MIN_FACE + sharedE * faceRate);
                float  cur     = n.faceSize[face];
                n.faceSize[face] = Math.max(MIN_FACE,
                        Math.min(1f, cur + (target - cur) * 0.01f));
            }

            // Пересчитать вершины: масштаб = среднее faceSize боковых граней (0,1,2)
            double cx = n.rx, cy = n.ry, cz = n.rz; // центр из updateCenter()
            float  s0 = n.faceSize[0];
            float  s1 = n.faceSize[1];
            float  s2 = n.faceSize[2];

            // Каждая вершина принадлежит определённым граням (см. EDGES в TetraNode)
            // v0 принадлежит граням 1,2,3 → масштаб = avg(s1, s2)
            // v1 принадлежит граням 0,2,3 → масштаб = avg(s0, s2)
            // v2 принадлежит граням 0,1,3 → масштаб = avg(s0, s1)
            // v3 принадлежит граням 0,1,2 → масштаб = avg(s0, s1, s2)
            float[] vertScale = {
                    (s1 + s2) / 2f,
                    (s0 + s2) / 2f,
                    (s0 + s1) / 2f,
                    (s0 + s1 + s2) / 3f
            };

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

    // ── Сброс ─────────────────────────────────────────────────────────────────
    public void reset() {
        tick            = 0;
        dynamicsEnabled = false;
        initPhase();
        computeVerts3D();   // восстановить базовые позиции
    }

    // ── Вспомогательные ───────────────────────────────────────────────────────
    public TetraNode node(int x, int y)  { return nodes.get(y * W + x); }

    public double normalizedPhase(TetraNode n) {
        double range = maxPhi - minPhi;
        if (range < 1e-9) return 0.5;
        return (n.phase - minPhi) / range;
    }

    public List<TetraNode> getNodes()          { return Collections.unmodifiableList(nodes); }
    public int     getW()                       { return W; }
    public int     getH()                       { return H; }
    public int     getTick()                    { return tick; }
    public Mode    getMode()                    { return mode; }
    public boolean isDynamicsEnabled()          { return dynamicsEnabled; }
    public void    setDynamicsEnabled(boolean v){ dynamicsEnabled = v; }
    public void    setDamping(double v)         { damping  = v; }
    public void    setLambda(double v)          { lambda   = v; }
    public void    setV(double v)               { this.V   = v; }
    public void    setFaceRate(double v)        { faceRate = v; }
    public BondStrength getBondStrength()       { return bondStrength; }

    public double getTotalEnergy() {
        double sum = 0;
        for (TetraNode n : nodes) sum += n.velocity * n.velocity;
        return sum;
    }

    public double getMinPhi() { return minPhi; }
    public double getMaxPhi() { return maxPhi; }
}