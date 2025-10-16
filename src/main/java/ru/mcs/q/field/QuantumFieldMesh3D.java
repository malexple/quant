package ru.mcs.q.field;

import java.util.*;
import java.util.concurrent.*;

public class QuantumFieldMesh3D {
    private final Map<String, Tetrahedron3D> tetrahedrons;
    private final ScheduledExecutorService scheduler;
    private boolean isActive;

    public QuantumFieldMesh3D() {
        this.tetrahedrons = new ConcurrentHashMap<>();
        this.scheduler = Executors.newScheduledThreadPool(1);
        this.isActive = false;
    }

    public void addTetrahedron(String id, Vector3D position, double size) {
        Tetrahedron3D tetrahedron = new Tetrahedron3D(id, position, size);
        tetrahedrons.put(id, tetrahedron);
        System.out.println("Added " + tetrahedron);
    }

    public boolean connectTetrahedrons(String id1, FaceColor face1, String id2, FaceColor face2) {
        Tetrahedron3D t1 = tetrahedrons.get(id1);
        Tetrahedron3D t2 = tetrahedrons.get(id2);

        if (t1 != null && t2 != null) {
            return t1.connect(face1, t2, face2);
        }
        return false;
    }

    public void startFieldOscillations() {
        if (!isActive) {
            isActive = true;
            scheduler.scheduleAtFixedRate(this::updateField, 0, 2, TimeUnit.SECONDS);
        }
    }

    public void stopFieldOscillations() {
        if (isActive) {
            isActive = false;
            scheduler.shutdown();
        }
    }

    private void updateField() {
        // Обновляем энергию и состояния
        tetrahedrons.values().forEach(tetrahedron -> {
            // Случайные колебания энергии
            double oscillation = (Math.random() - 0.5) * 0.1;
            double newEnergy = Math.max(0, tetrahedron.getEnergyLevel() + oscillation);

            // Обновляем размер в зависимости от энергии
            double newSize = 0.5 + newEnergy * 0.5;
        });
    }

    public Map<String, Tetrahedron3D> getTetrahedrons() {
        return Collections.unmodifiableMap(tetrahedrons);
    }
}