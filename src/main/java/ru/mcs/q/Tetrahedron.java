package ru.mcs.q;

import ru.mcs.q.field.FaceColor;
import ru.mcs.q.field.FaceState;

import java.util.*;

public class Tetrahedron {
    private final String id;
    private final Map<FaceColor, FaceState> faces;
    private final Map<FaceColor, Tetrahedron> connections;
    private double energyLevel;
    private volatile double pendingDelta = 0.0;

    public Tetrahedron(String id) {
        this.id = id;
        this.faces = new EnumMap<>(FaceColor.class);
        this.connections = new EnumMap<>(FaceColor.class);
        this.energyLevel = 0.0;

        // Инициализируем все грани
        for (FaceColor color : FaceColor.values()) {
            faces.put(color, FaceState.NEUTRAL);
        }
    }

    public boolean connect(FaceColor face, Tetrahedron other, FaceColor otherFace) {
        if (face == otherFace && face != FaceColor.TRANSPARENT) {
            if (!connections.containsKey(face) && !other.connections.containsKey(otherFace)) {
                connections.put(face, other);
                other.connections.put(otherFace, this);

                // При соединении увеличиваем энергию
                this.energyLevel += 0.1;
                other.energyLevel += 0.1;

                faces.put(face, FaceState.ATTRACTED);
                other.faces.put(otherFace, FaceState.ATTRACTED);

                System.out.printf("Соединение: %s[%s] ↔ %s[%s] (энергия: %.2f)%n",
                        id, face, other.id, otherFace, energyLevel);
                return true;
            }
        }
        return false;
    }

    public void disconnect(FaceColor face) {
        Tetrahedron connected = connections.get(face);
        if (connected != null) {
            // Находим соответствующую грань у подключенного тетраэдра
            for (Map.Entry<FaceColor, Tetrahedron> entry : connected.connections.entrySet()) {
                if (entry.getValue() == this) {
                    connected.connections.remove(entry.getKey());
                    connected.faces.put(entry.getKey(), FaceState.NEUTRAL);
                    break;
                }
            }
            connections.remove(face);
            faces.put(face, FaceState.NEUTRAL);

            // При разъединении уменьшаем энергию
            this.energyLevel = Math.max(0, energyLevel - 0.05);
        }
    }

    public void oscillate() {
        // Случайные колебания тетраэдра
        double oscillation = (Math.random() - 0.5) * 0.2;
        energyLevel = Math.max(0, energyLevel + oscillation);

        // Вероятность спонтанного соединения/разъединения
        if (Math.random() < 0.1) {
            spontaneousInteraction();
        }
    }

    private void spontaneousInteraction() {
        List<FaceColor> availableFaces = Arrays.stream(FaceColor.values())
                .filter(f -> !connections.containsKey(f) && f != FaceColor.TRANSPARENT)
                .toList();

        if (!availableFaces.isEmpty() && Math.random() < 0.3) {
            FaceColor face = availableFaces.get((int)(Math.random() * availableFaces.size()));
            faces.put(face, FaceState.VIBRATING);
        }
    }

    // Getters
    public String getId() { return id; }
    public double getEnergyLevel() { return energyLevel; }
    public Map<FaceColor, FaceState> getFaces() { return Collections.unmodifiableMap(faces); }
    public Map<FaceColor, Tetrahedron> getConnections() { return Collections.unmodifiableMap(connections); }

    @Override
    public String toString() {
        return String.format("Тетраэдр[%s] энергия=%.2f соединения=%s",
                id, energyLevel, connections.keySet());
    }

    /**
     * Фаза 1: вычислить дельту по градиенту соседей (НЕ применять).
     * Вызывать для всех тетраэдров ДО applyDelta().
     */
    public void computeGradientFlow(double flowRate) {
        double inflow = 0.0;
        for (Tetrahedron neighbor : connections.values()) {
            inflow += (neighbor.energyLevel - this.energyLevel) * flowRate;
        }
        this.pendingDelta = inflow;
    }

    /**
     * Фаза 2: применить ранее вычисленную дельту.
     * Вызывать ПОСЛЕ того как computeGradientFlow вызван у всех.
     */
    public void applyDelta(double damping) {
        energyLevel = Math.max(0, energyLevel * damping + pendingDelta);
        pendingDelta = 0;
    }
}