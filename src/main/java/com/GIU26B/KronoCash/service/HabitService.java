package com.GIU26B.KronoCash.service;

import com.GIU26B.KronoCash.dto.HabitFormDTO;
import com.GIU26B.KronoCash.model.Habit;
import com.GIU26B.KronoCash.model.User;
import com.GIU26B.KronoCash.repository.HabitRepository;
import com.GIU26B.KronoCash.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Gestión de hábitos y rachas.
 *
 * La migración V2 dispone de completed_today y streak_count: completar un
 * hábito es idempotente dentro del día (la bandera evita contar dos veces) y
 * incrementa la racha en 1. Al desmarcarlo se descuenta. Un proceso diario
 * debe llamar a resetDailyFlags para preparar el día siguiente.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class HabitService {

    private final HabitRepository habitRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public List<Habit> findByUser(Long userId) {
        return habitRepository.findByUserIdOrderByIdDesc(userId);
    }

    public Habit addHabit(Long userId, HabitFormDTO form) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado: " + userId));
        return habitRepository.save(Habit.builder()
                .user(user)
                .title(form.getTitle())
                .frequency(form.getFrequency())
                .completedToday(false)
                .streakCount(0)
                .build());
    }

    /** Marca el hábito como completado hoy y suma 1 a la racha (solo una vez al día). */
    public Habit completeHabit(Long userId, Long habitId) {
        Habit habit = findOwned(userId, habitId);
        if (!habit.isCompletedToday()) {
            habit.setCompletedToday(true);
            habit.setStreakCount(habit.getStreakCount() + 1);
        }
        return habitRepository.save(habit);
    }

    /** Desmarca el hábito de hoy y descuenta la racha, sin bajar de cero. */
    public Habit undoCompletion(Long userId, Long habitId) {
        Habit habit = findOwned(userId, habitId);
        if (habit.isCompletedToday()) {
            habit.setCompletedToday(false);
            habit.setStreakCount(Math.max(0, habit.getStreakCount() - 1));
        }
        return habitRepository.save(habit);
    }

    /** Prepara todos los hábitos del usuario para un nuevo día. */
    public void resetDailyFlags(Long userId) {
        habitRepository.findByUserIdAndCompletedTodayTrue(userId)
                .forEach(habit -> habit.setCompletedToday(false));
    }

    /** Reinicio diario automático a medianoche (hora de México). */
    @Scheduled(cron = "0 0 0 * * *", zone = "America/Mexico_City")
    @Transactional
    public void resetAllDailyFlags() {
        userRepository.findAll().forEach(user -> resetDailyFlags(user.getId()));
    }

    private Habit findOwned(Long userId, Long habitId) {
        return habitRepository.findById(habitId)
                .filter(habit -> habit.getUser().getId().equals(userId))
                .orElseThrow(() -> new IllegalArgumentException("Hábito no encontrado: " + habitId));
    }
}
