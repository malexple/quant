package ru.mcs.q.field;

import ru.mcs.q.Tetrahedron;

import java.util.*;
import java.util.concurrent.*;

public class QuantumFieldMesh {
    private final Map<String, Tetrahedron> tetrahedrons;
    private final ScheduledExecutorService scheduler;
    private boolean isActive;

    public QuantumFieldMesh() {
        this.tetrahedrons = new ConcurrentHashMap<>();
        this.scheduler = Executors.newScheduledThreadPool(1);
        this.isActive = false;
    }

    public void addTetrahedron(String id) {
        Tetrahedron tetrahedron = new Tetrahedron(id);
        tetrahedrons.put(id, tetrahedron);
        System.out.println("Добавлен " + tetrahedron);
    }

    public boolean connectTetrahedrons(String id1, FaceColor face1, String id2, FaceColor face2) {
        Tetrahedron t1 = tetrahedrons.get(id1);
        Tetrahedron t2 = tetrahedrons.get(id2);

        if (t1 != null && t2 != null) {
            return t1.connect(face1, t2, face2);
        }
        return false;
    }

    public void startFieldOscillations() {
        if (!isActive) {
            isActive = true;
            scheduler.scheduleAtFixedRate(this::updateField, 0, 2, TimeUnit.SECONDS);
            System.out.println("🔄 Квантовое поле активировано - начаты колебания");
        }
    }

    public void stopFieldOscillations() {
        if (isActive) {
            isActive = false;
            scheduler.shutdown();
            System.out.println("⏹️ Квантовое поле остановлено");
        }
    }

    private void updateField() {
        System.out.println("\n=== Обновление квантового поля ===");

        // Обновляем все тетраэдры
        tetrahedrons.values().forEach(tetrahedron -> {
            tetrahedron.oscillate();
            System.out.println(tetrahedron);
        });

        // Спонтанные соединения
        attemptSpontaneousConnections();

        printFieldState();
    }

    private void attemptSpontaneousConnections() {
        List<Tetrahedron> availableTetrahedrons = new ArrayList<>(tetrahedrons.values());
        Collections.shuffle(availableTetrahedrons);

        for (Tetrahedron t1 : availableTetrahedrons) {
            for (Tetrahedron t2 : availableTetrahedrons) {
                if (!t1.equals(t2) && Math.random() < 0.2) {
                    // Пытаемся соединить случайные грани
                    FaceColor[] colors = {FaceColor.RED, FaceColor.BLUE, FaceColor.GREEN};
                    FaceColor color = colors[(int)(Math.random() * colors.length)];

                    if (t1.getConnections().get(color) == null &&
                            t2.getConnections().get(color) == null) {
                        t1.connect(color, t2, color);
                    }
                }
            }
        }
    }

    public void printFieldState() {
        System.out.println("\n📊 Состояние квантовой сетки:");
        System.out.println("Всего тетраэдров: " + tetrahedrons.size());

        double totalEnergy = tetrahedrons.values().stream()
                .mapToDouble(Tetrahedron::getEnergyLevel)
                .sum();
        System.out.printf("Общая энергия поля: %.2f%n", totalEnergy);

        // Статистика по соединениям
        long connectionsCount = tetrahedrons.values().stream()
                .mapToLong(t -> t.getConnections().size())
                .sum() / 2; // Каждое соединение считается дважды

        System.out.println("Активных соединений: " + connectionsCount);
    }

    public void visualizeMesh() {
        System.out.println("\n🎨 Визуализация сетки:");
        tetrahedrons.values().forEach(t -> {
            System.out.print(t.getId() + " ");
            t.getFaces().forEach((color, state) -> {
                if (state == FaceState.ATTRACTED) {
                    System.out.print(color.getEmoji() + "↔ ");
                } else if (state == FaceState.VIBRATING) {
                    System.out.print(color.getEmoji() + "~ ");
                } else {
                    System.out.print(color.getEmoji() + " ");
                }
            });
            System.out.println();
        });
    }
}
