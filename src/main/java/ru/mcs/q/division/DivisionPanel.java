package ru.mcs.q.division;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.util.List;

public class DivisionPanel extends JPanel {

    private final TetraUniverse universe;

    // ── Рендер ────────────────────────────────────────────────────────────────
    private BufferedImage canvas;
    private final JPanel  canvasPanel;
    private final JLabel  infoLabel;

    // ── Анимация ──────────────────────────────────────────────────────────────
    private int             stepsPerFrame = 1;
    private volatile boolean running      = true;
    private Thread           simThread;
    private final Object     canvasLock   = new Object();

    // ── Слайдеры ──────────────────────────────────────────────────────────────
    private JSlider slDamping, slLambda, slV, slFaceRate;
    private JSlider slSame, slDiff, slDiffEq;
    private JLabel  lblDamping, lblLambda, lblV, lblFaceRate;
    private JLabel  lblSame, lblDiff, lblDiffEq;

    // ── Камера 3D ─────────────────────────────────────────────────────────────
    private double camRotX   =  0.45;
    private double camRotY   = -0.55;
    private double camZoom   =  18.0;
    private double camDist   =  800.0;
    private double panX      =  0.0;
    private double panY      =  0.0;
    private int    lastMX, lastMY;
    private boolean rotating = false;
    private boolean panning  = false;
    private boolean cameraFitted = false;

    // ── Цвета граней ─────────────────────────────────────────────────────────
    private static final Color[] FACE_COLORS = {
            new Color(220,  60,  60),   // 0 RED
            new Color( 60, 120, 220),   // 1 BLUE
            new Color( 60, 200,  80),   // 2 GREEN
            new Color( 60,  60,  55),   // 3 TRANSPARENT (dark)
    };

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
                synchronized (canvasLock) {
                    if (canvas != null)
                        g.drawImage(canvas, 0, 0, getWidth(), getHeight(), null);
                }
            }
        };
        canvasPanel.setBackground(new Color(13, 12, 11));
        canvasPanel.setPreferredSize(new Dimension(860, 700));
        add(canvasPanel, BorderLayout.CENTER);

        // Правая панель
        add(buildControlPanel(), BorderLayout.EAST);

        // Строка статуса
        infoLabel = new JLabel(" ");
        infoLabel.setFont(new Font("Monospaced", Font.PLAIN, 11));
        infoLabel.setForeground(new Color(120, 120, 110));
        infoLabel.setBorder(new EmptyBorder(3, 8, 3, 8));
        infoLabel.setBackground(new Color(20, 19, 17));
        infoLabel.setOpaque(true);
        add(infoLabel, BorderLayout.SOUTH);

        setupMouseHandlers();

        // Симуляционный поток
        simThread = new Thread(() -> {
            while (running) {
                try {
                    for (int i = 0; i < stepsPerFrame; i++)
                        universe.step();

                    renderToCanvas();

                    SwingUtilities.invokeLater(() -> {
                        canvasPanel.repaint();
                        updateInfo();
                    });
                    Thread.sleep(30);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }, "tetra-sim");
        simThread.setDaemon(true);
        simThread.start();

        renderToCanvas();
    }

    // ── Панель управления ─────────────────────────────────────────────────────
    private JPanel buildControlPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(new Color(20, 19, 17));
        panel.setBorder(new EmptyBorder(8, 8, 8, 8));
        panel.setPreferredSize(new Dimension(250, 600));

        // Кнопки
        JPanel btnRow = new JPanel(new GridLayout(2, 2, 4, 4));
        btnRow.setBackground(new Color(20, 19, 17));
        btnRow.setMaximumSize(new Dimension(260, 64));

        JButton btnStart  = darkButton("▶ Динамика");
        JButton btnStep   = darkButton("1 шаг");
        JButton btnReset  = darkButton("↺ Сброс");
        JButton btnExcite = darkButton("⚡ Возбудить");

        btnStart.addActionListener(e -> {
            boolean en = !universe.isDynamicsEnabled();
            universe.setDynamicsEnabled(en);
            btnStart.setText(en ? "⏸ Пауза" : "▶ Динамика");
        });

        btnStep.addActionListener(e -> {
            boolean was = universe.isDynamicsEnabled();
            universe.setDynamicsEnabled(true);
            universe.step();
            universe.setDynamicsEnabled(was);
        });

        btnReset.addActionListener(e -> {
            universe.reset();
            universe.setDynamicsEnabled(false);
            btnStart.setText("▶ Динамика");
            cameraFitted = false;
            panX = 0; panY = 0;
        });

        btnExcite.addActionListener(e -> universe.exciteCenter(600.0));

        btnRow.add(btnStart); btnRow.add(btnStep);
        btnRow.add(btnReset); btnRow.add(btnExcite);
        panel.add(btnRow);
        panel.add(Box.createVerticalStrut(10));

        // Шагов за кадр
        JPanel spfRow = new JPanel(new BorderLayout(4, 0));
        spfRow.setBackground(new Color(20, 19, 17));
        spfRow.setMaximumSize(new Dimension(260, 28));
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

        // Физика поля
        panel.add(sectionTitle("Физика поля"));

        slDamping  = makeSlider(990, 1000, 999); lblDamping  = miniLabel("0.999");
        slLambda   = makeSlider(0, 100, 2);       lblLambda   = miniLabel("2e-6");
        slV        = makeSlider(50, 600, 300);    lblV        = miniLabel("300");
        slFaceRate = makeSlider(1, 100, 10);      lblFaceRate = miniLabel("0.010");

        slDamping.addChangeListener(e -> {
            double v = slDamping.getValue() / 1000.0;
            universe.setDamping(v);
            lblDamping.setText(String.format("%.3f", v));
        });
        slLambda.addChangeListener(e -> {
            double v = slLambda.getValue() * 1e-6;
            universe.setLambda(v);
            lblLambda.setText(slLambda.getValue() + "e-6");
        });
        slV.addChangeListener(e -> {
            universe.setV(slV.getValue());
            lblV.setText(String.valueOf(slV.getValue()));
        });
        slFaceRate.addChangeListener(e -> {
            double v = slFaceRate.getValue() * 0.001;
            universe.setFaceRate(v);
            lblFaceRate.setText(String.format("%.3f", v));
        });

        panel.add(sliderRow("Затухание",       slDamping,  lblDamping));
        panel.add(sliderRow("λ потенциал",     slLambda,   lblLambda));
        panel.add(sliderRow("Вакуум V",        slV,        lblV));
        panel.add(sliderRow("Метрика faceRate",slFaceRate, lblFaceRate));
        panel.add(Box.createVerticalStrut(10));

        // Правила связей
        panel.add(sectionTitle("Правила связей"));

        BondStrength bs = universe.getBondStrength();
        slSame  = makeSlider(0, 500,  (int)(bs.getSameBond()        * 1000)); lblSame  = miniLabel(String.format("%.3f", bs.getSameBond()));
        slDiff  = makeSlider(0, 500,  (int)(bs.getDiffBond()        * 1000)); lblDiff  = miniLabel(String.format("%.3f", bs.getDiffBond()));
        slDiffEq= makeSlider(0, 1000, (int)(bs.getDiffEquilibrium() * 1000)); lblDiffEq= miniLabel(String.format("%.3f", bs.getDiffEquilibrium()));

        slSame.addChangeListener(e -> {
            float v = slSame.getValue() / 1000f; bs.setSameBond(v);
            lblSame.setText(String.format("%.3f", v));
        });
        slDiff.addChangeListener(e -> {
            float v = slDiff.getValue() / 1000f; bs.setDiffBond(v);
            lblDiff.setText(String.format("%.3f", v));
        });
        slDiffEq.addChangeListener(e -> {
            float v = slDiffEq.getValue() / 1000f; bs.setDiffEquilibrium(v);
            lblDiffEq.setText(String.format("%.3f", v));
        });

        panel.add(sliderRow("Same bond",        slSame,   lblSame));
        panel.add(sliderRow("Diff bond",        slDiff,   lblDiff));
        panel.add(sliderRow("Diff equilibrium", slDiffEq, lblDiffEq));
        panel.add(Box.createVerticalStrut(10));

        // Легенда
        panel.add(sectionTitle("Цвета фаз"));
        panel.add(buildLegend());
        panel.add(Box.createVerticalGlue());

        return panel;
    }

    // ── Проекция 3D → экран ───────────────────────────────────────────────────
    private double[] project(double wx, double wy, double wz, int cw, int ch) {
        double[] mc = universe.getMeshCenter();
        double ox = wx - mc[0];
        double oy = wy - mc[1];
        double oz = wz - mc[2];

        double cosY = Math.cos(camRotY), sinY = Math.sin(camRotY);
        double rx1  =  ox * cosY + oz * sinY;
        double ry1  =  oy;
        double rz1  = -ox * sinY + oz * cosY;

        double cosX = Math.cos(camRotX), sinX = Math.sin(camRotX);
        double rx2  =  rx1;
        double ry2  =  ry1 * cosX - rz1 * sinX;
        double rz2  =  ry1 * sinX + rz1 * cosX;

        double depth = camDist + rz2 * camZoom;
        if (depth < 1.0) depth = 1.0;
        double scale = camDist / depth;

        double sx = cw * 0.5 + panX + rx2 * scale * camZoom;
        double sy = ch * 0.5 + panY - ry2 * scale * camZoom;
        return new double[]{sx, sy, rz2};
    }

    // ── Рендер в буфер ────────────────────────────────────────────────────────
    private void renderToCanvas() {
        int cw = Math.max(1, canvasPanel.getWidth());
        int ch = Math.max(1, canvasPanel.getHeight());

        BufferedImage buf = new BufferedImage(cw, ch, BufferedImage.TYPE_INT_RGB);
        Graphics2D    g   = buf.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,  RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        g.setColor(new Color(13, 12, 11));
        g.fillRect(0, 0, cw, ch);

        // Авто-подгонка масштаба при первом рендере или после сброса
        if (!cameraFitted && cw > 10) {
            double r = universe.getMeshRadius();
            camZoom      = Math.min(cw, ch) * 0.35 / Math.max(r, 0.001);
            cameraFitted = true;
        }

        List<TetraNode> nodes = universe.getNodes();
        int N = nodes.size();
        if (N == 0) { g.dispose(); synchronized (canvasLock) { canvas = buf; } return; }

        // Глубины для сортировки
        double[] depthArr = new double[N];
        for (int i = 0; i < N; i++) {
            TetraNode n = nodes.get(i);
            depthArr[i] = project(n.rx, n.ry, n.rz, cw, ch)[2];
        }
        Integer[] order = new Integer[N];
        for (int i = 0; i < N; i++) order[i] = i;
        java.util.Arrays.sort(order, (a, b) -> Double.compare(depthArr[a], depthArr[b]));

        double depthMin   = depthArr[order[0]];
        double depthMax   = depthArr[order[N - 1]];
        double depthRange = Math.max(depthMax - depthMin, 0.001);

        // Рисуем тетраэдры
        for (int idx : order) {
            TetraNode n   = nodes.get(idx);
            double    phi = universe.normalizedPhase(n);

            double[][] sp = new double[4][];
            for (int v = 0; v < 4; v++)
                sp[v] = project(n.verts[v][0], n.verts[v][1], n.verts[v][2], cw, ch);

            double fog         = 1.0 - 0.55 * (depthArr[idx] - depthMin) / depthRange;
            double depthFactor = 1.0 - (depthArr[idx] - depthMin) / depthRange;
            float  baseWidth   = (float)(0.7 + depthFactor * 1.6);

            for (int[] edge : TetraNode.EDGES) {
                int va = edge[0], vb = edge[1];
                int fa = edge[2], fb = edge[3];

                int   colorFace = (fa != 3) ? fa : fb;
                Color base      = FACE_COLORS[colorFace];
                float sz        = n.faceSize[colorFace];
                float bri       = (float)(phi * 0.7 + 0.3) * sz * (float)fog;

                Color ec = new Color(
                        clamp(base.getRed()   * bri + 8),
                        clamp(base.getGreen() * bri + 8),
                        clamp(base.getBlue()  * bri + 8));

                float lineW = Math.max(0.4f, baseWidth * (0.5f + sz * 0.5f));
                g.setStroke(new BasicStroke(lineW, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g.setColor(ec);
                g.drawLine((int)sp[va][0], (int)sp[va][1], (int)sp[vb][0], (int)sp[vb][1]);
            }
        }

        // Подпись
        g.setStroke(new BasicStroke(1f));
        g.setFont(new Font("Monospaced", Font.PLAIN, 11));
        g.setColor(new Color(70, 70, 60));
        g.drawString(
                "Mode: " + universe.getMode()
                        + "  nodes: " + N
                        + "  ЛКМ=вращение  ПКМ=пан  колесо=zoom",
                8, ch - 6);
        g.dispose();

        synchronized (canvasLock) { canvas = buf; }
    }

    // ── Строка статуса ────────────────────────────────────────────────────────
    private void updateInfo() {
        infoLabel.setText(String.format(
                "Tick: %d  |  Nodes: %d  |  ΣE: %.1f  |  φ [%.1f .. %.1f]  |  %s  |  %s",
                universe.getTick(),
                universe.getNodes().size(),
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
                if (SwingUtilities.isRightMouseButton(e)) panning  = true;
                else                                       rotating = true;
            }
            @Override public void mouseReleased(MouseEvent e) {
                rotating = false; panning = false;
            }
            @Override public void mouseClicked(MouseEvent e) {
                if (SwingUtilities.isLeftMouseButton(e) && !rotating)
                    exciteNearest(e.getX(), e.getY(), 2000.0);
                else if (SwingUtilities.isRightMouseButton(e) && !panning)
                    exciteNearest(e.getX(), e.getY(), -1000.0);
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
            camZoom *= (e.getWheelRotation() < 0) ? 1.1 : 0.9;
            camZoom  = Math.max(4.0, Math.min(500.0, camZoom));
        });
    }

    // ── Возбудить ближайший к курсору тетраэдр ────────────────────────────────
    private void exciteNearest(int sx, int sy, double amount) {
        int    bestId   = -1;
        double bestDist = Double.MAX_VALUE;
        int    cw       = canvasPanel.getWidth();
        int    ch       = canvasPanel.getHeight();

        for (TetraNode n : universe.getNodes()) {
            double[] p = project(n.rx, n.ry, n.rz, cw, ch);
            double   d = (p[0]-sx)*(p[0]-sx) + (p[1]-sy)*(p[1]-sy);
            if (d < bestDist) { bestDist = d; bestId = n.id; }
        }
        if (bestId >= 0)
            universe.exciteById(bestId, amount);
    }

    // ── Вспомогательные ──────────────────────────────────────────────────────
    private static int clamp(float v) { return Math.max(0, Math.min(255, (int) v)); }

    private static Color phaseColor(double t) {
        t = Math.max(0, Math.min(1, t));
        if (t < 0.25) {
            float g = (float)(t / 0.25);
            return new Color(0f, g * 0.5f, 1f);
        } else if (t < 0.5) {
            float k = (float)((0.5 - t) / 0.25);
            return new Color(0f, k * 0.3f, k * 0.6f);
        } else if (t < 0.75) {
            float k = (float)((t - 0.5) / 0.25);
            return new Color(k, k * 0.85f, 0f);
        } else {
            float k = (float)((t - 0.75) / 0.25);
            return new Color(1f, 0.85f + k * 0.15f, k);
        }
    }

    private JButton darkButton(String text) {
        JButton b = new JButton(text);
        b.setBackground(new Color(35, 33, 30));
        b.setForeground(new Color(200, 198, 194));
        b.setFocusPainted(false);
        b.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(55, 53, 48)),
                new EmptyBorder(4, 6, 4, 6)));
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
        p.setMaximumSize(new Dimension(260, 36));
        JLabel nl = miniLabel(name);
        nl.setPreferredSize(new Dimension(110, 16));
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
        p.setPreferredSize(new Dimension(230, 22));
        p.setMaximumSize(new Dimension(260, 22));
        return p;
    }
}