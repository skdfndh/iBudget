package com.accounting.service;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.accounting.model.Budget;
import com.accounting.model.Transaction;
import com.accounting.repository.BudgetRepository;

/**
 * 预算服务类
 * 提供预算的增删改查和超额提醒功能
 */
@Service
@Transactional
public class BudgetService {
    private final BudgetRepository budgetRepository;
    private final TransactionService transactionService;
    
    public BudgetService(BudgetRepository budgetRepository, TransactionService transactionService) {
        this.budgetRepository = budgetRepository;
        this.transactionService = transactionService;
    }
    
    /**
     * 添加预算
     */
    public Budget addBudget(Budget budget) {
        if (budget.getId() == null || budget.getId().isEmpty()) {
            budget.setId(UUID.randomUUID().toString());
        }
        return budgetRepository.save(budget);
    }
    
    /**
     * 删除预算
     */
    public boolean deleteBudget(String budgetId) {
        if (budgetRepository.existsById(budgetId)) {
            budgetRepository.deleteById(budgetId);
            return true;
        }
        return false;
    }
    
    /**
     * 更新预算
     */
    public Budget updateBudget(String budgetId, Budget updatedBudget) {
        return budgetRepository.findById(budgetId).map(existing -> {
            updatedBudget.setId(budgetId);
            return budgetRepository.save(updatedBudget);
        }).orElse(null);
    }
    
    /**
     * 根据ID查询预算
     */
    public Budget getBudgetById(String budgetId) {
        return budgetRepository.findById(budgetId).orElse(null);
    }
    
    /**
     * 获取指定月份的总预算
     */
    public Budget getTotalBudget(String userId, int year, int month) {
        return budgetRepository.findByUserIdAndCategoryIdIsNullAndYearAndMonth(userId, year, month)
                .orElse(null);
    }
    
    /**
     * 获取指定月份的分类预算
     */
    public Budget getCategoryBudget(String userId, String categoryId, int year, int month) {
        return budgetRepository.findByUserIdAndCategoryIdAndYearAndMonth(userId, categoryId, year, month)
                .orElse(null);
    }
    
    /**
     * 获取用户的所有预算
     */
    public List<Budget> getBudgetsByUserId(String userId) {
        return budgetRepository.findByUserId(userId);
    }
    
    /**
     * 获取指定月份的预算列表
     */
    public List<Budget> getBudgetsByMonth(String userId, int year, int month) {
        return budgetRepository.findByUserIdAndYearAndMonth(userId, year, month);
    }
    
    /**
     * 计算已用金额
     */
    public double calculateUsedAmount(String userId, String categoryId, int year, int month) {
        YearMonth yearMonth = YearMonth.of(year, month);
        LocalDate startDate = yearMonth.atDay(1);
        LocalDate endDate = yearMonth.atEndOfMonth();
        
        // 先按月份范围获取交易，再基于用户、类型与分类在内存中过滤
        // 如需更高性能，可在TransactionRepository中增加按用户与分类的范围查询方法
        List<Transaction> transactions = transactionService.getTransactionsByDateRange(startDate.atStartOfDay(), endDate.atTime(23, 59, 59));
        
        return transactions.stream()
            .filter(t -> userId.equals(t.getUserId())) // 仅统计当前用户的交易
            .filter(t -> t.getType() == Transaction.TransactionType.EXPENSE) // 仅统计支出
            .filter(t -> categoryId == null || categoryId.equals(t.getCategoryId())) // 分类为空表示汇总
            .mapToDouble(Transaction::getAmount)
            .sum();
    }
    
    /**
     * 检查是否超额
     */
    public boolean isOverBudget(String userId, String categoryId, int year, int month) {
        Budget budget = categoryId == null ? 
            getTotalBudget(userId, year, month) : 
            getCategoryBudget(userId, categoryId, year, month);
        
        if (budget == null) {
            return false;
        }
        
        double usedAmount = calculateUsedAmount(userId, categoryId, year, month);
        return usedAmount > budget.getAmount();
    }
    
    /**
     * 获取超额金额
     */
    public double getOverBudgetAmount(String userId, String categoryId, int year, int month) {
        Budget budget = categoryId == null ? 
            getTotalBudget(userId, year, month) : 
            getCategoryBudget(userId, categoryId, year, month);
        
        if (budget == null) {
            return 0;
        }
        
        double usedAmount = calculateUsedAmount(userId, categoryId, year, month);
        double overAmount = usedAmount - budget.getAmount();
        return Math.max(0, overAmount);
    }
    
    /**
     * 获取预算使用率（0-1之间）
     */
    public double getBudgetUsageRate(String userId, String categoryId, int year, int month) {
        Budget budget = categoryId == null ? 
            getTotalBudget(userId, year, month) : 
            getCategoryBudget(userId, categoryId, year, month);
        
        if (budget == null || budget.getAmount() == 0) {
            return 0;
        }
        
        double usedAmount = calculateUsedAmount(userId, categoryId, year, month);
        return Math.min(1.0, usedAmount / budget.getAmount());
    }
    
    /**
     * 设置月度预算
     */
    public Budget setMonthlyBudget(String userId, String categoryId, double amount, int year, int month) {
        Budget existingBudget = categoryId == null ? 
            getTotalBudget(userId, year, month) : 
            getCategoryBudget(userId, categoryId, year, month);
        
        if (existingBudget != null) {
            existingBudget.setAmount(amount);
            return budgetRepository.save(existingBudget);
        } else {
            Budget newBudget = new Budget(userId, categoryId, amount, year, month);
            return addBudget(newBudget);
        }
    }

    /**
     * --------------- Migration additions: period-based budgets and analytics ---------------
     */

    public List<Budget> findActiveBudgets(String userId, String categoryId, LocalDate atDate) {
        LocalDate effectiveAt = atDate == null ? LocalDate.now() : atDate;
        List<Budget> all = budgetRepository.findByUserId(userId == null ? "" : userId);
        return all.stream()
            .filter(b -> (categoryId == null || categoryId.equals(b.getCategoryId())))
            .filter(b -> {
                LocalDate start = b.getStartDate();
                LocalDate end = b.getEndDate();
                if (start == null || end == null) return false;
                return (!effectiveAt.isBefore(start)) && (!effectiveAt.isAfter(end));
            })
            .collect(Collectors.toList());
    }

    public BudgetStats calculateStats(Budget b) {
        BudgetStats s = new BudgetStats();
        s.budget = b;
        if (b.getStartDate() == null) return s;
        LocalDate now = LocalDate.now();
        LocalDate end = b.getEndDate();
        LocalDate last = end == null ? now : (now.isBefore(end) ? now : end);

        long daysElapsed = Math.max(1, java.time.temporal.ChronoUnit.DAYS.between(b.getStartDate(), last) + 1);
        long totalDays = Math.max(1, b.getTotalDays());
        double amountSpent = calculateAmountSpentForBudget(b);
        double remaining = b.getAmount() - amountSpent;

        s.daysElapsed = daysElapsed;
        s.totalDays = totalDays;
        s.amountSpent = amountSpent;
        s.remaining = remaining;
        s.avgPerDayBudget = b.getAmount() / (double) totalDays;
        s.avgPerDayActual = amountSpent / (double) daysElapsed;
        s.projectedTotalByAvgSoFar = s.avgPerDayActual * (double) totalDays;
        s.projectedRemainingByAvgSoFar = b.getAmount() - s.projectedTotalByAvgSoFar;
        s.willBeOverspentByAvg = s.projectedTotalByAvgSoFar > b.getAmount();

        if (daysElapsed >= 7) {
            LocalDate from7 = now.minusDays(6);
            s.last7DaysSpent = calculateAmountSpentInRange(b, from7, now);
        }
        if (daysElapsed >= 30) {
            LocalDate from30 = now.minusDays(29);
            s.last30DaysSpent = calculateAmountSpentInRange(b, from30, now);
        }
        return s;
    }

    private double calculateAmountSpentForBudget(Budget b) {
        LocalDate start = b.getStartDate();
        LocalDate end = b.getEndDate();
        if (start == null || end == null) return 0.0;
        List<Transaction> txs = transactionService.getTransactionsByDateRange(start.atStartOfDay(), end.atTime(23, 59, 59));
        return txs.stream()
            .filter(t -> t.getType() == Transaction.TransactionType.EXPENSE)
            .filter(t -> b.getUserId() == null || b.getUserId().equals(t.getUserId()))
            .filter(t -> b.getCategoryId() == null || b.getCategoryId().equals(t.getCategoryId()))
            .mapToDouble(Transaction::getAmount)
            .sum();
    }

    private double calculateAmountSpentInRange(Budget b, LocalDate startInclusive, LocalDate endInclusive) {
        List<Transaction> txs = transactionService.getTransactionsByDateRange(startInclusive.atStartOfDay(), endInclusive.atTime(23, 59, 59));
        return txs.stream()
            .filter(t -> t.getType() == Transaction.TransactionType.EXPENSE)
            .filter(t -> b.getUserId() == null || b.getUserId().equals(t.getUserId()))
            .filter(t -> b.getCategoryId() == null || b.getCategoryId().equals(t.getCategoryId()))
            .mapToDouble(Transaction::getAmount)
            .sum();
    }

    public boolean canConsume(Transaction t) {
        if (t == null || t.getType() != Transaction.TransactionType.EXPENSE) return true;
        String cat = t.getCategoryId();
        String uid = t.getUserId();
        LocalDate at = t.getDate() == null ? LocalDate.now() : t.getDate().toLocalDate();
        if (cat == null || cat.isBlank()) return true;

        List<Budget> actives = findActiveBudgets(uid, cat, at);
        if (actives.isEmpty()) return true;

        for (Budget b : actives) {
            BudgetStats s = calculateStats(b);
            if (s.remaining >= t.getAmount()) return true;
        }
        return false;
    }

    public static class BudgetStats {
        public Budget budget;
        public long daysElapsed;
        public long totalDays;
        public double amountSpent;
        public double remaining;
        public double avgPerDayBudget;
        public double avgPerDayActual;
        public double projectedTotalByAvgSoFar;
        public double projectedRemainingByAvgSoFar;
        public boolean willBeOverspentByAvg;
        public double last7DaysSpent;
        public double last30DaysSpent;
    }
}

