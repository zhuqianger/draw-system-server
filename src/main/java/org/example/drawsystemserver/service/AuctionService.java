package org.example.drawsystemserver.service;

import org.example.drawsystemserver.entity.Auction;
import org.example.drawsystemserver.entity.Bid;
import org.example.drawsystemserver.entity.AuctionPickRecord;

import java.math.BigDecimal;
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

    /**
     * 按流程获取全部选人纪录（仅包含拍卖成功的记录）。
     */
    List<AuctionPickRecord> getPickRecordsBySession(Long sessionId);

    /**
     * 根据选人纪录ID回退到该条纪录之前的状态（包含队伍费用与队员归属）。
     */
    void rollbackToPickRecord(Long recordId);

    /**
     * 管理员从待拍卖池直接将队员以指定费用分配到某个队伍。
     */
    void assignPlayerDirect(Long playerId, Long teamId, BigDecimal amount);

    /**
     * 管理员手动修改队伍当前剩余费用。
     * 修改后会同步更新 totalCost = 新的剩余费用 + 该队伍已拍下队员的总费用。
     */
    void updateTeamCost(Long teamId, BigDecimal newNowCost);

    /**
     * 管理员从队伍中移除指定队员（非队长），并将其放回待拍卖池。
     * 同时会退还该队员对应的拍卖费用到队伍剩余费用，并删除对应的选人纪录。
     */
    void removePlayerFromTeam(Long teamId, Long playerId);
}
