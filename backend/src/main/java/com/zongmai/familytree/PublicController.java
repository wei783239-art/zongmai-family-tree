package com.zongmai.familytree;

import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

@RestController
public class PublicController {
  private final JdbcTemplate db;

  public PublicController(JdbcTemplate db) {
    this.db = db;
  }

  @GetMapping("/api/public/families")
  public List<Map<String, Object>> families() {
    return db.queryForList(
        "SELECT id,name,surname,description FROM family WHERE published=TRUE ORDER BY id");
  }
}
