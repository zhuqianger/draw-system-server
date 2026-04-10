package org.example.drawsystemserver.service;

import org.example.drawsystemserver.entity.Player;

import java.util.List;

public interface PlayerService {
    List<Player> getPoolPlayers();
    List<Player> getPoolPlayersBySession(Long sessionId);

    /**
     * 与 {@link #getPoolPlayersBySession(Long)} 一致：普通池中可参与摇号的人数（已排除队长，仅 NORMAL 池）
     */
    int countDrawableNormalPoolBySession(Long sessionId);
    Player getById(Long id);
    Player startAuction(Long playerId);
    Player assignToTeam(Long playerId, Long teamId);
    List<Player> getPlayersByTeamId(Long teamId);
    List<Player> getAllPlayers();

    /**
     * 管理员：在待拍卖池中调整队员所属池（普通池 / 流拍池）
     */
    void changePlayerPool(Long sessionId, Long playerId, String targetPoolType);
}
