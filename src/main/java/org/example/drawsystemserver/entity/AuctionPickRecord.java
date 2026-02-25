package org.example.drawsystemserver.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 选人纪录实体：
 * 每条纪录代表一次拍卖最终有队长成功拍到某位队员。
 * 无人拍到的拍卖不写入本表。
 */
public class AuctionPickRecord {
    private Long id;
    private Long sessionId;
    private Long auctionId;
    private Long playerId;
    private Long teamId;
    private BigDecimal amount; // 成交金额
    private Integer sequence;  // 在该流程中的顺序（第几次成功选人，从 1 开始递增）
    private LocalDateTime createTime;

    public AuctionPickRecord() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getSessionId() {
        return sessionId;
    }

    public void setSessionId(Long sessionId) {
        this.sessionId = sessionId;
    }

    public Long getAuctionId() {
        return auctionId;
    }

    public void setAuctionId(Long auctionId) {
        this.auctionId = auctionId;
    }

    public Long getPlayerId() {
        return playerId;
    }

    public void setPlayerId(Long playerId) {
        this.playerId = playerId;
    }

    public Long getTeamId() {
        return teamId;
    }

    public void setTeamId(Long teamId) {
        this.teamId = teamId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public Integer getSequence() {
        return sequence;
    }

    public void setSequence(Integer sequence) {
        this.sequence = sequence;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }
}

