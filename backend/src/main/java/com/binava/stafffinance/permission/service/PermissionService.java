package com.binava.stafffinance.permission.service;

import com.binava.stafffinance.role.Role;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class PermissionService {

    public Map<String, List<String>> permissions() {
        Map<String, List<String>> permissions = new LinkedHashMap<>();
        permissions.put(Role.MEMBER.name(), List.of("Own profile", "Own savings", "Own loans", "Own statement"));
        permissions.put(Role.INITIATOR.name(), List.of("Members", "Prepare savings", "Prepare loans",
                "Prepare repayments", "Record income and expenses", "Submit transactions"));
        permissions.put(Role.APPROVER.name(), List.of("Review approvals", "Approve or reject transactions",
                "View financial reports"));
        permissions.put(Role.ADMIN.name(), List.of("All management reports", "Users and roles",
                "Categories and settings", "Audit logs"));
        return permissions;
    }
}
