package com.GIU26B.KronoCash.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Formulario de preferencias visuales y de accesibilidad. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PreferencesRequest {

    @NotNull(message = "Seleccione un estilo visual.")
    @Pattern(regexp = "^(retro|minimal|cyberpunk)$", message = "Seleccione un tema válido.")
    @Builder.Default
    private String themeStyle = "minimal";

    @Builder.Default
    private boolean darkMode = false;

    @Builder.Default
    private boolean reduceMotion = false;

    @NotNull(message = "Indique la escala de fuente.")
    @Min(value = 100, message = "La escala mínima es 100%.")
    @Max(value = 200, message = "La escala máxima es 200%.")
    @Builder.Default
    private Integer fontScale = 100;
}
