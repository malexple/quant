package ru.mcs.q.field;

import java.util.*;

public class Tetrahedron3D {
    private final String id;
    private Vector3D position;
    private Quaternion orientation;
    private final Map<FaceColor, FaceState> faces;
    private final Map<FaceColor, Tetrahedron3D> connections;
    public double energyLevel;
    private final double size;

    // Вершины тетраэдра в локальной системе координат
    private final Vector3D[] localVertices;
    // Нормали к граням в локальной системе координат
    private final Vector3D[] faceNormals;
    // Центры граней в локальной системе координат
    private final Vector3D[] localFaceCenters;

    public Tetrahedron3D(String id, Vector3D position, double size) {
        this.id = id;
        this.position = position;
        this.orientation = Quaternion.identity();
        this.size = size;
        this.faces = new EnumMap<>(FaceColor.class);
        this.connections = new EnumMap<>(FaceColor.class);
        this.energyLevel = 0.0;

        // Инициализируем все грани
        for (FaceColor color : FaceColor.values()) {
            faces.put(color, FaceState.NEUTRAL);
        }

        // Вычисляем вершины, нормали и центры граней
        this.localVertices = calculateVertices();
        this.faceNormals = calculateFaceNormals();
        this.localFaceCenters = calculateFaceCenters();
    }

    private Vector3D[] calculateVertices() {
        Vector3D[] verts = new Vector3D[4];
        double a = size;

        // Вершины правильного тетраэдра с центром в начале координат
        double height = a * Math.sqrt(2.0/3.0);

        verts[0] = new Vector3D(0, height/2, 0);                    // Верхняя вершина
        verts[1] = new Vector3D(0, -height/3, a/2);                // Основание
        verts[2] = new Vector3D(a/2, -height/3, -a/4);             // Основание
        verts[3] = new Vector3D(-a/2, -height/3, -a/4);            // Основание

        return verts;
    }

    private Vector3D[] calculateFaceNormals() {
        Vector3D[] normals = new Vector3D[4];

        // Нормали для каждой грани (направлены наружу от центра)
        normals[0] = calculateFaceNormal(localVertices[0], localVertices[1], localVertices[2]); // RED
        normals[1] = calculateFaceNormal(localVertices[0], localVertices[2], localVertices[3]); // BLUE
        normals[2] = calculateFaceNormal(localVertices[0], localVertices[3], localVertices[1]); // GREEN
        normals[3] = calculateFaceNormal(localVertices[1], localVertices[3], localVertices[2]); // TRANSPARENT

        return normals;
    }

    private Vector3D[] calculateFaceCenters() {
        Vector3D[] centers = new Vector3D[4];
        int[][] faceIndices = getFacesIndices();

        for (int i = 0; i < 4; i++) {
            int[] indices = faceIndices[i];
            Vector3D sum = new Vector3D(0, 0, 0);
            for (int idx : indices) {
                sum = sum.add(localVertices[idx]);
            }
            centers[i] = new Vector3D(sum.x / 3, sum.y / 3, sum.z / 3);
        }

        return centers;
    }

    private Vector3D calculateFaceNormal(Vector3D v1, Vector3D v2, Vector3D v3) {
        Vector3D edge1 = new Vector3D(v2.x - v1.x, v2.y - v1.y, v2.z - v1.z);
        Vector3D edge2 = new Vector3D(v3.x - v1.x, v3.y - v1.y, v3.z - v1.z);

        // Векторное произведение
        Vector3D cross = edge1.cross(edge2);

        // Нормализация
        return cross.normalize();
    }

    public Vector3D[] getWorldVertices() {
        Vector3D[] worldVerts = new Vector3D[4];
        for (int i = 0; i < 4; i++) {
            // Поворачиваем вершину и добавляем позицию
            Vector3D rotated = orientation.rotateVector(localVertices[i]);
            worldVerts[i] = rotated.add(position);
        }
        return worldVerts;
    }

    public Vector3D getFaceNormal(int faceIndex) {
        return orientation.rotateVector(faceNormals[faceIndex]);
    }

    public Vector3D getFaceCenter(int faceIndex) {
        Vector3D rotatedCenter = orientation.rotateVector(localFaceCenters[faceIndex]);
        return rotatedCenter.add(position);
    }

    public Vector3D getLocalFaceCenter(int faceIndex) {
        return localFaceCenters[faceIndex];
    }

    public Vector3D getLocalFaceNormal(int faceIndex) {
        return faceNormals[faceIndex];
    }

    // Грани тетраэдра (индексы вершин)
    public int[][] getFacesIndices() {
        return new int[][] {
                {0, 1, 2}, // Грань 0 - RED
                {0, 2, 3}, // Грань 1 - BLUE
                {0, 3, 1}, // Грань 2 - GREEN
                {1, 3, 2}  // Грань 3 - TRANSPARENT (основание)
        };
    }

    public FaceColor getFaceColor(int faceIndex) {
        FaceColor[] colors = {FaceColor.RED, FaceColor.BLUE, FaceColor.GREEN, FaceColor.TRANSPARENT};
        return colors[faceIndex];
    }

    public int getFaceIndex(FaceColor color) {
        FaceColor[] colors = {FaceColor.RED, FaceColor.BLUE, FaceColor.GREEN, FaceColor.TRANSPARENT};
        for (int i = 0; i < colors.length; i++) {
            if (colors[i] == color) return i;
        }
        return -1;
    }

    public boolean connect(FaceColor face, Tetrahedron3D other, FaceColor otherFace) {
        if (other == null) {
            return false;
        }

        if (face == otherFace) {
            if (!connections.containsKey(face) && !other.connections.containsKey(otherFace)) {
                // Ориентируем второй тетраэдр относительно первого
                orientRelativeTo(this, face, other, otherFace);

                connections.put(face, other);
                other.connections.put(otherFace, this);

                this.energyLevel += 0.1;
                other.energyLevel += 0.1;

                faces.put(face, FaceState.ATTRACTED);
                other.faces.put(otherFace, FaceState.ATTRACTED);

                System.out.printf("Connection: %s[%s] ↔ %s[%s]%n",
                        id, face, other.id, otherFace);
                return true;
            }
        }
        return false;
    }

    private void orientRelativeTo(Tetrahedron3D reference, FaceColor refFace,
                                  Tetrahedron3D target, FaceColor targetFace) {
        int refFaceIndex = reference.getFaceIndex(refFace);
        int targetFaceIndex = target.getFaceIndex(targetFace);

        // Получаем нормаль и центр грани референсного тетраэдра
        Vector3D refNormal = reference.getFaceNormal(refFaceIndex);
        Vector3D refCenter = reference.getFaceCenter(refFaceIndex);

        // Получаем локальную нормаль целевой грани
        Vector3D targetLocalNormal = target.getLocalFaceNormal(targetFaceIndex);

        // Мы хотим, чтобы нормаль целевой грани была противоположна нормали референсной грани
        Vector3D desiredTargetNormal = refNormal.negate();

        // Находим вращение, которое переводит текущую мировую нормаль целевой грани в желаемую
        Vector3D currentTargetNormal = target.getFaceNormal(targetFaceIndex);
        Quaternion rotation = findRotation(currentTargetNormal, desiredTargetNormal);

        // Применяем вращение к целевому тетраэдру
        target.orientation = rotation.multiply(target.orientation);

        // Позиционируем целевой тетраэдр так, чтобы грани были обращены друг к другу
        Vector3D targetCenter = target.getFaceCenter(targetFaceIndex);
        Vector3D between = targetCenter.subtract(refCenter);

        // Проецируем вектор между центрами на нормаль референсной грани
        double projection = between.dot(refNormal);
        Vector3D correction = refNormal.multiply(projection);

        // Корректируем позицию целевого тетраэдра
        target.position = target.position.subtract(correction);

        // Добавляем небольшое расстояние между гранями
        double separation = size * 0.1;
        target.position = target.position.add(refNormal.multiply(separation));
    }

    private Quaternion findRotation(Vector3D from, Vector3D to) {
        from = from.normalize();
        to = to.normalize();

        double dot = from.dot(to);

        // Если векторы уже совпадают, возвращаем единичный кватернион
        if (dot > 0.99999) {
            return Quaternion.identity();
        }

        // Если векторы противоположны, выбираем произвольную ось
        if (dot < -0.99999) {
            // Выбираем ось, перпендикулярную from
            Vector3D axis = findPerpendicularAxis(from);
            return Quaternion.fromAxisAngle(axis, Math.PI);
        }

        // Общий случай: вычисляем ось и угол вращения
        Vector3D axis = from.cross(to).normalize();
        double angle = Math.acos(dot);

        return Quaternion.fromAxisAngle(axis, angle);
    }

    private Vector3D findPerpendicularAxis(Vector3D v) {
        // Находим произвольную ось, перпендикулярную v
        if (Math.abs(v.x) > 0.1) {
            return new Vector3D(-v.y, v.x, 0).normalize();
        } else if (Math.abs(v.y) > 0.1) {
            return new Vector3D(0, -v.z, v.y).normalize();
        } else {
            return new Vector3D(v.z, 0, -v.x).normalize();
        }
    }

    // Getters
    public String getId() { return id; }
    public Vector3D getPosition() { return position; }
    public void setPosition(Vector3D position) { this.position = position; }
    public Quaternion getOrientation() { return orientation; }
    public void setOrientation(Quaternion orientation) { this.orientation = orientation; }
    public double getEnergyLevel() { return energyLevel; }
    public Map<FaceColor, FaceState> getFaces() { return faces; }
    public Map<FaceColor, Tetrahedron3D> getConnections() { return connections; }
    public double getSize() { return size; }

    @Override
    public String toString() {
        return String.format("Tetrahedron3D[%s] pos=%s energy=%.2f", id, position, energyLevel);
    }
}