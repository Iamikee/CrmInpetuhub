package com.inpetu.connect.repository;

import com.inpetu.connect.model.HorarioFuncionamento;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface HorarioFuncionamentoRepository extends JpaRepository<HorarioFuncionamento, Integer> {
    List<HorarioFuncionamento> findAllByOrderByDiaSemana();
}
