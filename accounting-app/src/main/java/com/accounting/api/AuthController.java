package com.accounting.api;

import com.accounting.service.UserService;
import com.accounting.storage.StorageManager;
import com.accounting.util.JwtUtil;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final UserService userService;
    private final JwtUtil jwtUtil;
    public AuthController(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
        this.userService = new UserService(new StorageManager());
    }
    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody Map<String, String> body) {
        String username = body.get("username");
        String email = body.getOrDefault("email", "");
        String password = body.get("password");
        if (username == null || username.trim().isEmpty() || password == null || password.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "invalid_input"));
        }
        try {
            var u = userService.register(username.trim(), email, password);
            return ResponseEntity.ok(Map.of("id", u.getId(), "username", u.getUsername()));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(409).body(Map.of("error", "username_exists"));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", "server_error"));
        }
    }
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> body) {
        String username = body.get("username");
        String password = body.get("password");
        if (username == null || username.trim().isEmpty() || password == null || password.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "invalid_input"));
        }
        var opt = userService.login(username, password);
        if (opt.isEmpty()) return ResponseEntity.status(401).build();
        String token = jwtUtil.generate(opt.get().getUsername());
        return ResponseEntity.ok(Map.of("token", token));
    }
}
