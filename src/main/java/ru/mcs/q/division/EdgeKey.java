package ru.mcs.q.division;

import java.util.Objects;

public record EdgeKey(int v1, int v2) {
    // v1 всегда < v2 — порядок не важен
    public static EdgeKey of(int a, int b) {
        return new EdgeKey(Math.min(a, b), Math.max(a, b));
    }
}