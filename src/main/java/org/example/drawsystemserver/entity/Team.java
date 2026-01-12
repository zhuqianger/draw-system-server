package org.example.drawsystemserver.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 队伍实体类
 */
public class Team {
    private Long id;
    private Long sessionId; // 所属拍卖流程ID
    private Long captainId; // 队长player的ID（来自player表）
    private Long userId; // 队长用户的ID（来自user表）
    private String teamName;
    private String captainName; // 队长名称（从Excel导入）
    private Integer playerCount; // 队员数量，最多4人
    private BigDecimal totalCost; // 队伍总费用（Excel费用平均数×5）
    private BigDecimal nowCost; // 队伍当前剩余费用
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    
    // 关联数据（不存储在数据库）
    private List<Player> players; // 队员列表

    public Team() {
        this.playerCount = 0;
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

    public Long getCaptainId() {
        return captainId;
    }

    public void setCaptainId(Long captainId) {
        this.captainId = captainId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getTeamName() {
        return teamName;
    }

    public void setTeamName(String teamName) {
        this.teamName = teamName;
    }

    public String getCaptainName() {
        return captainName;
    }

    public void setCaptainName(String captainName) {
        this.captainName = captainName;
    }

    public Integer getPlayerCount() {
        return playerCount;
    }

    public void setPlayerCount(Integer playerCount) {
        this.playerCount = playerCount;
    }

    public BigDecimal getTotalCost() {
        return totalCost;
    }

    public void setTotalCost(BigDecimal totalCost) {
        this.totalCost = totalCost;
    }

    public BigDecimal getNowCost() {
        return nowCost;
    }

    public void setNowCost(BigDecimal nowCost) {
        this.nowCost = nowCost;
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

    public List<Player> getPlayers() {
        return players;
    }

    public void setPlayers(List<Player> players) {
        this.players = players;
    }
}
