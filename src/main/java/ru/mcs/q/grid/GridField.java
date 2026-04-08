package ru.mcs.q.grid;

import java.util.concurrent.*;

public class GridField {

    public static final int WIDTH  = 100;
    public static final int HEIGHT = 100;

    // FLOW_RATE < 0.25 — условие устойчивости для 4-связного Лапласиана
    private static final double FLOW_RATE = 0.24;
    private static final double DAMPING   = 0.999; // лёгкое затухание (энтропия)

    private final double[][] energy  = new double[HEIGHT][WIDTH];
    private final double[][] buffer  = new double[HEIGHT][WIDTH];
    private final double[][] display = new double[HEIGHT][WIDTH]; // снимок для рендера
    private final double[][] velocity = new double[HEIGHT][WIDTH];

    private final ScheduledExecutorService scheduler =
            Executors.newSingleThreadScheduledExecutor();

    private volatile int tick = 0;

    /**
     * Добавить возбуждение в узел (x, y).
     * Можно вызывать из любого потока.
     */
    public void excite(int x, int y, double amount) {
        synchronized (energy) {
            if (x >= 0 && x < WIDTH && y >= 0 && y < HEIGHT) {
                energy[y][x] = Math.max(0, energy[y][x] + amount);
            }
        }
    }

    public void start(long tickMs) {
        scheduler.scheduleAtFixedRate(this::update, 0, tickMs, TimeUnit.MILLISECONDS);
    }

    public void stop() {
        scheduler.shutdown();
    }

    /**
     * Один тик симуляции.
     * Правило: discrete Laplacian (уравнение теплопроводности):
     *   new_e = old_e * DAMPING + FLOW_RATE * (Σ_neighbors - 4 * old_e)
     *
     * Топология: ТОРИЧЕСКАЯ — граница = граница с противоположной стороны.
     * Это и есть "закольцованная вселенная".
     */
    private void update() {
        synchronized (energy) {
            for (int y = 0; y < HEIGHT; y++) {
                for (int x = 0; x < WIDTH; x++) {
                    double center = energy[y][x];
                    double left  = energy[y][(x - 1 + WIDTH)  % WIDTH];
                    double right = energy[y][(x + 1) % WIDTH];
                    double up    = energy[(y - 1 + HEIGHT) % HEIGHT][x];
                    double down  = energy[(y + 1) % HEIGHT][x];

                    double laplacian = left + right + up + down - 4.0 * center;

                    // Волновое уравнение: ускорение ∝ Лапласиан
                    velocity[y][x] = velocity[y][x] * DAMPING + FLOW_RATE * laplacian;
                    buffer[y][x]   = Math.max(0, center + velocity[y][x]);
                }
            }
            for (int y = 0; y < HEIGHT; y++) {
                System.arraycopy(buffer[y], 0, energy[y], 0, WIDTH);
                System.arraycopy(energy[y], 0, display[y], 0, WIDTH);
            }
            tick++;
        }
    }

    public double getDisplay(int x, int y) { return display[y][x]; }
    public int    getTick()                { return tick; }

    public double getTotalEnergy() {
        double sum = 0;
        for (double[] row : display)
            for (double v : row) sum += v;
        return sum;
    }

    public double getMaxEnergy() {
        double max = 0;
        for (double[] row : display)
            for (double v : row) if (v > max) max = v;
        return max;
    }
}