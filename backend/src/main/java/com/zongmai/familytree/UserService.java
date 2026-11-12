package com.zongmai.familytree;

import java.nio.charset.StandardCharsets;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class UserService {

    private final JdbcTemplate jdbcTemplate;
    private final PasswordEncoder passwordEncoder;
    private final String dummyPasswordHash;

    public UserService(JdbcTemplate jdbcTemplate, PasswordEncoder passwordEncoder) {
        this.jdbcTemplate = jdbcTemplate;
        this.passwordEncoder = passwordEncoder;
        this.dummyPasswordHash = passwordEncoder.encode(UUID.randomUUID().toString());
    }

    public void register(RegisterRequest request) {
        String username = request.username();
        String password = request.password();

        if (password.getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "密码内容过长，请缩短后重试");
        }

        String countSql = "SELECT COUNT(*) FROM sys_user WHERE username = ?";

        Integer userCount = jdbcTemplate.queryForObject(countSql, Integer.class, username);

        if (userCount != null && userCount > 0) {
            throw new  ResponseStatusException(HttpStatus.CONFLICT, "该用户名已被使用");
        }

        String passwordHash = passwordEncoder.encode(password);

        String insertSql = "INSERT INTO sys_user "
                         + "(username, password_hash) VALUES (?, ?)";

        try {
            jdbcTemplate.update(insertSql, username, passwordHash);
        } catch (DuplicateKeyException exception) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "该用户名已被使用", exception);
        }
    }

    public Map<String, Object> authenticate(LoginRequest request) {

        String password = request.password();

        if (password.getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "密码内容过长，请缩短后重试");
        }

        String sql = "SELECT id, username, password_hash, status "
                   + "FROM sys_user WHERE username = ?";

        List<Map<String, Object>> users = jdbcTemplate.queryForList(sql, request.username());

        String savedHash = dummyPasswordHash;

        if (!users.isEmpty()) {
            savedHash = (String) users.get(0).get("password_hash");
        }

        boolean passwordMatches = passwordEncoder.matches(password, savedHash);

        if (users.isEmpty() || !passwordMatches || !"ACTIVE".equals(users.get(0).get("status"))) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "用户名或密码错误");
        }

        Map<String, Object> user = users.get(0);

        return Map.of("id", user.get("id"), "username", user.get("username"));
    }
}