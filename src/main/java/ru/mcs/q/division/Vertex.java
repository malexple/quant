package ru.mcs.q.division;

public class Vertex {
    public final int id;
    public double x, y, z;

    public Vertex(int id, double x, double y, double z) {
        this.id = id;
        this.x  = x;
        this.y  = y;
        this.z  = z;
    }

    public double[] pos() { return new double[]{x, y, z}; }
}