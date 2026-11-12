package com.zongmai.familytree;

import java.util.*;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@Order(0)
public class SchemaUpgrade implements ApplicationRunner {
  private final JdbcTemplate db;

  public SchemaUpgrade(JdbcTemplate db) {
    this.db = db;
  }

  @Override
  public void run(ApplicationArguments args) {
    addColumns("person", Map.of("nickname", "VARCHAR(80)", "hometown", "VARCHAR(160)"));
    addColumns("family", Map.of("motto", "VARCHAR(2000)"));
    addColumns("family", Map.of("published", "BOOLEAN NOT NULL DEFAULT FALSE"));
    addColumns("family_media", Map.of("visibility", "VARCHAR(16) NOT NULL DEFAULT 'FAMILY'"));
  }

  private void addColumns(String table, Map<String, String> columns) {
    Set<String> existing =
        db.execute(
            (ConnectionCallback<Set<String>>)
                connection -> {
                  Set<String> names = new HashSet<>();
                  try (var statement = connection.createStatement();
                      var result =
                          statement.executeQuery("SELECT * FROM " + table + " WHERE 1=0")) {
                    var metadata = result.getMetaData();
                    for (int i = 1; i <= metadata.getColumnCount(); i++)
                      names.add(metadata.getColumnName(i).toLowerCase(Locale.ROOT));
                  }
                  return names;
                });
    columns.forEach(
        (name, type) -> {
          if (!existing.contains(name))
            db.execute("ALTER TABLE " + table + " ADD COLUMN " + name + " " + type);
        });
  }
}
