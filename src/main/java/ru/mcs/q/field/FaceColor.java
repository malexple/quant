package ru.mcs.q.field;

public enum FaceColor {
    RED("🔴"),      // Красный
    BLUE("🔵"),     // Синий
    GREEN("🟢"),    // Зеленый
    TRANSPARENT("⚪"); // Прозрачный

    private final String emoji;

    FaceColor(String emoji) {
        this.emoji = emoji;
    }

    public String getEmoji() {
        return emoji;
    }
}