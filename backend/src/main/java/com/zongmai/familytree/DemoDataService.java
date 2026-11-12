package com.zongmai.familytree;

import java.time.LocalDate;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DemoDataService {
  private final JdbcTemplate db;
  private final FamilyService families;

  public DemoDataService(JdbcTemplate db, FamilyService families) {
    this.db = db;
    this.families = families;
  }

  @Transactional
  public Long create(Long userId) {
    Long familyId =
        families.createFamily(
            userId,
            new FamilyRequest(
                "林氏五代演示家族", "林", "积善堂", "虚构·清溪镇", "全部人物与故事均为虚构，仅用于课程演示。", "敦亲睦族，诚实向学。", false));
    List<Long[]> previous = new ArrayList<>();
    long ancestor = person(familyId, "林守诚", 1, 0, true);
    long partner = person(familyId, "陈淑宁", 1, 1, false);
    spouse(familyId, ancestor, partner);
    previous.add(new Long[] {ancestor, partner});
    int serial = 2;
    int[] children = {3, 4, 5, 10};
    for (int generation = 2; generation <= 5; generation++) {
      List<Long[]> next = new ArrayList<>();
      for (int child = 0; child < children[generation - 2]; child++) {
        boolean male = child % 2 == 0;
        long id =
            person(
                familyId,
                "林"
                    + new String[] {"承", "景", "文", "知"}[generation - 2]
                    + new String[] {"安", "宁", "远", "和", "清", "明", "悦", "新", "澄", "初"}[child],
                generation,
                serial++,
                male);
        Long[] parents = previous.get(child % previous.size());
        parent(familyId, parents[0], id);
        parent(familyId, parents[1], id);
        if (generation < 5) {
          long mate =
              person(
                  familyId,
                  (male ? "周" : "许")
                      + new String[] {"若", "思", "云", "书", "晓"}[child]
                      + (male ? "兰" : "川"),
                  generation,
                  serial++,
                  !male);
          spouse(familyId, id, mate);
          next.add(new Long[] {id, mate});
        }
      }
      previous = next;
    }
    db.update("UPDATE family SET ancestor_person_id=? WHERE id=?", ancestor, familyId);
    db.update(
        "INSERT INTO family_event(family_id,title,event_date,location,content,person_id)"
            + " VALUES(?,?,?,?,?,?)",
        familyId,
        "始迁清溪",
        LocalDate.of(1928, 3, 12),
        "虚构·清溪镇",
        "演示事件，记录家族迁居的虚构故事。",
        ancestor);
    db.update(
        "INSERT INTO family_event(family_id,title,event_date,location,content) VALUES(?,?,?,?,?)",
        familyId,
        "家族修谱",
        LocalDate.of(2026, 10, 1),
        "虚构·积善堂",
        "汇集五代档案并建立电子族谱，所有资料均为虚构。");
    return familyId;
  }

  private long person(Long familyId, String name, int generation, int serial, boolean male) {
    var key = new GeneratedKeyHolder();
    db.update(
        connection -> {
          var statement =
              connection.prepareStatement(
                  "INSERT INTO"
                      + " person(family_id,name,gender,generation,birth_date,death_date,birth_place,hometown,nickname,biography)"
                      + " VALUES(?,?,?,?,?,?,?,?,?,?)",
                  new String[] {"id"});
          statement.setLong(1, familyId);
          statement.setString(2, name);
          statement.setString(3, male ? "MALE" : "FEMALE");
          statement.setInt(4, generation);
          statement.setObject(
              5,
              LocalDate.of(
                  1888 + (generation - 1) * 27 + serial % 7, serial % 12 + 1, serial % 25 + 1));
          statement.setObject(6, generation == 1 ? LocalDate.of(1968 + serial, 6, 15) : null);
          statement.setString(7, "虚构·清溪镇");
          statement.setString(8, "虚构·江南");
          statement.setString(9, new String[] {"守", "承", "景", "文", "知"}[generation - 1]);
          statement.setString(10, "虚构人物，用于电子族谱教学与验收。");
          return statement;
        },
        key);
    return Objects.requireNonNull(key.getKey()).longValue();
  }

  private void parent(Long familyId, long from, long to) {
    db.update(
        "INSERT INTO person_relation(family_id,from_person_id,to_person_id,relation_type)"
            + " VALUES(?,?,?,'PARENT_CHILD')",
        familyId,
        from,
        to);
  }

  private void spouse(Long familyId, long from, long to) {
    db.update(
        "INSERT INTO person_relation(family_id,from_person_id,to_person_id,relation_type)"
            + " VALUES(?,?,?,'SPOUSE')",
        familyId,
        Math.min(from, to),
        Math.max(from, to));
  }
}
