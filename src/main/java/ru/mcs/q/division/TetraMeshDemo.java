package ru.mcs.q.division;

import javax.swing.*;
import javax.swing.Timer;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.util.*;
import java.util.List;

public class TetraMeshDemo extends JFrame {

    static class TNode {
        final int id;
        int[]      vertexIds = new int[4];
        double[][] verts     = new double[4][3];
        TNode[]    neighbors = new TNode[4];
        int[]      nbrFace   = new int[4];
        double     cx, cy, cz;

        TNode(int id) { this.id = id; }

        void updateCenter() {
            cx = (verts[0][0] + verts[1][0] + verts[2][0] + verts[3][0]) * 0.25;
            cy = (verts[0][1] + verts[1][1] + verts[2][1] + verts[3][1]) * 0.25;
            cz = (verts[0][2] + verts[1][2] + verts[2][2] + verts[3][2]) * 0.25;
        }
    }

    static final double STEP = 2.0;
    static final double SQ3  = Math.sqrt(3.0);
    static final double SNAP = STEP * 0.001;

    final List<TNode>          nodes       = new ArrayList<>();
    final List<double[]>       vertices    = new ArrayList<>();
    final Map<String, Integer> vertexIndex = new HashMap<>();

    void meshReset() {
        nodes.clear();
        vertices.clear();
        vertexIndex.clear();
        double r = STEP / Math.sqrt(3.0);
        double h = STEP * Math.sqrt(2.0 / 3.0);
        int v0 = vert(r,           0,           0);
        int v1 = vert(-r * 0.5,    r * SQ3 * 0.5, 0);
        int v2 = vert(-r * 0.5,   -r * SQ3 * 0.5, 0);
        int v3 = vert(0,           0,            h);
        makeTet(v0, v1, v2, v3);
    }

    void grow() {
        // Снимок всех свободных граней на данный момент
        List<int[]> free = snapshot();
        // Обрабатываем КАЖДУЮ свободную грань
        for (int[] ff : free) {
            TNode src = nodes.get(ff[0]);
            if (src.neighbors[ff[1]] == null) {
                tryOut(src, ff[1]);
            }
        }
    }

    void tryOut(TNode src, int fi) {
        int[] oi = other(fi);
        int vA = src.vertexIds[oi[0]], vB = src.vertexIds[oi[1]], vC = src.vertexIds[oi[2]];
        int vO = src.vertexIds[fi];
        double[] pA = vertices.get(vA), pB = vertices.get(vB), pC = vertices.get(vC), pO = vertices.get(vO);

        // Отражение вершины vO относительно плоскости (A,B,C)
        double[] apex = reflect(pO, pA, pB, pC);

        // Ищем существующую вершину с такими же координатами (с точностью SNAP)
        String key = snapKey(apex[0], apex[1], apex[2]);
        int vApex;
        if (vertexIndex.containsKey(key)) {
            vApex = vertexIndex.get(key);
        } else {
            vApex = vert(apex[0], apex[1], apex[2]);
        }

        // Проверяем, не существует ли уже тетраэдр с этими четырьмя вершинами
        TNode nb = tetWith(vA, vB, vC, vApex);
        if (nb == null) {
            nb = makeTet(vA, vB, vC, vApex);
        }
        int fB = faceOf(nb, vA, vB, vC);
        if (fB >= 0 && nb.neighbors[fB] == null) {
            link(src, fi, nb, fB);
        }
    }

    // --------------------- ВСПОМОГАТЕЛЬНЫЕ МЕТОДЫ (без изменений) -------------------
    List<int[]> snapshot() {
        List<int[]> free = new ArrayList<>();
        for (int i = 0; i < nodes.size(); i++) {
            TNode n = nodes.get(i);
            for (int f = 0; f < 4; f++)
                if (n.neighbors[f] == null) free.add(new int[]{i, f});
        }
        return free;
    }

    int[] faceVerts(TNode n, int fi) {
        int[] oi = other(fi);
        return new int[]{n.vertexIds[oi[0]], n.vertexIds[oi[1]], n.vertexIds[oi[2]]};
    }

    TNode makeTet(int v0, int v1, int v2, int v3) {
        TNode n = new TNode(nodes.size());
        n.vertexIds = new int[]{v0, v1, v2, v3};
        for (int i = 0; i < 4; i++) {
            double[] p = vertices.get(n.vertexIds[i]);
            n.verts[i][0] = p[0];
            n.verts[i][1] = p[1];
            n.verts[i][2] = p[2];
        }
        n.updateCenter();
        nodes.add(n);
        return n;
    }

    int vert(double x, double y, double z) {
        return vertexIndex.computeIfAbsent(snapKey(x, y, z), k -> {
            int id = vertices.size();
            vertices.add(new double[]{x, y, z});
            return id;
        });
    }

    String snapKey(double x, double y, double z) {
        return Math.round(x / SNAP) + "," + Math.round(y / SNAP) + "," + Math.round(z / SNAP);
    }

    void link(TNode a, int fA, TNode b, int fB) {
        a.neighbors[fA] = b;
        a.nbrFace[fA] = fB;
        b.neighbors[fB] = a;
        b.nbrFace[fB] = fA;
    }

    static int[] other(int e) {
        int[] r = new int[3];
        int ri = 0;
        for (int i = 0; i < 4; i++) if (i != e) r[ri++] = i;
        return r;
    }

    TNode tetWith(int v0, int v1, int v2, int v3) {
        for (TNode n : nodes) {
            int m = 0;
            for (int v : n.vertexIds)
                if (v == v0 || v == v1 || v == v2 || v == v3) m++;
            if (m == 4) return n;
        }
        return null;
    }

    int faceOf(TNode n, int va, int vb, int vc) {
        for (int f = 0; f < 4; f++) {
            int found = 0;
            for (int v = 0; v < 4; v++) {
                if (v == f) continue;
                int id = n.vertexIds[v];
                if (id == va || id == vb || id == vc) found++;
            }
            if (found == 3) return f;
        }
        return -1;
    }

    static double[] reflect(double[] p, double[] a, double[] b, double[] c) {
        double[] n = cross(sub(b, a), sub(c, a));
        double nl = Math.sqrt(n[0]*n[0] + n[1]*n[1] + n[2]*n[2]);
        if (nl < 1e-12) return p.clone();
        n[0] /= nl; n[1] /= nl; n[2] /= nl;
        double d = dot(sub(p, a), n);
        return new double[]{p[0] - 2*d*n[0], p[1] - 2*d*n[1], p[2] - 2*d*n[2]};
    }

    static double[] sub(double[] a, double[] b) {
        return new double[]{a[0] - b[0], a[1] - b[1], a[2] - b[2]};
    }

    static double dot(double[] a, double[] b) {
        return a[0]*b[0] + a[1]*b[1] + a[2]*b[2];
    }

    static double[] cross(double[] a, double[] b) {
        return new double[]{a[1]*b[2] - a[2]*b[1],
                a[2]*b[0] - a[0]*b[2],
                a[0]*b[1] - a[1]*b[0]};
    }

    double[] meshCenter() {
        if (vertices.isEmpty()) return new double[]{0,0,0};
        double sx = 0, sy = 0, sz = 0;
        for (double[] v : vertices) { sx += v[0]; sy += v[1]; sz += v[2]; }
        int N = vertices.size();
        return new double[]{sx/N, sy/N, sz/N};
    }

    double meshRadius() {
        if (vertices.isEmpty()) return 1.0;
        double[] c = meshCenter();
        double r = 0;
        for (double[] v : vertices) {
            double dx = v[0] - c[0], dy = v[1] - c[1], dz = v[2] - c[2];
            double d = Math.sqrt(dx*dx + dy*dy + dz*dz);
            if (d > r) r = d;
        }
        return Math.max(r, 0.001);
    }

    // ---------------------------- ВИЗУАЛИЗАЦИЯ (без изменений) -------------------------
    static final int[][] EDGES = {{0,1},{0,2},{0,3},{1,2},{1,3},{2,3}};
    static final Color[] EC = {new Color(220,60,60), new Color(60,120,220),
            new Color(60,200,80), new Color(180,100,220),
            new Color(220,160,40), new Color(80,200,200)};

    double rotX = 0.4, rotY = -0.5, zoom = 1.0, panX = 0, panY = 0;
    int lastMX, lastMY;
    volatile BufferedImage buf;

    final JPanel canvas = new JPanel() {
        @Override protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            BufferedImage b = buf;
            if (b != null) g.drawImage(b, 0, 0, getWidth(), getHeight(), null);
            else { g.setColor(new Color(13,12,11)); g.fillRect(0,0,getWidth(),getHeight()); }
        }
    };
    final JLabel info = new JLabel("nodes: 1");

    public TetraMeshDemo() {
        super("TetraMesh Growth Demo");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLayout(new BorderLayout(4,4));
        getContentPane().setBackground(new Color(18,17,15));
        canvas.setBackground(new Color(13,12,11));
        canvas.setPreferredSize(new Dimension(960,720));
        add(canvas, BorderLayout.CENTER);
        add(buildBar(), BorderLayout.SOUTH);
        setupMouse();
        meshReset();
        pack();
        setLocationRelativeTo(null);
        setVisible(true);
        SwingUtilities.invokeLater(this::render);
    }

    JPanel buildBar() {
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT,8,5));
        bar.setBackground(new Color(20,19,17));
        JButton b1 = btn("1 шаг"), b5 = btn("5"), b10 = btn("10"), bR = btn("↺"), bA = btn("▶ Авто");
        info.setFont(new Font("Monospaced",Font.PLAIN,12));
        info.setForeground(new Color(100,180,130));
        bar.add(b1); bar.add(b5); bar.add(b10); bar.add(bR); bar.add(bA);
        bar.add(Box.createHorizontalStrut(12)); bar.add(info);
        b1.addActionListener(e -> { grow(); render(); });
        b5.addActionListener(e -> { for(int i=0;i<5;i++) grow(); render(); });
        b10.addActionListener(e -> { for(int i=0;i<10;i++) grow(); render(); });
        bR.addActionListener(e -> { meshReset(); render(); });
        Timer[] t = {null};
        bA.addActionListener(e -> {
            if (t[0] != null && t[0].isRunning()) {
                t[0].stop();
                bA.setText("▶ Авто");
            } else {
                t[0] = new Timer(80, ev -> {
                    if (nodes.size() < 4000) { grow(); render(); }
                    else { t[0].stop(); bA.setText("▶ Авто"); }
                });
                t[0].start();
                bA.setText("⏸ Стоп");
            }
        });
        return bar;
    }

    void setupMouse() {
        canvas.addMouseListener(new MouseAdapter() {
            @Override public void mousePressed(MouseEvent e) { lastMX = e.getX(); lastMY = e.getY(); }
        });
        canvas.addMouseMotionListener(new MouseMotionAdapter() {
            @Override public void mouseDragged(MouseEvent e) {
                int dx = e.getX() - lastMX, dy = e.getY() - lastMY;
                if (SwingUtilities.isLeftMouseButton(e)) { rotY += dx * 0.012; rotX += dy * 0.012; }
                if (SwingUtilities.isRightMouseButton(e)) { panX += dx; panY += dy; }
                lastMX = e.getX(); lastMY = e.getY();
                render();
            }
        });
        canvas.addMouseWheelListener(e -> {
            zoom *= (e.getWheelRotation() < 0) ? 1.1 : 0.9;
            zoom = Math.max(.05, Math.min(80, zoom));
            render();
        });
    }

    void render() {
        int cw = canvas.getWidth(), ch = canvas.getHeight();
        if (cw < 10 || ch < 10) { SwingUtilities.invokeLater(this::render); return; }
        BufferedImage b = new BufferedImage(cw, ch, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = b.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(new Color(13,12,11));
        g.fillRect(0, 0, cw, ch);

        int N = nodes.size();
        if (N > 0) {
            double r = meshRadius(), sc = Math.min(cw, ch) * 0.38 * zoom / r;
            double[] mc = meshCenter();
            double[] dep = new double[N];
            for (int i = 0; i < N; i++) dep[i] = proj(nodes.get(i).cx, nodes.get(i).cy, nodes.get(i).cz, cw, ch, sc, mc)[2];
            Integer[] ord = new Integer[N];
            for (int i = 0; i < N; i++) ord[i] = i;
            Arrays.sort(ord, (a, bb) -> Double.compare(dep[a], dep[bb]));
            double dMin = dep[ord[0]], dR = Math.max(dep[ord[N-1]] - dMin, .001);
            g.setStroke(new BasicStroke(1.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            for (int idx : ord) {
                TNode n = nodes.get(idx);
                double fog = 1. - 0.55 * (dep[idx] - dMin) / dR;
                double[][] sp = new double[4][];
                for (int v = 0; v < 4; v++)
                    sp[v] = proj(n.verts[v][0], n.verts[v][1], n.verts[v][2], cw, ch, sc, mc);
                for (int ei = 0; ei < EDGES.length; ei++) {
                    int va = EDGES[ei][0], vb = EDGES[ei][1];
                    Color base = EC[ei % EC.length];
                    float f = (float)(0.65 * fog);
                    g.setColor(new Color(clamp(base.getRed() * f + 8),
                            clamp(base.getGreen() * f + 8),
                            clamp(base.getBlue() * f + 8)));
                    g.drawLine((int)sp[va][0], (int)sp[va][1], (int)sp[vb][0], (int)sp[vb][1]);
                }
            }
        }
        g.setFont(new Font("Monospaced",Font.PLAIN,11));
        g.setColor(new Color(55,55,48));
        g.drawString("nodes:"+nodes.size()+"  verts:"+vertices.size()+"   ЛКМ=rotate  ПКМ=pan  wheel=zoom", 8, ch-6);
        g.dispose();
        buf = b;
        SwingUtilities.invokeLater(() -> { canvas.repaint(); info.setText("nodes:"+nodes.size()+"  verts:"+vertices.size()); });
    }

    double[] proj(double wx, double wy, double wz, int cw, int ch, double sc, double[] mc) {
        double ox = wx - mc[0], oy = wy - mc[1], oz = wz - mc[2];
        double cY = Math.cos(rotY), sY = Math.sin(rotY);
        double rx1 = ox * cY + oz * sY, ry1 = oy, rz1 = -ox * sY + oz * cY;
        double cX = Math.cos(rotX), sX = Math.sin(rotX);
        double rx2 = rx1, ry2 = ry1 * cX - rz1 * sX, rz2 = ry1 * sX + rz1 * cX;
        double d = Math.max(1., 800. + rz2 * sc), s = 800. / d;
        return new double[]{cw * 0.5 + panX + rx2 * s * sc,
                ch * 0.5 + panY - ry2 * s * sc,
                rz2};
    }

    static int clamp(float v) { return Math.max(0, Math.min(255, (int)v)); }

    JButton btn(String t) {
        JButton b = new JButton(t);
        b.setBackground(new Color(35,33,30));
        b.setForeground(new Color(200,198,194));
        b.setFocusPainted(false);
        b.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(55,53,48)),
                BorderFactory.createEmptyBorder(4,8,4,8)));
        b.setFont(new Font("SansSerif", Font.PLAIN, 11));
        return b;
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(TetraMeshDemo::new);
    }
}