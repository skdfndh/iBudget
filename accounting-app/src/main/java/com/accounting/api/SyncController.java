package com.accounting.api;

import com.accounting.model.Transaction;
import com.accounting.service.SyncService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/sync")
public class SyncController {

    private final SyncService syncService;

    @Autowired
    public SyncController(SyncService syncService) {
        this.syncService = syncService;
    }

    @GetMapping
    public ResponseEntity<Map<String, Object>> pull(
            @RequestParam(name = "last_version", defaultValue = "0") Long lastVersion,
            Authentication auth) {
        String userId = auth != null ? auth.getName() : null;
        return ResponseEntity.ok(syncService.pull(userId, lastVersion));
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> push(
            @RequestBody List<Transaction> incoming,
            Authentication auth) {
        String userId = auth != null ? auth.getName() : null;
        return ResponseEntity.ok(syncService.push(userId, incoming));
    }
}
