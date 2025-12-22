package com.accounting.api;

import com.accounting.model.Budget;
import com.accounting.service.BudgetService;
import com.accounting.service.TransactionService;
import com.accounting.storage.StorageManager;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/budgets")
public class BudgetController {
    private final TransactionService transactionService = new TransactionService(new StorageManager());
    private final BudgetService budgetService = new BudgetService(new StorageManager(), transactionService);

    @GetMapping
    public ResponseEntity<List<Budget>> list(Authentication auth) {
        String user = auth != null ? auth.getName() : null;
        return ResponseEntity.ok(budgetService.getBudgetsByUserId(user));
    }

    @PostMapping
    public ResponseEntity<Budget> createOrSet(@RequestBody Map<String, Object> body, Authentication auth) {
        String user = auth != null ? auth.getName() : null;
        String categoryId = (String) body.getOrDefault("categoryId", null);
        double amount = Double.parseDouble(String.valueOf(body.get("amount")));
        int year = Integer.parseInt(String.valueOf(body.get("year")));
        int month = Integer.parseInt(String.valueOf(body.get("month")));
        Budget b = budgetService.setMonthlyBudget(user, categoryId, amount, year, month);
        return ResponseEntity.ok(b);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Budget> update(@PathVariable String id, @RequestBody Budget incoming, Authentication auth) {
        String user = auth != null ? auth.getName() : null;
        incoming.setUserId(user);
        Budget b = budgetService.updateBudget(id, incoming);
        return b != null ? ResponseEntity.ok(b) : ResponseEntity.notFound().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        boolean ok = budgetService.deleteBudget(id);
        return ok ? ResponseEntity.noContent().build() : ResponseEntity.notFound().build();
    }

    @GetMapping("/usage")
    public ResponseEntity<Map<String, Object>> usage(@RequestParam(required = false) String categoryId,
                                                     @RequestParam int year,
                                                     @RequestParam int month,
                                                     Authentication auth) {
        String user = auth != null ? auth.getName() : null;
        boolean over = budgetService.isOverBudget(user, categoryId, year, month);
        double overAmount = budgetService.getOverBudgetAmount(user, categoryId, year, month);
        double rate = budgetService.getBudgetUsageRate(user, categoryId, year, month);
        return ResponseEntity.ok(Map.of("over", over, "overAmount", overAmount, "rate", rate));
    }
}

