package com.GIU26B.KronoCash.service;

import com.GIU26B.KronoCash.model.SavingsGoal;
import com.GIU26B.KronoCash.model.TransactionType;
import com.GIU26B.KronoCash.repository.SavingsGoalRepository;
import com.GIU26B.KronoCash.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

/**
 * Gestión integral de metas de ahorro. Apartar dinero en una meta NO crea un
 * egreso ni altera los totales mensuales: solo mueve saldo desde el disponible.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class SavingsGoalService {

    private final SavingsGoalRepository savingsGoalRepository;
    private final TransactionRepository transactionRepository;

    /** Balance total = ingresos - egresos (sin contar lo apartado en metas). */
    @Transactional(readOnly = true)
    public BigDecimal totalBalance(Long userId) {
        BigDecimal income = value(transactionRepository.sumByUserAndType(userId, TransactionType.INCOME));
        BigDecimal expense = value(transactionRepository.sumByUserAndType(userId, TransactionType.EXPENSE));
        return income.subtract(expense);
    }

    /** Suma de los importes acumulados en todas las metas del usuario. */
    @Transactional(readOnly = true)
    public BigDecimal totalSavedInGoals(Long userId) {
        return savingsGoalRepository.findByUserIdOrderByIdDesc(userId).stream()
                .map(goal -> value(goal.getCurrentAmount()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /** Dinero libre para gastar o apartar. */
    @Transactional(readOnly = true)
    public BigDecimal availableBalance(Long userId) {
        return totalBalance(userId).subtract(totalSavedInGoals(userId));
    }

    public SavingsGoal deposit(Long userId, Long goalId, BigDecimal amount) {
        BigDecimal normalized = scale(amount);
        if (normalized.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("El importe a añadir debe ser mayor que cero.");
        }
        SavingsGoal goal = findOwned(userId, goalId);
        BigDecimal available = availableBalance(userId);
        if (normalized.compareTo(available) > 0) {
            throw new IllegalArgumentException("Saldo disponible insuficiente para apartar " + normalized
                    + ". El saldo disponible es " + available + ".");
        }
        goal.setCurrentAmount(value(goal.getCurrentAmount()).add(normalized));
        return savingsGoalRepository.save(goal);
    }

    public SavingsGoal withdraw(Long userId, Long goalId, BigDecimal amount) {
        BigDecimal normalized = scale(amount);
        if (normalized.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("El importe a retirar debe ser mayor que cero.");
        }
        SavingsGoal goal = findOwned(userId, goalId);
        BigDecimal current = value(goal.getCurrentAmount());
        if (normalized.compareTo(current) > 0) {
            throw new IllegalArgumentException("No puedes retirar " + normalized
                    + " porque la meta solo tiene acumulados " + current + ".");
        }
        goal.setCurrentAmount(current.subtract(normalized));
        return savingsGoalRepository.save(goal);
    }

    public void delete(Long userId, Long goalId) {
        savingsGoalRepository.delete(findOwned(userId, goalId));
    }

    private SavingsGoal findOwned(Long userId, Long goalId) {
        return savingsGoalRepository.findById(goalId)
                .filter(candidate -> candidate.getUser().getId().equals(userId))
                .orElseThrow(() -> new IllegalArgumentException("Meta no encontrada: " + goalId));
    }

    private BigDecimal value(BigDecimal amount) {
        return amount == null ? BigDecimal.ZERO : amount;
    }

    private BigDecimal scale(BigDecimal amount) {
        return value(amount).setScale(2, java.math.RoundingMode.HALF_UP);
    }
}
