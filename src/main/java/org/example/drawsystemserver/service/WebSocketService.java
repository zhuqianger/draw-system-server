package org.example.drawsystemserver.service;

import org.example.drawsystemserver.dto.SystemStatusDTO;

public interface WebSocketService {
    void broadcastSystemStatus();
    void broadcastAuctionStart(Long auctionId);
    void broadcastBidPlaced(Long auctionId, Long bidId);
    void broadcastAuctionFinished(Long auctionId);
    void broadcastPlayerAssigned(Long playerId, Long teamId);
    void broadcastTeamCostUpdated(Long teamId);
    void broadcastPlayerPoolChanged(Long sessionId, Long playerId);
    void broadcastPlayerRemovedFromTeam(Long teamId, Long playerId);
    void broadcastRollbackCompleted(Long sessionId);
    void broadcastSystemChanged(Long sessionId);
}
