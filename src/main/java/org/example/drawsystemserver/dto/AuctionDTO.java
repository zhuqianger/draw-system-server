package org.example.drawsystemserver.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 拍卖DTO
 */
public class AuctionDTO {
    private Long id;
    private Long playerId;
    private String playerName; // 显示名称（群内名字）
    private String playerGroupName; // 群内名字
    private String playerGameId; // 游戏ID名字
    private String playerPosition; // 擅长位置
    private String playerHeroes; // 擅长英雄
    private String playerRank; // 段位
    private BigDecimal playerCost; // 费用
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Integer duration;
    private String status;
    private BigDecimal highestBidAmount;
    private Long highestBidTeamId;
    private String highestBidTeamName;
    private Integer bidCount;
    private List<BidDTO> recentBids; // 最近5条竞价

    public AuctionDTO() {
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getPlayerId() {
        return playerId;
    }

    public void setPlayerId(Long playerId) {
        this.playerId = playerId;
    }

    public String getPlayerName() {
        return playerName;
    }

    public void setPlayerName(String playerName) {
        this.playerName = playerName;
    }

    public String getPlayerPosition() {
        return playerPosition;
    }

    public void setPlayerPosition(String playerPosition) {
        this.playerPosition = playerPosition;
    }

    public String getPlayerGroupName() {
        return playerGroupName;
    }

    public void setPlayerGroupName(String playerGroupName) {
        this.playerGroupName = playerGroupName;
    }

    public String getPlayerGameId() {
        return playerGameId;
    }

    public void setPlayerGameId(String playerGameId) {
        this.playerGameId = playerGameId;
    }

    public String getPlayerHeroes() {
        return playerHeroes;
    }

    public void setPlayerHeroes(String playerHeroes) {
        this.playerHeroes = playerHeroes;
    }

    public String getPlayerRank() {
        return playerRank;
    }

    public void setPlayerRank(String playerRank) {
        this.playerRank = playerRank;
    }

    public BigDecimal getPlayerCost() {
        return playerCost;
    }

    public void setPlayerCost(BigDecimal playerCost) {
        this.playerCost = playerCost;
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

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public BigDecimal getHighestBidAmount() {
        return highestBidAmount;
    }

    public void setHighestBidAmount(BigDecimal highestBidAmount) {
        this.highestBidAmount = highestBidAmount;
    }

    public Long getHighestBidTeamId() {
        return highestBidTeamId;
    }

    public void setHighestBidTeamId(Long highestBidTeamId) {
        this.highestBidTeamId = highestBidTeamId;
    }

    public String getHighestBidTeamName() {
        return highestBidTeamName;
    }

    public void setHighestBidTeamName(String highestBidTeamName) {
        this.highestBidTeamName = highestBidTeamName;
    }

    public Integer getBidCount() {
        return bidCount;
    }

    public void setBidCount(Integer bidCount) {
        this.bidCount = bidCount;
    }

    public List<BidDTO> getRecentBids() {
        return recentBids;
    }

    public void setRecentBids(List<BidDTO> recentBids) {
        this.recentBids = recentBids;
    }
}
