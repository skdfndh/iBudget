package com.accounting.service;

import com.accounting.model.Budget;
import com.accounting.model.Transaction;
import com.accounting.repository.BudgetRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

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
}

