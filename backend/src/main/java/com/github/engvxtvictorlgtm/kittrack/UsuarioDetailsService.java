
package com.github.engvxtvictorlgtm.kittrack;

import java.util.List;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class UsuarioDetailsService implements UserDetailsService {

    private final JdbcTemplate jdbcTemplate;

    public UsuarioDetailsService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public UserDetails loadUserByUsername(String username)
            throws UsernameNotFoundException {

        List<UserDetails> usuarios = jdbcTemplate.query(
            """
            SELECT username, senha_hash, ativo
            FROM usuario
            WHERE username = ?
            """,
            (rs, rowNum) -> User.withUsername(rs.getString("username"))
                .password(rs.getString("senha_hash"))
                .roles("USER")
                .disabled(!rs.getBoolean("ativo"))
                .build(),
            username
        );

        if (usuarios.isEmpty()) {
            throw new UsernameNotFoundException("Usuário não encontrado");
        }

        return usuarios.get(0);
    }
}
