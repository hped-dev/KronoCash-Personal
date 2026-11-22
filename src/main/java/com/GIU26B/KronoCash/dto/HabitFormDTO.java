package com.GIU26B.KronoCash.dto;

import com.GIU26B.KronoCash.model.HabitFrequency;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Formulario de alta de hábitos. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HabitFormDTO {

    @NotBlank(message = "El título del hábito es obligatorio.")
    @Size(max = 100, message = "El título no puede superar los 100 caracteres.")
    private String title;

    @NotNull(message = "Seleccione la frecuencia.")
    @Builder.Default
    private HabitFrequency frequency = HabitFrequency.DAILY;

    public void setTitle(String title) {
        this.title = title == null ? null : title.trim();
    }
}
