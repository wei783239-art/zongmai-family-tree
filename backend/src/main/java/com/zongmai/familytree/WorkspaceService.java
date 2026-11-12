package com.zongmai.familytree;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Validator;
import java.time.LocalDate;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class WorkspaceService {
  private final JdbcTemplate db;
  private final ObjectMapper json;
  private final PersonService persons;
  private final RelationService relations;
  private final Validator validator;

  public WorkspaceService(
      JdbcTemplate db,
      ObjectMapper json,
      PersonService persons,
      RelationService relations,
      Validator validator) {
    this.db = db;
    this.json = json;
    this.persons = persons;
    this.relations = relations;
    this.validator = validator;
  }

  public Map<String, Object> access(Long familyId, Long userId) {
    var rows =
        db.queryForList(
            "SELECT role FROM family_user WHERE family_id=? AND user_id=?", familyId, userId);
    if (rows.isEmpty()) throw error(403, "无权访问该家族");
    String role = (String) rows.get(0).get("role");
    return Map.of("role", role, "canManage", "FAMILY_ADMIN".equals(role));
  }

  public List<Map<String, Object>> members(Long familyId) {
    return db.queryForList(
        "SELECT u.id,u.username,u.status,fu.role FROM family_user fu JOIN sys_user u ON"
            + " u.id=fu.user_id WHERE fu.family_id=? ORDER BY fu.id",
        familyId);
  }

  @Transactional
  public void saveMember(Long familyId, String username, String role) {
    checkRole(role);
    lockFamily(familyId);
    var users =
        db.queryForList("SELECT id FROM sys_user WHERE username=? AND status='ACTIVE'", username);
    if (users.isEmpty()) throw error(404, "未找到启用的账号，请先注册");
    Long userId = ((Number) users.get(0).get("id")).longValue();
    var existing =
        db.queryForList(
            "SELECT role FROM family_user WHERE family_id=? AND user_id=?", familyId, userId);
    if (existing.isEmpty()) {
      db.update(
          "INSERT INTO family_user(family_id,user_id,role) VALUES(?,?,?)", familyId, userId, role);
    } else {
      protectLastAdmin(familyId, userId, role);
      db.update(
          "UPDATE family_user SET role=? WHERE family_id=? AND user_id=?", role, familyId, userId);
    }
  }

  @Transactional
  public void removeMember(Long familyId, Long userId) {
    lockFamily(familyId);
    protectLastAdmin(familyId, userId, "REMOVED");
    if (db.update("DELETE FROM family_user WHERE family_id=? AND user_id=?", familyId, userId) == 0)
      throw error(404, "该账号未关联此家族");
  }

  private void protectLastAdmin(Long familyId, Long userId, String nextRole) {
    String current =
        db
            .queryForList(
                "SELECT role FROM family_user WHERE family_id=? AND user_id=?", familyId, userId)
            .stream()
            .map(row -> (String) row.get("role"))
            .findFirst()
            .orElse("");
    Long admins =
        db.queryForObject(
            "SELECT COUNT(*) FROM family_user fu JOIN sys_user u ON u.id=fu.user_id WHERE"
                + " fu.family_id=? AND fu.role='FAMILY_ADMIN' AND u.status='ACTIVE'",
            Long.class,
            familyId);
    if ("FAMILY_ADMIN".equals(current) && !"FAMILY_ADMIN".equals(nextRole) && admins <= 1)
      throw error(409, "必须保留至少一位启用的家族管理员");
  }

  private void checkRole(String role) {
    if (role == null || !List.of("MEMBER", "FAMILY_ADMIN").contains(role))
      throw error(400, "家族角色无效");
  }

  public void lockFamily(Long familyId) {
    if (db.queryForList("SELECT id FROM family WHERE id=? FOR UPDATE", familyId).isEmpty())
      throw error(404, "家族不存在");
  }

  public List<Map<String, Object>> events(Long familyId) {
    return db.queryForList(
        "SELECT e.*,p.name AS person_name FROM family_event e LEFT JOIN person p ON"
            + " p.id=e.person_id AND p.family_id=e.family_id WHERE e.family_id=? ORDER BY"
            + " e.event_date DESC,e.id DESC",
        familyId);
  }

  public record EventInput(
      String title, LocalDate event_date, String location, String content, Long person_id) {}

  @Transactional
  public void saveEvent(Long familyId, Long eventId, EventInput input) {
    lockFamily(familyId);
    checkText(input.title(), 160, "事件标题");
    if (input.location() != null && input.location().length() > 160) throw error(400, "地点过长");
    if (input.content() != null && input.content().length() > 20000) throw error(400, "事件内容过长");
    if (input.person_id() != null) persons.getPerson(familyId, input.person_id());
    if (eventId == null) {
      db.update(
          "INSERT INTO family_event(family_id,title,event_date,location,content,person_id)"
              + " VALUES(?,?,?,?,?,?)",
          familyId,
          input.title().strip(),
          input.event_date(),
          input.location(),
          input.content(),
          input.person_id());
    } else if (db.update(
            "UPDATE family_event SET title=?,event_date=?,location=?,content=?,person_id=? WHERE"
                + " family_id=? AND id=?",
            input.title().strip(),
            input.event_date(),
            input.location(),
            input.content(),
            input.person_id(),
            familyId,
            eventId)
        == 0) {
      throw error(404, "事件不存在");
    }
  }

  @Transactional
  public void deleteEvent(Long familyId, Long eventId) {
    lockFamily(familyId);
    db.update(
        "UPDATE family_media SET event_id=NULL WHERE family_id=? AND event_id=?",
        familyId,
        eventId);
    if (db.update("DELETE FROM family_event WHERE family_id=? AND id=?", familyId, eventId) == 0)
      throw error(404, "事件不存在");
  }

  public List<Map<String, Object>> requests(Long familyId, LoginUser user) {
    boolean admin = Boolean.TRUE.equals(access(familyId, user.id()).get("canManage"));
    String sql =
        "SELECT"
            + " c.id,c.kind,c.target_id,c.status,c.review_note,c.created_at,c.reviewed_at,c.payload_json,c.applicant_id,u.username"
            + " FROM change_request c JOIN sys_user u ON u.id=c.applicant_id WHERE c.family_id=?";
    return admin
        ? db.queryForList(sql + " ORDER BY c.id DESC", familyId)
        : db.queryForList(sql + " AND c.applicant_id=? ORDER BY c.id DESC", familyId, user.id());
  }

  public record ChangeInput(String kind, Long targetId, Map<String, Object> payload) {}

  @Transactional
  public void propose(Long familyId, LoginUser user, ChangeInput input) {
    if (input.payload() == null) throw error(400, "请提供修改内容");
    String original = null;
    switch (Objects.toString(input.kind(), "")) {
      case "PERSON_UPDATE" -> {
        original = encode(persons.getPerson(familyId, input.targetId()));
        validatePerson(input.payload());
      }
      case "PARENT_CHILD", "SPOUSE", "ADOPTIVE_PARENT_CHILD" -> {
        Long from = number(input.payload().get("fromPersonId"));
        Long to = number(input.payload().get("toPersonId"));
        persons.getPerson(familyId, from);
        persons.getPerson(familyId, to);
        if (from.equals(to)) throw error(400, "关系双方不能为同一人");
      }
      default -> throw error(400, "不支持的申请类型");
    }
    db.update(
        "INSERT INTO"
            + " change_request(family_id,applicant_id,kind,target_id,payload_json,original_json)"
            + " VALUES(?,?,?,?,?,?)",
        familyId,
        user.id(),
        input.kind(),
        input.targetId(),
        encode(input.payload()),
        original);
  }

  public record ReviewInput(Boolean approve, String note) {}

  @Transactional
  public void review(Long familyId, Long requestId, LoginUser user, ReviewInput input) {
    if (input.approve() == null) throw error(400, "请明确选择通过或驳回");
    lockFamily(familyId);
    var rows =
        db.queryForList(
            "SELECT * FROM change_request WHERE family_id=? AND id=? FOR UPDATE",
            familyId,
            requestId);
    if (rows.isEmpty()) throw error(404, "申请不存在");
    var request = rows.get(0);
    if (!"PENDING".equals(request.get("status"))) throw error(409, "申请已处理");
    if (input.note() != null && input.note().length() > 500) throw error(400, "审核意见不能超过500字");
    if (input.approve()) {
      Map<String, Object> payload = decode((String) request.get("payload_json"));
      String kind = (String) request.get("kind");
      if ("PERSON_UPDATE".equals(kind)) {
        Long target = ((Number) request.get("target_id")).longValue();
        if (!decode((String) request.get("original_json"))
            .equals(decode(encode(persons.getPerson(familyId, target)))))
          throw error(409, "人物资料已变化，请驳回后重新提交申请");
        persons.updatePerson(familyId, target, validatePerson(payload));
      } else if ("SPOUSE".equals(kind)) {
        relations.createSpouseRelation(
            familyId,
            new SpouseRelationRequest(
                number(payload.get("fromPersonId")), number(payload.get("toPersonId"))));
      } else {
        relations.createParentRelation(
            familyId, number(payload.get("fromPersonId")), number(payload.get("toPersonId")), kind);
      }
    }
    db.update(
        "UPDATE change_request SET"
            + " status=?,reviewer_id=?,review_note=?,reviewed_at=CURRENT_TIMESTAMP WHERE id=?",
        input.approve() ? "APPROVED" : "REJECTED",
        user.id(),
        input.note(),
        requestId);
  }

  private PersonRequest validatePerson(Map<String, Object> payload) {
    try {
      PersonRequest request = json.convertValue(payload, PersonRequest.class);
      var errors = validator.validate(request);
      if (!errors.isEmpty()) throw error(400, errors.iterator().next().getMessage());
      return request;
    } catch (IllegalArgumentException exception) {
      throw error(400, "人物资料格式无效");
    }
  }

  public String encode(Object value) {
    try {
      return json.writeValueAsString(value);
    } catch (JsonProcessingException exception) {
      throw new IllegalStateException(exception);
    }
  }

  @SuppressWarnings("unchecked")
  private Map<String, Object> decode(String value) {
    try {
      return json.readValue(value, Map.class);
    } catch (JsonProcessingException exception) {
      throw new IllegalStateException(exception);
    }
  }

  private Long number(Object value) {
    if (!(value instanceof Number number)
        || number.longValue() <= 0
        || number.doubleValue() != number.longValue()) throw error(400, "成员编号无效");
    return number.longValue();
  }

  public void checkText(String value, int max, String label) {
    if (value == null || value.isBlank() || value.codePointCount(0, value.length()) > max)
      throw error(400, label + "不能为空且不能超过" + max + "字");
  }

  public static ResponseStatusException error(int status, String message) {
    return new ResponseStatusException(HttpStatus.valueOf(status), message);
  }
}
