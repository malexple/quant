package ru.mcs.q.field;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;

public class QuantumFieldVisualization extends JPanel {
    private final QuantumFieldMesh3D mesh;
    private double scale = 50;
    private final Point center = new Point(400, 300);
    private double rotationX = 0;
    private double rotationY = 0;
    private double cameraDistance = 5;
    private String projectionType = "ISOMETRIC";

    public QuantumFieldVisualization(QuantumFieldMesh3D mesh) {
        this.mesh = mesh;
        setPreferredSize(new Dimension(800, 600));
        setBackground(Color.WHITE);

        // Добавляем обработчики для вращения
        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                // Переключение проекции по клику
                switch (projectionType) {
                    case "ISOMETRIC" -> projectionType = "PERSPECTIVE";
                    case "PERSPECTIVE" -> projectionType = "ORTHOGRAPHIC";
                    default -> projectionType = "ISOMETRIC";
                }
                repaint();
            }
        });

        addMouseMotionListener(new MouseMotionAdapter() {
            private int lastX, lastY;

            @Override
            public void mouseDragged(MouseEvent e) {
                int dx = e.getX() - lastX;
                int dy = e.getY() - lastY;

                rotationY += dx * 0.01;
                rotationX += dy * 0.01;

                lastX = e.getX();
                lastY = e.getY();
                repaint();
            }

            @Override
            public void mouseMoved(MouseEvent e) {
                lastX = e.getX();
                lastY = e.getY();
            }
        });

        addMouseWheelListener(e -> {
            scale += e.getWheelRotation() * 5;
            scale = Math.max(10, Math.min(200, scale));
            repaint();
        });
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;

        // Включаем сглаживание
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Рисуем соединения
        drawConnections(g2d);

        // Рисуем тетраэдры
        drawTetrahedrons(g2d);

        // Рисуем информацию
        drawInfo(g2d);
    }

    private void drawConnections(Graphics2D g2d) {
        for (Tetrahedron3D tetra : mesh.getTetrahedrons().values()) {
            Vector3D pos1 = tetra.getPosition();

            for (Map.Entry<FaceColor, Tetrahedron3D> entry : tetra.getConnections().entrySet()) {
                Tetrahedron3D connected = entry.getValue();
                FaceColor color = entry.getKey();

                Vector3D pos2 = connected.getPosition();

                // Рисуем линию соединения
                Point p1 = projectPoint(rotatePoint(pos1));
                Point p2 = projectPoint(rotatePoint(pos2));

                g2d.setColor(toAwtColor(color));
                g2d.setStroke(new BasicStroke(2));
                g2d.drawLine(p1.x, p1.y, p2.x, p2.y);
            }
        }
    }

    private void drawTetrahedrons(Graphics2D g2d) {
        for (Tetrahedron3D tetra : mesh.getTetrahedrons().values()) {
            drawTetrahedron(g2d, tetra);
        }
    }

    private void drawTetrahedron(Graphics2D g2d, Tetrahedron3D tetra) {
        Vector3D[] vertices = tetra.getWorldVertices();
        int[][] faces = tetra.getFacesIndices();

        // Проецируем и вращаем все вершины
        Point[] projectedVertices = new Point[vertices.length];
        for (int i = 0; i < vertices.length; i++) {
            projectedVertices[i] = projectPoint(rotatePoint(vertices[i]));
        }

        // Рисуем грани
        for (int i = 0; i < faces.length; i++) {
            int[] face = faces[i];
            FaceColor faceColor = tetra.getFaceColor(i);

            // Пропускаем прозрачные грани
            if (faceColor == FaceColor.TRANSPARENT) continue;

            Polygon polygon = new Polygon();
            for (int vertexIndex : face) {
                Point p = projectedVertices[vertexIndex];
                polygon.addPoint(p.x, p.y);
            }

            // Заливаем грань цветом с прозрачностью
            Color fillColor = toAwtColor(faceColor);
            g2d.setColor(new Color(fillColor.getRed(), fillColor.getGreen(), fillColor.getBlue(), 100));
            g2d.fill(polygon);

            // Рисуем контур
            g2d.setColor(fillColor);
            g2d.setStroke(new BasicStroke(1));
            g2d.draw(polygon);
        }

        // Рисуем центр тетраэдра
        Point centerPoint = projectPoint(rotatePoint(tetra.getPosition()));
        g2d.setColor(Color.BLACK);
        g2d.fillOval(centerPoint.x - 3, centerPoint.y - 3, 6, 6);

        // Подписываем тетраэдр
        g2d.drawString(tetra.getId(), centerPoint.x + 5, centerPoint.y - 5);

        // Показываем энергию
        g2d.setColor(Color.DARK_GRAY);
        g2d.drawString(String.format("%.1f", tetra.getEnergyLevel()), centerPoint.x + 5, centerPoint.y + 15);
    }

    private Vector3D rotatePoint(Vector3D point) {
        // Вращение вокруг оси Y
        double x1 = point.x * Math.cos(rotationY) - point.z * Math.sin(rotationY);
        double z1 = point.x * Math.sin(rotationY) + point.z * Math.cos(rotationY);

        // Вращение вокруг оси X
        double y1 = point.y * Math.cos(rotationX) - z1 * Math.sin(rotationX);
        double z2 = point.y * Math.sin(rotationX) + z1 * Math.cos(rotationX);

        return new Vector3D(x1, y1, z2);
    }

    private Point projectPoint(Vector3D point) {
        return switch (projectionType) {
            case "PERSPECTIVE" -> ProjectionUtils.projectPerspective(point, cameraDistance, scale, center);
            case "ORTHOGRAPHIC" -> ProjectionUtils.projectOrthographic(point, scale, center);
            default -> ProjectionUtils.projectIsometric(point, scale, center);
        };
    }

    private Color toAwtColor(FaceColor color) {
        return switch (color) {
            case RED -> Color.RED;
            case BLUE -> Color.BLUE;
            case GREEN -> Color.GREEN;
            case TRANSPARENT -> new Color(200, 200, 200, 100);
        };
    }

    private void drawInfo(Graphics2D g2d) {
        g2d.setColor(Color.BLACK);
        g2d.drawString("Projection: " + projectionType, 10, 20);
        g2d.drawString("Scale: " + scale, 10, 40);
        g2d.drawString("Tetrahedrons: " + mesh.getTetrahedrons().size(), 10, 60);
        g2d.drawString("Click to change projection, Drag to rotate, Scroll to zoom", 10, 80);
    }
}