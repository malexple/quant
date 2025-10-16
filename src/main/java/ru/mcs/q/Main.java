package ru.mcs.q;

import ru.mcs.q.field.FaceColor;
import ru.mcs.q.field.QuantumFieldMesh;

import java.io.PrintStream;
import java.io.UnsupportedEncodingException;

public class Main {
    public static void main(String[] args) throws UnsupportedEncodingException {
        System.setOut(new PrintStream(System.out, false, "UTF-8"));
        System.out.println("🌌 Запуск симуляции квантового поля на основе тетраэдров\n");

        QuantumFieldMesh quantumField = new QuantumFieldMesh();

        // Создаем начальные тетраэдры
        for (int i = 1; i <= 5; i++) {
            quantumField.addTetrahedron("T" + i);
        }

        // Создаем некоторые соединения
        System.out.println("\n🔗 Создаем начальные соединения:");
        quantumField.connectTetrahedrons("T1", FaceColor.RED, "T2", FaceColor.RED);
        quantumField.connectTetrahedrons("T3", FaceColor.BLUE, "T4", FaceColor.BLUE);
        quantumField.connectTetrahedrons("T2", FaceColor.GREEN, "T5", FaceColor.GREEN);

        // Визуализируем начальное состояние
        quantumField.visualizeMesh();
        quantumField.printFieldState();

        // Запускаем колебания поля
        quantumField.startFieldOscillations();

        // Даем симуляции поработать
        try {
            Thread.sleep(10000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // Останавливаем симуляцию
        quantumField.stopFieldOscillations();

        System.out.println("\n🎯 Финальное состояние:");
        quantumField.visualizeMesh();
        quantumField.printFieldState();
    }
}