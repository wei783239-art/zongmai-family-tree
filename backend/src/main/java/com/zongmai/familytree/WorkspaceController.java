package com.zongmai.familytree;

import static com.zongmai.familytree.WorkspaceService.error;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;
import javax.imageio.ImageIO;
import org.springframework.http.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/families/{familyId}")
@PreAuthorize("@familyAccess.canRead(#familyId, authentication)")
public class WorkspaceController {
  private final WorkspaceService workspace;
  private final JdbcTemplate db;
  private final PersonService persons;
  private final RelationService relations;
  private final FamilyService families;

  public WorkspaceController(
      WorkspaceService workspace,
      JdbcTemplate db,
      PersonService persons,
      RelationService relations,
      FamilyService families) {
    this.workspace = workspace;
    this.db = db;
    this.persons = persons;
    this.relations = relations;
    this.families = families;
  }

  @GetMapping("/access")
  public Map<String, Object> access(
      @PathVariable Long familyId, @AuthenticationPrincipal LoginUser user) {
    return workspace.access(familyId, user.id());
  }

  @GetMapping("/tree")
  public Map<String, Object> tree(@PathVariable Long familyId) {
    return Map.of(
        "family",
        families.getFamily(familyId),
        "persons",
        persons.listPersonsByFamily(familyId, null),
        "relations",
        relations.listRelationsByFamily(familyId));
  }

  @GetMapping("/stats")
  public Map<String, Object> stats(@PathVariable Long familyId) {
    var people = persons.listPersonsByFamily(familyId, null);
    var generations = new TreeMap<Integer, Long>();
    for (var person : people)
      generations.merge(
          person.get("generation") == null ? 0 : ((Number) person.get("generation")).intValue(),
          1L,
          Long::sum);
    return Map.of(
        "total",
        people.size(),
        "withoutDeathDate",
        people.stream().filter(p -> p.get("death_date") == null).count(),
        "relations",
        relations.listRelationsByFamily(familyId).size(),
        "generations",
        generations.entrySet().stream()
            .map(e -> Map.of("generation", e.getKey(), "count", e.getValue()))
            .toList(),
        "events",
        events(familyId).size());
  }

  @GetMapping("/members")
  @PreAuthorize("@familyAccess.canManage(#familyId, authentication)")
  public List<Map<String, Object>> members(@PathVariable Long familyId) {
    return workspace.members(familyId);
  }

  public record MemberInput(String username, String role) {}

  @PutMapping("/members")
  @PreAuthorize("@familyAccess.canManage(#familyId, authentication)")
  public Map<String, String> saveMember(
      @PathVariable Long familyId, @RequestBody MemberInput input) {
    workspace.checkText(input.username(), 64, "用户名");
    workspace.saveMember(familyId, input.username(), input.role());
    return Map.of("message", "家族账号已更新");
  }

  @DeleteMapping("/members/{userId}")
  @PreAuthorize("@familyAccess.canManage(#familyId, authentication)")
  public void removeMember(@PathVariable Long familyId, @PathVariable Long userId) {
    workspace.removeMember(familyId, userId);
  }

  @GetMapping("/events")
  public List<Map<String, Object>> events(@PathVariable Long familyId) {
    Set<Long> visible = new HashSet<>();
    persons
        .listPersonsByFamily(familyId, null)
        .forEach(p -> visible.add(((Number) p.get("id")).longValue()));
    return workspace.events(familyId).stream()
        .filter(
            e ->
                e.get("person_id") == null
                    || visible.contains(((Number) e.get("person_id")).longValue()))
        .toList();
  }

  @PostMapping("/events")
  @PreAuthorize("@familyAccess.canManage(#familyId, authentication)")
  public void createEvent(
      @PathVariable Long familyId, @RequestBody WorkspaceService.EventInput input) {
    workspace.saveEvent(familyId, null, input);
  }

  @PutMapping("/events/{eventId}")
  @PreAuthorize("@familyAccess.canManage(#familyId, authentication)")
  public void updateEvent(
      @PathVariable Long familyId,
      @PathVariable Long eventId,
      @RequestBody WorkspaceService.EventInput input) {
    workspace.saveEvent(familyId, eventId, input);
  }

  @DeleteMapping("/events/{eventId}")
  @PreAuthorize("@familyAccess.canManage(#familyId, authentication)")
  public void deleteEvent(@PathVariable Long familyId, @PathVariable Long eventId) {
    workspace.deleteEvent(familyId, eventId);
  }

  @GetMapping("/requests")
  public List<Map<String, Object>> requests(
      @PathVariable Long familyId, @AuthenticationPrincipal LoginUser user) {
    return workspace.requests(familyId, user);
  }

  @PostMapping("/requests")
  public void propose(
      @PathVariable Long familyId,
      @AuthenticationPrincipal LoginUser user,
      @RequestBody WorkspaceService.ChangeInput input) {
    workspace.propose(familyId, user, input);
  }

  @PostMapping("/requests/{requestId}/review")
  @PreAuthorize("@familyAccess.canManage(#familyId, authentication)")
  public void review(
      @PathVariable Long familyId,
      @PathVariable Long requestId,
      @AuthenticationPrincipal LoginUser user,
      @RequestBody WorkspaceService.ReviewInput input) {
    workspace.review(familyId, requestId, user, input);
  }

  @PostMapping("/relations/adoptive")
  @PreAuthorize("@familyAccess.canManage(#familyId, authentication)")
  public void adoptive(@PathVariable Long familyId, @RequestBody ParentChildRelationRequset input) {
    relations.createParentRelation(
        familyId, input.parentPersonId(), input.childPersonId(), "ADOPTIVE_PARENT_CHILD");
  }

  @GetMapping("/audit")
  @PreAuthorize("@familyAccess.canManage(#familyId, authentication)")
  public List<Map<String, Object>> audit(@PathVariable Long familyId) {
    return db.queryForList(
        "SELECT a.*,u.username FROM audit_log a LEFT JOIN sys_user u ON u.id=a.operator_id WHERE"
            + " family_id=? ORDER BY a.id DESC LIMIT 500",
        familyId);
  }

  @GetMapping("/media")
  public List<Map<String, Object>> media(
      @PathVariable Long familyId, @AuthenticationPrincipal LoginUser user) {
    boolean admin = Boolean.TRUE.equals(workspace.access(familyId, user.id()).get("canManage"));
    Set<Long> visibleEvents =
        events(familyId).stream()
            .map(e -> ((Number) e.get("id")).longValue())
            .collect(java.util.stream.Collectors.toSet());
    Set<Long> visible = new HashSet<>();
    persons
        .listPersonsByFamily(familyId, null)
        .forEach(p -> visible.add(((Number) p.get("id")).longValue()));
    return db
        .queryForList(
            "SELECT"
                + " id,family_id,title,person_id,event_id,file_name,content_type,visibility,created_at"
                + " FROM family_media WHERE family_id=?"
                + (admin ? "" : " AND visibility<>'PRIVATE'")
                + " ORDER BY id DESC",
            familyId)
        .stream()
        .filter(
            m ->
                m.get("person_id") == null
                    || visible.contains(((Number) m.get("person_id")).longValue()))
        .filter(
            m ->
                m.get("event_id") == null
                    || visibleEvents.contains(((Number) m.get("event_id")).longValue()))
        .toList();
  }

  @PostMapping("/media")
  @PreAuthorize("@familyAccess.canManage(#familyId, authentication)")
  @Transactional
  public void upload(
      @PathVariable Long familyId,
      @RequestParam MultipartFile file,
      @RequestParam String title,
      @RequestParam(required = false) Long personId,
      @RequestParam(required = false) Long eventId,
      @RequestParam(defaultValue = "FAMILY") String visibility)
      throws java.io.IOException {
    workspace.lockFamily(familyId);
    workspace.checkText(title, 160, "档案标题");
    if (!List.of("FAMILY", "PRIVATE").contains(visibility)) throw error(400, "可见范围无效");
    if (file.isEmpty() || file.getSize() > 8 * 1024 * 1024) throw error(400, "文件大小须为1字节至8MB");
    if (personId != null) persons.getPerson(familyId, personId);
    if (eventId != null
        && db.queryForObject(
                "SELECT COUNT(*) FROM family_event WHERE id=? AND family_id=?",
                Long.class,
                eventId,
                familyId)
            == 0) throw error(404, "事件不存在");
    byte[] bytes = file.getBytes();
    String type = detectType(bytes);
    if (!"application/pdf".equals(type) && ImageIO.read(new ByteArrayInputStream(bytes)) == null)
      throw error(400, "图片内容无效");
    String name = Objects.toString(file.getOriginalFilename(), "archive").replace('\\', '/');
    name = name.substring(name.lastIndexOf('/') + 1).replaceAll("[\\p{Cntrl}]", "_");
    if (name.length() > 255) name = name.substring(name.length() - 255);
    db.update(
        "INSERT INTO"
            + " family_media(family_id,title,person_id,event_id,file_name,content_type,file_data,visibility)"
            + " VALUES(?,?,?,?,?,?,?,?)",
        familyId,
        title.strip(),
        personId,
        eventId,
        name,
        type,
        bytes,
        visibility);
  }

  private String detectType(byte[] b) {
    if (b.length >= 8 && b[0] == (byte) 0x89 && b[1] == 80 && b[2] == 78 && b[3] == 71)
      return "image/png";
    if (b.length >= 3 && b[0] == (byte) 0xff && b[1] == (byte) 0xd8 && b[2] == (byte) 0xff)
      return "image/jpeg";
    if (b.length >= 6 && new String(b, 0, 6, StandardCharsets.US_ASCII).matches("GIF8[79]a"))
      return "image/gif";
    if (b.length >= 5 && new String(b, 0, 5, StandardCharsets.US_ASCII).equals("%PDF-"))
      return "application/pdf";
    throw error(400, "支持有效的 PNG、JPEG、GIF 图片和 PDF 文献");
  }

  @GetMapping("/media/{mediaId}/file")
  public ResponseEntity<byte[]> file(
      @PathVariable Long familyId,
      @PathVariable Long mediaId,
      @AuthenticationPrincipal LoginUser user) {
    boolean allowed =
        media(familyId, user).stream().anyMatch(m -> ((Number) m.get("id")).longValue() == mediaId);
    if (!allowed) throw error(404, "档案不存在或不可见");
    var row =
        db.queryForMap(
            "SELECT file_data,file_name,content_type FROM family_media WHERE id=? AND family_id=?",
            mediaId,
            familyId);
    String type = (String) row.get("content_type");
    var disposition =
        (type.startsWith("image/") ? ContentDisposition.inline() : ContentDisposition.attachment())
            .filename((String) row.get("file_name"), StandardCharsets.UTF_8)
            .build();
    return ResponseEntity.ok()
        .contentType(MediaType.parseMediaType(type))
        .header("Content-Disposition", disposition.toString())
        .header("X-Content-Type-Options", "nosniff")
        .header("Cache-Control", "no-store")
        .body((byte[]) row.get("file_data"));
  }

  @DeleteMapping("/media/{mediaId}")
  @PreAuthorize("@familyAccess.canManage(#familyId, authentication)")
  @Transactional
  public void deleteMedia(@PathVariable Long familyId, @PathVariable Long mediaId) {
    workspace.lockFamily(familyId);
    db.update(
        "UPDATE person SET avatar_url=NULL WHERE family_id=? AND avatar_url=?",
        familyId,
        "/api/families/" + familyId + "/media/" + mediaId + "/file");
    if (db.update("DELETE FROM family_media WHERE id=? AND family_id=?", mediaId, familyId) == 0)
      throw error(404, "档案不存在");
  }

  @GetMapping("/export")
  @PreAuthorize("@familyAccess.canManage(#familyId, authentication)")
  @Transactional(readOnly = true)
  public ResponseEntity<byte[]> export(
      @PathVariable Long familyId,
      @RequestParam(defaultValue = "json") String format,
      @AuthenticationPrincipal LoginUser user) {
    String result;
    MediaType type;
    if ("csv".equals(format)) {
      StringBuilder csv = new StringBuilder("\ufeff编号,姓名,性别,世代,出生日期,逝世日期,出生地,籍贯,字辈,介绍\r\n");
      for (var person : persons.listPersonsByFamily(familyId, null)) {
        csv.append(
                List.of(
                        "id",
                        "name",
                        "gender",
                        "generation",
                        "birth_date",
                        "death_date",
                        "birth_place",
                        "hometown",
                        "nickname",
                        "biography")
                    .stream()
                    .map(key -> csvCell(person.get(key)))
                    .collect(java.util.stream.Collectors.joining(",")))
            .append("\r\n");
      }
      result = csv.toString();
      type = MediaType.parseMediaType("text/csv;charset=UTF-8");
    } else if ("json".equals(format)) {
      result =
          workspace.encode(
              Map.of(
                  "schemaVersion",
                  1,
                  "exportedAt",
                  java.time.Instant.now().toString(),
                  "family",
                  families.getFamily(familyId),
                  "persons",
                  persons.listPersonsByFamily(familyId, null),
                  "relations",
                  relations.listRelationsByFamily(familyId),
                  "events",
                  workspace.events(familyId),
                  "media",
                  db.queryForList("SELECT * FROM family_media WHERE family_id=?", familyId)));
      type = MediaType.APPLICATION_JSON;
    } else throw error(400, "支持 json 或 csv 格式");
    return ResponseEntity.ok()
        .contentType(type)
        .header("Content-Disposition", "attachment; filename=family-" + familyId + "." + format)
        .header("Cache-Control", "no-store")
        .body(result.getBytes(StandardCharsets.UTF_8));
  }

  private String csvCell(Object value) {
    String text = Objects.toString(value, "");
    if (text.stripLeading().matches("(?s)^[=+@\\-].*")) text = "'" + text;
    return "\"" + text.replace("\"", "\"\"") + "\"";
  }
}
