package com.GIU26B.KronoCash.repository;

import com.GIU26B.KronoCash.model.Habit;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface HabitRepository extends JpaRepository<Habit, Long> {

    List<Habit> findByUserIdOrderByIdDesc(Long userId);

    List<Habit> findByUserIdAndCompletedTodayTrue(Long userId);
}
