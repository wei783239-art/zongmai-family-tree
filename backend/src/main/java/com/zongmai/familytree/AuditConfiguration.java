package com.zongmai.familytree;

import jakarta.servlet.http.*;
import java.util.regex.Pattern;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.*;

@Configuration
public class AuditConfiguration implements WebMvcConfigurer {
  private final JdbcTemplate db;
  private static final Pattern FAMILY = Pattern.compile("^/api/families/([1-9][0-9]*)(?:/.*)?$");

  public AuditConfiguration(JdbcTemplate db) {
    this.db = db;
  }

  @Override
  public void addInterceptors(InterceptorRegistry registry) {
    registry
        .addInterceptor(
            new HandlerInterceptor() {
              @Override
              public void afterCompletion(
                  HttpServletRequest request,
                  HttpServletResponse response,
                  Object handler,
                  Exception exception) {
                if (!java.util.List.of("POST", "PUT", "PATCH", "DELETE")
                        .contains(request.getMethod())
                    || response.getStatus() >= 400
                    || exception != null) return;
                var match = FAMILY.matcher(request.getRequestURI());
                var auth = SecurityContextHolder.getContext().getAuthentication();
                if (!match.matches()
                    || auth == null
                    || !(auth.getPrincipal() instanceof LoginUser user)) return;
                Long familyId = Long.valueOf(match.group(1));
                try {
                  if (db.queryForObject(
                          "SELECT COUNT(*) FROM family WHERE id=?", Long.class, familyId)
                      > 0)
                    db.update(
                        "INSERT INTO audit_log(family_id,operator_id,action,target)"
                            + " VALUES(?,?,?,?)",
                        familyId,
                        user.id(),
                        request.getMethod(),
                        request.getRequestURI());
                } catch (org.springframework.dao.DataAccessException failure) {
                  org.slf4j.LoggerFactory.getLogger(AuditConfiguration.class)
                      .error("操作日志写入失败 family={}", familyId, failure);
                }
              }
            })
        .addPathPatterns("/api/families/**");
  }
}
