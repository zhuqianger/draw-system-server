package org.example.drawsystemserver.service;

import org.example.drawsystemserver.entity.Auction;
import org.example.drawsystemserver.entity.Bid;

import java.util.List;

public interface AuctionService {
    Auction startAuction(Long sessionId, Long playerId, Integer durationSeconds);
    Auction getCurrentAuction();
    Auction getCurrentAuctionBySession(Long sessionId);
    Auction getById(Long id);
    Bid placeBid(Long auctionId, Long teamId, Long captainId, java.math.BigDecimal amount);
    Auction finishAuction(Long auctionId);
    List<Auction> getActiveAuctions();
    void checkAndFinishExpiredAuctions();
    Bid getHighestBid(Long auctionId);
    List<Bid> getRecentBids(Long auctionId, int limit);
}
