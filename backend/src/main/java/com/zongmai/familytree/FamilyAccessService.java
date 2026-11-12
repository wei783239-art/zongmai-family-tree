package com.zongmai.familytree;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

@Service("familyAccess")
public class FamilyAccessService {

    private final JdbcTemplate jdbcTemplate;

    public FamilyAccessService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public boolean canRead(Long familyId, Authentication authentication) {
        return hasAccess(familyId, authentication, false);
    }

    public boolean canManage(Long familyId, Authentication authentication) {
        return hasAccess(familyId, authentication, true);
    }

    private boolean hasAccess(Long familyId, Authentication authentication, boolean requireAdmin) {

        if (authentication == null 
                        || !authentication.isAuthenticated()
                        || !(authentication.getPrincipal() instanceof LoginUser user)) {
                    return false;
        }

        String sql = "SELECT COUNT(*) FROM family_user fu "
                   + "JOIN sys_user u ON u.id = fu.user_id "
                   + "WHERE fu.family_id = ? AND fu.user_id = ? "
                   + "AND u.status = 'ACTIVE'";

        if (requireAdmin) {
            sql += " AND fu.role = 'FAMILY_ADMIN'";
        }

        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, familyId, user.id());

        return count != null && count > 0;
    }

    public boolean isActive(Authentication authentication) {

        if (authentication == null
                       || !authentication.isAuthenticated()
                       || !(authentication.getPrincipal() instanceof LoginUser user)) {
            return false;
        }

        String sql = "SELECT COUNT(*) FROM sys_user "
                   + "WHERE id = ? AND status = 'ACTIVE'";

        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, user.id());

        return  count != null && count > 0;
    }
}