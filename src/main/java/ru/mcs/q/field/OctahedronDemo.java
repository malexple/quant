package ru.mcs.q.field;

import javax.swing.*;

public class OctahedronDemo {
    public static void main(String[] args) {
        System.out.println("Starting Octahedron Formation Demo...");

        // Создаем сетку тетраэдров
        QuantumFieldMesh3D mesh = new QuantumFieldMesh3D();

        // Создаем тетраэдры, которые будут образовывать октаэдр
        mesh.addTetrahedron("T1", new Vector3D(0, 0, 0), 1.0);
        mesh.addTetrahedron("T2", new Vector3D(2, 0, 0), 1.0);

        // Создаем соединение между красными гранями - должно образовать октаэдр
        System.out.println("Creating octahedral connection:");
        mesh.connectTetrahedrons("T1", FaceColor.RED, "T2", FaceColor.RED);

        // Добавляем еще тетраэдры для демонстрации других соединений
        mesh.addTetrahedron("T3", new Vector3D(0, 2, 0), 1.0);
        mesh.addTetrahedron("T4", new Vector3D(0, 0, 2), 1.0);

        mesh.connectTetrahedrons("T1", FaceColor.BLUE, "T3", FaceColor.BLUE);
        mesh.connectTetrahedrons("T1", FaceColor.GREEN, "T4", FaceColor.GREEN);

        // Запускаем колебания
        mesh.startFieldOscillations();

        // Создаем и показываем визуализацию
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Octahedral Quantum Field Visualization");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

            QuantumFieldVisualization visualization = new QuantumFieldVisualization(mesh);
            frame.add(visualization);
            frame.pack();
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }
}