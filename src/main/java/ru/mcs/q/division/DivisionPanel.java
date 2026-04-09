package ru.mcs.q.division;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.util.List;

/**
 * Swing-панель для визуализации TetraUniverse.
 *
 * Левая часть  — холст: тепловая карта поля + сетка рёбер деформированная faceSize
 * Правая часть — панель управления: слайдеры + кнопки
 *
 * Управление мышью на холсте:
 *   ЛКМ drag — возбудить поле +amount
 *   ПКМ drag — погасить поле  -amount
 *   Колесо   — zoom
 */
public class DivisionPanel extends JPanel {

    // ── Ссылка на модель ──────────────────────────────────────────────────────
    private final TetraUniverse universe;

    // ── Рендер ────────────────────────────────────────────────────────────────
    private BufferedImage canvas;
    private final JPanel  canvasPanel;
    private final JLabel  infoLabel;

    // Камера (pan + zoom)
    private double camX = 0, camY = 0;
    private double zoom = 1.0;

    // ── Таймер анимации ───────────────────────────────────────────────────────
    private final Timer animTimer;
    private int stepsPerFrame = 1;

    // ── Слайдеры ──────────────────────────────────────────────────────────────
    private JSlider slDamping, slLambda, slV, slFaceRate;
    private JSlider slSame, slDiff, slDiffEq;
    private JLabel  lblDamping, lblLambda, lblV, lblFaceRate;
    private JLabel  lblSame, lblDiff, lblDiffEq;

    // ── Камера 3D ────────────────────────────────────────────────────────────────
    private double camRotX   =  0.45;   // наклон вверх/вниз (радианы)
    private double camRotY   = -0.55;   // поворот влево/вправо
    private double camZoom   =  18.0;   // расстояние камеры
    private double camDist   =  800.0;  // фокусное расстояние (перспектива)
    private double panX      =  0.0;    // смещение центра X
    private double panY      =  0.0;    // смещение центра Y
    private int    lastMX, lastMY;
    private boolean rotating = false;
    private boolean panning  = false;

    // ── Цветовая карта (двухполярная) ─────────────────────────────────────────
    // 0.0 → синий | 0.5 → чёрный | 1.0 → белый
    private static Color phaseColor(double t) {
        t = Math.max(0, Math.min(1, t));
        if (t < 0.25) {
            // синий → голубой
            float r = 0f;
            float g = (float)(t / 0.25);
            float b = 1f;
            return new Color(r, g * 0.5f, b);
        } else if (t < 0.5) {
            // голубой → чёрный
            float k = (float)((0.5 - t) / 0.25);
            return new Color(0f, k * 0.3f, k * 0.6f);
        } else if (t < 0.75) {
            // чёрный → жёлтый
            float k = (float)((t - 0.5) / 0.25);
            return new Color(k, k * 0.85f, 0f);
        } else {
            // жёлтый → белый
            float k = (float)((t - 0.75) / 0.25);
            return new Color(1f, 0.85f + k * 0.15f, k);
        }
    }

    // ── Конструктор ───────────────────────────────────────────────────────────
    public DivisionPanel(TetraUniverse universe) {
        this.universe = universe;
        setLayout(new BorderLayout(4, 4));
        setBackground(new Color(18, 17, 15));

        // Холст
        canvasPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                if (canvas != null) {
                    g.drawImage(canvas, 0, 0,
                            getWidth(), getHeight(), null);
                }
            }
        };
        canvasPanel.setBackground(new Color(13, 12, 11));
        canvasPanel.setPreferredSize(new Dimension(800, 700));
        add(canvasPanel, BorderLayout.CENTER);

        // Правая панель управления
        JPanel ctrl = buildControlPanel();
        add(ctrl, BorderLayout.EAST);

        // Нижняя строка статуса
        infoLabel = new JLabel(" ");
        infoLabel.setFont(new Font("Monospaced", Font.PLAIN, 11));
        infoLabel.setForeground(new Color(120, 120, 110));
        infoLabel.setBorder(new EmptyBorder(3, 8, 3, 8));
        infoLabel.setBackground(new Color(20, 19, 17));
        infoLabel.setOpaque(true);
        add(infoLabel, BorderLayout.SOUTH);

        // Мышь на холсте
        setupMouseHandlers();

        // Таймер — 30 fps
        animTimer = new Timer(33, e -> {
            for (int i = 0; i < stepsPerFrame; i++) universe.step();
            renderToCanvas();
            canvasPanel.repaint();
            updateInfo();
        });
        animTimer.start();

        // Первый рендер (статика)
        renderToCanvas();
    }

    // ── Построение панели управления ──────────────────────────────────────────
    private JPanel buildControlPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(new Color(20, 19, 17));
        panel.setBorder(new EmptyBorder(8, 8, 8, 8));
        panel.setPreferredSize(new Dimension(230, 600));

        // ── Кнопки ────────────────────────────────────────────────────────────
        JPanel btnRow = new JPanel(new GridLayout(2, 2, 4, 4));
        btnRow.setBackground(new Color(20, 19, 17));
        btnRow.setMaximumSize(new Dimension(240, 64));

        JButton btnStart  = darkButton("▶ Динамика");
        JButton btnStep   = darkButton("1 шаг");
        JButton btnReset  = darkButton("↺ Сброс");
        JButton btnExcite = darkButton("⚡ Возбудить");

        btnStart.addActionListener(e -> {
            boolean en = !universe.isDynamicsEnabled();
            universe.setDynamicsEnabled(en);
            btnStart.setText(en ? "⏸ Пауза" : "▶ Динамика");
        });
        btnStep.addActionListener(e -> universe.step());
        btnReset.addActionListener(e -> {
            universe.reset();
            universe.setDynamicsEnabled(false);
            btnStart.setText("▶ Динамика");
            renderToCanvas();
        });
        btnExcite.addActionListener(e -> {
            universe.exciteCenter(600.0);
            renderToCanvas();
        });

        btnRow.add(btnStart); btnRow.add(btnStep);
        btnRow.add(btnReset); btnRow.add(btnExcite);
        panel.add(btnRow);
        panel.add(Box.createVerticalStrut(10));

        // ── Шагов за кадр ─────────────────────────────────────────────────────
        JPanel spfRow = new JPanel(new BorderLayout(4, 0));
        spfRow.setBackground(new Color(20, 19, 17));
        spfRow.setMaximumSize(new Dimension(240, 28));
        JLabel spfLbl = miniLabel("Шагов/кадр:");
        JSlider slSpf = new JSlider(1, 20, 1);
        styleSlider(slSpf);
        JLabel spfVal = miniLabel("1");
        slSpf.addChangeListener(e -> {
            stepsPerFrame = slSpf.getValue();
            spfVal.setText(String.valueOf(stepsPerFrame));
        });
        spfRow.add(spfLbl, BorderLayout.WEST);
        spfRow.add(slSpf,  BorderLayout.CENTER);
        spfRow.add(spfVal, BorderLayout.EAST);
        panel.add(spfRow);
        panel.add(Box.createVerticalStrut(10));

        // ── Физика поля ───────────────────────────────────────────────────────
        panel.add(sectionTitle("Физика поля"));

        slDamping = makeSlider(990, 1000, 999);
        lblDamping = miniLabel("0.999");
        slDamping.addChangeListener(e -> {
            double v = slDamping.getValue() / 1000.0;
            universe.setDamping(v);
            lblDamping.setText(String.format("%.3f", v));
        });
        panel.add(sliderRow("Затухание", slDamping, lblDamping));

        slLambda = makeSlider(0, 100, 2);
        lblLambda = miniLabel("2e-6");
        slLambda.addChangeListener(e -> {
            double v = slLambda.getValue() * 1e-6;
            universe.setLambda(v);
            lblLambda.setText(slLambda.getValue() + "e-6");
        });
        panel.add(sliderRow("λ потенциал", slLambda, lblLambda));

        slV = makeSlider(50, 600, 300);
        lblV = miniLabel("300");
        slV.addChangeListener(e -> {
            universe.setV(slV.getValue());
            lblV.setText(String.valueOf(slV.getValue()));
        });
        panel.add(sliderRow("Вакуум V", slV, lblV));

        slFaceRate = makeSlider(1, 100, 10);
        lblFaceRate = miniLabel("0.010");
        slFaceRate.addChangeListener(e -> {
            double v = slFaceRate.getValue() * 0.001;
            universe.setFaceRate(v);
            lblFaceRate.setText(String.format("%.3f", v));
        });
        panel.add(sliderRow("Метрика faceRate", slFaceRate, lblFaceRate));
        panel.add(Box.createVerticalStrut(10));

        // ── Правила связей ────────────────────────────────────────────────────
        panel.add(sectionTitle("Правила связей"));

        BondStrength bs = universe.getBondStrength();

        slSame = makeSlider(0, 500, (int)(bs.getSameBond() * 1000));
        lblSame = miniLabel(String.format("%.3f", bs.getSameBond()));
        slSame.addChangeListener(e -> {
            float v = slSame.getValue() / 1000f;
            bs.setSameBond(v);
            lblSame.setText(String.format("%.3f", v));
        });
        panel.add(sliderRow("Same bond", slSame, lblSame));

        slDiff = makeSlider(0, 500, (int)(bs.getDiffBond() * 1000));
        lblDiff = miniLabel(String.format("%.3f", bs.getDiffBond()));
        slDiff.addChangeListener(e -> {
            float v = slDiff.getValue() / 1000f;
            bs.setDiffBond(v);
            lblDiff.setText(String.format("%.3f", v));
        });
        panel.add(sliderRow("Diff bond", slDiff, lblDiff));

        slDiffEq = makeSlider(0, 1000, (int)(bs.getDiffEquilibrium() * 1000));
        lblDiffEq = miniLabel(String.format("%.3f", bs.getDiffEquilibrium()));
        slDiffEq.addChangeListener(e -> {
            float v = slDiffEq.getValue() / 1000f;
            bs.setDiffEquilibrium(v);
            lblDiffEq.setText(String.format("%.3f", v));
        });
        panel.add(sliderRow("Diff equilibrium", slDiffEq, lblDiffEq));
        panel.add(Box.createVerticalStrut(10));

        // ── Легенда ───────────────────────────────────────────────────────────
        panel.add(sectionTitle("Цвета фаз"));
        panel.add(buildLegend());
        panel.add(Box.createVerticalGlue());

        return panel;
    }

    // ── Цвета граней (статические — для рёбер треугольников) ──────────────────
    private static final Color[] FACE_COLORS = {
            new Color(220,  60,  60),   // 0 = RED
            new Color( 60, 120, 220),   // 1 = BLUE
            new Color( 60, 200,  80),   // 2 = GREEN
            new Color( 60,  60,  55),   // 3 = TRANSPARENT
    };

    // ── Перспективная проекция одной точки ────────────────────────────────────────
// Возвращает {screenX, screenY, depth}
    private double[] project(double wx, double wy, double wz,
                             int cw, int ch) {
        // 1. Сдвигаем мировые координаты к центру сетки
        double ox = wx - universe.getW() * TetraUniverse.STEP * 0.5;
        double oy = wy - universe.getH() * TetraUniverse.STEP * 0.5;
        double oz = wz;

        // 2. Вращение вокруг Y (camRotY)
        double cosY = Math.cos(camRotY), sinY = Math.sin(camRotY);
        double rx1  =  ox * cosY + oz * sinY;
        double ry1  =  oy;
        double rz1  = -ox * sinY + oz * cosY;

        // 3. Вращение вокруг X (camRotX)
        double cosX = Math.cos(camRotX), sinX = Math.sin(camRotX);
        double rx2  =  rx1;
        double ry2  =  ry1 * cosX - rz1 * sinX;
        double rz2  =  ry1 * sinX + rz1 * cosX;

        // 4. Перспективная проекция
        double depth = camDist + rz2 * camZoom;
        if (depth < 1.0) depth = 1.0;
        double scale = camDist / depth;

        double sx = cw * 0.5 + panX + rx2 * scale * camZoom;
        double sy = ch * 0.5 + panY - ry2 * scale * camZoom;

        return new double[]{ sx, sy, rz2 };
    }

    // ── Цвет ребра из двух граней ─────────────────────────────────────────────────
// Ребро принадлежит двум граням. Берём "главный" цвет — не TRANSPARENT.
// Яркость от phase + faceSize.
    private Color edgeColor(TetraNode n, int faceA, int faceB, double phi) {
        int   colorFace = (faceA != 3) ? faceA : faceB;   // не TRANSPARENT
        Color base      = FACE_COLORS[colorFace];
        float sz        = n.faceSize[colorFace];
        float bri       = 0.35f + (float) phi * 0.65f;
        float s         = sz * bri;
        return new Color(
                clamp(base.getRed()   * s + 15),
                clamp(base.getGreen() * s + 15),
                clamp(base.getBlue()  * s + 15));
    }

    // ── Главный рендер ────────────────────────────────────────────────────────────
    private void renderToCanvas() {
        int cw = Math.max(1, canvasPanel.getWidth());
        int ch = Math.max(1, canvasPanel.getHeight());

        if (canvas == null
                || canvas.getWidth()  != cw
                || canvas.getHeight() != ch) {
            canvas = new BufferedImage(cw, ch, BufferedImage.TYPE_INT_RGB);
        }

        Graphics2D g = canvas.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL,
                RenderingHints.VALUE_STROKE_PURE);
        g.setColor(new Color(13, 12, 11));
        g.fillRect(0, 0, cw, ch);

        List<TetraNode> nodes = universe.getNodes();

        // ── Сортировка по глубине (painter's algorithm) ───────────────────────────
        // Проецируем центр каждого узла, сортируем от дальнего к ближнему
        int N = nodes.size();
        double[] depthArr = new double[N];
        for (int i = 0; i < N; i++) {
            TetraNode n = nodes.get(i);
            depthArr[i] = project(n.rx, n.ry, n.rz, cw, ch)[2];
        }
        Integer[] order = new Integer[N];
        for (int i = 0; i < N; i++) order[i] = i;
        // сортируем от дальнего (малое z после поворота) к ближнему
        java.util.Arrays.sort(order,
                (a, b) -> Double.compare(depthArr[a], depthArr[b]));

        // ── Рисуем тетраэдры ──────────────────────────────────────────────────────
        for (int idx : order) {
            TetraNode n   = nodes.get(idx);
            double    phi = universe.normalizedPhase(n);

            // Проецируем 4 вершины
            double[][] sp = new double[4][3];
            for (int v = 0; v < 4; v++) {
                sp[v] = project(n.verts[v][0], n.verts[v][1], n.verts[v][2],
                        cw, ch);
            }

            // Средняя глубина для толщины линий — ближние чуть толще
            double depth = depthArr[idx];
            float  lw    = (float) Math.max(0.5,
                    2.5 * (1.0 - (depth + 20) / 60.0));

            // 6 рёбер тетраэдра
            for (int[] edge : TetraNode.EDGES) {
                int va = edge[0], vb = edge[1];
                int fa = edge[2], fb = edge[3];

                // Пропускаем рёбра основания (оба конца в грани TRANSPARENT=3)
                // — можно раскомментировать для "скрытой грани"
                // if (fa == 3 && fb == 3) continue;

                Color ec = edgeColor(n, fa, fb, phi);
                float sz = n.faceSize[Math.min(fa, fb) < 3
                        ? Math.min(fa, fb) : Math.max(fa, fb)];
                float lineW = Math.max(0.5f, lw * (0.4f + sz * 0.6f));

                g.setStroke(new BasicStroke(lineW,
                        BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g.setColor(ec);
                g.drawLine((int) sp[va][0], (int) sp[va][1],
                        (int) sp[vb][0], (int) sp[vb][1]);
            }
        }

        // ── Оверлей: инфо ────────────────────────────────────────────────────────
        g.setFont(new Font("Monospaced", Font.PLAIN, 11));
        g.setColor(new Color(70, 70, 60));
        g.drawString("Mode: " + universe.getMode()
                        + "  " + universe.getW() + "×" + universe.getH()
                        + "  ЛКМ drag=вращение  ПКМ drag=пан  колесо=zoom",
                8, ch - 6);

        g.dispose();
    }

    /** Clamp float→int [0..255] */
    private static int clamp(float v) {
        return Math.max(0, Math.min(255, (int) v));
    }

    // ── Строка статуса ────────────────────────────────────────────────────────
    private void updateInfo() {
        infoLabel.setText(String.format(
                "Tick: %d  |  ΣE: %.1f  |  φ [%.1f .. %.1f]  |  %s  |  %s",
                universe.getTick(),
                universe.getTotalEnergy(),
                universe.getMinPhi(),
                universe.getMaxPhi(),
                universe.isDynamicsEnabled() ? "RUNNING" : "PAUSED",
                universe.getBondStrength()
        ));
    }

    // ── Мышь ─────────────────────────────────────────────────────────────────
    private void setupMouseHandlers() {
        canvasPanel.addMouseListener(new MouseAdapter() {
            @Override public void mousePressed(MouseEvent e) {
                lastMX = e.getX(); lastMY = e.getY();
                if (SwingUtilities.isRightMouseButton(e))  panning  = true;
                else                                        rotating = true;
            }
            @Override public void mouseReleased(MouseEvent e) {
                rotating = false; panning = false;
            }
            @Override public void mouseClicked(MouseEvent e) {
                // ЛКМ одиночный клик — возбудить ближайший узел
                if (SwingUtilities.isLeftMouseButton(e) && !rotating) {
                    exciteNearest(e.getX(), e.getY(),
                            SwingUtilities.isLeftMouseButton(e) ? 2000.0 : -1000.0);
                }
            }
        });

        canvasPanel.addMouseMotionListener(new MouseMotionAdapter() {
            @Override public void mouseDragged(MouseEvent e) {
                int dx = e.getX() - lastMX;
                int dy = e.getY() - lastMY;
                if (rotating) {
                    camRotY += dx * 0.012;
                    camRotX += dy * 0.012;
                    camRotX  = Math.max(-Math.PI / 2, Math.min(Math.PI / 2, camRotX));
                } else if (panning) {
                    panX += dx;
                    panY += dy;
                }
                lastMX = e.getX(); lastMY = e.getY();
            }
        });

        canvasPanel.addMouseWheelListener(e -> {
            camZoom *= (e.getWheelRotation() < 0) ? 0.9 : 1.1;
            camZoom  = Math.max(4.0, Math.min(200.0, camZoom));
        });
    }

    /** Найти узел ближайший к точке на экране и возбудить */
    private void exciteNearest(int sx, int sy, double amount) {
        int    bestId   = -1;
        double bestDist = Double.MAX_VALUE;
        int    cw       = canvasPanel.getWidth();
        int    ch       = canvasPanel.getHeight();

        for (TetraNode n : universe.getNodes()) {
            double[] p = project(n.rx, n.ry, n.rz, cw, ch);
            double   d = (p[0] - sx) * (p[0] - sx) + (p[1] - sy) * (p[1] - sy);
            if (d < bestDist) { bestDist = d; bestId = n.id; }
        }
        if (bestId >= 0) {
            TetraNode n = universe.getNodes().get(bestId);
            int x = bestId % universe.getW();
            int y = bestId / universe.getW();
            universe.excite(x, y, amount);
        }
    }

    private void applyExcite(MouseEvent e) {
        int cw   = canvasPanel.getWidth();
        int ch   = canvasPanel.getHeight();
        int W    = universe.getW();
        int H    = universe.getH();
        double cellW = (cw - 40) / (double) W * zoom;
        double cellH = (ch - 40) / (double) H * zoom;
        double offX  = 20 + camX;
        double offY  = 20 + camY;

        int gx = (int)((e.getX() - offX) / cellW);
        int gy = (int)((e.getY() - offY) / cellH);

        double amount = SwingUtilities.isRightMouseButton(e) ? -1000.0 : 2000.0;
        universe.excite(gx, gy, amount);
    }

    // ── Вспомогательные UI-методы ─────────────────────────────────────────────
    private JButton darkButton(String text) {
        JButton b = new JButton(text);
        b.setBackground(new Color(35, 33, 30));
        b.setForeground(new Color(200, 198, 194));
        b.setFocusPainted(false);
        b.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(55, 53, 48)),
                new EmptyBorder(4, 6, 4, 6)
        ));
        b.setFont(new Font("SansSerif", Font.PLAIN, 11));
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return b;
    }

    private JLabel miniLabel(String text) {
        JLabel l = new JLabel(text);
        l.setFont(new Font("Monospaced", Font.PLAIN, 11));
        l.setForeground(new Color(160, 158, 150));
        return l;
    }

    private JSlider makeSlider(int min, int max, int val) {
        JSlider s = new JSlider(min, max, val);
        styleSlider(s);
        return s;
    }

    private void styleSlider(JSlider s) {
        s.setBackground(new Color(20, 19, 17));
        s.setForeground(new Color(79, 152, 163));
    }

    private JLabel sectionTitle(String text) {
        JLabel l = new JLabel(text.toUpperCase());
        l.setFont(new Font("SansSerif", Font.BOLD, 10));
        l.setForeground(new Color(79, 152, 163));
        l.setBorder(new EmptyBorder(6, 0, 3, 0));
        l.setAlignmentX(LEFT_ALIGNMENT);
        return l;
    }

    private JPanel sliderRow(String name, JSlider slider, JLabel valLabel) {
        JPanel p = new JPanel(new BorderLayout(4, 0));
        p.setBackground(new Color(20, 19, 17));
        p.setMaximumSize(new Dimension(240, 36));
        JLabel nl = miniLabel(name);
        nl.setPreferredSize(new Dimension(100, 16));
        valLabel.setPreferredSize(new Dimension(40, 16));
        p.add(nl,       BorderLayout.WEST);
        p.add(slider,   BorderLayout.CENTER);
        p.add(valLabel, BorderLayout.EAST);
        return p;
    }

    private JPanel buildLegend() {
        JPanel p = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                int w = getWidth(), h = getHeight();
                for (int i = 0; i < w; i++) {
                    g.setColor(phaseColor((double) i / w));
                    g.fillRect(i, 0, 1, h);
                }
                g.setColor(new Color(130, 128, 120));
                g.setFont(new Font("SansSerif", Font.PLAIN, 9));
                g.drawString("-max", 2, h - 2);
                g.drawString(" 0",  w/2 - 4, h - 2);
                g.drawString("+max", w - 26, h - 2);
            }
        };
        p.setPreferredSize(new Dimension(220, 22));
        p.setMaximumSize(new Dimension(240, 22));
        return p;
    }
}