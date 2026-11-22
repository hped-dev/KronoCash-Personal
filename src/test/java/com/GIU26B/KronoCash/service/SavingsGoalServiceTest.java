package com.GIU26B.KronoCash.service;

import com.GIU26B.KronoCash.model.SavingsGoal;
import com.GIU26B.KronoCash.model.TransactionType;
import com.GIU26B.KronoCash.model.User;
import com.GIU26B.KronoCash.repository.SavingsGoalRepository;
import com.GIU26B.KronoCash.repository.TransactionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SavingsGoalServiceTest {

    @Mock private SavingsGoalRepository savingsGoalRepository;
    @Mock private TransactionRepository transactionRepository;
    @InjectMocks private SavingsGoalService savingsGoalService;

    private final User user = User.builder().id(1L).username("u").build();

    @Test
    void depositCannotExceedAvailableBalance() {
        SavingsGoal goal = SavingsGoal.builder()
                .id(2L).user(user).title("Viaje")
                .targetAmount(new BigDecimal("500.00")).currentAmount(BigDecimal.ZERO).build();
        when(savingsGoalRepository.findById(2L)).thenReturn(Optional.of(goal));
        when(transactionRepository.sumByUserAndType(1L, TransactionType.INCOME))
                .thenReturn(new BigDecimal("100.00"));
        when(transactionRepository.sumByUserAndType(1L, TransactionType.EXPENSE))
                .thenReturn(BigDecimal.ZERO);

        assertThrows(IllegalArgumentException.class,
                () -> savingsGoalService.deposit(1L, 2L, new BigDecimal("101.00")));
        verify(savingsGoalRepository, never()).save(any());
    }

    @Test
    void withdrawCannotExceedGoalBalance() {
        SavingsGoal goal = SavingsGoal.builder()
                .id(2L).user(user).title("Viaje")
                .targetAmount(new BigDecimal("500.00")).currentAmount(new BigDecimal("30.00")).build();
        when(savingsGoalRepository.findById(2L)).thenReturn(Optional.of(goal));

        assertThrows(IllegalArgumentException.class,
                () -> savingsGoalService.withdraw(1L, 2L, new BigDecimal("31.00")));
        verify(savingsGoalRepository, never()).save(any());
    }

    @Test
    void cannotAccessAnotherUsersGoal() {
        User other = User.builder().id(99L).username("otro").build();
        SavingsGoal goal = SavingsGoal.builder()
                .id(2L).user(other).title("Ajena")
                .targetAmount(BigDecimal.TEN).currentAmount(BigDecimal.ZERO).build();
        when(savingsGoalRepository.findById(2L)).thenReturn(Optional.of(goal));

        assertThrows(IllegalArgumentException.class,
                () -> savingsGoalService.withdraw(1L, 2L, BigDecimal.ONE));
    }

    @Test
    void validDepositUpdatesCurrentAmountAndAvailableBalance() {
        SavingsGoal goal = SavingsGoal.builder()
                .id(2L).user(user).title("Viaje")
                .targetAmount(new BigDecimal("500.00")).currentAmount(BigDecimal.ZERO).build();
        when(savingsGoalRepository.findById(2L)).thenReturn(Optional.of(goal));
        when(transactionRepository.sumByUserAndType(1L, TransactionType.INCOME))
                .thenReturn(new BigDecimal("1000.00"));
        when(transactionRepository.sumByUserAndType(1L, TransactionType.EXPENSE))
                .thenReturn(BigDecimal.ZERO);
        when(savingsGoalRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        SavingsGoal saved = savingsGoalService.deposit(1L, 2L, new BigDecimal("250.00"));
        assertEquals(0, new BigDecimal("250.00").compareTo(saved.getCurrentAmount()));
        when(savingsGoalRepository.findByUserIdOrderByIdDesc(1L)).thenReturn(List.of(saved));
        assertEquals(0, new BigDecimal("750.00").compareTo(savingsGoalService.availableBalance(1L)));
    }
}
