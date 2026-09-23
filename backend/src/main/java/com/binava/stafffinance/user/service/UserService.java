package com.binava.stafffinance.user.service;

import com.binava.stafffinance.audit.service.AuditService;
import com.binava.stafffinance.auth.repository.TokenRepository;
import com.binava.stafffinance.auth.service.AuthService;
import com.binava.stafffinance.exception.BusinessException;
import com.binava.stafffinance.exception.NotFoundException;
import com.binava.stafffinance.member.entity.Member;
import com.binava.stafffinance.member.repository.MemberRepository;
import com.binava.stafffinance.role.Role;
import com.binava.stafffinance.user.dto.PasswordRequest;
import com.binava.stafffinance.user.dto.UserRequest;
import com.binava.stafffinance.user.dto.UserView;
import com.binava.stafffinance.user.entity.AppUser;
import com.binava.stafffinance.user.mapper.UserMapper;
import com.binava.stafffinance.user.repository.UserRepository;
import java.util.List;
import java.util.Locale;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {
    private final UserRepository users;
    private final TokenRepository tokens;
    private final MemberRepository members;
    private final PasswordEncoder passwords;
    private final AuthService auth;
    private final AuditService audit;

    public UserService(UserRepository users, TokenRepository tokens, MemberRepository members, PasswordEncoder passwords, AuthService auth, AuditService audit) {
        this.users = users;
        this.tokens = tokens;
        this.members = members;
        this.passwords = passwords;
        this.auth = auth;
        this.audit = audit;
    }

    @Transactional(readOnly = true)
    public List<UserView> users() {
        return users.findAll().stream()
                .sorted(java.util.Comparator.comparing(user -> user.fullName))
                .map(UserMapper::user).toList();
    }

    @Transactional
    public UserView createUser(UserRequest request) {
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
        return UserMapper.user(user);
    }

    @Transactional
    public UserView updateUser(Long id, UserRequest request) {
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
        return UserMapper.user(user);
    }

    @Transactional
    public void changePassword(Long id, PasswordRequest request) {
        AppUser user = requireUser(id);
        user.passwordHash = passwords.encode(request.password());
        tokens.deleteAllByUser(user);
        AppUser actor = auth.currentUser();
        audit.log(actor, "PASSWORD_CHANGE", "USER", user.id, user.username,
                null, null, "Password reset by administrator");
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
