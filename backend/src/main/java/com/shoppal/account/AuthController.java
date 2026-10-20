package com.shoppal.account;

import com.shoppal.product.BusinessProfile;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthService authService;
    private final ShopAccountRepository accounts;

    public AuthController(AuthService authService, ShopAccountRepository accounts) {
        this.authService = authService;
        this.accounts = accounts;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest req) {
        if (req == null) return ResponseEntity.badRequest().body(Map.of("message", "Invalid login request."));
        var res = authService.login(req.username(), req.password());
        return ResponseEntity.ok(res);
    }

    @PostMapping("/signup")
    public ResponseEntity<?> signup(@RequestBody SignupRequest req) {
        if (req == null) return ResponseEntity.badRequest().body(Map.of("message", "Invalid signup request."));
        var res = authService.signup(req.username(), req.password(), req.ownerName(), req.shopName(), req.businessProfile());
        return ResponseEntity.ok(res);
    }

    @GetMapping("/me")
    public ResponseEntity<?> me(@RequestHeader(value = "Authorization", required = false) String authHeader) {
        var opt = authService.resolveAccount(authHeader);
        if (opt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "Not authenticated"));
        }
        return ResponseEntity.ok(new MeResponse(AuthService.AccountDto.from(opt.get())));
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(@RequestHeader(value = "Authorization", required = false) String authHeader) {
        authService.logout(authHeader);
        return ResponseEntity.ok(Map.of("message", "Logged out successfully"));
    }

    @GetMapping("/demo-accounts")
    public List<DemoAccountInfo> demoAccounts() {
        return List.of(
            new DemoAccountInfo("kirana@shoppal.local", "Kirana@123", "Amit Sharma", "Sharma General Store", BusinessProfile.KIRANA),
            new DemoAccountInfo("pharmacy@shoppal.local", "Pharmacy@123", "Dr. Ramesh Verma", "Verma Medicos & Healthcare", BusinessProfile.PHARMACY),
            new DemoAccountInfo("footwear@shoppal.local", "Footwear@123", "Suresh Mehra", "Mehra Footwear Collection", BusinessProfile.FOOTWEAR)
        );
    }

    public record LoginRequest(String username, String password) {}
    public record SignupRequest(String username, String password, String ownerName, String shopName, BusinessProfile businessProfile) {}
    public record MeResponse(AuthService.AccountDto user) {}
    public record DemoAccountInfo(String username, String password, String ownerName, String shopName, BusinessProfile profile) {}
}
