package ru.mcs.q.field;

import java.awt.Color;
import java.util.*;

public class Tetrahedron3D {
    private final String id;
    private Vector3D position;
    private final Map<FaceColor, FaceState> faces;
    private final Map<FaceColor, Tetrahedron3D> connections;
    private double energyLevel;
    private double size;

    // Вершины тетраэдра относительно его центра
    private final Vector3D[] vertices;

    public Tetrahedron3D(String id, Vector3D position, double size) {
        this.id = id;
        this.position = position;
        this.size = size;
        this.faces = new EnumMap<>(FaceColor.class);
        this.connections = new EnumMap<>(FaceColor.class);
        this.energyLevel = 0.0;

        // Инициализируем все грани
        for (FaceColor color : FaceColor.values()) {
            faces.put(color, FaceState.NEUTRAL);
        }

        // Вычисляем вершины тетраэдра
        this.vertices = calculateVertices();
    }

    private Vector3D[] calculateVertices() {
        Vector3D[] verts = new Vector3D[4];

        // Вершины правильного тетраэдра
        double a = size;
        verts[0] = new Vector3D(0, 0, a * Math.sqrt(2.0/3.0)); // Верхняя вершина
        verts[1] = new Vector3D(0, a/Math.sqrt(3), -a/Math.sqrt(6)); // Основание
        verts[2] = new Vector3D(a/2, -a/(2*Math.sqrt(3)), -a/Math.sqrt(6)); // Основание
        verts[3] = new Vector3D(-a/2, -a/(2*Math.sqrt(3)), -a/Math.sqrt(6)); // Основание

        return verts;
    }

    public Vector3D[] getWorldVertices() {
        Vector3D[] worldVerts = new Vector3D[4];
        for (int i = 0; i < 4; i++) {
            worldVerts[i] = vertices[i].add(position);
        }
        return worldVerts;
    }

    // Грани тетраэдра (индексы вершин)
    public int[][] getFacesIndices() {
        return new int[][] {
                {0, 1, 2}, // Грань 0
                {0, 2, 3}, // Грань 1
                {0, 3, 1}, // Грань 2
                {1, 3, 2}  // Грань 3 (основание)
        };
    }

    public FaceColor getFaceColor(int faceIndex) {
        FaceColor[] colors = {FaceColor.RED, FaceColor.BLUE, FaceColor.GREEN, FaceColor.TRANSPARENT};
        return colors[faceIndex];
    }

    public boolean connect(FaceColor face, Tetrahedron3D other, FaceColor otherFace) {
        if (other == null) {
            return false;
        }

        if (face == otherFace && face != FaceColor.TRANSPARENT) {
            if (!connections.containsKey(face) && !other.connections.containsKey(otherFace)) {
                connections.put(face, other);
                other.connections.put(otherFace, this);

                this.energyLevel += 0.1;
                other.energyLevel += 0.1;

                faces.put(face, FaceState.ATTRACTED);
                other.faces.put(otherFace, FaceState.ATTRACTED);

                return true;
            }
        }
        return false;
    }

    // Getters
    public String getId() { return id; }
    public Vector3D getPosition() { return position; }
    public void setPosition(Vector3D position) { this.position = position; }
    public double getEnergyLevel() { return energyLevel; }
    public Map<FaceColor, FaceState> getFaces() { return faces; }
    public Map<FaceColor, Tetrahedron3D> getConnections() { return connections; }
    public double getSize() { return size; }

    @Override
    public String toString() {
        return String.format("Tetrahedron3D[%s] pos=%s energy=%.2f", id, position, energyLevel);
    }
}