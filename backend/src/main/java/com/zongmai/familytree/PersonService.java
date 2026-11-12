package com.zongmai.familytree;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class PersonService {

  private final JdbcTemplate jdbcTemplate;
  private final FamilyService familyService;

  private String visibilityFilter(String alias, Long familyId) {
    var authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication != null && authentication.getPrincipal() instanceof LoginUser user) {
      Long count =
          jdbcTemplate.queryForObject(
              "SELECT COUNT(*) FROM family_user WHERE family_id=? AND user_id=? AND"
                  + " role='FAMILY_ADMIN'",
              Long.class,
              familyId,
              user.id());
      if (count > 0) return "";
    }
    return " AND " + alias + "visibility <> 'PRIVATE'";
  }

  public PersonService(JdbcTemplate jdbcTemplate, FamilyService familyService) {
    this.jdbcTemplate = jdbcTemplate;
    this.familyService = familyService;
  }

  private void validateGender(Object gender) {
    if (gender != null && !List.of("MALE", "FEMALE", "UNKNOWN").contains(gender)) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "性别必须是 MALE, FEMALE 或 UNKNOWN");
    }
  }

  private void validateName(Object name) {
    if (!(name instanceof String)) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "成员必须是文字");
    }

    String personName = (String) name;

    if (personName.isBlank()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "成员姓名不能为空");
    }

    if (personName.codePointCount(0, personName.length()) > 120) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "成员姓名不能超过120个字符");
    }
  }

  private void validateDates(LocalDate birthDate, LocalDate deathDate) {
    if (birthDate != null && deathDate != null && deathDate.isBefore(birthDate)) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "去世时间不能早于出生日期");
    }
  }

  public List<Map<String, Object>> listPersonsByFamily(Long familyId, String name) {

    familyService.requireFamilyExists(familyId);

    String sql =
        "SELECT id, family_id, name, gender, birth_date, death_date, generation, "
            + "birth_place, biography, avatar_url, visibility, nickname, hometown "
            + "FROM person WHERE family_id = ?"
            + visibilityFilter("", familyId);

    if (name == null || name.isBlank()) {
      return jdbcTemplate.queryForList(sql + " ORDER BY generation, id", familyId);
    }

    return jdbcTemplate.queryForList(
        sql + " AND name LIKE ? ORDER BY generation, id", familyId, "%" + name.strip() + "%");
  }

  public Map<String, Object> getPerson(Long familyId, Long personId) {
    String sql =
        "SELECT id, family_id, name, gender, birth_date, death_date, generation, "
            + "birth_place, biography, avatar_url, visibility, nickname, hometown "
            + "From person WHERE family_id = ? AND id = ?"
            + visibilityFilter("", familyId);

    List<Map<String, Object>> persons = jdbcTemplate.queryForList(sql, familyId, personId);

    if (persons.isEmpty()) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "未找到该家族成员");
    }

    return persons.get(0);
  }

  @Transactional
  public void createPerson(Long familyId, PersonRequest request) {
    jdbcTemplate.queryForList("SELECT id FROM family WHERE id=? FOR UPDATE", familyId);
    validateGender(request.gender());
    validateName(request.name());
    validateDates(request.birth_date(), request.death_date());
    familyService.requireFamilyExists(familyId);

    String sql =
        "INSERT INTO person "
            + "(family_id, name, gender, birth_date, death_date, "
            + "generation, birth_place, biography, nickname, hometown, visibility, avatar_url) "
            + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

    jdbcTemplate.update(
        sql,
        familyId,
        request.name(),
        request.gender(),
        request.birth_date(),
        request.death_date(),
        request.generation(),
        request.birth_place(),
        request.biography(),
        request.nickname(),
        request.hometown(),
        request.visibility() == null ? "FAMILY" : request.visibility(),
        checkedAvatar(familyId, request.avatar_url()));
  }

  @Transactional
  public void updatePerson(Long familyId, Long personId, PersonRequest request) {
    jdbcTemplate.queryForList("SELECT id FROM family WHERE id=? FOR UPDATE", familyId);

    validateGender(request.gender());
    validateName(request.name());
    validateDates(request.birth_date(), request.death_date());

    String sql =
        "UPDATE person SET name = ?, gender = ?, "
            + "birth_date = ?, death_date = ?, generation = ?, "
            + "birth_place = ?, biography = ?, nickname = ?, hometown = ?, "
            + "visibility = COALESCE(?, visibility), avatar_url = ? "
            + "WHERE id = ? AND family_id = ?";

    int updatedRows =
        jdbcTemplate.update(
            sql,
            request.name(),
            request.gender(),
            request.birth_date(),
            request.death_date(),
            request.generation(),
            request.birth_place(),
            request.biography(),
            request.nickname(),
            request.hometown(),
            request.visibility(),
            checkedAvatar(familyId, request.avatar_url()),
            personId,
            familyId);

    if (updatedRows == 0) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "未找到该家族成员");
    }
  }

  @Transactional
  public void deletePerson(Long familyId, Long personId) {
    jdbcTemplate.queryForList("SELECT id FROM family WHERE id=? FOR UPDATE", familyId);
    Long references =
        jdbcTemplate.queryForObject(
            "SELECT (SELECT COUNT(*) FROM family_media WHERE family_id=? AND person_id=?) + (SELECT"
                + " COUNT(*) FROM family_event WHERE family_id=? AND person_id=?)",
            Long.class,
            familyId,
            personId,
            familyId,
            personId);
    if (references > 0) throw new ResponseStatusException(HttpStatus.CONFLICT, "请先解除该人物关联的事件或档案");

    String ancestorSql = "SELECT COUNT(*) FROM family " + "WHERE id = ? AND ancestor_person_id = ?";

    Integer ancestorCount =
        jdbcTemplate.queryForObject(ancestorSql, Integer.class, familyId, personId);

    if (ancestorCount != null && ancestorCount > 0) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "该成员是家族始祖，请先取消或更换始祖");
    }

    try {
      String sql = "DELETE FROM person WHERE id = ? AND family_id = ?";
      int deleteRows = jdbcTemplate.update(sql, personId, familyId);

      if (deleteRows == 0) {
        throw new ResponseStatusException(HttpStatus.NOT_FOUND, "未找到该家族成员");
      }
    } catch (DataIntegrityViolationException exception) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "该成员仍有相关资料，暂时不能删除", exception);
    }
  }

  public Map<String, Object> pagePersonsByFamily(Long familyId, int page, int size, String name) {
    return pagePersonsByFamily(familyId, page, size, name, null, null);
  }

  public Map<String, Object> pagePersonsByFamily(
      Long familyId, int page, int size, String name, Integer generation, Integer birthYear) {

    if (page < 1 || size < 1 || size > 100) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "页数必须大于等于1，每页数必须再1到100之间");
    }

    familyService.requireFamilyExists(familyId);

    long offset = ((long) page - 1) * size;

    String keyword = "";
    if (name != null) {
      keyword = name.strip();
    }
    String namePattern = "%" + keyword + "%";

    String fromSql =
        " FROM person WHERE family_id = ? AND name LIKE ?" + visibilityFilter("", familyId);
    java.util.List<Object> parameters =
        new java.util.ArrayList<>(java.util.List.of(familyId, namePattern));
    if (generation != null) {
      if (generation < 1) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "世代必须大于0");
      fromSql += " AND generation=?";
      parameters.add(generation);
    }
    if (birthYear != null) {
      if (birthYear < 1 || birthYear > 9999)
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "出生年份范围为1至9999");
      fromSql += " AND YEAR(birth_date)=?";
      parameters.add(birthYear);
    }

    Long total =
        jdbcTemplate.queryForObject("SELECT COUNT(*)" + fromSql, Long.class, parameters.toArray());

    String sql =
        "SELECT id, family_id, name, gender, birth_date, death_date, generation,"
            + "birth_place, biography, avatar_url, visibility, nickname, hometown"
            + fromSql
            + " ORDER BY generation, id LIMIT ? OFFSET ?";

    parameters.add(size);
    parameters.add(offset);
    List<Map<String, Object>> records = jdbcTemplate.queryForList(sql, parameters.toArray());

    return Map.of(
        "records", records,
        "total", total,
        "page", page,
        "size", size);
  }

  public List<Map<String, Object>> listDescendants(Long familyId, Long personId) {

    familyService.requireFamilyExists(familyId);
    getPerson(familyId, personId);

    String sql =
        """
WITH RECURSIVE descendants(person_id) AS (
    SELECT to_person_id AS person_id
    FROM person_relation
    WHERE family_id = ?
       AND from_person_id = ?
       AND relation_type IN ('PARENT_CHILD','ADOPTIVE_PARENT_CHILD') AND status='CONFIRMED'

    UNION DISTINCT

    SELECT r.to_person_id AS person_id
    FROM person_relation r
    JOIN descendants d ON r.from_person_id = d.person_id
    WHERE r.family_id = ?
       AND r.relation_type IN ('PARENT_CHILD','ADOPTIVE_PARENT_CHILD') AND r.status='CONFIRMED'
)
SELECT p.id, p.family_id, p.name, p.gender,
       p.birth_date, p.death_date, p.generation,
       p.birth_place, p.biography, p.avatar_url, p.visibility
FROM person p
JOIN descendants d ON p.id = d.person_id
WHERE p.family_id = ?
   AND p.id <> ?
ORDER BY p.generation, p.id
""";
    sql =
        sql.replace(
            "ORDER BY p.generation", visibilityFilter("p.", familyId) + " ORDER BY p.generation");

    return jdbcTemplate.queryForList(sql, familyId, personId, familyId, familyId, personId);
  }

  public List<Map<String, Object>> listAncestors(Long familyId, Long personId) {

    familyService.requireFamilyExists(familyId);
    getPerson(familyId, personId);

    String sql =
        """
WITH RECURSIVE ancestors(person_id) AS (
    SELECT from_person_id AS person_id
    FROM person_relation
    WHERE family_id = ?
       AND to_person_id = ?
       AND relation_type IN ('PARENT_CHILD','ADOPTIVE_PARENT_CHILD') AND status='CONFIRMED'

    UNION DISTINCT

    SELECT r.from_person_id AS person_id
    FROM person_relation r
    JOIN ancestors a ON r.to_person_id = a.person_id
    WHERE r.family_id = ?
       AND r.relation_type IN ('PARENT_CHILD','ADOPTIVE_PARENT_CHILD') AND r.status='CONFIRMED'
)
SELECT p.id, p.family_id, p.name, p.gender,
       p.birth_date, p.death_date, p.generation,
       p.birth_place, p.biography, p.avatar_url, p.visibility
FROM person p
JOIN ancestors a ON p.id = a.person_id
WHERE p.family_id = ?
   AND p.id <> ?
ORDER BY p.generation, p.id
""";
    sql =
        sql.replace(
            "ORDER BY p.generation", visibilityFilter("p.", familyId) + " ORDER BY p.generation");

    return jdbcTemplate.queryForList(sql, familyId, personId, familyId, familyId, personId);
  }

  private String checkedAvatar(Long familyId, String url) {
    if (url == null || url.isBlank()) return null;
    String prefix = "/api/families/" + familyId + "/media/";
    if (!url.matches(java.util.regex.Pattern.quote(prefix) + "[1-9][0-9]*/file"))
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "请使用本家族相册中的图片作为头像");
    String id = url.substring(prefix.length(), url.length() - 5);
    Long count =
        jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM family_media WHERE family_id=? AND id=? AND content_type LIKE"
                + " 'image/%'",
            Long.class, familyId, Long.valueOf(id));
    if (count == 0) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "头像图片不存在");
    return url;
  }
}
