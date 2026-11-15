package com.inpetu.connect.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalTime;

@Entity
@Table(name = "horario_funcionamento", uniqueConstraints = @UniqueConstraint(
        name = "uk_horario_funcionamento_dia", columnNames = "dia_semana"))
@Getter
@Setter
@NoArgsConstructor
public class HorarioFuncionamento {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_horario_funcionamento")
    private Integer idHorarioFuncionamento;

    @Column(name = "dia_semana", nullable = false)
    private Integer diaSemana;

    @Column(name = "fechado", nullable = false)
    private Boolean fechado = false;

    @Column(name = "exige_aprovacao", nullable = false)
    private Boolean exigeAprovacao = false;

    @Column(name = "horario_abertura")
    private LocalTime horarioAbertura;

    @Column(name = "horario_fechamento")
    private LocalTime horarioFechamento;
}
