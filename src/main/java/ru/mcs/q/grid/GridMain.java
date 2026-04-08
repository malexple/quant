package ru.mcs.q.grid;

import javax.swing.*;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;

public class GridMain {
    public static void main(String[] args) throws UnsupportedEncodingException {
        System.setOut(new PrintStream(System.out, false, "UTF-8"));

        System.out.println("🌌 Quantum Field — 10,000 узлов");
        System.out.println("Топология : ТОРУС");
        System.out.println("Правило   : Волновое уравнение + φ⁴ потенциал");

        GridField field = new GridField();

        // БЫЛО: 10000 — слишком большое, потенциал взрывается
        // СТАЛО: 2*V = 600 — поле немного выше вакуума, устойчиво
        field.excite(50, 50, 600.0);
        System.out.println("✦ Возбуждение: (50,50) = 600  [= 2*V]");

        field.start(50);

        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame(
                    "Quantum Field — 10,000 Nodes — φ⁴ Solitons (Toroidal)");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setResizable(false);
            frame.add(new GridVisualization(field));
            frame.pack();
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }
}