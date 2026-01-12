package org.example.drawsystemserver.service;

import org.example.drawsystemserver.entity.Auction;
import org.example.drawsystemserver.entity.Bid;

import java.util.List;

public interface AuctionService {
    Auction createAuction(Long sessionId, Long playerId); // 创建拍卖（抽取后，等待开始）
    Auction beginAuction(Long auctionId); // 管理员点击开始拍卖
    Auction getCurrentAuction();
    Auction getCurrentAuctionBySession(Long sessionId);
    Auction getById(Long id);
    Bid placeBid(Long auctionId, Long teamId, Long captainId, java.math.BigDecimal amount);
    Auction finishAuction(Long auctionId);
    Auction finishAuction(Long auctionId, boolean autoFinish); // autoFinish=true表示自动结束（第一阶段可能进入捡漏环节），false表示管理员手动结束（直接结束）
    Auction enterPickupPhase(Long auctionId); // 进入捡漏环节
    List<Auction> getActiveAuctions();
    Bid getHighestBid(Long auctionId);
    List<Bid> getRecentBids(Long auctionId, int limit);
    // 兼容旧接口
    Auction startAuction(Long sessionId, Long playerId, Integer durationSeconds);
}
