package com.github.engvxtvictorlgtm.kittrack;

import java.util.List;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class AulaRepository {

    private final JdbcTemplate jdbc;

    public AulaRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<AulaResumo> listarAulas() {
        String sql = """
            SELECT a.id,
                   k.id AS kit_id,
                   t.nome AS transmissor,
                   a.data_aula,
                   a.horario_inicio,
                   a.horario_fim,
                   a.sala,
                   k.status AS status_kit,
                   COALESCE(
                       (SELECT string_agg(
                           ti.nome || ' (' || ki.quantidade || ')',
                           ', ' ORDER BY ti.id
                       )
                       FROM kit_item ki
                       JOIN tipo_item ti ON ti.id = ki.tipo_item_id
                       WHERE ki.kit_id = k.id),
                       'Sem itens'
                   ) AS itens
            FROM aula a
            JOIN transmissor t ON t.id = a.transmissor_id
            LEFT JOIN kit k ON k.aula_id = a.id
            ORDER BY a.data_aula, a.horario_inicio
            """;

        return jdbc.query(sql, (rs, row) -> new AulaResumo(
            rs.getLong("id"),
            rs.getObject("kit_id", Long.class),
            rs.getString("transmissor"),
            rs.getDate("data_aula").toLocalDate(),
            rs.getTime("horario_inicio").toLocalTime(),
            rs.getTime("horario_fim").toLocalTime(),
            rs.getString("sala"),
            rs.getString("status_kit"),
            rs.getString("itens")
        ));
    }

    public void entregarKit(Long id) {
        jdbc.update(
            "UPDATE kit SET status = 'ENTREGUE' WHERE id = ? AND status = 'PENDENTE'",
            id
        );
    }

    public void devolverKit(Long id) {
        jdbc.update(
            "UPDATE kit SET status = 'DEVOLVIDO' WHERE id = ? AND status = 'ENTREGUE'",
            id
        );
    }
}
