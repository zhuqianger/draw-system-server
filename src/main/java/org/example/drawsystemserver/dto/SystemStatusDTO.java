package org.example.drawsystemserver.dto;

import java.util.List;

/**
 * 系统状态DTO（用于掉线后重新登录同步数据）
 */
public class SystemStatusDTO {
    private AuctionDTO currentAuction; // 当前进行的拍卖
    private List<PlayerDTO> poolPlayers; // 待拍卖池中的队员
    private List<TeamDTO> teams; // 所有队伍信息
    private List<PlayerDTO> soldPlayers; // 已售出的队员

    public SystemStatusDTO() {
    }

    public AuctionDTO getCurrentAuction() {
        return currentAuction;
    }

    public void setCurrentAuction(AuctionDTO currentAuction) {
        this.currentAuction = currentAuction;
    }

    public List<PlayerDTO> getPoolPlayers() {
        return poolPlayers;
    }

    public void setPoolPlayers(List<PlayerDTO> poolPlayers) {
        this.poolPlayers = poolPlayers;
    }

    public List<TeamDTO> getTeams() {
        return teams;
    }

    public void setTeams(List<TeamDTO> teams) {
        this.teams = teams;
    }

    public List<PlayerDTO> getSoldPlayers() {
        return soldPlayers;
    }

    public void setSoldPlayers(List<PlayerDTO> soldPlayers) {
        this.soldPlayers = soldPlayers;
    }
}
