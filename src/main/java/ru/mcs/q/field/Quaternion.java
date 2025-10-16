package ru.mcs.q.field;

public class Quaternion {
    public double w, x, y, z;

    public Quaternion(double w, double x, double y, double z) {
        this.w = w;
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public static Quaternion identity() {
        return new Quaternion(1, 0, 0, 0);
    }

    // Создание кватерниона из оси и угла вращения
    public static Quaternion fromAxisAngle(Vector3D axis, double angle) {
        double halfAngle = angle / 2;
        double sinHalf = Math.sin(halfAngle);
        double cosHalf = Math.cos(halfAngle);

        Vector3D normalizedAxis = normalize(axis);
        return new Quaternion(
                cosHalf,
                normalizedAxis.x * sinHalf,
                normalizedAxis.y * sinHalf,
                normalizedAxis.z * sinHalf
        );
    }

    private static Vector3D normalize(Vector3D v) {
        double length = Math.sqrt(v.x * v.x + v.y * v.y + v.z * v.z);
        if (length == 0) return new Vector3D(0, 0, 0);
        return new Vector3D(v.x / length, v.y / length, v.z / length);
    }

    // Умножение кватернионов
    public Quaternion multiply(Quaternion other) {
        return new Quaternion(
                w * other.w - x * other.x - y * other.y - z * other.z,
                w * other.x + x * other.w + y * other.z - z * other.y,
                w * other.y - x * other.z + y * other.w + z * other.x,
                w * other.z + x * other.y - y * other.x + z * other.w
        );
    }

    // Применение вращения к вектору
    public Vector3D rotateVector(Vector3D v) {
        Quaternion qVec = new Quaternion(0, v.x, v.y, v.z);
        Quaternion conjugate = new Quaternion(w, -x, -y, -z);
        Quaternion result = this.multiply(qVec).multiply(conjugate);
        return new Vector3D(result.x, result.y, result.z);
    }

    @Override
    public String toString() {
        return String.format("(%.2f, %.2f, %.2f, %.2f)", w, x, y, z);
    }
}