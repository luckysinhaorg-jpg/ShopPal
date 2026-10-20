package com.shoppal.account;

import com.shoppal.product.BusinessProfile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class AuthService {
    private final ShopAccountRepository accounts;
    private final Map<String, SessionInfo> activeSessions = new ConcurrentHashMap<>();

    public AuthService(ShopAccountRepository accounts) {
        this.accounts = accounts;
    }

    @Transactional
    public AuthResponse login(String usernameOrEmail, String password) {
        if (usernameOrEmail == null || usernameOrEmail.isBlank() || password == null || password.isBlank()) {
            throw new IllegalArgumentException("Username/email and password are required.");
        }
        ShopAccount account = accounts.findByUsernameIgnoreCase(usernameOrEmail.trim())
                .orElseThrow(() -> new IllegalArgumentException("Invalid username or password."));

        if (!PasswordSecurity.verify(password, account.getPasswordHash())) {
            throw new IllegalArgumentException("Invalid username or password.");
        }

        String token = UUID.randomUUID().toString();
        activeSessions.put(token, new SessionInfo(account.getId(), Instant.now()));
        return new AuthResponse(token, AccountDto.from(account));
    }

    @Transactional
    public AuthResponse signup(String username, String password, String ownerName, String shopName, BusinessProfile profile) {
        if (username == null || username.trim().length() < 3) {
            throw new IllegalArgumentException("Username must be at least 3 characters.");
        }
        if (password == null || password.length() < 6) {
            throw new IllegalArgumentException("Password must be at least 6 characters.");
        }
        if (ownerName == null || ownerName.isBlank()) {
            throw new IllegalArgumentException("Owner name is required.");
        }
        if (shopName == null || shopName.isBlank()) {
            throw new IllegalArgumentException("Shop name is required.");
        }
        if (profile == null) {
            throw new IllegalArgumentException("Shop type is required.");
        }

        String normalized = username.trim().toLowerCase(java.util.Locale.ROOT);
        if (accounts.existsByUsernameIgnoreCase(normalized)) {
            throw new IllegalArgumentException("An account with this username/email already exists.");
        }

        String passwordHash = PasswordSecurity.hash(password);
        ShopAccount account = accounts.save(new ShopAccount(normalized, passwordHash, ownerName, shopName, profile));

        String token = UUID.randomUUID().toString();
        activeSessions.put(token, new SessionInfo(account.getId(), Instant.now()));
        return new AuthResponse(token, AccountDto.from(account));
    }

    public Optional<ShopAccount> resolveAccount(String authHeader) {
        if (authHeader == null || authHeader.isBlank()) {
            return Optional.empty();
        }
        String token = authHeader.startsWith("Bearer ") ? authHeader.substring(7).trim() : authHeader.trim();
        SessionInfo session = activeSessions.get(token);
        if (session == null) {
            return Optional.empty();
        }
        return accounts.findById(session.accountId());
    }

    public void logout(String authHeader) {
        if (authHeader == null || authHeader.isBlank()) return;
        String token = authHeader.startsWith("Bearer ") ? authHeader.substring(7).trim() : authHeader.trim();
        activeSessions.remove(token);
    }

    public record SessionInfo(Long accountId, Instant createdAt) {}
    public record AccountDto(Long id, String username, String ownerName, String shopName, BusinessProfile businessProfile) {
        public static AccountDto from(ShopAccount account) {
            return new AccountDto(account.getId(), account.getUsername(), account.getOwnerName(), account.getShopName(), account.getBusinessProfile());
        }
    }
    public record AuthResponse(String token, AccountDto user) {}
}
