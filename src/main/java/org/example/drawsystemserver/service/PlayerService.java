package org.example.drawsystemserver.service;

import org.example.drawsystemserver.entity.Player;

import java.util.List;

public interface PlayerService {
    List<Player> getPoolPlayers();
    List<Player> getPoolPlayersBySession(Long sessionId);
    Player getById(Long id);
    Player startAuction(Long playerId);
    Player assignToTeam(Long playerId, Long teamId);
    List<Player> getPlayersByTeamId(Long teamId);
    List<Player> getAllPlayers();
}
