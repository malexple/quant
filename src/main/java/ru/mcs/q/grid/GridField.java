package ru.mcs.q.grid;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

public class GridField {

    public static final int WIDTH  = 100;
    public static final int HEIGHT = 100;

    // FLOW_RATE < 0.25 — условие устойчивости для 4-связного Лапласиана
//    private static final double FLOW_RATE = 0.24;
//    private static final double DAMPING   = 0.9995; // лёгкое затухание (энтропия)

//    private final double[][] energy  = new double[HEIGHT][WIDTH];
//    private final double[][] buffer  = new double[HEIGHT][WIDTH];
//    private final double[][] display = new double[HEIGHT][WIDTH]; // снимок для рендера
//    private final double[][] velocity = new double[HEIGHT][WIDTH];

//    private static final double LAMBDA = 0.000001; // сила нелинейности
//    public static final double V      = 300.0;    // положение вакуума

    // ===== Константы =====
    private static final double FLOW_RATE = 0.20;    // чуть меньше для устойчивости
    private static final double DAMPING   = 0.998;   // сильнее гасит расходимость
    public  static final double V         = 300.0;   // public — нужен в визуализации
    private static final double LAMBDA    = 0.000002;// потенциал φ⁴
    private static final double MAX_PHI   = V * 20;  // ограничитель взрыва

    // ===== Поля =====
    private final double[][] energy   = new double[HEIGHT][WIDTH];
    private final double[][] buffer   = new double[HEIGHT][WIDTH];
    private final double[][] velocity = new double[HEIGHT][WIDTH];
    private final double[][] display  = new double[HEIGHT][WIDTH];
    private volatile int tick = 0;


    private final ScheduledExecutorService scheduler =
            Executors.newSingleThreadScheduledExecutor();

//    private volatile int tick = 0;

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
    // ===== Полный update() с защитой от NaN/Infinity =====
    private void update() {
        synchronized (energy) {
            for (int y = 0; y < HEIGHT; y++) {
                for (int x = 0; x < WIDTH; x++) {
                    double phi = energy[y][x];

                    // Защита: если уже NaN — сбросить узел
                    if (!Double.isFinite(phi)) {
                        energy[y][x]   = 0;
                        velocity[y][x] = 0;
                        buffer[y][x]   = 0;
                        continue;
                    }

                    // 4 соседа, торическая топология
                    double left  = energy[y][(x - 1 + WIDTH)  % WIDTH];
                    double right = energy[y][(x + 1) % WIDTH];
                    double up    = energy[(y - 1 + HEIGHT) % HEIGHT][x];
                    double down  = energy[(y + 1) % HEIGHT][x];

                    // Дискретный Лапласиан
                    double laplacian = left + right + up + down - 4.0 * phi;

                    // Потенциал φ⁴: вычисляем по ЗАЖАТОМУ phi чтобы не взорваться
                    double phiC     = Math.max(-MAX_PHI, Math.min(MAX_PHI, phi));
                    double potential = LAMBDA * (phiC * phiC - V * V) * phiC;

                    // Волновое уравнение + нелинейность
                    double vel = velocity[y][x] * DAMPING
                            + FLOW_RATE * laplacian
                            - potential;

                    // Зажать скорость — второй уровень защиты
                    velocity[y][x] = Math.max(-MAX_PHI, Math.min(MAX_PHI, vel));
                    buffer[y][x]   = phi + velocity[y][x];
                }
            }

            // Атомарный своп буферов
            for (int y = 0; y < HEIGHT; y++) {
                System.arraycopy(buffer[y], 0, energy[y], 0, WIDTH);
                System.arraycopy(energy[y], 0, display[y], 0, WIDTH);
            }
            tick++;
        }
    }

    /**
     * Детектирует "частицы" как границы доменов — узлы где поле
     * пересекает границу между вакуумами +V и -V.
     * Возвращает список координат [x, y] границ.
     */
    public List<int[]> detectParticles() {
        List<int[]> particles = new ArrayList<>();
        double threshold = V * 0.5;

        for (int y = 0; y < HEIGHT; y++) {
            for (int x = 0; x < WIDTH; x++) {
                double phi   = display[y][x];
                double right = display[y][(x + 1) % WIDTH];
                double down  = display[(y + 1) % HEIGHT][x];

                // Граница домена: поле меняет знак вакуума по горизонтали или вертикали
                boolean crossH = (phi > threshold  && right < -threshold)
                        || (phi < -threshold && right > threshold);
                boolean crossV = (phi > threshold  && down  < -threshold)
                        || (phi < -threshold && down  > threshold);

                if (crossH || crossV) {
                    particles.add(new int[]{x, y});
                }
            }
        }
        return particles;
    }

    public double getDisplay(int x, int y) { return display[y][x]; }
    public int    getTick()                { return tick; }

    public double getTotalEnergy() {
        double sum = 0;
        for (double[] row : display)
            for (double v : row)
                if (Double.isFinite(v)) sum += v;
        return sum;
    }

    public double getMaxEnergy() {
        double max = 0;
        for (double[] row : display)
            for (double v : row) if (v > max) max = v;
        return max;
    }

    public double getMinEnergy() {
        double min = Double.MAX_VALUE;
        for (double[] row : display)
            for (double v : row) if (v < min) min = v;
        return min;
    }
}