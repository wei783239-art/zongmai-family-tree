package com.zongmai.familytree;

import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

@Service("systemAccess")
public class SystemAccess {
  private final Set<String> admins;
  private final JdbcTemplate db;

  public SystemAccess(@Value("${app.system-admins:}") String names, JdbcTemplate db) {
    admins = new HashSet<>(Arrays.asList(names.split(",")));
    this.db = db;
  }

  public boolean allowed(Authentication authentication) {
    return authentication != null
        && authentication.getPrincipal() instanceof LoginUser user
        && allowed(user);
  }

  public boolean allowed(LoginUser user) {
    return user != null
        && admins.contains(user.username())
        && db.queryForObject(
                "SELECT COUNT(*) FROM sys_user WHERE id=? AND status='ACTIVE'",
                Long.class,
                user.id())
            > 0;
  }
}
