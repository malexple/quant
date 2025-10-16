package ru.mcs.q;

import ru.mcs.q.field.FaceColor;
import ru.mcs.q.field.QuantumFieldMesh3D;
import ru.mcs.q.field.QuantumFieldVisualization;
import ru.mcs.q.field.Vector3D;

import javax.swing.*;

public class TriangularBipyramidDemo {
    public static void main(String[] args) {
        System.out.println("Starting Perfect Alignment Demo...");

        // Создаем сетку тетраэдров
        QuantumFieldMesh3D mesh = new QuantumFieldMesh3D();

        // Создаем тетраэдры с начальными позициями, которые позволят им соединиться правильно
        mesh.addTetrahedron("T1", new Vector3D(0, 0, 0), 1.0);
        mesh.addTetrahedron("T2", new Vector3D(3, 0, 0), 1.0);  // Смещен в сторону для соединения красными гранями
        mesh.addTetrahedron("T3", new Vector3D(0, 3, 0), 1.0);  // Смещен вверх для соединения синими гранями
        mesh.addTetrahedron("T4", new Vector3D(0, 0, 3), 1.0);  // Смещен вперед для соединения зелеными гранями
        mesh.addTetrahedron("T5", new Vector3D(3, 0, 0), 1.0); // Смещен влево для соединения с T2

        // Создаем соединения - теперь грани должны быть идеально параллельны
        System.out.println("Creating perfectly aligned connections:");

        // T1 и T2 соединяются красными гранями
        mesh.connectTetrahedrons("T1", FaceColor.RED, "T2", FaceColor.RED);

        // T1 и T3 соединяются синими гранями
        mesh.connectTetrahedrons("T1", FaceColor.BLUE, "T3", FaceColor.BLUE);

        // T1 и T4 соединяются зелеными гранями
        mesh.connectTetrahedrons("T1", FaceColor.GREEN, "T4", FaceColor.GREEN);

        // T2 и T5 соединяются синими гранями
        mesh.connectTetrahedrons("T2", FaceColor.BLUE, "T5", FaceColor.BLUE);

        // Запускаем колебания
        mesh.startFieldOscillations();

        // Создаем и показываем визуализацию
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Perfectly Aligned Quantum Field");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

            QuantumFieldVisualization visualization = new QuantumFieldVisualization(mesh);
            frame.add(visualization);
            frame.pack();
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });

    }
}