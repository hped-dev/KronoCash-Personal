package com.GIU26B.KronoCash.controller;

import com.GIU26B.KronoCash.dto.DashboardSummaryDTO;
import com.GIU26B.KronoCash.dto.HabitFormDTO;
import com.GIU26B.KronoCash.dto.SavingsGoalFormDTO;
import com.GIU26B.KronoCash.dto.TransactionFormDTO;
import com.GIU26B.KronoCash.dto.TransactionResponseDTO;
import com.GIU26B.KronoCash.model.Habit;
import com.GIU26B.KronoCash.model.Transaction;
import com.GIU26B.KronoCash.model.TransactionType;
import com.GIU26B.KronoCash.repository.UserRepository;
import com.GIU26B.KronoCash.service.FinanceService;
import com.GIU26B.KronoCash.service.HabitService;
import com.GIU26B.KronoCash.service.InsufficientBalanceException;
import com.GIU26B.KronoCash.service.SavingsGoalService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** Panel financiero: resumen, altas, metas, hábitos y filtros. */
@Controller
@RequiredArgsConstructor
public class DashboardController {

    private static final String DASHBOARD = "redirect:/dashboard";

    private final FinanceService financeService;
    private final HabitService habitService;
    private final SavingsGoalService savingsGoalService;
    private final UserRepository userRepository;

    @ModelAttribute("transactionForm")
    public TransactionFormDTO transactionForm() {
        return new TransactionFormDTO();
    }

    @ModelAttribute("goalForm")
    public SavingsGoalFormDTO goalForm() {
        return new SavingsGoalFormDTO();
    }

    @ModelAttribute("habitForm")
    public HabitFormDTO habitForm() {
        return new HabitFormDTO();
    }

    @GetMapping("/dashboard")
    public String dashboard(@RequestParam(required = false) TransactionType type,
                            @RequestParam(required = false) String category,
                            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
                            Authentication authentication,
                            Model model) {
        Long userId = currentUserId(authentication);
        populate(userId, model, type, category, from, to);
        return "public/dashboard";
    }

    @PostMapping("/dashboard/transactions")
    public String addTransaction(@Valid @ModelAttribute("transactionForm") TransactionFormDTO form,
                                 BindingResult bindingResult,
                                 Authentication authentication,
                                 Model model) {
        Long userId = currentUserId(authentication);
        if (bindingResult.hasErrors()) {
            populate(userId, model, null, null, null, null);
            return "public/dashboard";
        }
        try {
            financeService.addTransaction(userId, form);
        } catch (InsufficientBalanceException ex) {
            model.addAttribute("balanceError", ex.getMessage());
            populate(userId, model, null, null, null, null);
            return "public/dashboard";
        }
        return DASHBOARD + "?created#seccion-movimientos";
    }

    @PostMapping("/dashboard/transactions/{id}/delete")
    public String deleteTransaction(@PathVariable Long id, Authentication authentication) {
        try {
            financeService.deleteTransaction(currentUserId(authentication), id);
        } catch (IllegalArgumentException ex) {
            return DASHBOARD + "?error=delete#seccion-movimientos";
        }
        return DASHBOARD + "?deleted#seccion-movimientos";
    }

    @PostMapping("/dashboard/goals")
    public String addGoal(@Valid @ModelAttribute("goalForm") SavingsGoalFormDTO form,
                          BindingResult bindingResult,
                          Authentication authentication,
                          Model model) {
        Long userId = currentUserId(authentication);
        if (bindingResult.hasErrors()) {
            populate(userId, model, null, null, null, null);
            return "public/dashboard";
        }
        financeService.addGoal(userId, form);
        return DASHBOARD + "?goalCreated#seccion-metas";
    }

    @PostMapping("/dashboard/goals/{id}/deposit")
    public String depositGoal(@PathVariable Long id,
                              @RequestParam BigDecimal amount,
                              Authentication authentication) {
        try {
            savingsGoalService.deposit(currentUserId(authentication), id, amount);
        } catch (IllegalArgumentException ex) {
            return DASHBOARD + "?error=" + encode(ex.getMessage()) + "#seccion-metas";
        }
        return DASHBOARD + "?goalUpdated#seccion-metas";
    }

    @PostMapping("/dashboard/goals/{id}/withdraw")
    public String withdrawGoal(@PathVariable Long id,
                               @RequestParam BigDecimal amount,
                               Authentication authentication) {
        try {
            savingsGoalService.withdraw(currentUserId(authentication), id, amount);
        } catch (IllegalArgumentException ex) {
            return DASHBOARD + "?error=" + encode(ex.getMessage()) + "#seccion-metas";
        }
        return DASHBOARD + "?goalUpdated#seccion-metas";
    }

    @PostMapping("/dashboard/goals/{id}/delete")
    public String deleteGoal(@PathVariable Long id, Authentication authentication) {
        try {
            savingsGoalService.delete(currentUserId(authentication), id);
        } catch (IllegalArgumentException ex) {
            return DASHBOARD + "?error=delete#seccion-metas";
        }
        return DASHBOARD + "?goalDeleted#seccion-metas";
    }

    @PostMapping("/dashboard/habits")
    public String addHabit(@Valid @ModelAttribute("habitForm") HabitFormDTO form,
                           BindingResult bindingResult,
                           Authentication authentication,
                           Model model) {
        Long userId = currentUserId(authentication);
        if (bindingResult.hasErrors()) {
            populate(userId, model, null, null, null, null);
            return "public/dashboard";
        }
        habitService.addHabit(userId, form);
        return DASHBOARD + "?habitCreated#seccion-habitos";
    }

    @PostMapping("/dashboard/habits/{id}/complete")
    public String completeHabit(@PathVariable Long id, Authentication authentication) {
        try {
            habitService.completeHabit(currentUserId(authentication), id);
        } catch (IllegalArgumentException ex) {
            return DASHBOARD + "?error=habit#seccion-habitos";
        }
        return DASHBOARD + "?habitDone#seccion-habitos";
    }

    @PostMapping("/dashboard/habits/{id}/undo")
    public String undoHabit(@PathVariable Long id, Authentication authentication) {
        try {
            habitService.undoCompletion(currentUserId(authentication), id);
        } catch (IllegalArgumentException ex) {
            return DASHBOARD + "?error=habit#seccion-habitos";
        }
        return DASHBOARD + "?habitUndone#seccion-habitos";
    }

    @GetMapping("/api/dashboard/transactions")
    @ResponseBody
    public List<TransactionResponseDTO> filterTransactions(
            @RequestParam(required = false) TransactionType type,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            Authentication authentication) {
        return financeService.searchTransactions(currentUserId(authentication), type, category, from, to)
                .stream()
                .map(TransactionResponseDTO::from)
                .toList();
    }

    @GetMapping("/api/dashboard/summary")
    @ResponseBody
    public DashboardSummaryDTO summary(Authentication authentication) {
        return financeService.getSummary(currentUserId(authentication));
    }

    private void populate(Long userId, Model model, TransactionType type, String category,
                          LocalDate from, LocalDate to) {
        model.addAttribute("summary", financeService.getSummary(userId));
        List<Transaction> transactions = financeService.searchTransactions(userId, type, category, from, to);
        model.addAttribute("transactions", transactions);
        model.addAttribute("habits", habitService.findByUser(userId));
        model.addAttribute("type", type);
        model.addAttribute("category", category);
        model.addAttribute("from", from);
        model.addAttribute("to", to);
    }

    private Long currentUserId(Authentication authentication) {
        return userRepository.findByUsername(authentication.getName())
                .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado"))
                .getId();
    }

    private String encode(String message) {
        return java.net.URLEncoder.encode(message, java.nio.charset.StandardCharsets.UTF_8);
    }
}
