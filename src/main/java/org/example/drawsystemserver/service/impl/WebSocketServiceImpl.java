package org.example.drawsystemserver.service.impl;

import org.example.drawsystemserver.dto.ResponseDTO;
import org.example.drawsystemserver.dto.SystemStatusDTO;
import org.example.drawsystemserver.dto.WebSocketEventDTO;
import org.example.drawsystemserver.entity.Auction;
import org.example.drawsystemserver.entity.Player;
import org.example.drawsystemserver.entity.Team;
import org.example.drawsystemserver.mapper.AuctionMapper;
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
    private TeamMapper teamMapper;

    @Autowired
    private PlayerMapper playerMapper;

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

    private void sendEvent(String eventType, Long sessionId, Long auctionId, Long bidId, Long playerId, Long teamId) {
        WebSocketEventDTO event = new WebSocketEventDTO();
        event.setEventId(java.util.UUID.randomUUID().toString());
        event.setEventType(eventType);
        event.setSessionId(sessionId);
        event.setAuctionId(auctionId);
        event.setBidId(bidId);
        event.setPlayerId(playerId);
        event.setTeamId(teamId);
        event.setTimestamp(System.currentTimeMillis());
        safeSend("/topic/event", ResponseDTO.success(event));
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
            SystemStatusDTO status = systemService.getSystemStatus();
            safeSend("/topic/system-status", ResponseDTO.success(status));
            sendEvent("SYSTEM_STATUS", null, null, null, null, null);
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
            sendEvent("AUCTION_STARTED", sessionId, auctionId, null, null, null);
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
            sendEvent("BID_PLACED", sessionId, auctionId, bidId, null, null);
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
            sendEvent("AUCTION_FINISHED", sessionId, auctionId, null, null, null);
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
            sendEvent("PLAYER_ASSIGNED", sessionId, null, null, playerId, teamId);
            logger.info("广播队员分配事件: sessionId={}, playerId={}, teamId={}", sessionId, playerId, teamId);
        } catch (Exception e) {
            logger.error("广播队员分配失败: playerId={}, teamId={}, error={}", playerId, teamId, e.getMessage(), e);
        }
    }

    @Override
    public void broadcastSystemChanged(Long sessionId) {
        try {
            sendEvent("SYSTEM_CHANGED", sessionId, null, null, null, null);
            logger.info("广播系统变更事件: sessionId={}", sessionId);
        } catch (Exception e) {
            logger.error("广播系统变更事件失败: sessionId={}, error={}", sessionId, e.getMessage(), e);
        }
    }
}
