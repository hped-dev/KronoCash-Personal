package com.GIU26B.KronoCash.model;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

/** Mapeo de la tabla {@code user_preferences} (V1__users_and_preferences.sql). */
@Entity
@Table(name = "user_preferences")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(exclude = "user")
@ToString(exclude = "user")
public class UserPreferences {

    /** PK y FK compartida con users.id gracias a @MapsId. */
    @Id
    @Column(name = "user_id")
    private Long userId;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "user_id")
    private User user;

    @Convert(converter = ThemeStyleConverter.class)
    @Column(name = "theme_style", nullable = false, length = 20)
    @Builder.Default
    private ThemeStyle themeStyle = ThemeStyle.MINIMAL;

    @Column(name = "dark_mode", nullable = false)
    @Builder.Default
    private boolean darkMode = false;

    @Column(name = "reduce_motion", nullable = false)
    @Builder.Default
    private boolean reduceMotion = false;

    @Column(name = "font_scale", nullable = false)
    @Builder.Default
    private int fontScale = 100;

    /** Valores por defecto exigidos por la migración V1. */
    public static UserPreferences defaultFor(User user) {
        return UserPreferences.builder()
                .user(user)
                .themeStyle(ThemeStyle.MINIMAL)
                .darkMode(false)
                .reduceMotion(false)
                .fontScale(100)
                .build();
    }
}
