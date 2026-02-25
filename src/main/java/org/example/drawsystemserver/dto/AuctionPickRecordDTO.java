package org.example.drawsystemserver.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 选人纪录 DTO
 */
public class AuctionPickRecordDTO {
    private Long id;
    private Long sessionId;
    private Long auctionId;
    private Long playerId;
    private Long teamId;
    private BigDecimal amount;
    private Integer sequence;
    private LocalDateTime createTime;

    // 展示用字段
    private String playerGroupName;
    private String playerGameId;
    private String teamName;
    private String captainName;

    public AuctionPickRecordDTO() {
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
}

