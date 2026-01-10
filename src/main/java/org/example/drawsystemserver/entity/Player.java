package org.example.drawsystemserver.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 队员实体类
 */
public class Player {
    private Long id;
    private Long sessionId; // 所属拍卖流程ID
    private Integer groupId; // 队员序号
    private String groupName; // 群内名字
    private String gameId; // 游戏ID名字
    private String position; // 擅长的位置（可多个，逗号分隔）
    private String heroes; // 擅长的英雄（可多个，逗号分隔）
    private String rank; // 段位
    private BigDecimal cost; // 费用
    private String status; // POOL-待拍卖池, AUCTIONING-拍卖中, SOLD-已售出
    private Long currentAuctionId; // 当前拍卖ID
    private Long teamId; // 所属队伍ID
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    public Player() {
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

    public Integer getGroupId() {
        return groupId;
    }

    public void setGroupId(Integer groupId) {
        this.groupId = groupId;
    }

    public String getGroupName() {
        return groupName;
    }

    public void setGroupName(String groupName) {
        this.groupName = groupName;
    }

    public String getGameId() {
        return gameId;
    }

    public void setGameId(String gameId) {
        this.gameId = gameId;
    }

    public String getPosition() {
        return position;
    }

    public void setPosition(String position) {
        this.position = position;
    }

    public String getHeroes() {
        return heroes;
    }

    public void setHeroes(String heroes) {
        this.heroes = heroes;
    }

    public String getRank() {
        return rank;
    }

    public void setRank(String rank) {
        this.rank = rank;
    }

    public BigDecimal getCost() {
        return cost;
    }

    public void setCost(BigDecimal cost) {
        this.cost = cost;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Long getCurrentAuctionId() {
        return currentAuctionId;
    }

    public void setCurrentAuctionId(Long currentAuctionId) {
        this.currentAuctionId = currentAuctionId;
    }

    public Long getTeamId() {
        return teamId;
    }

    public void setTeamId(Long teamId) {
        this.teamId = teamId;
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
}
