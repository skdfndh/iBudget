package com.accounting.controller;

import com.accounting.model.Budget;
import com.accounting.service.AIAnalysisService;
import com.accounting.service.BudgetService;
import com.accounting.service.StatisticService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/ai")
public class AIAnalysisController {
    
    @Autowired
    private AIAnalysisService aiService;
    
    @Autowired
    private BudgetService budgetService;
    
    @Autowired
    private StatisticService statisticService;
    
    @PostMapping("/analyze")
    public ResponseEntity<Map<String, String>> analyzeSpending(
            @RequestParam String userId,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month) {
        
        try {
            // 使用当前年月如果未提供
            int targetYear = year != null ? year : LocalDate.now().getYear();
            int targetMonth = month != null ? month : LocalDate.now().getMonthValue();
            YearMonth yearMonth = YearMonth.of(targetYear, targetMonth);
            
            // 收集预算数据
            Map<String, Object> budgetData = new HashMap<>();
            
            // 获取预算信息
            Budget budget = budgetService.getTotalBudget(userId, targetYear, targetMonth);
            if (budget != null && budget.getAmount() > 0) {
                budgetData.put("monthlyBudget", budget.getAmount());
                double usedAmount = budgetService.calculateUsedAmount(userId, null, targetYear, targetMonth);
                budgetData.put("usedAmount", usedAmount);
                budgetData.put("remainingBudget", budget.getAmount() - usedAmount);
                budgetData.put("isOverBudget", budgetService.isOverBudget(userId, null, targetYear, targetMonth));
            }
            
            // 获取统计数据
            Map<String, Object> stats = statisticService.getMonthlyStatistics(userId, targetYear, targetMonth);
            budgetData.putAll(stats);
            
            // 获取分类支出
            Map<String, Double> categoryExpenses = statisticService.getExpensesByCategory(userId, yearMonth);
            if (categoryExpenses != null && !categoryExpenses.isEmpty()) {
                budgetData.put("categoryExpenses", categoryExpenses);
            }
            
            // 获取月度趋势
            Map<YearMonth, Double> monthlyExpenses = statisticService.getMonthlyExpenses(userId, 6);
            if (monthlyExpenses != null && !monthlyExpenses.isEmpty()) {
                budgetData.put("monthlyTrend", monthlyExpenses.values().toString());
            }
            
            // 调用AI分析
            String analysis = aiService.analyzeSpending(budgetData);
            
            Map<String, String> response = new HashMap<>();
            response.put("analysis", analysis);
            response.put("status", "success");
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("analysis", "AI分析服务暂时不可用");
            errorResponse.put("status", "error");
            errorResponse.put("error", e.getMessage());
            return ResponseEntity.status(500).body(errorResponse);
        }
    }
}
