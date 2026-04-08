package ru.mcs.q.grid;

import javax.swing.*;

public class GridMain {
    public static void main(String[] args) {
        System.out.println("🌌 Quantum Field — 10,000 узлов");
        System.out.println("Топология : ТОРУС (закольцованная вселенная)");
        System.out.println("Правило   : ΔE = FLOW_RATE * Laplacian(E)  [уравнение теплопроводности]");
        System.out.println("FLOW_RATE = 0.24  |  DAMPING = 0.999/tick  |  20 тиков/сек");
        System.out.println();

        GridField field = new GridField();

        // Одна точка возбуждения — смотрим как волна расходится
        field.excite(50, 50, 10000.0);
        System.out.println("✦ Начальное возбуждение: (50, 50) E=10000");
        System.out.println("  Наблюдайте: волна уйдёт к краям, обогнёт тор и вернётся.");
        System.out.println("  При встрече двух фронтов — интерференционный узор.");

        // Можно раскомментировать для сразу двух источников (интерференция):
        // field.excite(25, 50, 10000.0);

        field.start(50);

        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame(
                    "Quantum Field — 10,000 Nodes — Gradient Propagation (Toroidal)");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setResizable(false);
            frame.add(new GridVisualization(field));
            frame.pack();
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }
}