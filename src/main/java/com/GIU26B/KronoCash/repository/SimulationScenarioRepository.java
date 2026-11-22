package com.GIU26B.KronoCash.repository;

import com.GIU26B.KronoCash.model.SimulationScenario;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SimulationScenarioRepository extends JpaRepository<SimulationScenario, Long> {

    List<SimulationScenario> findByUserIdOrderByCreatedAtDesc(Long userId);

    long countByUserId(Long userId);
}
