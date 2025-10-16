package ru.mcs.q.field;

import java.awt.Point;

public class ProjectionUtils {

    // Изометрическая проекция 3D -> 2D
    public static Point projectIsometric(Vector3D point3D, double scale, Point center) {
        double x2d = center.x + scale * (point3D.x - point3D.z) * Math.cos(Math.PI/6);
        double y2d = center.y + scale * (point3D.y - (point3D.x + point3D.z) * Math.sin(Math.PI/6));
        return new Point((int)x2d, (int)y2d);
    }

    // Перспективная проекция
    public static Point projectPerspective(Vector3D point3D, double cameraDistance, double scale, Point center) {
        double depth = cameraDistance - point3D.z;
        if (depth <= 0) depth = 0.1;

        double x2d = center.x + scale * point3D.x / depth;
        double y2d = center.y + scale * point3D.y / depth;
        return new Point((int)x2d, (int)y2d);
    }

    // Простая ортографическая проекция
    public static Point projectOrthographic(Vector3D point3D, double scale, Point center) {
        double x2d = center.x + scale * point3D.x;
        double y2d = center.y + scale * point3D.y;
        return new Point((int)x2d, (int)y2d);
    }
}