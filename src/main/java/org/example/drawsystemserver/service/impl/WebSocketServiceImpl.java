package org.example.drawsystemserver.service.impl;

import org.example.drawsystemserver.dto.ResponseDTO;
import org.example.drawsystemserver.dto.SystemStatusDTO;
import org.example.drawsystemserver.service.SystemService;
import org.example.drawsystemserver.service.WebSocketService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
public class WebSocketServiceImpl implements WebSocketService {

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @Autowired
    private SystemService systemService;

    /**
     * 广播系统状态（用于掉线后重新登录同步数据）
     */
    @Override
    public void broadcastSystemStatus() {
        SystemStatusDTO status = systemService.getSystemStatus();
        messagingTemplate.convertAndSend("/topic/system-status", ResponseDTO.success(status));
    }

    /**
     * 广播拍卖开始
     */
    @Override
    public void broadcastAuctionStart(Long auctionId) {
        broadcastSystemStatus(); // 更新整个系统状态
        messagingTemplate.convertAndSend("/topic/auction", ResponseDTO.success("拍卖开始", auctionId));
    }

    /**
     * 广播竞价
     */
    @Override
    public void broadcastBidPlaced(Long auctionId, Long bidId) {
        broadcastSystemStatus(); // 更新整个系统状态
        messagingTemplate.convertAndSend("/topic/bid", ResponseDTO.success("有新竞价", bidId));
    }

    /**
     * 广播拍卖结束
     */
    @Override
    public void broadcastAuctionFinished(Long auctionId) {
        broadcastSystemStatus(); // 更新整个系统状态
        messagingTemplate.convertAndSend("/topic/auction", ResponseDTO.success("拍卖结束", auctionId));
    }

    /**
     * 广播队员分配
     */
    @Override
    public void broadcastPlayerAssigned(Long playerId, Long teamId) {
        broadcastSystemStatus(); // 更新整个系统状态
        messagingTemplate.convertAndSend("/topic/assignment", ResponseDTO.success("队员已分配", playerId));
    }
}
