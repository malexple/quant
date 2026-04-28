package ru.mcs.q.division;

import java.util.List;

public class TetraNode {

    public final int id;

    // Геометрия — локальные вершины (для рендера + деформации)
    public double[][] verts = new double[4][3];

    // Центр тетраэдра
    public double rx, ry, rz;

    // Индексы в общем пуле вершин (для роста)
    public int[] vertexIds = new int[4];

    // Поле
    public double phase    = 0;
    public double velocity = 0;

    // Метрика граней
    public float[] faceSize = new float[]{1f, 1f, 1f, 1f};

    // Топология
    public TetraNode[] neighbors    = new TetraNode[4];
    public int[]       neighborFace = new int[4];

    // Рёбра: [va, vb, faceA, faceB]  — ребро принадлежит двум граням
    public static final int[][] EDGES = {
            {0, 1,  2, 3},
            {0, 2,  1, 3},
            {0, 3,  1, 2},
            {1, 2,  0, 3},
            {1, 3,  0, 2},
            {2, 3,  0, 1},
    };

    public TetraNode(int id) { this.id = id; }

    public void bond(int myFace, TetraNode other, int otherFace) {
        neighbors[myFace]         = other;
        neighborFace[myFace]      = otherFace;
        other.neighbors[otherFace]    = this;
        other.neighborFace[otherFace] = myFace;
    }

    public void updateCenter() {
        rx = (verts[0][0]+verts[1][0]+verts[2][0]+verts[3][0]) * 0.25;
        ry = (verts[0][1]+verts[1][1]+verts[2][1]+verts[3][1]) * 0.25;
        rz = (verts[0][2]+verts[1][2]+verts[2][2]+verts[3][2]) * 0.25;
    }

    public void updateCenter(java.util.List<Vertex> pool) {
        for (int v = 0; v < 4; v++) {
            Vertex p    = pool.get(vertexIds[v]);
            verts[v][0] = p.x;
            verts[v][1] = p.y;
            verts[v][2] = p.z;
        }
        updateCenter();
    }
}