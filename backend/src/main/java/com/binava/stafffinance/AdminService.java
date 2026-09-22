package com.binava.stafffinance;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
class AdminService {
    private final UserRepository users;
    private final TokenRepository tokens;
    private final MemberRepository members;
    private final FinanceCategoryRepository categories;
    private final SettingRepository settings;
    private final AuditLogRepository logs;
    private final PasswordEncoder passwords;
    private final AuthService auth;
    private final AuditService audit;

    AdminService(UserRepository users, TokenRepository tokens, MemberRepository members,
                 FinanceCategoryRepository categories, SettingRepository settings,
                 AuditLogRepository logs, PasswordEncoder passwords, AuthService auth,
                 AuditService audit) {
        this.users = users;
        this.tokens = tokens;
        this.members = members;
        this.categories = categories;
        this.settings = settings;
        this.logs = logs;
        this.passwords = passwords;
        this.auth = auth;
        this.audit = audit;
    }

    @Transactional(readOnly = true)
    List<UserView> users() {
        return users.findAll().stream()
                .sorted(java.util.Comparator.comparing(user -> user.fullName))
                .map(Views::user).toList();
    }

    @Transactional
    UserView createUser(UserRequest request) {
        if (users.existsByUsernameIgnoreCase(request.username().trim())) {
            throw new BusinessException("Username is already in use");
        }
        if (request.password() == null || request.password().length() < 8) {
            throw new BusinessException("A new user password must contain at least 8 characters");
        }
        AppUser user = new AppUser();
        applyUser(user, request, true);
        users.save(user);
        AppUser actor = auth.currentUser();
        audit.log(actor, "CREATE", "USER", user.id, user.username,
                null, user.role, "User account created");
        return Views.user(user);
    }

    @Transactional
    UserView updateUser(Long id, UserRequest request) {
        AppUser user = requireUser(id);
        users.findByUsernameIgnoreCase(request.username().trim())
                .filter(existing -> !existing.id.equals(id))
                .ifPresent(existing -> { throw new BusinessException("Username is already in use"); });
        AppUser actor = auth.currentUser();
        if (actor.id.equals(user.id) && Boolean.FALSE.equals(request.enabled())) {
            throw new BusinessException("You cannot disable your own account");
        }
        Role previous = user.role;
        applyUser(user, request, false);
        if (!user.enabled) tokens.deleteAllByUser(user);
        audit.log(actor, "UPDATE", "USER", user.id, user.username,
                previous, user.role, "User account updated; enabled=" + user.enabled);
        return Views.user(user);
    }

    @Transactional
    void changePassword(Long id, PasswordRequest request) {
        AppUser user = requireUser(id);
        user.passwordHash = passwords.encode(request.password());
        tokens.deleteAllByUser(user);
        AppUser actor = auth.currentUser();
        audit.log(actor, "PASSWORD_CHANGE", "USER", user.id, user.username,
                null, null, "Password reset by administrator");
    }

    @Transactional(readOnly = true)
    List<CategoryView> categories(CategoryType type, boolean activeOnly) {
        return categories.findAllByOrderByCategoryTypeAscNameAsc().stream()
                .filter(item -> type == null || item.categoryType == type)
                .filter(item -> !activeOnly || item.active)
                .map(item -> new CategoryView(item.id, item.name, item.categoryType, item.active))
                .toList();
    }

    @Transactional
    CategoryView createCategory(CategoryRequest request) {
        FinanceCategory category = new FinanceCategory();
        category.name = request.name().trim();
        category.categoryType = request.categoryType();
        category.active = request.active() == null || request.active();
        categories.save(category);
        AppUser actor = auth.currentUser();
        audit.log(actor, "CREATE", "CATEGORY", category.id, category.name,
                null, category.categoryType, "Finance category created");
        return new CategoryView(category.id, category.name, category.categoryType, category.active);
    }

    @Transactional
    CategoryView updateCategory(Long id, CategoryRequest request) {
        FinanceCategory category = categories.findById(id)
                .orElseThrow(() -> new NotFoundException("Category not found"));
        category.name = request.name().trim();
        category.categoryType = request.categoryType();
        category.active = request.active() == null || request.active();
        AppUser actor = auth.currentUser();
        audit.log(actor, "UPDATE", "CATEGORY", category.id, category.name,
                null, category.categoryType, "Finance category updated; active=" + category.active);
        return new CategoryView(category.id, category.name, category.categoryType, category.active);
    }

    @Transactional(readOnly = true)
    List<SettingView> settings() {
        return settings.findAllByOrderBySettingKeyAsc().stream()
                .map(item -> new SettingView(item.id, item.settingKey, item.settingValue, item.description))
                .toList();
    }

    @Transactional
    SettingView updateSetting(Long id, SettingRequest request) {
        SystemSetting setting = settings.findById(id)
                .orElseThrow(() -> new NotFoundException("Setting not found"));
        String previous = setting.settingValue;
        setting.settingValue = request.value().trim();
        AppUser actor = auth.currentUser();
        audit.log(actor, "UPDATE", "SETTING", setting.id, setting.settingKey,
                previous, setting.settingValue, "System setting updated");
        return new SettingView(setting.id, setting.settingKey, setting.settingValue, setting.description);
    }

    @Transactional(readOnly = true)
    List<AuditView> auditLogs() {
        return logs.findTop250ByOrderByCreatedAtDesc().stream()
                .map(item -> new AuditView(item.id, item.user == null ? "System" : item.user.fullName,
                        item.action, item.entityType, item.entityId, item.reference,
                        item.previousStatus, item.newStatus, item.details, item.createdAt))
                .toList();
    }

    Map<String, List<String>> permissions() {
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

    private AppUser requireUser(Long id) {
        return users.findById(id).orElseThrow(() -> new NotFoundException("User not found"));
    }

    private void applyUser(AppUser user, UserRequest request, boolean newUser) {
        user.username = request.username().trim().toLowerCase(Locale.ROOT);
        user.fullName = request.fullName().trim();
        user.email = request.email().trim().toLowerCase(Locale.ROOT);
        user.role = request.role();
        user.enabled = request.enabled() == null || request.enabled();
        user.member = request.memberId() == null ? null : members.findById(request.memberId())
                .orElseThrow(() -> new NotFoundException("Member not found"));
        if (user.role == Role.MEMBER && user.member == null) {
            throw new BusinessException("A Member user account must be linked to a member record");
        }
        if (request.password() != null && !request.password().isBlank()) {
            if (request.password().length() < 8) {
                throw new BusinessException("Password must contain at least 8 characters");
            }
            user.passwordHash = passwords.encode(request.password());
        } else if (newUser) {
            throw new BusinessException("Password is required");
        }
    }
}

