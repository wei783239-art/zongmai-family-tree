package com.zongmai.familytree;

import java.sql.PreparedStatement;
import java.util.List;
import java.util.Map;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class FamilyService {

  private final JdbcTemplate jdbcTemplate;

  private String validateFamilyName(Object name) {
    if (!(name instanceof String)) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "家族名称必须是文字");
    }

    String familyName = (String) name;

    if (familyName.isBlank() || familyName.codePointCount(0, familyName.length()) > 120) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "家族名称不能为空，且不能超过120个字符");
    }

    return familyName;
  }

  public FamilyService(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  public List<Map<String, Object>> listFamilies(Long userId) {
    String sql =
        """
 SELECT f.id, f.name, f.surname, f.ancestral_hall,
        f.hometown, f.description, f.motto, f.published, f.ancestor_person_id, fu.role,
        (SELECT COUNT(*) FROM person p WHERE p.family_id=f.id AND (fu.role='FAMILY_ADMIN' OR p.visibility<>'PRIVATE')) AS person_count
 FROM family f
 JOIN family_user fu ON fu.family_id = f.id
 JOIN sys_user u ON u.id = fu.user_id
 WHERE  FU.USER_ID = ?
   AND u.status = 'ACTIVE'
 ORDER BY f.id
""";

    return jdbcTemplate.queryForList(sql, userId);
  }

  @Transactional
  public Long createFamily(Long userId, FamilyRequest request) {
    String familyName = validateFamilyName(request.name());

    String sql =
        "INSERT INTO family "
            + "(name, surname, ancestral_hall, hometown, description, motto, published) "
            + "VALUES (?, ?, ?, ?, ?, ?, ?)";

    KeyHolder keyHolder = new GeneratedKeyHolder();

    jdbcTemplate.update(
        connection -> {
          PreparedStatement statement = connection.prepareStatement(sql, new String[] {"id"});

          statement.setString(1, familyName);
          statement.setString(2, request.surname());
          statement.setString(3, request.ancestral_hall());
          statement.setString(4, request.hometown());
          statement.setString(5, request.description());
          statement.setString(6, request.motto());
          statement.setBoolean(7, Boolean.TRUE.equals(request.published()));

          return statement;
        },
        keyHolder);

    Number key = keyHolder.getKey();

    if (key == null) {
      throw new IllegalStateException("未能取得新家族编号");
    }

    Long familyId = key.longValue();

    String memberSql =
        "INSERT INTO family_user "
            + "(family_id, user_id, role) "
            + "VALUES (?, ?, 'FAMILY_ADMIN')";

    jdbcTemplate.update(memberSql, familyId, userId);

    return familyId;
  }

  @Transactional
  public void deleteFamily(Long familyId) {
    jdbcTemplate.queryForList("SELECT id FROM family WHERE id=? FOR UPDATE", familyId);
    requireFamilyExists(familyId);

    try {
      jdbcTemplate.update("DELETE FROM audit_log WHERE family_id = ?", familyId);
      jdbcTemplate.update("DELETE FROM family_user WHERE family_id = ?", familyId);

      int deletedRows = jdbcTemplate.update("DELETE FROM family WHERE id = ?", familyId);

      if (deletedRows == 0) {
        throw new ResponseStatusException(HttpStatus.NOT_FOUND, "未找到该家族");
      }

    } catch (DataIntegrityViolationException exception) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "该家族仍有关联数据，暂时不能删除", exception);
    }
  }

  public Map<String, Object> getFamily(Long familyId) {
    String sql =
        "SELECT id, name, surname, ancestral_hall, hometown, "
            + "description, motto, published, ancestor_person_id "
            + "FROM family WHERE id = ?";

    List<Map<String, Object>> families = jdbcTemplate.queryForList(sql, familyId);

    if (families.isEmpty()) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "未找到该家族");
    }

    return families.get(0);
  }

  public void requireFamilyExists(Long familyId) {
    String sql = "SELECT COUNT(*) FROM family WHERE id = ?";

    Integer familyCount = jdbcTemplate.queryForObject(sql, Integer.class, familyId);

    if (familyCount == null || familyCount == 0) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "未找到该家族");
    }
  }

  @Transactional
  public void setAncestor(Long familyId, AncestorRequest request) {
    jdbcTemplate.queryForList("SELECT id FROM family WHERE id=? FOR UPDATE", familyId);

    requireFamilyExists(familyId);

    Long personId = request.personId();

    String personSql = "SELECT COUNT(*) FROM person " + "WHERE id = ? AND family_id = ?";

    Integer personCount = jdbcTemplate.queryForObject(personSql, Integer.class, personId, familyId);

    if (personCount == null || personCount == 0) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "找不到该家族的始祖成员");
    }

    String updateSql = "UPDATE family SET ancestor_person_id = ? " + "WHERE id = ?";

    jdbcTemplate.update(updateSql, personId, familyId);
  }

  @Transactional
  public void clearAncestor(Long familyId) {
    jdbcTemplate.queryForList("SELECT id FROM family WHERE id=? FOR UPDATE", familyId);
    requireFamilyExists(familyId);

    String sql = "UPDATE family SET ancestor_person_id = NULL " + "WHERE id = ?";

    jdbcTemplate.update(sql, familyId);
  }

  public void updateFamily(Long familyId, FamilyRequest request) {

    String familyName = validateFamilyName(request.name());

    String sql =
        "UPDATE family SET name = ?, surname = ?, ancestral_hall = ?, hometown = ?, description ="
            + " ?, motto = ?, published=COALESCE(?,published) WHERE id = ?";

    int updatedRows =
        jdbcTemplate.update(
            sql,
            familyName,
            request.surname(),
            request.ancestral_hall(),
            request.hometown(),
            request.description(),
            request.motto(),
            request.published(),
            familyId);

    if (updatedRows == 0) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "未找到该家族");
    }
  }
}
