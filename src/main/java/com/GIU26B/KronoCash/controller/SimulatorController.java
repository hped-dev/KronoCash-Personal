package com.GIU26B.KronoCash.controller;

import com.GIU26B.KronoCash.dto.SimulationRequestDTO;
import com.GIU26B.KronoCash.dto.SimulationResultDTO;
import com.GIU26B.KronoCash.model.SimulationScenario;
import com.GIU26B.KronoCash.repository.UserRepository;
import com.GIU26B.KronoCash.service.FinanceService;
import com.GIU26B.KronoCash.service.SimulatorService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.math.BigDecimal;
import java.util.Map;

/** Vista del simulador, metas vinculadas y CRUD de escenarios. */
@Controller
@RequiredArgsConstructor
public class SimulatorController {

    private static final String SIMULATOR = "redirect:/simulator";

    private final SimulatorService simulatorService;
    private final FinanceService financeService;
    private final UserRepository userRepository;

    @ModelAttribute("simulationRequest")
    public SimulationRequestDTO simulationRequest() {
        return new SimulationRequestDTO();
    }

    @GetMapping("/simulator")
    public String simulator(@RequestParam(required = false) Long load,
                            @RequestParam(required = false) Long edit,
                            Model model,
                            Authentication authentication) {
        Long userId = currentUserId(authentication);
        populate(model, userId);

        if (load != null) {
            SimulationScenario scenario = simulatorService.findById(userId, load);
            model.addAttribute("simulationRequest", fromScenario(scenario));
        }
        if (edit != null) {
            model.addAttribute("editingScenario", simulatorService.findById(userId, edit));
        }
        return "public/simulator";
    }

    /** Fallback sin JavaScript: calcula y renderiza la tabla en servidor. */
    @PostMapping("/simulator/calculate")
    public String calculateForm(@Valid @ModelAttribute("simulationRequest") SimulationRequestDTO request,
                                BindingResult bindingResult,
                                Model model,
                                Authentication authentication) {
        Long userId = currentUserId(authentication);
        populate(model, userId);
        if (!bindingResult.hasErrors()) {
            model.addAttribute("result", simulatorService.calculate(request));
        }
        return "public/simulator";
    }

    /** Fallback sin JavaScript: guarda el escenario y redirige. */
    @PostMapping("/simulator/save")
    public String saveForm(@Valid @ModelAttribute("simulationRequest") SimulationRequestDTO request,
                           BindingResult bindingResult,
                           Authentication authentication) {
        Long userId = currentUserId(authentication);
        if (bindingResult.hasErrors()) {
            return SIMULATOR + "?error=validation#seccion-escenarios";
        }
        simulatorService.save(userId, request, simulatorService.calculate(request));
        return SIMULATOR + "?saved#seccion-escenarios";
    }

    @PostMapping("/api/simulator/calculate")
    @ResponseBody
    public SimulationResultDTO calculate(@Valid @RequestBody SimulationRequestDTO request) {
        return simulatorService.calculate(request);
    }

    @PostMapping("/api/simulator/save")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> save(@Valid @RequestBody SimulationRequestDTO request,
                                                    Authentication authentication) {
        Long userId = currentUserId(authentication);
        SimulationResultDTO result = simulatorService.calculate(request);
        SimulationScenario saved = simulatorService.save(userId, request, result);
        return ResponseEntity.ok(Map.of("id", saved.getId(), "saved", true));
    }

    /** Edición validada en servidor: @Valid sobre el DTO enlazado, no .builder(). */
    @PostMapping("/simulator/scenarios/{id}/edit")
    public String edit(@PathVariable Long id,
                       @Valid @ModelAttribute("simulationRequest") SimulationRequestDTO request,
                       BindingResult bindingResult,
                       Authentication authentication) {
        if (bindingResult.hasErrors()) {
            return SIMULATOR + "?error=validation#seccion-escenarios";
        }
        try {
            simulatorService.update(currentUserId(authentication), id, request);
        } catch (IllegalArgumentException ex) {
            return SIMULATOR + "?error=edit#seccion-escenarios";
        }
        return SIMULATOR + "?updated#seccion-escenarios";
    }

    @PostMapping("/simulator/scenarios/{id}/delete")
    public String delete(@PathVariable Long id, Authentication authentication) {
        try {
            simulatorService.delete(currentUserId(authentication), id);
        } catch (IllegalArgumentException ex) {
            return SIMULATOR + "?error=delete#seccion-escenarios";
        }
        return SIMULATOR + "?deleted#seccion-escenarios";
    }

    private void populate(Model model, Long userId) {
        model.addAttribute("scenarios", simulatorService.findByUser(userId));
        model.addAttribute("goals", financeService.findActiveGoals(userId));
    }

    private SimulationRequestDTO fromScenario(SimulationScenario scenario) {
        SimulationRequestDTO request = new SimulationRequestDTO();
        request.setScenarioName(scenario.getScenarioName());
        request.setMonthlyIncome(scenario.getMonthlyIncome());
        request.setSavingsPercentage(scenario.getSavingsPercentage());
        request.setAntExpensesCut(scenario.getAntExpensesCut());
        request.setMonthsHorizon(scenario.getMonthsHorizon());
        return request;
    }

    private Long currentUserId(Authentication authentication) {
        return userRepository.findByUsername(authentication.getName())
                .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado"))
                .getId();
    }
}
