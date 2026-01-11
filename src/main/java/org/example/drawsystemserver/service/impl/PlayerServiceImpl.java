package org.example.drawsystemserver.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.drawsystemserver.entity.AuctionSession;
import org.example.drawsystemserver.entity.Player;
import org.example.drawsystemserver.mapper.AuctionSessionMapper;
import org.example.drawsystemserver.mapper.PlayerMapper;
import org.example.drawsystemserver.service.PlayerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class PlayerServiceImpl implements PlayerService {

    @Autowired
    private PlayerMapper playerMapper;

    @Autowired
    private AuctionSessionMapper sessionMapper;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public List<Player> getPoolPlayers() {
        return playerMapper.selectByStatus("POOL");
    }

    @Override
    public List<Player> getPoolPlayersBySession(Long sessionId) {
        List<Player> poolPlayers = playerMapper.selectBySessionIdAndStatus(sessionId, "POOL");
        
        // 获取该session的队长序号列表，过滤掉队长
        AuctionSession session = sessionMapper.selectById(sessionId);
        if (session != null && session.getCaptainIds() != null) {
            try {
                List<Integer> captainIndices = objectMapper.readValue(
                    session.getCaptainIds(), 
                    new TypeReference<List<Integer>>() {}
                );
                Set<Integer> captainIndexSet = captainIndices.stream().collect(Collectors.toSet());
                
                // 过滤掉groupId在队长序号列表中的player
                poolPlayers = poolPlayers.stream()
                    .filter(player -> player.getGroupId() == null || !captainIndexSet.contains(player.getGroupId()))
                    .collect(Collectors.toList());
            } catch (Exception e) {
                // 如果解析失败，记录日志但继续返回所有POOL状态的player
                e.printStackTrace();
            }
        }
        
        return poolPlayers;
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
