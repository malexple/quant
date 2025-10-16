package ru.mcs.q.field;

public enum FaceState {
    NEUTRAL("нейтральная"),      // Обычное состояние
    ATTRACTED("притягивается"),  // Притягивается к другой грани
    VIBRATING("вибрирует"),      // Вибрация - готовность к соединению
    REPELLED("отталкивается");   // Отталкивается

    private final String description;

    FaceState(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}