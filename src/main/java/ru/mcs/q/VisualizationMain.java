package ru.mcs.q;

import ru.mcs.q.field.FaceColor;
import ru.mcs.q.field.QuantumFieldMesh3D;
import ru.mcs.q.field.QuantumFieldVisualization;
import ru.mcs.q.field.Vector3D;

import javax.swing.*;

public class VisualizationMain {
    public static void main(String[] args) {
        System.out.println("Starting 3D Quantum Field Visualization with Face Orientation...");

        // Создаем сетку тетраэдров
        QuantumFieldMesh3D mesh = new QuantumFieldMesh3D();

        // Добавляем тетраэдры в различных позициях
        mesh.addTetrahedron("T1", new Vector3D(0, 0, 0), 0.5);
        mesh.addTetrahedron("T2", new Vector3D(2, 0, 0), 0.5);
        mesh.addTetrahedron("T3", new Vector3D(0, 2, 0), 0.5);
        mesh.addTetrahedron("T4", new Vector3D(0, 0, 2), 0.5);
        mesh.addTetrahedron("T5", new Vector3D(-2, 0, 0), 0.5);
        mesh.addTetrahedron("T6", new Vector3D(1, 1, 1), 0.5);
        mesh.addTetrahedron("T7", new Vector3D(-1, -1, -1), 0.5);

        // Создаем соединения - теперь грани будут ориентированы друг к другу
        System.out.println("\nCreating oriented connections:");
        mesh.connectTetrahedrons("T1", FaceColor.RED, "T2", FaceColor.RED);
        mesh.connectTetrahedrons("T1", FaceColor.BLUE, "T3", FaceColor.BLUE);
        mesh.connectTetrahedrons("T1", FaceColor.GREEN, "T4", FaceColor.GREEN);
        mesh.connectTetrahedrons("T2", FaceColor.BLUE, "T6", FaceColor.BLUE);
        mesh.connectTetrahedrons("T3", FaceColor.GREEN, "T6", FaceColor.GREEN);
        mesh.connectTetrahedrons("T5", FaceColor.RED, "T7", FaceColor.RED);

        // Запускаем колебания
        mesh.startFieldOscillations();

        // Создаем и показываем визуализацию
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("3D Quantum Field Visualization - Oriented Faces");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

            QuantumFieldVisualization visualization = new QuantumFieldVisualization(mesh);
            frame.add(visualization);
            frame.pack();
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }
}