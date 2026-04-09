package ru.mcs.q.division;

/**
 * Правила силы связи между гранями тетраэдров.
 *
 * Два параметра настраиваются через UI:
 *   SAME_BOND  — сила связи одноимённых граней (RED↔RED, etc.)
 *   DIFF_BOND  — сила связи разноимённых граней (RED↔BLUE, etc.)
 *
 * TRANSPARENT (индекс 3) всегда даёт нулевую связь.
 *
 * faceSize обоих граней участвует в итоговой силе:
 *   effectiveBond = bondBase × sizeA × sizeB
 *
 * Физический смысл faceSize:
 *   если пространство сжато (faceSize → 0) — связь слабеет,
 *   волна через эту грань почти не проходит.
 */
public class BondStrength {

    // Индекс TRANSPARENT грани
    private static final int TRANSPARENT = 3;

    // Параметры — меняются через слайдеры в UI
    private float sameBond;   // сила одноимённой связи
    private float diffBond;   // сила разноимённой связи

    // Равновесное смещение для разноимённых граней.
    // Одноимённые: равновесие при текущей faceSize (нет толчка).
    // Разноимённые: равновесие смещено на этот множитель от faceSize.
    // 0.0 = нет смещения (как одноимённые), 1.0 = максимальный толчок.
    private float diffEquilibrium;

    public BondStrength(float sameBond, float diffBond, float diffEquilibrium) {
        this.sameBond        = sameBond;
        this.diffBond        = diffBond;
        this.diffEquilibrium = diffEquilibrium;
    }

    /** Значения по умолчанию для первого запуска */
    public static BondStrength defaults() {
        return new BondStrength(0.25f, 0.08f, 0.4f);
    }

    // ── Основной метод ──────────────────────────────────────────────────────

    /**
     * Возвращает эффективную силу связи между двумя гранями.
     *
     * @param faceA    индекс грани узла A (0=RED,1=BLUE,2=GREEN,3=TRANSPARENT)
     * @param faceB    индекс грани узла B
     * @param sizeA    faceSize грани A [0..1]
     * @param sizeB    faceSize грани B [0..1]
     * @return         сила связи [0..sameBond]
     */
    public float strength(int faceA, int faceB, float sizeA, float sizeB) {
        if (faceA == TRANSPARENT || faceB == TRANSPARENT) return 0f;
        float base = (faceA == faceB) ? sameBond : diffBond;
        // Метрический вес: оба конца грани участвуют
        return base * sizeA * sizeB;
    }

    /**
     * Равновесное значение phase-разницы для данной пары граней.
     *
     * Одноимённые → 0.0  (хотят быть в одной фазе, нет толчка)
     * Разноимённые → diffEquilibrium (хотят быть сдвинуты)
     *
     * Это смещение и создаёт постоянное "напряжение" в разноимённых связях,
     * которое при распространении по сети даёт колебание.
     */
    public float equilibrium(int faceA, int faceB) {
        if (faceA == TRANSPARENT || faceB == TRANSPARENT) return 0f;
        return (faceA == faceB) ? 0f : diffEquilibrium;
    }

    /**
     * Вклад одного соседа в Лапласиан узла A.
     *
     * Стандартный Лапласиан: (φ_B - φ_A)
     * С равновесным смещением: (φ_B - φ_A - equilibrium)
     * Умноженный на эффективную силу связи.
     *
     * Если φ_B - φ_A == equilibrium → вклад = 0, нет силы → покой.
     * Если отклонились → возникает возвращающая сила → колебание.
     */
    public double laplacianContrib(
            int faceA, int faceB,
            float sizeA, float sizeB,
            double phaseA, double phaseB) {

        float s = strength(faceA, faceB, sizeA, sizeB);
        if (s == 0f) return 0.0;
        float eq = equilibrium(faceA, faceB);
        return s * (phaseB - phaseA - eq);
    }

    // ── Getters / Setters для слайдеров ─────────────────────────────────────

    public float getSameBond()        { return sameBond; }
    public float getDiffBond()        { return diffBond; }
    public float getDiffEquilibrium() { return diffEquilibrium; }

    public void setSameBond(float v)        { this.sameBond = v; }
    public void setDiffBond(float v)        { this.diffBond = v; }
    public void setDiffEquilibrium(float v) { this.diffEquilibrium = v; }

    @Override
    public String toString() {
        return String.format("BondStrength[same=%.3f diff=%.3f eq=%.3f]",
                sameBond, diffBond, diffEquilibrium);
    }
}