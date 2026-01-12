package org.example.drawsystemserver.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * 队伍DTO
 */
public class TeamDTO {
    private Long id;
    private Long captainId;
    private String captainName;
    private String teamName;
    private Integer playerCount;
    private BigDecimal totalCost; // 队伍总费用
    private BigDecimal nowCost; // 队伍当前剩余费用
    private Long userId; // 队长用户ID
    private List<PlayerDTO> players;

    public TeamDTO() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getCaptainId() {
        return captainId;
    }

    public void setCaptainId(Long captainId) {
        this.captainId = captainId;
    }

    public String getCaptainName() {
        return captainName;
    }

    public void setCaptainName(String captainName) {
        this.captainName = captainName;
    }

    public String getTeamName() {
        return teamName;
    }

    public void setTeamName(String teamName) {
        this.teamName = teamName;
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

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public List<PlayerDTO> getPlayers() {
        return players;
    }

    public void setPlayers(List<PlayerDTO> players) {
        this.players = players;
    }
}
