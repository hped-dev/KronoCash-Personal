package com.GIU26B.KronoCash.service;

import com.GIU26B.KronoCash.dto.SimulationRequestDTO;
import com.GIU26B.KronoCash.dto.SimulationResultDTO;
import com.GIU26B.KronoCash.repository.SimulationScenarioRepository;
import com.GIU26B.KronoCash.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(MockitoExtension.class)
class SimulatorServiceTest {

    @Mock private SimulationScenarioRepository scenarioRepository;
    @Mock private UserRepository userRepository;
    @InjectMocks private SimulatorService simulatorService;

    private SimulationRequestDTO request(String income) {
        return SimulationRequestDTO.builder()
                .scenarioName("Escenario")
                .monthlyIncome(new BigDecimal(income))
                .savingsPercentage(new BigDecimal("20"))
                .antExpensesCut(new BigDecimal("50"))
                .monthsHorizon(12)
                .build();
    }

    @Test
    void calculatesWithTheMaximumAllowedIncome() {
        SimulationResultDTO result = simulatorService.calculate(request("25000"));
        assertEquals(12, result.getBreakdown().size());
        assertEquals(0, new BigDecimal("60600.00").compareTo(result.getProjectedTotal()));
    }

    @Test
    void rejectsIncomeAboveMaximum() {
        assertThrows(IllegalArgumentException.class, () -> simulatorService.calculate(request("25001")));
    }

    @Test
    void rejectsNegativeExpenseCut() {
        SimulationRequestDTO invalid = request("1000");
        invalid.setAntExpensesCut(new BigDecimal("-1"));
        assertThrows(IllegalArgumentException.class, () -> simulatorService.calculate(invalid));
    }
}
