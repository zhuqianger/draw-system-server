package org.example.drawsystemserver.service.impl;

import org.example.drawsystemserver.dto.AuctionPickRecordDTO;
import org.example.drawsystemserver.dto.BidDTO;
import org.example.drawsystemserver.dto.PlayerDTO;
import org.example.drawsystemserver.dto.ResponseDTO;
import org.example.drawsystemserver.dto.SystemStatusDTO;
import org.example.drawsystemserver.dto.TeamDTO;
import org.example.drawsystemserver.dto.WebSocketEventDTO;
import org.example.drawsystemserver.entity.Auction;
import org.example.drawsystemserver.entity.AuctionPickRecord;
import org.example.drawsystemserver.entity.Bid;
import org.example.drawsystemserver.entity.Player;
import org.example.drawsystemserver.entity.Team;
import org.example.drawsystemserver.mapper.AuctionMapper;
import org.example.drawsystemserver.mapper.AuctionPickRecordMapper;
import org.example.drawsystemserver.mapper.BidMapper;
import org.example.drawsystemserver.mapper.PlayerMapper;
import org.example.drawsystemserver.mapper.TeamMapper;
import org.example.drawsystemserver.service.SystemService;
import org.example.drawsystemserver.service.WebSocketService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class WebSocketServiceImpl implements WebSocketService {

    private static final Logger logger = LoggerFactory.getLogger(WebSocketServiceImpl.class);

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @Autowired
    private SystemService systemService;

    @Autowired
    private AuctionMapper auctionMapper;

    @Autowired
    private BidMapper bidMapper;

    @Autowired
    private TeamMapper teamMapper;

    @Autowired
    private PlayerMapper playerMapper;

    @Autowired
    private AuctionPickRecordMapper auctionPickRecordMapper;

    /**
     * 安全发送消息，捕获异常并记录日志
     */
    private void safeSend(String destination, Object payload) {
        try {
            messagingTemplate.convertAndSend(destination, payload);
            logger.debug("WebSocket消息发送成功: destination={}", destination);
        } catch (MessagingException e) {
            logger.error("WebSocket消息发送失败: destination={}, error={}", destination, e.getMessage(), e);
        } catch (Exception e) {
            logger.error("WebSocket消息发送异常: destination={}, error={}", destination, e.getMessage(), e);
        }
    }

    private void sendEvent(String eventType,
                           Long sessionId,
                           Long auctionId,
                           Long bidId,
                           Long playerId,
                           Long teamId,
                           boolean includeSnapshot,
                           Object data) {
        WebSocketEventDTO event = new WebSocketEventDTO();
        event.setEventId(java.util.UUID.randomUUID().toString());
        event.setEventType(eventType);
        event.setSessionId(sessionId);
        event.setAuctionId(auctionId);
        event.setBidId(bidId);
        event.setPlayerId(playerId);
        event.setTeamId(teamId);
        event.setTimestamp(System.currentTimeMillis());
        if (includeSnapshot) {
            event.setSystemStatus(systemService.getSystemStatusBySession(sessionId));
        }
        if (data != null) {
            event.setData(data);
        }
        safeSend("/topic/event", ResponseDTO.success(event));
    }

    private Map<String, Object> buildAuctionData(Long auctionId) {
        if (auctionId == null) {
            return Collections.emptyMap();
        }
        Auction auction = auctionMapper.selectById(auctionId);
        if (auction == null) {
            return Collections.emptyMap();
        }

        Map<String, Object> auctionPatch = new HashMap<>();
        auctionPatch.put("id", auction.getId());
        auctionPatch.put("status", auction.getStatus());
        auctionPatch.put("phase", auction.getPhase());
        auctionPatch.put("startTime", auction.getStartTime());
        auctionPatch.put("endTime", auction.getEndTime());
        auctionPatch.put("duration", auction.getDuration());
        auctionPatch.put("startingPrice", auction.getStartingPrice());
        auctionPatch.put("maxPrice", auction.getMaxPrice());
        auctionPatch.put("playerId", auction.getPlayerId());

        Player player = playerMapper.selectById(auction.getPlayerId());
        if (player != null) {
            auctionPatch.put("playerName", player.getGroupName());
            auctionPatch.put("playerGroupName", player.getGroupName());
            auctionPatch.put("playerGameId", player.getGameId());
            auctionPatch.put("playerPosition", player.getPosition());
            auctionPatch.put("playerHeroes", player.getHeroes());
            auctionPatch.put("playerRank", player.getRank());
            auctionPatch.put("playerCost", player.getCost());
        }

        Bid highestBid = bidMapper.selectHighestByAuctionId(auction.getId());
        if (highestBid != null) {
            auctionPatch.put("highestBidAmount", highestBid.getAmount());
            auctionPatch.put("highestBidTeamId", highestBid.getTeamId());
            Team team = teamMapper.selectById(highestBid.getTeamId());
            if (team != null) {
                auctionPatch.put("highestBidTeamName", team.getTeamName());
            }
        }
        return auctionPatch;
    }

    private BidDTO toBidLiteDTO(Bid bid) {
        if (bid == null) {
            return null;
        }
        BidDTO dto = new BidDTO();
        dto.setId(bid.getId());
        dto.setAuctionId(bid.getAuctionId());
        dto.setTeamId(bid.getTeamId());
        dto.setAmount(bid.getAmount());
        dto.setBidTime(bid.getBidTime());
        dto.setIsWinner(bid.getIsWinner());

        Team team = teamMapper.selectById(bid.getTeamId());
        if (team != null) {
            dto.setTeamName(team.getTeamName());
        }
        return dto;
    }

    private TeamDTO buildTeamDelta(Long teamId) {
        if (teamId == null) {
            return null;
        }
        Team team = teamMapper.selectById(teamId);
        if (team == null) {
            return null;
        }

        TeamDTO dto = new TeamDTO();
        dto.setId(team.getId());
        dto.setCaptainId(team.getCaptainId());
        dto.setTeamName(team.getTeamName());
        dto.setPlayerCount(team.getPlayerCount());
        dto.setTotalCost(team.getTotalCost());
        dto.setNowCost(team.getNowCost());
        dto.setUserId(team.getUserId());

        Player captain = team.getCaptainId() == null ? null : playerMapper.selectById(team.getCaptainId());
        dto.setCaptainName(captain != null ? captain.getGroupName() : team.getCaptainName());

        List<Player> teamPlayers = playerMapper.selectByTeamId(team.getId());
        dto.setPlayers(teamPlayers.stream().map(p -> {
            PlayerDTO playerDTO = new PlayerDTO();
            playerDTO.setId(p.getId());
            playerDTO.setGroupName(p.getGroupName());
            playerDTO.setGameId(p.getGameId());
            return playerDTO;
        }).collect(Collectors.toList()));

        return dto;
    }

    private AuctionPickRecordDTO buildLatestPickRecord(Long sessionId) {
        if (sessionId == null) {
            return null;
        }
        List<AuctionPickRecord> records = auctionPickRecordMapper.selectBySessionIdOrderBySequence(sessionId);
        if (records.isEmpty()) {
            return null;
        }
        AuctionPickRecord record = records.get(records.size() - 1);
        AuctionPickRecordDTO dto = new AuctionPickRecordDTO();
        dto.setId(record.getId());
        dto.setSessionId(record.getSessionId());
        dto.setAuctionId(record.getAuctionId());
        dto.setPlayerId(record.getPlayerId());
        dto.setTeamId(record.getTeamId());
        dto.setAmount(record.getAmount());
        dto.setSequence(record.getSequence());
        dto.setCreateTime(record.getCreateTime());

        Player player = playerMapper.selectById(record.getPlayerId());
        if (player != null) {
            dto.setPlayerGroupName(player.getGroupName());
            dto.setPlayerGameId(player.getGameId());
        }
        Team team = teamMapper.selectById(record.getTeamId());
        if (team != null) {
            dto.setTeamName(team.getTeamName());
            Player captain = team.getCaptainId() == null ? null : playerMapper.selectById(team.getCaptainId());
            dto.setCaptainName(captain != null ? captain.getGroupName() : team.getCaptainName());
        }
        return dto;
    }

    private PlayerDTO buildPoolPlayerDelta(Long playerId) {
        if (playerId == null) {
            return null;
        }
        Player player = playerMapper.selectById(playerId);
        if (player == null) {
            return null;
        }
        PlayerDTO dto = new PlayerDTO();
        dto.setId(player.getId());
        dto.setGroupId(player.getGroupId());
        dto.setPoolType(player.getPoolType());
        dto.setFailedOrder(player.getFailedOrder());
        dto.setGroupName(player.getGroupName());
        dto.setGameId(player.getGameId());
        dto.setPosition(player.getPosition());
        dto.setHeroes(player.getHeroes());
        dto.setRank(player.getRank());
        dto.setCost(player.getCost());
        dto.setStatus(player.getStatus());
        dto.setTeamId(player.getTeamId());
        return dto;
    }

    private Long resolveSessionIdByAuctionId(Long auctionId) {
        if (auctionId == null) {
            return null;
        }
        try {
            Auction auction = auctionMapper.selectById(auctionId);
            return auction != null ? auction.getSessionId() : null;
        } catch (Exception e) {
            logger.warn("根据auctionId解析sessionId失败: auctionId={}, error={}", auctionId, e.getMessage());
            return null;
        }
    }

    private Long resolveSessionIdByTeamOrPlayer(Long teamId, Long playerId) {
        if (teamId != null) {
            try {
                Team team = teamMapper.selectById(teamId);
                if (team != null) {
                    return team.getSessionId();
                }
            } catch (Exception e) {
                logger.warn("根据teamId解析sessionId失败: teamId={}, error={}", teamId, e.getMessage());
            }
        }

        if (playerId != null) {
            try {
                Player player = playerMapper.selectById(playerId);
                if (player != null) {
                    return player.getSessionId();
                }
            } catch (Exception e) {
                logger.warn("根据playerId解析sessionId失败: playerId={}, error={}", playerId, e.getMessage());
            }
        }
        return null;
    }

    /**
     * 广播系统状态（用于掉线后重新登录同步数据）
     */
    @Override
    public void broadcastSystemStatus() {
        try {
            SystemStatusDTO status = systemService.getSystemStatusBySession(null);
            safeSend("/topic/system-status", ResponseDTO.success(status));
            sendEvent("SYSTEM_STATUS", null, null, null, null, null, true, null);
            logger.info("广播系统状态更新事件");
        } catch (Exception e) {
            logger.error("广播系统状态失败: {}", e.getMessage(), e);
        }
    }

    /**
     * 广播拍卖开始
     */
    @Override
    public void broadcastAuctionStart(Long auctionId) {
        try {
            Long sessionId = resolveSessionIdByAuctionId(auctionId);
            Map<String, Object> data = new HashMap<>();
            data.put("auction", buildAuctionData(auctionId));
            sendEvent("AUCTION_STARTED", sessionId, auctionId, null, null, null, false, data);
            logger.info("广播拍卖开始事件: sessionId={}, auctionId={}", sessionId, auctionId);
        } catch (Exception e) {
            logger.error("广播拍卖开始失败: auctionId={}, error={}", auctionId, e.getMessage(), e);
        }
    }

    /**
     * 广播竞价
     */
    @Override
    public void broadcastBidPlaced(Long auctionId, Long bidId) {
        try {
            Long sessionId = resolveSessionIdByAuctionId(auctionId);
            Map<String, Object> data = new HashMap<>();
            data.put("bid", toBidLiteDTO(bidMapper.selectById(bidId)));
            data.put("auction", buildAuctionData(auctionId));
            sendEvent("BID_PLACED", sessionId, auctionId, bidId, null, null, false, data);
            logger.info("广播竞价事件: sessionId={}, auctionId={}, bidId={}", sessionId, auctionId, bidId);
        } catch (Exception e) {
            logger.error("广播竞价失败: auctionId={}, bidId={}, error={}", auctionId, bidId, e.getMessage(), e);
        }
    }

    /**
     * 广播拍卖结束
     */
    @Override
    public void broadcastAuctionFinished(Long auctionId) {
        try {
            Long sessionId = resolveSessionIdByAuctionId(auctionId);
            Map<String, Object> data = new HashMap<>();
            data.put("auction", buildAuctionData(auctionId));
            sendEvent("AUCTION_FINISHED", sessionId, auctionId, null, null, null, false, data);
            logger.info("广播拍卖结束事件: sessionId={}, auctionId={}", sessionId, auctionId);
        } catch (Exception e) {
            logger.error("广播拍卖结束失败: auctionId={}, error={}", auctionId, e.getMessage(), e);
        }
    }

    /**
     * 广播队员分配
     */
    @Override
    public void broadcastPlayerAssigned(Long playerId, Long teamId) {
        try {
            Long sessionId = resolveSessionIdByTeamOrPlayer(teamId, playerId);
            Map<String, Object> data = new HashMap<>();
            data.put("team", buildTeamDelta(teamId));
            data.put("poolRemovedPlayerId", playerId);
            data.put("pickRecord", buildLatestPickRecord(sessionId));
            sendEvent("PLAYER_ASSIGNED", sessionId, null, null, playerId, teamId, false, data);
            logger.info("广播队员分配事件: sessionId={}, playerId={}, teamId={}", sessionId, playerId, teamId);
        } catch (Exception e) {
            logger.error("广播队员分配失败: playerId={}, teamId={}, error={}", playerId, teamId, e.getMessage(), e);
        }
    }

    @Override
    public void broadcastTeamCostUpdated(Long teamId) {
        try {
            Team team = teamMapper.selectById(teamId);
            Long sessionId = team != null ? team.getSessionId() : null;
            Map<String, Object> data = new HashMap<>();
            data.put("team", buildTeamDelta(teamId));
            sendEvent("TEAM_COST_UPDATED", sessionId, null, null, null, teamId, false, data);
            logger.info("广播队伍费用更新事件: sessionId={}, teamId={}", sessionId, teamId);
        } catch (Exception e) {
            logger.error("广播队伍费用更新失败: teamId={}, error={}", teamId, e.getMessage(), e);
        }
    }

    @Override
    public void broadcastPlayerPoolChanged(Long sessionId, Long playerId) {
        try {
            Map<String, Object> data = new HashMap<>();
            data.put("player", buildPoolPlayerDelta(playerId));
            sendEvent("PLAYER_POOL_CHANGED", sessionId, null, null, playerId, null, false, data);
            logger.info("广播队员池变更事件: sessionId={}, playerId={}", sessionId, playerId);
        } catch (Exception e) {
            logger.error("广播队员池变更失败: sessionId={}, playerId={}, error={}", sessionId, playerId, e.getMessage(), e);
        }
    }

    @Override
    public void broadcastPlayerRemovedFromTeam(Long teamId, Long playerId) {
        try {
            Team team = teamMapper.selectById(teamId);
            Long sessionId = team != null ? team.getSessionId() : null;
            Map<String, Object> data = new HashMap<>();
            data.put("team", buildTeamDelta(teamId));
            data.put("poolPlayer", buildPoolPlayerDelta(playerId));
            Map<String, Object> pickRecordRemoved = new HashMap<>();
            pickRecordRemoved.put("teamId", teamId);
            pickRecordRemoved.put("playerId", playerId);
            data.put("pickRecordRemoved", pickRecordRemoved);
            sendEvent("PLAYER_REMOVED_FROM_TEAM", sessionId, null, null, playerId, teamId, false, data);
            logger.info("广播队员移出队伍事件: sessionId={}, teamId={}, playerId={}", sessionId, teamId, playerId);
        } catch (Exception e) {
            logger.error("广播队员移出队伍失败: teamId={}, playerId={}, error={}", teamId, playerId, e.getMessage(), e);
        }
    }

    @Override
    public void broadcastRollbackCompleted(Long sessionId) {
        try {
            Map<String, Object> data = new HashMap<>();
            data.put("systemStatus", systemService.getSystemStatusBySession(sessionId));
            sendEvent("ROLLBACK_COMPLETED", sessionId, null, null, null, null, false, data);
            logger.info("广播回退完成事件: sessionId={}", sessionId);
        } catch (Exception e) {
            logger.error("广播回退完成失败: sessionId={}, error={}", sessionId, e.getMessage(), e);
        }
    }

    @Override
    public void broadcastSystemChanged(Long sessionId) {
        try {
            sendEvent("SYSTEM_CHANGED", sessionId, null, null, null, null, false, null);
            logger.info("广播系统变更事件: sessionId={}", sessionId);
        } catch (Exception e) {
            logger.error("广播系统变更事件失败: sessionId={}, error={}", sessionId, e.getMessage(), e);
        }
    }
}
