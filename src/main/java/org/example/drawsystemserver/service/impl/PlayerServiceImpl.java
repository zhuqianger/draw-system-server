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

import java.util.Comparator;
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

        poolPlayers.sort(Comparator
                .comparing((Player p) -> isNormalPool(p) ? 0 : 1)
                .thenComparing(p -> isNormalPool(p)
                        ? p.getGroupId() == null ? Integer.MAX_VALUE : p.getGroupId()
                        : p.getFailedOrder() == null ? Integer.MAX_VALUE : p.getFailedOrder()));
        
        return poolPlayers;
    }

    private static boolean isNormalPool(Player p) {
        String t = p.getPoolType();
        return t == null || t.isEmpty() || "NORMAL".equals(t);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void changePlayerPool(Long sessionId, Long playerId, String targetPoolType) {
        Player player = playerMapper.selectById(playerId);
        if (player == null || !sessionId.equals(player.getSessionId())) {
            throw new RuntimeException("队员不存在或不属于该流程");
        }
        if (!"POOL".equals(player.getStatus())) {
            throw new RuntimeException("仅在待拍卖池中的队员可调整池类型");
        }

        AuctionSession session = sessionMapper.selectById(sessionId);
        if (session != null && session.getCaptainIds() != null && player.getGroupId() != null) {
            try {
                List<Integer> captainIndices = objectMapper.readValue(
                        session.getCaptainIds(),
                        new TypeReference<List<Integer>>() {});
                if (captainIndices != null && captainIndices.contains(player.getGroupId())) {
                    throw new RuntimeException("不能调整队长的池类型");
                }
            } catch (RuntimeException e) {
                throw e;
            } catch (Exception e) {
                throw new RuntimeException("解析队长信息失败", e);
            }
        }

        if ("NORMAL".equals(targetPoolType)) {
            playerMapper.updatePoolTypeAndFailedOrder(playerId, "NORMAL", null);
        } else if ("FAILED".equals(targetPoolType)) {
            Integer maxFo = playerMapper.selectMaxFailedOrderBySession(sessionId);
            int next = (maxFo == null ? 1 : maxFo + 1);
            playerMapper.updatePoolTypeAndFailedOrder(playerId, "FAILED", next);
        } else {
            throw new RuntimeException("无效的池类型");
        }
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
