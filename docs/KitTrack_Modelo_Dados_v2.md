# KitTrack — Modelo de Dados v2

**Versão:** 2.0 — 11/10/2026  
**Estado:** modelo lógico em revisão; **ainda não é SQL**.  
**Objetivo:** consolidar as regras de negócio definidas e representar entidades, campos e relacionamentos antes das migrações PostgreSQL/Flyway.

## 1. Regras de negócio confirmadas

1. Somente a equipe administrativa entra no painel com usuário e senha. Os transmissores não recebem conta administrativa.
2. Você e Hiroshi têm o mesmo nível de permissão administrativa. Ambos podem registrar operações, resolver conflitos de regras, alterar regras gerais e autorizar a liberação de equipamentos danificados.
3. O transmissor confirma o recebimento por link individual enviado por mensagem. O link é associado a uma entrega específica; não equivale a uma conta de usuário.
4. Cada professor tem exatamente um transmissor responsável. Um transmissor pode ser responsável por até três professores.
5. A sala não é fixa do professor. Cada registro de `aula` é uma ocorrência específica, com uma turma, um professor, uma matéria, uma sala, data e horários próprios.
6. Uma aula pode ter alteração de horário, sala, professor, matéria, turma ou situação. Toda mudança relevante gera histórico; o motivo é opcional.
7. Cancelar/alterar uma aula depois de uma entrega não apaga a entrega nem a confirmação. A entrega fica sinalizada para revisão operacional.
8. Todo kit entregue contém obrigatoriamente **um passador e três pincéis**. Câmera(s) e microfone(s) variam conforme regras aplicáveis à aula.
9. Câmeras, microfones e passadores são identificados individualmente. Pincéis são controlados por quantidade, sem identificar cada unidade.
10. Regras de câmera e microfone podem depender de turma, professor, matéria e/ou sala. Havendo conflito entre regras aplicáveis, o sistema alerta e aguarda decisão manual; não escolhe nem soma automaticamente.
11. Na resolução do conflito, a equipe escolhe explicitamente entre uma exceção válida apenas para a aula ou uma alteração da regra geral. Alterar uma regra geral não deve reescrever planejamentos, entregas ou históricos anteriores.
12. Você e Hiroshi podem fazer ambas as escolhas de regra e autorizar a liberação de item danificado.
13. A devolução pode ser parcial. Cada equipamento individual e cada quantidade de pincéis têm seus próprios dados de retorno.
14. Equipamento ou quantidade danificada não retorna automaticamente ao saldo disponível: aguarda avaliação e autorização. O projeto não terá acompanhamento detalhado das etapas/datas de manutenção.
15. O saldo atual e seu registro de movimentação devem ser atualizados juntos em uma transação. Não pode haver estoque negativo ou duplicação de um item em duas entregas abertas.

## 2. Diagrama de alto nível

Os diagramas editáveis e as imagens que acompanham este documento estão disponíveis em `docs/diagramas/` após serem adicionados ao repositório. O MER organiza o sistema em três áreas: **pessoas e aulas**, **regras e planejamento**, **entrega e estoque**.

## 3. Entidades propostas

Os nomes e tipos definitivos serão decididos antes do SQL. PK = chave primária; FK = chave estrangeira; UQ = valor único.

### 3.1 Pessoas, cadastros e aulas

#### `usuario` — contas administrativas
- `id` (PK): identificador da conta.
- `nome` (obrigatório): nome do integrante.
- `email` (UQ, obrigatório): login único.
- `senha_hash` (obrigatório): hash seguro; nunca armazenar senha em texto aberto.
- `perfil` (obrigatório): inicialmente `ADMINISTRADOR` para ambas as contas administrativas.
- `ativo`: permite bloquear acesso sem excluir registros históricos.
- `criado_em`: data/hora de criação.

**Permissões:** você e Hiroshi possuem o mesmo perfil e podem resolver conflitos, editar regras gerais e autorizar liberação de itens danificados. As ações relevantes continuam registrando o `usuario_id` responsável.

#### `transmissor`
- `id` (PK), `nome` (obrigatório), `telefone` (contato usado para envio do link, confirmar canal), `ativo`, `criado_em`.
- Não tem credenciais de login administrativo.

#### `professor`
- `id` (PK), `nome` (obrigatório), `transmissor_id` (FK, obrigatório), `ativo`.
- Cada professor deve ter exatamente um transmissor responsável.
- Cada transmissor pode ter no máximo três professores. Uma FK não implementa esse limite sozinha; será preciso validação transacional na aplicação ou no banco.

#### `turma`
- `id` (PK), `codigo` (UQ recomendado; ex. `PMPI`), `ativo`.
- Confirmar se os códigos são globalmente únicos no contexto real.

#### `materia`
- `id` (PK), `nome` (UQ recomendado), `ativo`.

#### `sala`
- `id` (PK), `codigo` (UQ; ex. `46`), `descricao` (opcional), `ativo`.

#### `aula` — uma ocorrência individual
- `id` (PK).
- `turma_id`, `professor_id`, `materia_id`, `sala_id` (FKs obrigatórias).
- `inicio_previsto`, `fim_previsto` (fim posterior ao início).
- `situacao`: proposta `AGENDADA`, `REALIZADA`, `CANCELADA`.
- `criado_por_usuario_id` (FK), `criado_em`, `atualizado_em`.
- Uma aula refere-se a uma turma, professor, matéria e sala. A turma pode ter aulas diferentes com professores e matérias diferentes.
- Validar sobreposição de horários para sala e decidir se também deve validar professor/turma.

#### `historico_aula`
- `id` (PK), `aula_id` (FK), `usuario_id` (FK), `tipo_evento`, `data_hora`.
- `valores_anteriores` e `valores_novos` (snapshot; JSONB é uma possibilidade no PostgreSQL).
- `motivo` opcional.
- Alterar a aula e criar o evento histórico deve ocorrer na mesma transação.

### 3.2 Regras de equipamentos e planejamento do kit

#### `regra_camera`
- `id` (PK).
- Critérios de aplicação: referências opcionais a `turma`, `professor`, `materia` e `sala`.
- `tipo_item_id` (FK para o tipo/modelo de câmera requerido), `quantidade_necessaria`, `ativo`, `criado_em`, `atualizado_em`.
- A regra é aplicável à aula quando seus critérios preenchidos correspondem à aula; a definição exata de combinações e regra global deve ser fechada antes do SQL.

#### `regra_microfone`
- Mesmo padrão de `regra_camera`, mas o tipo referenciado deve pertencer à categoria microfone.

#### `planejamento_kit`
- `id` (PK), `aula_id` (FK), `versao`, `situacao`, `criado_em`.
- Situações propostas: `PENDENTE`, `CONFLITO`, `AGUARDANDO_DECISAO`, `APROVADO`, `SUBSTITUIDO`.
- `escopo_decisao`: `PONTUAL` ou `ALTERAR_REGRA_GERAL` quando houver decisão manual.
- `resolvido_por_usuario_id` (FK, opcional), `resolvido_em` (opcional), `observacao` (opcional).
- Manter versões anteriores em vez de sobrescrever um planejamento já aprovado. Uma versão atual deve ser identificável.

#### `planejamento_item`
- `id` (PK), `planejamento_kit_id` (FK), `tipo_item_id` (FK), `quantidade_necessaria`.
- `origem`: por exemplo `OBRIGATORIO`, `REGRA`, `DECISAO_MANUAL`.
- Snapshot da exigência aprovada. Não identifica ainda os objetos físicos separados para entrega.
- Todo planejamento aprovado precisa conter **passador × 1** e **pincel × 3**, além das exigências aprovadas de câmera e microfone.

#### `planejamento_regra`
- `id` (PK), `planejamento_kit_id` (FK).
- `regra_camera_id` (FK opcional) e `regra_microfone_id` (FK opcional); exatamente uma referência deve ser preenchida.
- `resultado_avaliacao`: por exemplo `APLICADA`, `CONFLITANTE`, `DESCARTADA_POR_DECISAO`.
- Permite mostrar quais regras foram avaliadas e quais entraram em conflito.

#### `historico_regra_equipamento`
- `id` (PK), `usuario_id` (FK), `tipo_evento`, `data_hora`.
- `regra_camera_id` (FK opcional) ou `regra_microfone_id` (FK opcional), exatamente uma preenchida.
- `valores_anteriores`, `valores_novos` (snapshots), `motivo` opcional.
- Registra alterações de regra geral, preservando o antes/depois, quem alterou e quando. As novas regras afetam novos planejamentos; não reescrevem entregas nem planejamentos anteriores.

### 3.3 Tipos, equipamentos físicos, entrega e estoque

#### `tipo_item`
- `id` (PK), `categoria`, `nome`, `modo_controle`, `ativo`.
- Categorias mínimas: `CAMERA`, `MICROFONE`, `PASSADOR`, `PINCEL`.
- `modo_controle`: `INDIVIDUAL` para câmera, microfone e passador; `QUANTIDADE` para pincel.
- `nome` pode distinguir o tipo/modelo, para que regras exijam o equipamento correto.

#### `item` — objeto físico individual
- `id` (PK), `tipo_item_id` (FK; deve ser modo individual), `codigo` (UQ), `situacao`, `observacao`, `criado_em`.
- Situações propostas: `DISPONIVEL`, `RESERVADO`, `EM_USO`, `AGUARDANDO_AVALIACAO`, `EM_MANUTENCAO`, `INUTILIZADO`.
- Item danificado não pode estar `DISPONIVEL` até que você ou Hiroshi autorize a liberação. Não será implementado acompanhamento detalhado de reparos.

#### `entrega` — operação de entrega/montagem
- `id` (PK), `aula_id` (FK), `transmissor_id` (FK), `usuario_registrou_id` (FK).
- `situacao_operacional`: proposta `EM_MONTAGEM`, `PRONTO_PARA_ENTREGA`, `ENTREGUE`, `DEVOLUCAO_PARCIAL`, `FINALIZADA` (e `CANCELADA` somente quando ainda não houve entrega física).
- `montado_por_usuario_id` (FK, opcional), `montado_em`, `entregue_em`, `confirmada_em`, `criado_em`.
- A pessoa destinatária deve corresponder ao transmissor do professor relacionado à aula no momento da entrega; validar na aplicação.
- Se a aula for alterada/cancelada depois da entrega, manter a entrega e gerar revisão operacional. Avaliar se o contexto da aula no momento da entrega precisa de snapshot explícito.

#### `entrega_confirmacao` — link individual
- `id` (PK), `entrega_id` (FK), `token_hash` (UQ), `gerado_por_usuario_id` (FK), `criado_em`, `expira_em`, `usado_em`, `invalidado_em`.
- Token aleatório, imprevisível, com validade limitada, uso único e associado somente à entrega correspondente. Não guardar o token secreto em texto aberto.
- O canal de mensagem ainda precisa ser escolhido. Posse do link, sozinha, não comprova de forma absoluta a identidade de quem clicou.

#### `entrega_item` — item individual associado à entrega
- `id` (PK), `entrega_id` (FK), `item_id` (FK), `devolvido_em`, `condicao_retorno`, `registrado_por_usuario_id` (FK), `observacao`.
- `condicao_retorno`: proposta `BOA` ou `DANIFICADA`.
- Cada câmera, microfone ou passador tem associação individual. Não pode estar em duas entregas abertas simultaneamente.
- A devolução física não libera automaticamente um item danificado.

#### `entrega_quantidade` — quantidade entregue
- `id` (PK), `entrega_id` (FK), `tipo_item_id` (FK; deve ter modo `QUANTIDADE`), `quantidade_entregue` (positiva).
- Usada para registrar os três pincéis sem identificar cada unidade.

#### `devolucao_quantidade` — retornos parciais por quantidade
- `id` (PK), `entrega_quantidade_id` (FK), `usuario_id` (FK), `quantidade_boa`, `quantidade_danificada`, `data_hora`, `observacao`.
- A soma das unidades devolvidas ao longo dos eventos não pode ultrapassar a quantidade entregue.
- As unidades danificadas aguardam decisão; não retornam automaticamente ao saldo disponível.

#### `estoque_quantidade` — saldo agregado
- `tipo_item_id` (PK/FK; para tipos controlados por quantidade), `quantidade_disponivel`, `quantidade_reservada`, `quantidade_com_transmissor`, `quantidade_aguardando_avaliacao`, `quantidade_em_manutencao`, `quantidade_inutilizada`, `atualizado_em`.
- O saldo é um retrato atual, não substitui o histórico de movimentações.

#### `movimentacao_estoque` — auditoria
- `id` (PK), `usuario_id` (FK), `item_id` (FK opcional), `tipo_item_id` (FK opcional), `entrega_id` (FK opcional).
- `tipo_movimento`: por exemplo `ENTRADA`, `RESERVA`, `CANCELAMENTO_RESERVA`, `ENTREGA`, `DEVOLUCAO`, `DANO`, `LIBERACAO`, `MANUTENCAO`, `BAIXA`.
- `quantidade`, `situacao_origem`, `situacao_destino`, `data_hora`, `observacao`.
- A movimentação deve ser de item individual **ou** de quantidade agregada; não preencher as duas referências de produto ao mesmo tempo. Saldo e movimento atualizados na mesma transação.

#### `revisao_entrega` — revisão após mudança de aula
- `id` (PK), `entrega_id` (FK), `historico_aula_id` (FK opcional), `status`, `aberta_em`, `decisao`, `resolvida_por_usuario_id` (FK opcional), `resolvida_em`, `observacao`.
- Situações propostas: `PENDENTE`, `RESOLVIDA`.
- Decisão proposta: `DEVOLVER`, `REDISTRIBUIR`, `MANTER_COM_TRANSMISSOR`.
- Não apagar entrega/confirmação; qualquer devolução ou redistribuição deve ser registrada como operação própria.

## 4. Relacionamentos e cardinalidades principais

- `transmissor` 1 → 0..3 `professor`; cada `professor` → exatamente 1 `transmissor`.
- `turma`, `professor`, `materia` e `sala` 1 → N `aula`; cada aula aponta para uma de cada.
- `aula` 1 → N `historico_aula` e 1 → N `planejamento_kit` ao longo das versões.
- `planejamento_kit` 1 → N `planejamento_item` e 1 → N `planejamento_regra`.
- `regra_camera` e `regra_microfone` podem ser avaliadas em muitos planejamentos.
- `usuario` 1 → N eventos de histórico, decisões, entregas e movimentações.
- `aula` 1 → N `entrega` ao longo do tempo; `transmissor` 1 → N `entrega`.
- `entrega` 1 → N `entrega_confirmacao`, 1 → N `entrega_item`, 1 → N `entrega_quantidade` e 1 → N `revisao_entrega`.
- `entrega_quantidade` 1 → N `devolucao_quantidade`.
- `tipo_item` 1 → N `item` para controle individual; tipo por quantidade tem um saldo agregado em `estoque_quantidade`.

## 5. Restrições de integridade a planejar

1. Email do usuário único; senha original nunca armazenada.
2. Professor sempre tem um transmissor; transmissor tem no máximo três professores.
3. Horário final da aula posterior ao inicial; conflitos de sala precisam ser impedidos.
4. Aula e histórico gravados atomicamente.
5. Câmera/microfone/passador somente em registro individual; pincéis somente em controle por quantidade.
6. Todo planejamento aprovado possui passador × 1 e pincel × 3.
7. Conflitos de regras colocam o planejamento em espera até decisão manual por usuário autorizado (você ou Hiroshi).
8. `PONTUAL` muda somente aquele planejamento; `ALTERAR_REGRA_GERAL` altera a regra e registra snapshot anterior/novo, responsável e data/hora.
9. Não permitir saldo negativo, entrega de quantidade acima do disponível ou devolução acima da pendência.
10. Item individual não pode estar em duas entregas abertas ao mesmo tempo.
11. Link de confirmação expira, é de uso único e somente confirma a entrega associada.
12. Alteração/cancelamento de aula depois da entrega abre revisão e não apaga a entrega.
13. Dano não libera item/quantidade automaticamente. Você ou Hiroshi registra a autorização; a decisão aparece no histórico.
14. Histórico operacional não deve ser apagado em cascata.

## 6. Decisões ainda pendentes antes do SQL

1. Definir como combinar critérios de regra (turma/professor/matéria/sala) e se haverá regra global sem critérios. Conflitos que produzam exigências incompatíveis sempre exigem decisão manual.
2. Confirmar se a aula precisa guardar horários reais ou apenas previstos na primeira versão.
3. Confirmar se o contexto de aula na entrega deve ser preservado em um snapshot além de `historico_aula`.
4. Confirmar o canal de envio do link e o método de identificar o transmissor, caso a posse do link não seja suficiente.
5. Definir quais mudanças de estoque exigem motivo obrigatório ou opcional.
6. Confirmar se há uma maleta/conjunto físico reutilizável com código próprio, além dos equipamentos individuais.
7. Rever se é desejável bloquear horários sobrepostos para professor além de sala.

## 7. Ordem recomendada de implementação

1. Validar as decisões pendentes desta seção.
2. Revisar o MER v2 e colocar os diagramas editáveis e PNGs em `docs/diagramas/`.
3. Gerar a primeira migração Flyway com tabelas-base, chaves e restrições.
4. Acrescentar constraints e índices adicionais.
5. Inserir dados fictícios e testar regras críticas: limite de três professores, conflito de regras, kit obrigatório, devolução parcial, token de uso único e liberação de item danificado.
