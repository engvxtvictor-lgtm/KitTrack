package com.github.engvxtvictorlgtm.kittrack;

import java.time.LocalDate;
import java.time.LocalTime;

public record AulaResumo(
    Long id,
    Long kitId,
    String transmissor,
    LocalDate dataAula,
    LocalTime horarioInicio,
    LocalTime horarioFim,
    String sala,
    String statusKit,
    String itens
) {}
