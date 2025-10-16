package ru.mcs.q;

import ru.mcs.q.field.FaceColor;
import ru.mcs.q.field.QuantumFieldMesh3D;
import ru.mcs.q.field.QuantumFieldVisualization;
import ru.mcs.q.field.Tetrahedron3D;
import ru.mcs.q.field.Vector3D;

import javax.swing.*;
import java.util.Map;

public class TriangularBipyramidDemo {
    public static void main(String[] args) {
        System.out.println("Starting Sequential Connection Demo...");

        QuantumFieldMesh3D mesh = new QuantumFieldMesh3D();

        // Создаем тетраэдры в последовательности
        // Сначала создаем центральный тетраэдр T1
        mesh.addTetrahedron("T1", new Vector3D(0, 0, 0), 1.0);

        // Создаем T2, T3, T4 и соединяем их с T1
        mesh.addTetrahedron("T2", new Vector3D(2, 0, 0), 1.0);
        mesh.addTetrahedron("T3", new Vector3D(0, 2, 0), 1.0);
        mesh.addTetrahedron("T4", new Vector3D(0, 0, 2), 1.0);

        // Соединяем T1 с T2, T3, T4
        System.out.println("Step 1: Connecting T1 with T2, T3, T4");
        mesh.connectTetrahedrons("T1", FaceColor.RED, "T2", FaceColor.RED);
        mesh.connectTetrahedrons("T1", FaceColor.BLUE, "T3", FaceColor.BLUE);
        mesh.connectTetrahedrons("T1", FaceColor.GREEN, "T4", FaceColor.GREEN);

        // Теперь создаем T5 и соединяем его с T2
        mesh.addTetrahedron("T5", new Vector3D(4, 0, 0), 1.0);

        System.out.println("Step 2: Connecting T2 with T5");
        mesh.connectTetrahedrons("T2", FaceColor.BLUE, "T5", FaceColor.BLUE);

        // Визуализируем нормали для отладки
        visualizeConnections(mesh);

        mesh.startFieldOscillations();

        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Sequential Connection Demo");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

            QuantumFieldVisualization visualization = new QuantumFieldVisualization(mesh);
            frame.add(visualization);
            frame.pack();
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }

    private static void visualizeConnections(QuantumFieldMesh3D mesh) {
        System.out.println("\n--- Connection Visualization ---");
        for (Tetrahedron3D tetra : mesh.getTetrahedrons().values()) {
            System.out.println(tetra.getId() + " connections:");
            for (Map.Entry<FaceColor, Tetrahedron3D> entry : tetra.getConnections().entrySet()) {
                FaceColor color = entry.getKey();
                Tetrahedron3D connected = entry.getValue();
                System.out.printf("  %s → %s[%s]%n", color, connected.getId(), color);
            }
        }
    }
}