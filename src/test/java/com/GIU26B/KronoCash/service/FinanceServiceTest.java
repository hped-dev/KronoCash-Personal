package com.GIU26B.KronoCash.service;

import com.GIU26B.KronoCash.dto.TransactionFormDTO;
import com.GIU26B.KronoCash.model.Transaction;
import com.GIU26B.KronoCash.model.TransactionType;
import com.GIU26B.KronoCash.model.User;
import com.GIU26B.KronoCash.repository.SavingsGoalRepository;
import com.GIU26B.KronoCash.repository.TransactionRepository;
import com.GIU26B.KronoCash.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FinanceServiceTest {

    @Mock private TransactionRepository transactionRepository;
    @Mock private SavingsGoalRepository savingsGoalRepository;
    @Mock private UserRepository userRepository;
    @Mock private SavingsGoalService savingsGoalService;
    @InjectMocks private FinanceService financeService;

    private final User user = User.builder().id(1L).username("u").build();

    @Test
    void expenseIsRejectedWhenMoneyIsReservedInGoals() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(savingsGoalService.availableBalance(1L)).thenReturn(BigDecimal.ZERO);

        TransactionFormDTO form = TransactionFormDTO.builder()
                .type(TransactionType.EXPENSE)
                .category("Comida")
                .amount(new BigDecimal("100.00"))
                .transactionDate(LocalDate.now())
                .build();

        InsufficientBalanceException ex = assertThrows(InsufficientBalanceException.class,
                () -> financeService.addTransaction(1L, form));
        assertEquals(BigDecimal.ZERO.compareTo(ex.getAvailableBalance()), 0);
        verify(transactionRepository, never()).save(any());
    }

    @Test
    void futureDatedTransactionIsRejected() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        TransactionFormDTO form = TransactionFormDTO.builder()
                .type(TransactionType.INCOME)
                .category("Sueldo")
                .amount(BigDecimal.TEN)
                .transactionDate(LocalDate.now().plusDays(1))
                .build();

        assertThrows(IllegalArgumentException.class, () -> financeService.addTransaction(1L, form));
        verify(transactionRepository, never()).save(any());
    }

    @Test
    void deletingIncomeThatWouldLeaveNegativeAvailableBalanceIsRejected() {
        Transaction income = Transaction.builder()
                .id(5L).user(user).type(TransactionType.INCOME)
                .amount(new BigDecimal("100.00")).transactionDate(LocalDate.now()).build();
        when(transactionRepository.findById(5L)).thenReturn(Optional.of(income));
        when(savingsGoalService.availableBalance(1L)).thenReturn(new BigDecimal("50.00"));

        assertThrows(IllegalArgumentException.class, () -> financeService.deleteTransaction(1L, 5L));
        verify(transactionRepository, never()).delete(any(Transaction.class));
    }
}
