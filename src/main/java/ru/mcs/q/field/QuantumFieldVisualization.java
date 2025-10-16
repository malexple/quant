package ru.mcs.q.field;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;

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
            for (Map.Entry<FaceColor, Tetrahedron3D> entry : tetra.getConnections().entrySet()) {
                Tetrahedron3D connected = entry.getValue();
                FaceColor color = entry.getKey();

                // Рисуем линию между центрами соединенных граней
                int faceIndex1 = tetra.getFaceIndex(color);
                int faceIndex2 = connected.getFaceIndex(color);

                Vector3D faceCenter1 = tetra.getFaceCenter(faceIndex1);
                Vector3D faceCenter2 = connected.getFaceCenter(faceIndex2);

                Point p1 = projectPoint(rotatePoint(faceCenter1));
                Point p2 = projectPoint(rotatePoint(faceCenter2));

                g2d.setColor(toAwtColor(color));
                g2d.setStroke(new BasicStroke(3, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g2d.drawLine(p1.x, p1.y, p2.x, p2.y);

                // Рисуем маленькие кружки в центрах граней
                g2d.setColor(toAwtColor(color).darker());
                g2d.fillOval(p1.x - 3, p1.y - 3, 6, 6);
                g2d.fillOval(p2.x - 3, p2.y - 3, 6, 6);
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

        // Сначала рисуем непрозрачные грани
        for (int i = 0; i < faces.length; i++) {
            FaceColor faceColor = tetra.getFaceColor(i);
            if (faceColor == FaceColor.TRANSPARENT) continue;

            int[] face = faces[i];
            Polygon polygon = new Polygon();
            for (int vertexIndex : face) {
                Point p = projectedVertices[vertexIndex];
                polygon.addPoint(p.x, p.y);
            }

            // Определяем, является ли грань соединенной
            boolean isConnected = tetra.getConnections().containsKey(faceColor);

            // Заливаем грань цветом с разной прозрачностью
            Color fillColor = toAwtColor(faceColor);
            if (isConnected) {
                g2d.setColor(new Color(fillColor.getRed(), fillColor.getGreen(), fillColor.getBlue(), 150));
            } else {
                g2d.setColor(new Color(fillColor.getRed(), fillColor.getGreen(), fillColor.getBlue(), 80));
            }
            g2d.fill(polygon);

            // Рисуем контур
            g2d.setColor(isConnected ? fillColor.darker() : fillColor);
            g2d.setStroke(new BasicStroke(isConnected ? 2 : 1));
            g2d.draw(polygon);
        }

        // Затем рисуем прозрачную грань (основание)
        for (int i = 0; i < faces.length; i++) {
            FaceColor faceColor = tetra.getFaceColor(i);
            if (faceColor != FaceColor.TRANSPARENT) continue;

            int[] face = faces[i];
            Polygon polygon = new Polygon();
            for (int vertexIndex : face) {
                Point p = projectedVertices[vertexIndex];
                polygon.addPoint(p.x, p.y);
            }

            // Рисуем прозрачную грань пунктиром
            g2d.setColor(new Color(150, 150, 150, 100));
            g2d.fill(polygon);

            g2d.setColor(Color.GRAY);
            g2d.setStroke(new BasicStroke(1, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL,
                    0, new float[]{3}, 0));
            g2d.draw(polygon);
        }

        // Рисуем центр тетраэдра
        Point centerPoint = projectPoint(rotatePoint(tetra.getPosition()));
        g2d.setColor(Color.BLACK);
        g2d.fillOval(centerPoint.x - 2, centerPoint.y - 2, 4, 4);

        // Подписываем тетраэдр
        g2d.setColor(Color.BLACK);
        g2d.drawString(tetra.getId(), centerPoint.x + 5, centerPoint.y - 5);
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
        g2d.drawString("Connections: " + countConnections(), 10, 80);
        g2d.drawString("Click to change projection, Drag to rotate, Scroll to zoom", 10, 100);
    }

    private int countConnections() {
        return mesh.getTetrahedrons().values().stream()
                .mapToInt(t -> t.getConnections().size())
                .sum() / 2;
    }
}