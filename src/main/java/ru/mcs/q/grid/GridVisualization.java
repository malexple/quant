package ru.mcs.q.grid;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

public class GridVisualization extends JPanel {

    private static final int CELL   = 8;  // пикселей на узел → 800×800
    private static final int INFO_H = 50;

    private final GridField field;

    public GridVisualization(GridField field) {
        this.field = field;
        setPreferredSize(new Dimension(
                GridField.WIDTH  * CELL,
                GridField.HEIGHT * CELL + INFO_H));
        setBackground(Color.BLACK);

        // ~20 fps
        new Timer(50, e -> repaint()).start();

        // ЛКМ = добавить энергию, ПКМ = убрать
        MouseAdapter mouse = new MouseAdapter() {
            @Override public void mousePressed(MouseEvent e)  { addEnergy(e); }
            @Override public void mouseDragged(MouseEvent e)  { addEnergy(e); }

            private void addEnergy(MouseEvent e) {
                int cx = e.getX() / CELL;
                int cy = e.getY() / CELL;
                double amt = SwingUtilities.isLeftMouseButton(e) ? 2000.0 : -1000.0;
                field.excite(cx, cy, amt);
            }
        };
        addMouseListener(mouse);
        addMouseMotionListener(mouse);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;

        double maxE = field.getMaxEnergy();
        double minE = field.getMinEnergy();
        double range = maxE - minE;
        if (range < 1e-9) range = 1.0;

        for (int y = 0; y < GridField.HEIGHT; y++) {
            for (int x = 0; x < GridField.WIDTH; x++) {
                // t=0.0 → минимум (отрицательная амплитуда, синий)
                // t=0.5 → ноль (чёрный)
                // t=1.0 → максимум (красный/белый)
                double t = (field.getDisplay(x, y) - minE) / range;
                g2.setColor(heatColor(t));
                g2.fillRect(x * CELL, y * CELL, CELL, CELL);
            }
        }

        // Инфо-панель
        g2.setColor(new Color(15, 15, 15));
        g2.fillRect(0, GridField.HEIGHT * CELL, GridField.WIDTH * CELL, INFO_H);

        g2.setFont(new Font("Monospaced", Font.PLAIN, 13));
        g2.setColor(Color.WHITE);
        g2.drawString(String.format(
                        "Tick: %6d  |  Nodes: %,d  |  ΣE: %,.1f  |  min: %.3f  max: %.3f",
                        field.getTick(),
                        GridField.WIDTH * GridField.HEIGHT,
                        field.getTotalEnergy(),
                        minE, maxE),
                8, GridField.HEIGHT * CELL + 18);

        g2.setColor(Color.GRAY);
        g2.drawString(
                "LMB / drag = +энергия    RMB = -энергия    Topology: TORUS",
                8, GridField.HEIGHT * CELL + 36);
    }

    /**
     * Двухполярная шкала для волновой функции:
     *   t=0.0  → синий    (−амплитуда, destructive)
     *   t=0.25 → голубой
     *   t=0.5  → чёрный   (ноль, нет возбуждения)
     *   t=0.75 → жёлтый
     *   t=1.0  → белый    (+амплитуда, constructive)
     */
    private Color heatColor(double t) {
        t = Math.max(0, Math.min(1, t));

        float[][] keys = {
                {0.0f, 0.0f, 0.8f},  // 0.00  синий (−max)
                {0.0f, 0.5f, 1.0f},  // 0.25  голубой
                {0.0f, 0.0f, 0.0f},  // 0.50  чёрный (ноль)
                {1.0f, 0.8f, 0.0f},  // 0.75  жёлтый
                {1.0f, 1.0f, 1.0f},  // 1.00  белый (+max)
        };

        double pos  = t * (keys.length - 1);
        int    seg  = (int) Math.min(pos, keys.length - 2);
        double frac = pos - seg;

        float r  = lerp(keys[seg][0], keys[seg + 1][0], frac);
        float gv = lerp(keys[seg][1], keys[seg + 1][1], frac);
        float b  = lerp(keys[seg][2], keys[seg + 1][2], frac);

        return new Color(
                Math.max(0f, Math.min(1f, r)),
                Math.max(0f, Math.min(1f, gv)),
                Math.max(0f, Math.min(1f, b))
        );
    }

    private float lerp(float a, float b, double t) {
        return (float)(a + (b - a) * t);
    }
}