package com.GIU26B.KronoCash.service;

import com.GIU26B.KronoCash.dto.SimulationRequestDTO;
import com.GIU26B.KronoCash.dto.SimulationResultDTO;
import com.GIU26B.KronoCash.model.SimulationScenario;
import com.GIU26B.KronoCash.model.User;
import com.GIU26B.KronoCash.repository.SimulationScenarioRepository;
import com.GIU26B.KronoCash.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

/**
 * Proyección patrimonial lineal:
 *   ahorro base mensual = ingreso mensual × porcentaje de ahorro / 100
 *   ahorro extra mensual = recorte de gastos hormiga
 *   total acumulado en el mes m = m × (base + extra)
 * No se aplica interés compuesto: el criterio académico es mostrar el efecto
 * de la constancia y del recorte de gastos.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class SimulatorService {

    private static final BigDecimal ONE_HUNDRED = BigDecimal.valueOf(100);

    private final SimulationScenarioRepository scenarioRepository;
    private final UserRepository userRepository;

    public SimulationResultDTO calculate(SimulationRequestDTO request) {
        validate(request);
        BigDecimal income = value(request.getMonthlyIncome());
        BigDecimal percentage = value(request.getSavingsPercentage());
        BigDecimal cut = value(request.getAntExpensesCut());
        int months = request.getMonthsHorizon() == null ? 12 : request.getMonthsHorizon();

        BigDecimal baseMonthly = income.multiply(percentage).divide(ONE_HUNDRED, 2, RoundingMode.HALF_UP);
        BigDecimal extraMonthly = scale(cut);

        List<SimulationResultDTO.MonthlyProjection> breakdown = new ArrayList<>(months);
        BigDecimal accumulatedBase = BigDecimal.ZERO;
        BigDecimal accumulatedExtra = BigDecimal.ZERO;

        for (int month = 1; month <= months; month++) {
            accumulatedBase = scale(accumulatedBase.add(baseMonthly));
            accumulatedExtra = scale(accumulatedExtra.add(extraMonthly));
            breakdown.add(SimulationResultDTO.MonthlyProjection.builder()
                    .month(month)
                    .accumulatedBaseSavings(accumulatedBase)
                    .accumulatedExtraSavings(accumulatedExtra)
                    .accumulatedTotal(scale(accumulatedBase.add(accumulatedExtra)))
                    .build());
        }

        return SimulationResultDTO.builder()
                .monthlyIncome(scale(income))
                .savingsPercentage(scale(percentage))
                .antExpensesCut(extraMonthly)
                .monthsHorizon(months)
                .baseMonthlySavings(baseMonthly)
                .extraMonthlySavings(extraMonthly)
                .totalBaseSavings(accumulatedBase)
                .totalExtraSavings(accumulatedExtra)
                .projectedTotal(scale(accumulatedBase.add(accumulatedExtra)))
                .breakdown(breakdown)
                .build();
    }

    /** Guarda el escenario ya calculado, asociado al usuario autenticado. */
    public SimulationScenario save(Long userId, SimulationRequestDTO request, SimulationResultDTO result) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado: " + userId));
        String name = request.getScenarioName() == null || request.getScenarioName().isBlank()
                ? "Escenario " + (scenarioRepository.countByUserId(userId) + 1)
                : request.getScenarioName();

        return scenarioRepository.save(SimulationScenario.builder()
                .user(user)
                .scenarioName(name)
                .monthlyIncome(scale(value(request.getMonthlyIncome())))
                .savingsPercentage(scale(value(request.getSavingsPercentage())))
                .antExpensesCut(scale(value(request.getAntExpensesCut())))
                .monthsHorizon(result.getMonthsHorizon())
                .projectedTotal(scale(result.getProjectedTotal()))
                .build());
    }

    /** Actualiza nombre y parámetros de un escenario propio. */
    public SimulationScenario update(Long userId, Long scenarioId, SimulationRequestDTO request) {
        validate(request);
        SimulationScenario scenario = findOwned(userId, scenarioId);
        SimulationResultDTO result = calculate(request);
        scenario.setScenarioName(request.getScenarioName());
        scenario.setMonthlyIncome(scale(value(request.getMonthlyIncome())));
        scenario.setSavingsPercentage(scale(value(request.getSavingsPercentage())));
        scenario.setAntExpensesCut(scale(value(request.getAntExpensesCut())));
        scenario.setMonthsHorizon(result.getMonthsHorizon());
        scenario.setProjectedTotal(scale(result.getProjectedTotal()));
        return scenarioRepository.save(scenario);
    }

    @Transactional(readOnly = true)
    public SimulationScenario findById(Long userId, Long scenarioId) {
        return findOwned(userId, scenarioId);
    }

    @Transactional(readOnly = true)
    public List<SimulationScenario> findByUser(Long userId) {
        return scenarioRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    public void delete(Long userId, Long scenarioId) {
        scenarioRepository.delete(findOwned(userId, scenarioId));
    }

    private SimulationScenario findOwned(Long userId, Long scenarioId) {
        return scenarioRepository.findById(scenarioId)
                .filter(candidate -> candidate.getUser().getId().equals(userId))
                .orElseThrow(() -> new IllegalArgumentException("Escenario no encontrado: " + scenarioId));
    }

    /** Defensa en profundidad: la UI y @Valid no son la única barrera. */
    private void validate(SimulationRequestDTO request) {
        BigDecimal income = value(request.getMonthlyIncome());
        BigDecimal percentage = value(request.getSavingsPercentage());
        BigDecimal cut = value(request.getAntExpensesCut());
        int months = request.getMonthsHorizon() == null ? 12 : request.getMonthsHorizon();
        if (income.signum() < 0 || income.compareTo(BigDecimal.valueOf(25000)) > 0) {
            throw new IllegalArgumentException("El ingreso mensual debe estar entre 0 y 25,000 MXN.");
        }
        if (percentage.signum() < 0 || percentage.compareTo(ONE_HUNDRED) > 0) {
            throw new IllegalArgumentException("El porcentaje de ahorro debe estar entre 0 y 100.");
        }
        if (cut.signum() < 0) {
            throw new IllegalArgumentException("El recorte de gastos no puede ser negativo.");
        }
        if (months < 1 || months > 120) {
            throw new IllegalArgumentException("El horizonte debe estar entre 1 y 120 meses.");
        }
        if (request.getScenarioName() != null && request.getScenarioName().length() > 100) {
            throw new IllegalArgumentException("El nombre del escenario no puede superar 100 caracteres.");
        }
    }

    private BigDecimal value(BigDecimal amount) {
        return amount == null ? BigDecimal.ZERO : amount;
    }

    private BigDecimal scale(BigDecimal amount) {
        return amount.setScale(2, RoundingMode.HALF_UP);
    }
}
