package com.GIU26B.KronoCash.model;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * Guarda el enum en minúsculas para cumplir el CHECK de PostgreSQL:
 * {@code theme_style IN ('retro', 'minimal', 'cyberpunk')}.
 */
@Converter
public class ThemeStyleConverter implements AttributeConverter<ThemeStyle, String> {

    @Override
    public String convertToDatabaseColumn(ThemeStyle attribute) {
        return attribute == null ? null : attribute.getValue();
    }

    @Override
    public ThemeStyle convertToEntityAttribute(String dbData) {
        return ThemeStyle.fromValue(dbData);
    }
}
