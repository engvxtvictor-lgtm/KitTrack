
package com.github.engvxtvictorlgtm.kittrack;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class AulaRepository {

    private final JdbcTemplate jdbc;

    public AulaRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<AulaResumo> listarAulas() {
        return listarAulas(null, null, null, null);
    }

    public List<AulaResumo> listarAulas(
            LocalDate data,
            String sala,
            String status,
            String transmissor) {

        StringBuilder sql = new StringBuilder("""
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
            WHERE 1 = 1
            """);

        List<Object> parametros = new ArrayList<>();

        if (data != null) {
            sql.append(" AND a.data_aula = ?");
            parametros.add(data);
        }

        if (sala != null && !sala.isBlank()) {
            sql.append(" AND LOWER(a.sala) LIKE LOWER(?)");
            parametros.add("%" + sala.trim() + "%");
        }

        if (transmissor != null && !transmissor.isBlank()) {
            sql.append(" AND LOWER(t.nome) LIKE LOWER(?)");
            parametros.add("%" + transmissor.trim() + "%");
        }

        if (status != null && !status.isBlank()) {
            if ("SEM KIT".equals(status)) {
                sql.append(" AND k.id IS NULL");
            } else {
                sql.append(" AND k.status = ?");
                parametros.add(status);
            }
        }

        sql.append(" ORDER BY a.data_aula, a.horario_inicio");

        return jdbc.query(
            sql.toString(),
            (rs, row) -> new AulaResumo(
                rs.getLong("id"),
                rs.getObject("kit_id", Long.class),
                rs.getString("transmissor"),
                rs.getDate("data_aula").toLocalDate(),
                rs.getTime("horario_inicio").toLocalTime(),
                rs.getTime("horario_fim").toLocalTime(),
                rs.getString("sala"),
                rs.getString("status_kit"),
                rs.getString("itens")
            ),
            parametros.toArray()
        );
    }

    public List<String> listarTransmissores() {
        return jdbc.query(
            "SELECT nome FROM transmissor ORDER BY nome",
            (rs, row) -> rs.getString("nome")
        );
    }

    public List<String> listarTiposItem() {
        return jdbc.query(
            "SELECT nome FROM tipo_item ORDER BY nome",
            (rs, row) -> rs.getString("nome")
        );
    }

    public void cadastrarTransmissor(String nome) {
        jdbc.update(
            "INSERT INTO transmissor (nome) VALUES (?)",
            nome.trim()
        );
    }

    @Transactional
    public void cadastrarAula(
            String nomeTransmissor,
            LocalDate data,
            LocalTime inicio,
            LocalTime fim,
            String sala,
            int camera,
            int microfone,
            int passador,
            int pincel) {

        if (nomeTransmissor == null || nomeTransmissor.isBlank()
                || sala == null || sala.isBlank()) {
            throw new IllegalArgumentException(
                "Informe o transmissor e a sala."
            );
        }

        if (data == null || inicio == null || fim == null
                || !fim.isAfter(inicio)) {
            throw new IllegalArgumentException(
                "Confira a data e os horarios da aula."
            );
        }

        if (camera < 0 || microfone < 0
                || passador < 0 || pincel < 0) {
            throw new IllegalArgumentException(
                "As quantidades nao podem ser negativas."
            );
        }

        Long transmissorId = jdbc.queryForObject(
            "SELECT id FROM transmissor WHERE nome = ?",
            Long.class,
            nomeTransmissor
        );

        Long aulaId = jdbc.queryForObject("""
            INSERT INTO aula (
                transmissor_id, data_aula,
                horario_inicio, horario_fim, sala
            )
            VALUES (?, ?, ?, ?, ?)
            RETURNING id
            """,
            Long.class,
            transmissorId, data, inicio, fim, sala.trim()
        );

        Long kitId = jdbc.queryForObject(
            "INSERT INTO kit (aula_id) VALUES (?) RETURNING id",
            Long.class,
            aulaId
        );

        adicionarItem(kitId, "Camera", camera);
        adicionarItem(kitId, "Microfone", microfone);
        adicionarItem(kitId, "Passador", passador);
        adicionarItem(kitId, "Pincel", pincel);
    }

    private void adicionarItem(Long kitId, String nome, int quantidade) {
        if (quantidade == 0) {
            return;
        }

        Long tipoItemId = jdbc.queryForObject(
            "SELECT id FROM tipo_item WHERE nome = ?",
            Long.class,
            nome
        );

        jdbc.update("""
            INSERT INTO kit_item (kit_id, tipo_item_id, quantidade)
            VALUES (?, ?, ?)
            """,
            kitId, tipoItemId, quantidade
        );
    }

    @Transactional
    public void entregarKit(Long id) {
        List<String> statusList = jdbc.query(
            "SELECT status FROM kit WHERE id = ? FOR UPDATE",
            (rs, row) -> rs.getString("status"),
            id
        );

        if (statusList.isEmpty()) {
            throw new IllegalArgumentException("Kit nao encontrado.");
        }

        if (!"PENDENTE".equals(statusList.get(0))) {
            throw new IllegalArgumentException(
                "Somente kits pendentes podem ser entregues."
            );
        }

        List<ItemMovimento> itens = jdbc.query("""
            SELECT ti.nome,
                   ti.quantidade_disponivel,
                   ki.quantidade
            FROM kit_item ki
            JOIN tipo_item ti ON ti.id = ki.tipo_item_id
            WHERE ki.kit_id = ?
            ORDER BY ti.id
            FOR UPDATE OF ti
            """,
            (rs, row) -> new ItemMovimento(
                rs.getString("nome"),
                rs.getInt("quantidade"),
                rs.getInt("quantidade_disponivel")
            ),
            id
        );

        for (ItemMovimento item : itens) {
            if (item.quantidade() > item.disponivel()) {
                throw new IllegalArgumentException(
                    "Estoque insuficiente para " + item.nome()
                    + ". Necessario: " + item.quantidade()
                    + "; disponivel: " + item.disponivel() + "."
                );
            }
        }

        for (ItemMovimento item : itens) {
            jdbc.update("""
                UPDATE tipo_item
                SET quantidade_disponivel = quantidade_disponivel - ?
                WHERE nome = ?
                """,
                item.quantidade(), item.nome()
            );
        }

        jdbc.update(
            "UPDATE kit SET status = 'ENTREGUE' WHERE id = ?",
            id
        );
    }

    @Transactional
    public void devolverKit(Long id) {
        List<String> statusList = jdbc.query(
            "SELECT status FROM kit WHERE id = ? FOR UPDATE",
            (rs, row) -> rs.getString("status"),
            id
        );

        if (statusList.isEmpty()) {
            throw new IllegalArgumentException("Kit nao encontrado.");
        }

        if (!"ENTREGUE".equals(statusList.get(0))) {
            throw new IllegalArgumentException(
                "Somente kits entregues podem ser devolvidos."
            );
        }

        List<ItemMovimento> itens = jdbc.query("""
            SELECT ti.nome,
                   ti.quantidade_disponivel,
                   ki.quantidade
            FROM kit_item ki
            JOIN tipo_item ti ON ti.id = ki.tipo_item_id
            WHERE ki.kit_id = ?
            ORDER BY ti.id
            FOR UPDATE OF ti
            """,
            (rs, row) -> new ItemMovimento(
                rs.getString("nome"),
                rs.getInt("quantidade"),
                rs.getInt("quantidade_disponivel")
            ),
            id
        );

        for (ItemMovimento item : itens) {
            jdbc.update("""
                UPDATE tipo_item
                SET quantidade_disponivel = quantidade_disponivel + ?
                WHERE nome = ?
                """,
                item.quantidade(), item.nome()
            );
        }

        jdbc.update(
            "UPDATE kit SET status = 'DEVOLVIDO' WHERE id = ?",
            id
        );
    }

    private record ItemMovimento(
        String nome,
        int quantidade,
        int disponivel
    ) {}
}
