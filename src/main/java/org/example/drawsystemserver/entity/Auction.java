package org.example.drawsystemserver.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 拍卖实体类
 */
public class Auction {
    private Long id;
    private Long sessionId; // 所属拍卖流程ID
    private Long playerId;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Integer duration; // 拍卖时长（秒）
    private String status; // WAITING-等待开始, FIRST_PHASE-第一阶段, PICKUP_PHASE-捡漏环节, FINISHED-已结束, CANCELLED-已取消
    private String phase; // 阶段：WAITING-等待开始, FIRST_PHASE-第一阶段(30s), PICKUP_PHASE-捡漏环节(20s)
    private BigDecimal startingPrice; // 起拍价
    private BigDecimal maxPrice; // 最高出价（基础定价+3）
    private Long winningBidId; // 获胜竞价ID
    private Long winningTeamId; // 获胜队伍ID
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    
    // 关联数据（不存储在数据库）
    private Player player; // 被拍卖的队员
    private List<Bid> bids; // 竞价列表
    private Bid highestBid; // 最高竞价

    public Auction() {
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

    public Long getPlayerId() {
        return playerId;
    }

    public void setPlayerId(Long playerId) {
        this.playerId = playerId;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalDateTime startTime) {
        this.startTime = startTime;
    }

    public LocalDateTime getEndTime() {
        return endTime;
    }

    public void setEndTime(LocalDateTime endTime) {
        this.endTime = endTime;
    }

    public Integer getDuration() {
        return duration;
    }

    public void setDuration(Integer duration) {
        this.duration = duration;
    }

    public String getPhase() {
        return phase;
    }

    public void setPhase(String phase) {
        this.phase = phase;
    }

    public BigDecimal getStartingPrice() {
        return startingPrice;
    }

    public void setStartingPrice(BigDecimal startingPrice) {
        this.startingPrice = startingPrice;
    }

    public BigDecimal getMaxPrice() {
        return maxPrice;
    }

    public void setMaxPrice(BigDecimal maxPrice) {
        this.maxPrice = maxPrice;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Long getWinningBidId() {
        return winningBidId;
    }

    public void setWinningBidId(Long winningBidId) {
        this.winningBidId = winningBidId;
    }

    public Long getWinningTeamId() {
        return winningTeamId;
    }

    public void setWinningTeamId(Long winningTeamId) {
        this.winningTeamId = winningTeamId;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }

    public LocalDateTime getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(LocalDateTime updateTime) {
        this.updateTime = updateTime;
    }

    public Player getPlayer() {
        return player;
    }

    public void setPlayer(Player player) {
        this.player = player;
    }

    public List<Bid> getBids() {
        return bids;
    }

    public void setBids(List<Bid> bids) {
        this.bids = bids;
    }

    public Bid getHighestBid() {
        return highestBid;
    }

    public void setHighestBid(Bid highestBid) {
        this.highestBid = highestBid;
    }
}
