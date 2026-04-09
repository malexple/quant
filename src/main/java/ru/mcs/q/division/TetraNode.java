package ru.mcs.q.division;

import ru.mcs.q.field.FaceColor;

public class TetraNode {

    public final int id;

    // Метрика: размер каждой грани [0..1]
    public final float[] faceSize = {1f, 1f, 1f, 1f};

    // Волновое поле
    public double phase;
    public double velocity;

    // Граф связей
    public final TetraNode[] neighbors    = new TetraNode[4];
    public final int[]       neighborFace = new int[4];

    // ── 4 реальных 3D-вершины тетраэдра ──────────────────────────────────────
    // verts[0..3][0..2] = x,y,z каждой вершины
    // Грани определяются тройками вершин:
    //   Face 0 RED         : v1, v2, v3  (боковая)
    //   Face 1 BLUE        : v0, v2, v3  (боковая)
    //   Face 2 GREEN       : v0, v1, v3  (боковая)
    //   Face 3 TRANSPARENT : v0, v1, v2  (основание)
    public final double[][] verts = new double[4][3];

    // Рёбра тетраэдра — 6 штук, каждое принадлежит двум граням
    // edge[i] = {v_a, v_b, face_a, face_b}
    public static final int[][] EDGES = {
            {0, 1,  2, 3},   // ребро v0-v1: грани GREEN + TRANSPARENT
            {0, 2,  1, 3},   // ребро v0-v2: грани BLUE  + TRANSPARENT
            {0, 3,  1, 2},   // ребро v0-v3: грани BLUE  + GREEN
            {1, 2,  0, 3},   // ребро v1-v2: грани RED   + TRANSPARENT
            {1, 3,  0, 2},   // ребро v1-v3: грани RED   + GREEN
            {2, 3,  0, 1},   // ребро v2-v3: грани RED   + BLUE
    };

    // Позиция центра для spring-relaxation (производная от verts)
    public double rx, ry, rz;

    public TetraNode(int id) {
        this.id = id;
    }

    public FaceColor color(int face) {
        return FaceColor.values()[face];
    }

    public boolean isFree(int face) {
        return neighbors[face] == null;
    }

    public int bondCount() {
        int n = 0;
        for (TetraNode nb : neighbors) if (nb != null) n++;
        return n;
    }

    public void bond(int face, TetraNode other, int otherFace) {
        this.neighbors[face]          = other;
        this.neighborFace[face]       = otherFace;
        other.neighbors[otherFace]    = this;
        other.neighborFace[otherFace] = face;
    }

    public void unbond(int face) {
        TetraNode other = neighbors[face];
        if (other == null) return;
        int of = neighborFace[face];
        other.neighbors[of]    = null;
        other.neighborFace[of] = 0;
        this.neighbors[face]   = null;
        this.neighborFace[face] = 0;
    }

    /** Центр тетраэдра из 4 вершин */
    public void updateCenter() {
        rx = (verts[0][0] + verts[1][0] + verts[2][0] + verts[3][0]) * 0.25;
        ry = (verts[0][1] + verts[1][1] + verts[2][1] + verts[3][1]) * 0.25;
        rz = (verts[0][2] + verts[1][2] + verts[2][2] + verts[3][2]) * 0.25;
    }

    @Override
    public String toString() {
        return "T" + id + "[φ=" + String.format("%.2f", phase)
                + " bonds=" + bondCount() + "]";
    }
}