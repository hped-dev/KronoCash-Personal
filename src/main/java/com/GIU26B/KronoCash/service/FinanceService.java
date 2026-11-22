package com.GIU26B.KronoCash.service;

import com.GIU26B.KronoCash.dto.DashboardSummaryDTO;
import com.GIU26B.KronoCash.dto.SavingsGoalFormDTO;
import com.GIU26B.KronoCash.dto.TransactionFormDTO;
import com.GIU26B.KronoCash.model.SavingsGoal;
import com.GIU26B.KronoCash.model.Transaction;
import com.GIU26B.KronoCash.model.TransactionType;
import com.GIU26B.KronoCash.model.User;
import com.GIU26B.KronoCash.repository.SavingsGoalRepository;
import com.GIU26B.KronoCash.repository.TransactionRepository;
import com.GIU26B.KronoCash.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.criteria.Predicate;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Cálculos financieros del panel: balance, totales mensuales y avance de metas. */
@Service
@RequiredArgsConstructor
@Transactional
public class FinanceService {

    private static final BigDecimal ONE_HUNDRED = BigDecimal.valueOf(100);

    private final TransactionRepository transactionRepository;
    private final SavingsGoalRepository savingsGoalRepository;
    private final UserRepository userRepository;
    private final SavingsGoalService savingsGoalService;

    @Transactional(readOnly = true)
    public DashboardSummaryDTO getSummary(Long userId) {
        YearMonth currentMonth = YearMonth.now();
        LocalDate from = currentMonth.atDay(1);
        LocalDate to = currentMonth.atEndOfMonth();

        BigDecimal monthlyIncome = value(transactionRepository.sumByUserTypeAndPeriod(
                userId, TransactionType.INCOME, from, to));
        BigDecimal monthlyExpense = value(transactionRepository.sumByUserTypeAndPeriod(
                userId, TransactionType.EXPENSE, from, to));
        BigDecimal totalIncome = value(transactionRepository.sumByUserAndType(userId, TransactionType.INCOME));
        BigDecimal totalExpense = value(transactionRepository.sumByUserAndType(userId, TransactionType.EXPENSE));

        List<DashboardSummaryDTO.GoalProgress> goals = savingsGoalRepository.findByUserIdOrderByIdDesc(userId)
                .stream()
                .map(this::toProgress)
                .toList();

        BigDecimal totalBalance = totalIncome.subtract(totalExpense);
        BigDecimal totalSaved = savingsGoalService.totalSavedInGoals(userId);

        return DashboardSummaryDTO.builder()
                .totalBalance(totalBalance)
                .totalSavedInGoals(totalSaved)
                .availableBalance(totalBalance.subtract(totalSaved))
                .monthlyIncome(monthlyIncome)
                .monthlyExpense(monthlyExpense)
                .monthlyNet(monthlyIncome.subtract(monthlyExpense))
                .transactionCount(transactionRepository.countByUserId(userId))
                .periodStart(from)
                .periodEnd(to)
                .goals(goals)
                .build();
    }

    @Transactional(readOnly = true)
    public List<Transaction> searchTransactions(Long userId, TransactionType type, String category,
                                                 LocalDate from, LocalDate to) {
        Specification<Transaction> specification = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.<Long>get("user").get("id"), userId));
            if (type != null) {
                predicates.add(cb.equal(root.get("type"), type));
            }
            if (category != null && !category.isBlank()) {
                String escaped = category.trim().toLowerCase(Locale.ROOT)
                        .replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
                predicates.add(cb.like(cb.lower(root.<String>get("category")),
                        "%" + escaped + "%", '\\'));
            }
            if (from != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.<LocalDate>get("transactionDate"), from));
            }
            if (to != null) {
                predicates.add(cb.lessThanOrEqualTo(root.<LocalDate>get("transactionDate"), to));
            }
            return cb.and(predicates.toArray(Predicate[]::new));
        };
        return transactionRepository.findAll(specification,
                Sort.by(Sort.Direction.DESC, "transactionDate").and(Sort.by(Sort.Direction.DESC, "id")));
    }

    public Transaction addTransaction(Long userId, TransactionFormDTO form) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado: " + userId));

        if (form.getType() == TransactionType.EXPENSE) {
            // El dinero apartado en metas no está disponible para gastar.
            BigDecimal availableBalance = savingsGoalService.availableBalance(userId);
            BigDecimal amount = scale(form.getAmount());
            if (amount.compareTo(availableBalance) > 0) {
                throw new InsufficientBalanceException(availableBalance, amount);
            }
        }

        if (form.getTransactionDate() != null && form.getTransactionDate().isAfter(LocalDate.now())) {
            throw new IllegalArgumentException("La fecha del movimiento no puede ser futura.");
        }

        return transactionRepository.save(Transaction.builder()
                .user(user)
                .type(form.getType())
                .category(form.getCategory())
                .amount(scale(form.getAmount()))
                .description(form.getDescription())
                .transactionDate(form.getTransactionDate())
                .build());
    }

    /** Metas con importe restante por alcanzar, para vincularlas al simulador. */
    @Transactional(readOnly = true)
    public List<SavingsGoal> findActiveGoals(Long userId) {
        return savingsGoalRepository.findByUserIdOrderByIdDesc(userId).stream()
                .filter(goal -> value(goal.getTargetAmount())
                        .subtract(value(goal.getCurrentAmount())).compareTo(BigDecimal.ZERO) > 0)
                .toList();
    }

    public SavingsGoal addGoal(Long userId, SavingsGoalFormDTO form) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado: " + userId));
        return savingsGoalRepository.save(SavingsGoal.builder()
                .user(user)
                .title(form.getTitle())
                .targetAmount(form.getTargetAmount())
                .currentAmount(BigDecimal.ZERO)
                .deadline(form.getDeadline())
                .build());
    }

    /** Elimina un movimiento solo si pertenece al usuario autenticado. */
    public void deleteTransaction(Long userId, Long transactionId) {
        Transaction transaction = transactionRepository.findById(transactionId)
                .filter(candidate -> candidate.getUser().getId().equals(userId))
                .orElseThrow(() -> new IllegalArgumentException("Movimiento no encontrado: " + transactionId));
        if (transaction.getType() == TransactionType.INCOME) {
            BigDecimal afterDeletion = savingsGoalService.availableBalance(userId)
                    .subtract(value(transaction.getAmount()));
            if (afterDeletion.compareTo(BigDecimal.ZERO) < 0) {
                throw new IllegalArgumentException("No se puede eliminar este ingreso: dejaría saldo disponible negativo.");
            }
        }
        transactionRepository.delete(transaction);
    }

    private BigDecimal scale(BigDecimal amount) {
        return value(amount).setScale(2, RoundingMode.HALF_UP);
    }

    private DashboardSummaryDTO.GoalProgress toProgress(SavingsGoal goal) {
        BigDecimal target = value(goal.getTargetAmount());
        BigDecimal current = value(goal.getCurrentAmount());
        BigDecimal percent = target.compareTo(BigDecimal.ZERO) == 0
                ? BigDecimal.ZERO
                : current.multiply(ONE_HUNDRED).divide(target, 2, RoundingMode.HALF_UP);
        return DashboardSummaryDTO.GoalProgress.builder()
                .id(goal.getId())
                .title(goal.getTitle())
                .targetAmount(target)
                .currentAmount(current)
                .remainingAmount(target.subtract(current).max(BigDecimal.ZERO))
                .progressPercent(percent)
                .build();
    }

    private BigDecimal value(BigDecimal amount) {
        return amount == null ? BigDecimal.ZERO : amount;
    }
}
