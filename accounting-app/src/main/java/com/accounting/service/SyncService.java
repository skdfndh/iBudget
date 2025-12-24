package com.accounting.service;

import com.accounting.model.SyncLog;
import com.accounting.model.Transaction;
import com.accounting.repository.SyncLogRepository;
import com.accounting.repository.TransactionRepository;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonSerializer;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Transactional
/**
 * 数据同步核心服务 (Data Synchronization Service)
 * <p>
 * 实现了“离线优先”架构的核心同步逻辑。
 * 核心策略：
 * 1. 增量同步 (Incremental Sync)：仅传输自上次同步以来发生变化的数据。
 * 2. 最终一致性 (Eventual Consistency)：通过 LWW (Last Write Wins) 策略解决多端写入冲突。
 * </p>
 */
public class SyncService {
    private final TransactionRepository transactionRepository;
    private final SyncLogRepository syncLogRepository;
    private final Gson gson;

    public SyncService(TransactionRepository transactionRepository, SyncLogRepository syncLogRepository) {
        this.transactionRepository = transactionRepository;
        this.syncLogRepository = syncLogRepository;

        JsonSerializer<LocalDateTime> lts = (src, typeOfSrc, context) -> new com.google.gson.JsonPrimitive(src.toString());
        JsonDeserializer<LocalDateTime> ltd = (json, typeOfT, context) -> LocalDateTime.parse(json.getAsString());
        this.gson = new GsonBuilder().registerTypeAdapter(LocalDateTime.class, lts).registerTypeAdapter(LocalDateTime.class, ltd).create();
    }

    /**
     * 拉取增量更新 (Pull)
     * <p>
     * 客户端提供上次同步的版本号 (lastVersion)。
     * 服务端查询 SyncLog 表，返回所有版本号大于 lastVersion 的变更记录。
     * 这种方式极大地减少了网络流量，适合移动端弱网环境。
     * </p>
     * @param userId 当前用户ID
     * @param lastVersion 客户端持有的最新版本号
     * @return 包含变更列表(changes)和当前最大版本号(current_version)的 Map
     */
    public Map<String, Object> pull(String userId, Long lastVersion) {
        List<SyncLog> changes = syncLogRepository.findChanges(userId, lastVersion);
        Long maxVersion = syncLogRepository.getMaxVersion(userId);
        
        Map<String, Object> result = new HashMap<>();
        result.put("changes", changes);
        result.put("current_version", maxVersion);
        return result;
    }

    /**
     * 推送并合并更改 (Push & Merge)
     * <p>
     * 接收客户端上传的离线数据，并将其合并到服务器数据库。
     * 采用 Last Write Wins (LWW) 策略解决冲突：
     * 如果服务器已存在该记录，则比较更新时间 (updatedAt)。
     * 仅当客户端数据的更新时间晚于服务器数据时，才执行覆盖操作。
     * </p>
     * @param userId 当前用户ID
     * @param incomingTransactions 客户端上传的交易列表
     * @return 同步结果，包含成功ID列表、失败ID列表及新版本号
     */
    public Map<String, Object> push(String userId, List<Transaction> incomingTransactions) {
        List<String> successIds = new ArrayList<>();
        List<String> failedIds = new ArrayList<>();
        Map<String, String> idMapping = new HashMap<>();
        
        Long currentMaxVersion = syncLogRepository.getMaxVersion(userId);
        
        for (Transaction incoming : incomingTransactions) {
            try {
                String clientId = incoming.getId();
                // 如果客户端上传的数据没有ID（新创建），则由服务器生成UUID
                if (clientId == null || clientId.isEmpty()) {
                    String newId = java.util.UUID.randomUUID().toString();
                    incoming.setId(newId);
                    // 无法映射空ID键，客户端应始终提供临时ID；此处仅记录生成的ID
                }
                processIncomingTransaction(userId, incoming, currentMaxVersion);
                successIds.add(incoming.getId());
                if (clientId != null && !clientId.isEmpty()) {
                    idMapping.put(clientId, incoming.getId());
                }
                currentMaxVersion++; // Increment for next item in batch
            } catch (Exception e) {
                failedIds.add(incoming.getId());
                e.printStackTrace();
            }
        }
        
        Map<String, Object> result = new HashMap<>();
        result.put("success_ids", successIds);
        result.put("failed_ids", failedIds);
        result.put("id_mapping", idMapping);
        result.put("new_version", currentMaxVersion);
        return result;
    }

    private void processIncomingTransaction(String userId, Transaction incoming, Long currentVersion) {
        incoming.setUserId(userId);
        if (incoming.getUpdatedAt() == null) {
            incoming.setUpdatedAt(LocalDateTime.now());
        }
        Transaction existing = transactionRepository.findById(incoming.getId()).orElse(null);
        if (existing == null) {
            saveAndLog(incoming, SyncLog.Action.ADD, currentVersion + 1);
        } else {
            int affected = transactionRepository.updateIfNewer(
                    incoming.getId(),
                    incoming.getUserId(),
                    incoming.getType(),
                    incoming.getAmount(),
                    incoming.getCategoryId(),
                    incoming.getDescription(),
                    incoming.getDate(),
                    incoming.getUpdatedAt(),
                    incoming.getTags()
            );
            if (affected > 0) {
                saveAndLog(incoming, SyncLog.Action.UPDATE, currentVersion + 1);
            }
        }
    }
    
    private void saveAndLog(Transaction transaction, SyncLog.Action action, Long version) {
        Transaction saved = transactionRepository.save(transaction);
        SyncLog log = new SyncLog(
                saved.getId(),
                saved.getUserId(),
                action,
                "Transaction",
                gson.toJson(saved),
                version
        );
        syncLogRepository.save(log);
    }
}
