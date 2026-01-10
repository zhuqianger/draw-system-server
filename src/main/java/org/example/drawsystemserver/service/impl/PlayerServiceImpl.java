package org.example.drawsystemserver.service.impl;

import org.example.drawsystemserver.entity.Player;
import org.example.drawsystemserver.mapper.PlayerMapper;
import org.example.drawsystemserver.service.PlayerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class PlayerServiceImpl implements PlayerService {

    @Autowired
    private PlayerMapper playerMapper;

    @Override
    public List<Player> getPoolPlayers() {
        return playerMapper.selectByStatus("POOL");
    }

    @Override
    public List<Player> getPoolPlayersBySession(Long sessionId) {
        return playerMapper.selectBySessionIdAndStatus(sessionId, "POOL");
    }

    @Override
    public Player getById(Long id) {
        return playerMapper.selectById(id);
    }

    @Override
    @Transactional
    public Player startAuction(Long playerId) {
        Player player = playerMapper.selectById(playerId);
        if (player != null && "POOL".equals(player.getStatus())) {
            player.setStatus("AUCTIONING");
            playerMapper.update(player);
        }
        return player;
    }

    @Override
    @Transactional
    public Player assignToTeam(Long playerId, Long teamId) {
        Player player = playerMapper.selectById(playerId);
        if (player != null) {
            player.setStatus("SOLD");
            player.setTeamId(teamId);
            player.setCurrentAuctionId(null);
            playerMapper.update(player);
        }
        return player;
    }

    @Override
    public List<Player> getPlayersByTeamId(Long teamId) {
        return playerMapper.selectByTeamId(teamId);
    }

    @Override
    public List<Player> getAllPlayers() {
        return playerMapper.selectAll();
    }
}
