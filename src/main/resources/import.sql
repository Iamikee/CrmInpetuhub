INSERT IGNORE INTO perfil_acesso (id_perfil, nome_perfil, descricao) VALUES
(1, 'Coordenador', 'Acesso total ao InPETU Connect'),
(2, 'Gestor de Espaços', 'Gestão de ambientes e aprovações'),
(3, 'Staff/Atendente', 'Atendimento e consulta de disponibilidade'),
(4, 'Solicitante Público', 'Acesso ao portal público');
INSERT IGNORE INTO organizacao (id_organizacao, nome_organizacao, tipo_organizacao, ativo) VALUES (1, 'UFSC / InPETU Hub', 'Instituição', true);
INSERT IGNORE INTO horario_funcionamento
    (dia_semana, horario_abertura, horario_fechamento, fechado, exige_aprovacao)
VALUES
    (1, '08:00:00', '22:00:00', false, false),
    (2, '08:00:00', '22:00:00', false, false),
    (3, '08:00:00', '22:00:00', false, false),
    (4, '08:00:00', '22:00:00', false, false),
    (5, '08:00:00', '22:00:00', false, false),
    (6, '08:00:00', '22:00:00', false, false),
    (7, '08:00:00', '22:00:00', false, true);
