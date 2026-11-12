package com.zongmai.familytree;

import static com.zongmai.familytree.WorkspaceService.error;

import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/system")
@PreAuthorize("@systemAccess.allowed(authentication)")
public class SystemController {
  private final JdbcTemplate db;

  public SystemController(JdbcTemplate db) {
    this.db = db;
  }

  @GetMapping("/users")
  public List<Map<String, Object>> users() {
    return db.queryForList("SELECT id,username,status,created_at FROM sys_user ORDER BY id");
  }

  public record StatusInput(String status) {}

  @PatchMapping("/users/{userId}")
  @Transactional
  public void status(
      @PathVariable Long userId,
      @RequestBody StatusInput input,
      @AuthenticationPrincipal LoginUser user) {
    if (input.status() == null || !List.of("ACTIVE", "DISABLED").contains(input.status()))
      throw error(400, "账号状态无效");
    if (user.id().equals(userId)) throw error(409, "当前账号的状态需由其他系统管理员维护");
    var familyIds =
        db.queryForList(
            "SELECT family_id FROM family_user WHERE user_id=? AND role='FAMILY_ADMIN' ORDER BY"
                + " family_id",
            userId);
    for (var row : familyIds)
      db.queryForList("SELECT id FROM family WHERE id=? FOR UPDATE", row.get("family_id"));
    if ("DISABLED".equals(input.status())) {
      for (var row : familyIds) {
        Long count =
            db.queryForObject(
                "SELECT COUNT(*) FROM family_user fu JOIN sys_user u ON u.id=fu.user_id WHERE"
                    + " fu.family_id=? AND fu.role='FAMILY_ADMIN' AND u.status='ACTIVE' AND"
                    + " u.id<>?",
                Long.class,
                row.get("family_id"),
                userId);
        if (count == 0) throw error(409, "该账号是某家族最后一位启用管理员，请先移交管理权");
      }
    }
    if (db.update("UPDATE sys_user SET status=? WHERE id=?", input.status(), userId) == 0)
      throw error(404, "账号不存在");
  }
}
