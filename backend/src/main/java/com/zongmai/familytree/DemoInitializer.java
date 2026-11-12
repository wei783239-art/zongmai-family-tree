package com.zongmai.familytree;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.*;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Order(10)
@ConditionalOnProperty(name = "app.demo", havingValue = "true")
public class DemoInitializer implements ApplicationRunner {
  private final JdbcTemplate db;
  private final PasswordEncoder passwords;
  private final DemoDataService demo;
  private final String password;

  public DemoInitializer(
      JdbcTemplate db,
      PasswordEncoder passwords,
      DemoDataService demo,
      @Value("${DEMO_PASSWORD:Demo123!}") String password) {
    this.db = db;
    this.passwords = passwords;
    this.demo = demo;
    this.password = password;
  }

  @Override
  @Transactional
  public void run(ApplicationArguments args) {
    for (String name : new String[] {"demo_admin", "demo_member", "demo_outsider"}) {
      if (db.queryForObject("SELECT COUNT(*) FROM sys_user WHERE username=?", Long.class, name)
          == 0)
        db.update(
            "INSERT INTO sys_user(username,password_hash) VALUES(?,?)",
            name,
            passwords.encode(password));
    }
    Long admin =
        db.queryForObject("SELECT id FROM sys_user WHERE username='demo_admin'", Long.class);
    Long member =
        db.queryForObject("SELECT id FROM sys_user WHERE username='demo_member'", Long.class);
    if (db.queryForObject("SELECT COUNT(*) FROM family_user WHERE user_id=?", Long.class, admin)
        == 0) {
      Long familyId = demo.create(admin);
      db.update(
          "INSERT INTO family_user(family_id,user_id,role) VALUES(?,?,'MEMBER')", familyId, member);
    }
  }
}
