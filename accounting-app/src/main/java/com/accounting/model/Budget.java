package com.accounting.model;

import com.google.gson.annotations.SerializedName;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.UUID;

/**
 * 预算实体类
 * 支持月度预算设置
 */
@Entity
@Table(name = "budgets")
public class Budget {
    @Id
    @SerializedName("id")
    private String id;
    
    @SerializedName("userId")
    private String userId;
    
    @SerializedName("categoryId")
    private String categoryId; // null表示总预算
    
    @SerializedName("amount")
    private double amount; // 预算金额
    
    @SerializedName("year")
    private int year;
    
    @SerializedName("month")
    private int month; // 1-12
    
    @SerializedName("createdAt")
    private LocalDateTime createdAt;
    
    @SerializedName("updatedAt")
    private LocalDateTime updatedAt;
    
    public Budget() {
        this.id = UUID.randomUUID().toString();
        LocalDate now = LocalDate.now();
        this.year = now.getYear();
        this.month = now.getMonthValue();
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }
    
    public Budget(String userId, String categoryId, double amount, int year, int month) {
        this();
        this.userId = userId;
        this.categoryId = categoryId;
        this.amount = amount;
        this.year = year;
        this.month = month;
    }
    
    public String getId() {
        return id;
    }
    
    public void setId(String id) {
        this.id = id;
    }
    
    public String getUserId() {
        return userId;
    }
    
    public void setUserId(String userId) {
        this.userId = userId;
    }
    
    public String getCategoryId() {
        return categoryId;
    }
    
    public void setCategoryId(String categoryId) {
        this.categoryId = categoryId;
    }
    
    public double getAmount() {
        return amount;
    }
    
    public void setAmount(double amount) {
        this.amount = amount;
    }
    
    public int getYear() {
        return year;
    }
    
    public void setYear(int year) {
        this.year = year;
    }
    
    public int getMonth() {
        return month;
    }
    
    public void setMonth(int month) {
        this.month = month;
    }
    
    public YearMonth getYearMonth() {
        return YearMonth.of(year, month);
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    
    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
    
    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
    
    /**
     * 检查是否为总预算（非分类预算）
     */
    public boolean isTotalBudget() {
        return categoryId == null || categoryId.isEmpty();
    }
    
    @Override
    public String toString() {
        return "Budget{" +
                "id='" + id + '\'' +
                ", categoryId='" + categoryId + '\'' +
                ", amount=" + amount +
                ", year=" + year +
                ", month=" + month +
                '}';
    }
}

