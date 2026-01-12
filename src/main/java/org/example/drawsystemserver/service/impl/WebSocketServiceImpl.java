package org.example.drawsystemserver.service.impl;

import org.example.drawsystemserver.dto.ResponseDTO;
import org.example.drawsystemserver.dto.SystemStatusDTO;
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

    /**
     * 广播系统状态（用于掉线后重新登录同步数据）
     */
    @Override
    public void broadcastSystemStatus() {
        try {
            SystemStatusDTO status = systemService.getSystemStatus();
            safeSend("/topic/system-status", ResponseDTO.success(status));
            logger.info("广播系统状态更新");
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
            broadcastSystemStatus(); // 更新整个系统状态
            safeSend("/topic/auction", ResponseDTO.success("拍卖开始", auctionId));
            logger.info("广播拍卖开始: auctionId={}", auctionId);
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
            broadcastSystemStatus(); // 更新整个系统状态
            safeSend("/topic/bid", ResponseDTO.success("有新竞价", bidId));
            logger.info("广播竞价: auctionId={}, bidId={}", auctionId, bidId);
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
            broadcastSystemStatus(); // 更新整个系统状态
            safeSend("/topic/auction", ResponseDTO.success("拍卖结束", auctionId));
            logger.info("广播拍卖结束: auctionId={}", auctionId);
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
            broadcastSystemStatus(); // 更新整个系统状态
            safeSend("/topic/assignment", ResponseDTO.success("队员已分配", playerId));
            logger.info("广播队员分配: playerId={}, teamId={}", playerId, teamId);
        } catch (Exception e) {
            logger.error("广播队员分配失败: playerId={}, teamId={}, error={}", playerId, teamId, e.getMessage(), e);
        }
    }
}
