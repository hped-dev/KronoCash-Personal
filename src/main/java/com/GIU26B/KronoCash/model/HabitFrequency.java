package com.GIU26B.KronoCash.model;

/**
 * Frecuencias soportadas para los hábitos (default DAILY en V2).
 * El valor interno (name()) se conserva en BD; la etiqueta es la que se
 * muestra en la interfaz, siempre en español.
 */
public enum HabitFrequency {
    DAILY("Diaria"),
    WEEKLY("Semanal"),
    MONTHLY("Mensual");

    private final String label;

    HabitFrequency(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
