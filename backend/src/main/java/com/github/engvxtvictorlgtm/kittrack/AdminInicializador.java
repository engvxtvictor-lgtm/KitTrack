
package com.github.engvxtvictorlgtm.kittrack;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class AdminInicializador implements ApplicationRunner {

    private final JdbcTemplate jdbcTemplate;
    private final PasswordEncoder passwordEncoder;

    @Value("${KITTRACK_ADMIN_USERNAME:}")
    private String username;

    @Value("${KITTRACK_ADMIN_PASSWORD:}")
    private String password;

    @Value("${KITTRACK_ADMIN_NOME:Administrador}")
    private String nome;

    public AdminInicializador(
            JdbcTemplate jdbcTemplate,
            PasswordEncoder passwordEncoder) {
        this.jdbcTemplate = jdbcTemplate;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (username == null || username.isBlank()
                || password == null || password.isBlank()) {
            throw new IllegalStateException(
                "Configure KITTRACK_ADMIN_USERNAME e KITTRACK_ADMIN_PASSWORD no .env."
            );
        }

        Integer quantidade = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM usuario WHERE username = ?",
            Integer.class,
            username
        );

        if (quantidade != null && quantidade == 0) {
            jdbcTemplate.update(
                """
                INSERT INTO usuario (nome, username, senha_hash, ativo)
                VALUES (?, ?, ?, TRUE)
                """,
                nome,
                username,
                passwordEncoder.encode(password)
            );

            System.out.println("Conta inicial do KitTrack criada.");
        }
    }
}
