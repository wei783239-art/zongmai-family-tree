package com.zongmai.familytree;

import java.util.List;
import java.util.Map;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class RelationService {

  private final JdbcTemplate jdbcTemplate;
  private final FamilyService familyService;
  private final PersonService personService;

  private void validateNoParentChildCycle(Long familyId, Long parentPersonId, Long childPersonId) {

    String sql =
        """
        WITH RECURSIVE descendants(person_id) AS (
            SELECT to_person_id AS person_id
            FROM person_relation
            WHERE family_id = ?
             AND from_person_id = ?
             AND relation_type IN ('PARENT_CHILD', 'ADOPTIVE_PARENT_CHILD')

            UNION DISTINCT

            SELECT r.to_person_id AS person_id
            FROM person_relation r
            JOIN descendants d ON r.from_person_id = d.person_id
            WHERE r.family_id = ?
             AND r.relation_type IN ('PARENT_CHILD', 'ADOPTIVE_PARENT_CHILD')
        )
             SELECT COUNT(*)
             FROM descendants
             WHERE person_id = ?
        """;

    Integer count =
        jdbcTemplate.queryForObject(
            sql, Integer.class, familyId, childPersonId, familyId, parentPersonId);

    if (count != null && count > 0) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "该亲子关系会形成环路，不能添加");
    }
  }

  public RelationService(
      JdbcTemplate jdbcTemplate, FamilyService familyService, PersonService personService) {
    this.jdbcTemplate = jdbcTemplate;
    this.familyService = familyService;
    this.personService = personService;
  }

  public List<Map<String, Object>> listRelationsByFamily(Long familyId) {

    familyService.requireFamilyExists(familyId);

    String sql =
        """
        SELECT r.id,
               r.from_person_id,
               p_from.name AS from_person_name,
               r.to_person_id,
               p_to.name AS to_person_name,
               r.relation_type,
               r.status
        FROM person_relation r
        JOIN person p_from ON p_from.id = r.from_person_id
        JOIN person p_to ON p_to.id = r.to_person_id
        WHERE r.family_id = ?
        ORDER BY r.id
        """;

    var visible =
        personService.listPersonsByFamily(familyId, null).stream()
            .map(p -> ((Number) p.get("id")).longValue())
            .collect(java.util.stream.Collectors.toSet());
    return jdbcTemplate.queryForList(sql, familyId).stream()
        .filter(
            r ->
                visible.contains(((Number) r.get("from_person_id")).longValue())
                    && visible.contains(((Number) r.get("to_person_id")).longValue()))
        .toList();
  }

  @Transactional
  public void createParentChildRelation(Long familyId, ParentChildRelationRequset request) {
    createParentRelation(
        familyId, request.parentPersonId(), request.childPersonId(), "PARENT_CHILD");
  }

  @Transactional
  public void createParentRelation(
      Long familyId, Long parentPersonId, Long childPersonId, String kind) {
    if (!List.of("PARENT_CHILD", "ADOPTIVE_PARENT_CHILD").contains(kind)) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "亲子关系类型无效");
    }
    jdbcTemplate.queryForList("SELECT id FROM family WHERE id=? FOR UPDATE", familyId);

    if (parentPersonId == null || childPersonId == null) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "必须提供父母和子女编号");
    }

    if (parentPersonId.equals(childPersonId)) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "父母和子女不能是同一个人");
    }

    String peopleSql = "SELECT COUNT(*) FROM person " + "WHERE family_id = ? AND id IN (?, ?)";

    Integer personCount =
        jdbcTemplate.queryForObject(
            peopleSql, Integer.class, familyId, parentPersonId, childPersonId);

    if (personCount == null || personCount != 2) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "找不到属于该家族的父母或子女");
    }

    String duplicateSql =
        "SELECT COUNT(*) FROM person_relation "
            + "WHERE family_id = ? AND from_person_id = ? "
            + "AND to_person_id = ? AND relation_type = ?";

    Integer relationCount =
        jdbcTemplate.queryForObject(
            duplicateSql, Integer.class, familyId, parentPersonId, childPersonId, kind);

    if (relationCount != null && relationCount > 0) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "这条亲子关系已经存在");
    }

    validateNoParentChildCycle(familyId, parentPersonId, childPersonId);

    String insertSql =
        "INSERT INTO person_relation "
            + "(family_id, from_person_id, to_person_id, relation_type) "
            + "VALUES (?, ?, ?, ?)";

    jdbcTemplate.update(insertSql, familyId, parentPersonId, childPersonId, kind);
  }

  @Transactional
  public void deleteRelation(Long familyId, Long relationId) {
    jdbcTemplate.queryForList("SELECT id FROM family WHERE id=? FOR UPDATE", familyId);

    String sql = "DELETE FROM person_relation WHERE id = ? AND family_id = ?";
    int deletedRows = jdbcTemplate.update(sql, relationId, familyId);

    if (deletedRows == 0) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "未找到该亲属关系");
    }
  }

  @Transactional
  public void createSpouseRelation(Long familyId, SpouseRelationRequest request) {
    jdbcTemplate.queryForList("SELECT id FROM family WHERE id=? FOR UPDATE", familyId);

    Long personId = request.personId();
    Long spousePersonId = request.spousePersonId();

    if (personId == null || spousePersonId == null) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "必须提供双方成员编号");
    }

    if (personId.equals(spousePersonId)) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "配偶双方不能是同一个人");
    }

    familyService.requireFamilyExists(familyId);

    String peopleSql = "SELECT COUNT(*) FROM person " + "WHERE family_id = ? AND id IN (?, ?)";

    Integer personCount =
        jdbcTemplate.queryForObject(peopleSql, Integer.class, familyId, personId, spousePersonId);

    if (personCount == null || personCount != 2) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "找不到属于该家族的配偶双方成员");
    }

    Long firstPersonId = Math.min(personId, spousePersonId);
    Long secondPersonId = Math.max(personId, spousePersonId);

    String duplicateSql =
        "SELECT COUNT(*) FROM person_relation "
            + "WHERE family_id = ? AND from_person_id = ? "
            + "AND to_person_id = ? AND relation_type = 'SPOUSE'";

    Integer relationCount =
        jdbcTemplate.queryForObject(
            duplicateSql, Integer.class, familyId, firstPersonId, secondPersonId);

    if (relationCount != null && relationCount > 0) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "这对配偶关系已经存在");
    }

    String insertSql =
        "INSERT INTO person_relation "
            + "(family_id, from_person_id, to_person_id, relation_type) "
            + "VALUES (?, ?, ?, 'SPOUSE')";

    try {
      jdbcTemplate.update(insertSql, familyId, firstPersonId, secondPersonId);
    } catch (DuplicateKeyException exception) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "这对配偶关系已经存在", exception);
    }
  }
}
