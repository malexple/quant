package ru.mcs.q.division;

import javax.swing.*;
import java.awt.*;

/**
 * Точка запуска тетраэдральной вселенной.
 *
 * Запуск через IntelliJ: правая кнопка → Run 'DivisionMain.main()'
 * Запуск через Gradle:   ./gradlew run  (добавь mainClass в build.gradle)
 *
 * Два сценария на выбор — переключай MODE:
 *   BIG_BANG — вся энергия с рождения, расширение = разбавление
 *   GROWTH   — вакуум, возбуждай мышью или кнопкой ⚡
 */
public class DivisionMain {

    // ── Параметры вселенной ───────────────────────────────────────────────────

    private static final int   W    = 60;               // ширина тора
    private static final int   H    = 60;               // высота тора
    private static final TetraUniverse.Mode MODE =
            TetraUniverse.Mode.GROWTH;                  // BIG_BANG или GROWTH

    // ── Начальный сценарий ────────────────────────────────────────────────────

    /**
     * Что возбуждаем при старте.
     * Вызывается ПОСЛЕ создания вселенной, ДО показа окна.
     */
    private static void applyScenario(TetraUniverse u) {
        switch (MODE) {

            case BIG_BANG -> {
                // Один мощный импульс в центре — взрыв
                u.exciteCenter(600.0);
                // Сразу включаем динамику
                u.setDynamicsEnabled(true);
            }

            case GROWTH -> {
                // Вакуум — два источника для интерференции (как опыт Юнга)
                // Можно закомментировать и возбуждать мышью вручную
                u.excite(W / 2 - 10, H / 2, 600.0);
                u.excite(W / 2 + 10, H / 2, 600.0);
                // Динамика выключена — нажми ▶ сам
            }
        }
    }

    // ── main ──────────────────────────────────────────────────────────────────

    public static void main(String[] args) {

        // Правила связей — значения по умолчанию (крутятся слайдерами)
        BondStrength bs = BondStrength.defaults();

        // Создаём вселенную
        TetraUniverse universe = new TetraUniverse(W, H, MODE, bs);

        // Применяем начальный сценарий
        applyScenario(universe);

        // Swing в EDT
        SwingUtilities.invokeLater(() -> {
            JFrame frame = buildFrame(universe);
            frame.setVisible(true);
        });
    }

    // ── Сборка окна ───────────────────────────────────────────────────────────

    private static JFrame buildFrame(TetraUniverse universe) {
        JFrame frame = new JFrame(
                "Tetra Universe  [" + universe.getMode() + "]  "
                        + universe.getW() + "×" + universe.getH()
                        + "  —  ru.mcs.q.division"
        );

        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        // Тёмный фон чтобы не мелькало белое при ресайзе
        frame.getContentPane().setBackground(new Color(13, 12, 11));

        DivisionPanel panel = new DivisionPanel(universe);
        frame.setContentPane(panel);

        frame.pack();
        frame.setLocationRelativeTo(null);  // по центру экрана

        // Горячие клавиши
        bindKeys(frame, universe, panel);

        return frame;
    }

    // ── Горячие клавиши ───────────────────────────────────────────────────────

    private static void bindKeys(JFrame frame,
                                 TetraUniverse universe,
                                 DivisionPanel panel) {

        KeyboardFocusManager.getCurrentKeyboardFocusManager()
                .addKeyEventDispatcher(e -> {
                    if (e.getID() != java.awt.event.KeyEvent.KEY_PRESSED) return false;

                    switch (e.getKeyCode()) {

                        // Пробел — пауза / старт
                        case java.awt.event.KeyEvent.VK_SPACE -> {
                            universe.setDynamicsEnabled(!universe.isDynamicsEnabled());
                            frame.setTitle(
                                    "Tetra Universe  [" + universe.getMode() + "]  "
                                            + (universe.isDynamicsEnabled() ? "▶" : "⏸")
                            );
                        }

                        // R — полный сброс
                        case java.awt.event.KeyEvent.VK_R -> {
                            universe.reset();
                            applyScenario(universe);
                            frame.setTitle(
                                    "Tetra Universe  [" + universe.getMode() + "]"
                            );
                        }

                        // E — возбудить центр
                        case java.awt.event.KeyEvent.VK_E ->
                                universe.exciteCenter(600.0);

                        // D — одиночный шаг (debug)
                        case java.awt.event.KeyEvent.VK_D -> {
                            universe.setDynamicsEnabled(false);
                            universe.step();
                        }

                        default -> { return false; }
                    }
                    return true;
                });
    }
}