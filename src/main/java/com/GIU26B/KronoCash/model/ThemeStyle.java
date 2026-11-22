package com.GIU26B.KronoCash.model;

/**
 * Temas visuales disponibles. El valor {@code value} coincide exactamente con
 * los VALUES permitidos por el CHECK de la columna {@code theme_style} y con
 * los valores del atributo {@code data-theme} (WCAG: mismo valor en HTML y BD).
 */
public enum ThemeStyle {

    RETRO("retro"),
    MINIMAL("minimal"),
    CYBERPUNK("cyberpunk");

    private final String value;

    ThemeStyle(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    /** Resuelve un valor proveniente de la BD o de un formulario. */
    public static ThemeStyle fromValue(String value) {
        if (value == null) {
            return MINIMAL;
        }
        for (ThemeStyle style : values()) {
            if (style.value.equalsIgnoreCase(value.trim())) {
                return style;
            }
        }
        throw new IllegalArgumentException("Estilo de tema no válido: " + value);
    }
}
