package com.GIU26B.KronoCash.repository;

import com.GIU26B.KronoCash.model.SavingsGoal;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface SavingsGoalRepository extends JpaRepository<SavingsGoal, Long> {

    List<SavingsGoal> findByUserIdOrderByIdDesc(Long userId);

    List<SavingsGoal> findByUserIdAndDeadlineBeforeOrderByDeadlineAsc(Long userId, LocalDate date);
}
