package org.example.drawsystemserver.service.impl;

import org.example.drawsystemserver.entity.Auction;
import org.example.drawsystemserver.entity.Bid;
import org.example.drawsystemserver.entity.Player;
import org.example.drawsystemserver.entity.Team;
import org.example.drawsystemserver.mapper.AuctionMapper;
import org.example.drawsystemserver.mapper.BidMapper;
import org.example.drawsystemserver.mapper.PlayerMapper;
import org.example.drawsystemserver.mapper.TeamMapper;
import org.example.drawsystemserver.service.AuctionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class AuctionServiceImpl implements AuctionService {

    @Autowired
    private AuctionMapper auctionMapper;

    @Autowired
    private BidMapper bidMapper;

    @Autowired
    private PlayerMapper playerMapper;

    @Autowired
    private TeamMapper teamMapper;

    /**
     * 创建拍卖（抽取后，等待管理员开始）
     */
    @Override
    @Transactional
    public Auction createAuction(Long sessionId, Long playerId) {
        // 检查该流程是否已有进行中的拍卖
        Auction currentAuction = auctionMapper.selectCurrentActiveBySessionId(sessionId);
        if (currentAuction != null) {
            throw new RuntimeException("该流程已有进行中的拍卖，请先结束当前拍卖");
        }

        Player player = playerMapper.selectById(playerId);
        if (player == null || !"POOL".equals(player.getStatus())) {
            throw new RuntimeException("队员不存在或不在待拍卖池中");
        }

        // 计算费用梯度和起拍价
        BigDecimal baseCost = player.getCost() != null ? player.getCost() : new BigDecimal("3");
        boolean isFirstTier = baseCost.compareTo(new BigDecimal("3")) >= 0;
        
        // 第一梯度：基础定价-0.5，第二梯度：基础定价-1
        BigDecimal startingPrice = isFirstTier 
            ? baseCost.subtract(new BigDecimal("0.5"))
            : baseCost.subtract(new BigDecimal("1"));
        
        // 最高出价 = 基础定价 + 3
        BigDecimal maxPrice = baseCost.add(new BigDecimal("3"));

        Auction auction = new Auction();
        auction.setSessionId(sessionId);
        auction.setPlayerId(playerId);
        auction.setStartTime(null); // 等待管理员开始
        auction.setEndTime(null);
        auction.setDuration(null);
        auction.setStatus("WAITING");
        auction.setPhase("WAITING");
        auction.setStartingPrice(startingPrice);
        auction.setMaxPrice(maxPrice);

        auctionMapper.insert(auction);

        // 更新队员状态
        player.setStatus("AUCTIONING");
        player.setCurrentAuctionId(auction.getId());
        playerMapper.update(player);

        return auction;
    }

    /**
     * 管理员点击开始拍卖
     */
    @Override
    @Transactional
    public Auction beginAuction(Long auctionId) {
        Auction auction = auctionMapper.selectById(auctionId);
        if (auction == null || !"WAITING".equals(auction.getStatus())) {
            throw new RuntimeException("拍卖不存在或不是等待开始状态");
        }

        LocalDateTime now = LocalDateTime.now();
        // 第一阶段30秒
        auction.setStartTime(now);
        auction.setEndTime(now.plusSeconds(30));
        auction.setDuration(30);
        auction.setStatus("FIRST_PHASE");
        auction.setPhase("FIRST_PHASE");
        
        auctionMapper.update(auction);
        return auction;
    }

    /**
     * 进入捡漏环节
     */
    @Override
    @Transactional
    public Auction enterPickupPhase(Long auctionId) {
        Auction auction = auctionMapper.selectById(auctionId);
        if (auction == null || !"FIRST_PHASE".equals(auction.getStatus())) {
            throw new RuntimeException("拍卖不存在或不是第一阶段");
        }

        // 检查第一阶段是否有出价
        Bid highestBid = bidMapper.selectHighestByAuctionId(auctionId);
        if (highestBid != null) {
            throw new RuntimeException("第一阶段已有出价，不能进入捡漏环节");
        }

        // 重新计算起拍价（捡漏环节）
        Player player = playerMapper.selectById(auction.getPlayerId());
        BigDecimal baseCost = player.getCost() != null ? player.getCost() : new BigDecimal("3");
        boolean isFirstTier = baseCost.compareTo(new BigDecimal("3")) >= 0;
        
        // 捡漏环节：第一梯度：基础定价-1，第二梯度：基础定价-1.5
        BigDecimal startingPrice = isFirstTier 
            ? baseCost.subtract(new BigDecimal("1"))
            : baseCost.subtract(new BigDecimal("1.5"));

        LocalDateTime now = LocalDateTime.now();
        // 捡漏环节20秒
        auction.setStartTime(now);
        auction.setEndTime(now.plusSeconds(20));
        auction.setDuration(20);
        auction.setStatus("PICKUP_PHASE");
        auction.setPhase("PICKUP_PHASE");
        auction.setStartingPrice(startingPrice);
        
        auctionMapper.update(auction);
        return auction;
    }

    /**
     * 兼容旧接口
     */
    @Override
    @Transactional
    public Auction startAuction(Long sessionId, Long playerId, Integer durationSeconds) {
        // 使用新的createAuction方法
        Auction auction = createAuction(sessionId, playerId);
        // 自动开始
        return beginAuction(auction.getId());
    }

    @Override
    public Auction getCurrentAuction() {
        return auctionMapper.selectCurrentActive();
    }

    @Override
    public Auction getCurrentAuctionBySession(Long sessionId) {
        return auctionMapper.selectCurrentActiveBySessionId(sessionId);
    }

    @Override
    public Auction getById(Long id) {
        return auctionMapper.selectById(id);
    }

    @Override
    @Transactional
    public Bid placeBid(Long auctionId, Long teamId, Long captainId, java.math.BigDecimal amount) {
        Auction auction = auctionMapper.selectById(auctionId);
        if (auction == null || !auction.getStatus().equals("FIRST_PHASE") && !auction.getStatus().equals("PICKUP_PHASE")) {
            throw new RuntimeException("拍卖不存在或未开始");
        }

        if (auction.getStartTime() == null || LocalDateTime.now().isAfter(auction.getEndTime())) {
            throw new RuntimeException("拍卖时间已结束");
        }

        // 检查队伍是否已满员（最多4人）
        Team team = teamMapper.selectById(teamId);
        if (team == null) {
            throw new RuntimeException("队伍不存在");
        }
        if (team.getPlayerCount() >= 4) {
            throw new RuntimeException("队伍已满员（最多4人）");
        }
        
        // 检查出价是否超过队伍剩余费用
        if (team.getNowCost() == null || amount.compareTo(team.getNowCost()) > 0) {
            throw new RuntimeException("出价不能超过队伍剩余费用（剩余：" + team.getNowCost() + "）");
        }
        
        // 检查出价后剩余费用是否足够：出价后剩余费用必须 >= 还差的队员数-1（因为出价后要减去这个出价，还要再招remainingSlots-1个队员）
        int remainingSlots = 4 - team.getPlayerCount(); // 还差几个队员
        BigDecimal remainingCostAfterBid = team.getNowCost().subtract(amount);
        if (remainingSlots > 1 && remainingCostAfterBid.compareTo(new BigDecimal(remainingSlots - 1)) < 0) {
            throw new RuntimeException("出价后剩余费用不足，无法出价（出价后剩余：" + remainingCostAfterBid + "，还需：" + (remainingSlots - 1) + "）");
        }

        // 检查出价是否低于起拍价
        if (auction.getStartingPrice() != null && amount.compareTo(auction.getStartingPrice()) < 0) {
            throw new RuntimeException("出价不能低于起拍价（起拍价：" + auction.getStartingPrice() + "）");
        }

        // 检查出价是否超过最高价
        if (auction.getMaxPrice() != null && amount.compareTo(auction.getMaxPrice()) > 0) {
            throw new RuntimeException("出价不能超过最高价（最高价：" + auction.getMaxPrice() + "）");
        }

        // 获取当前最高价
        Bid highestBid = bidMapper.selectHighestByAuctionId(auctionId);
        BigDecimal minBidAmount;
        
        if (highestBid != null) {
            // 如果已有出价，最低出价 = 当前最高价 + 0.5
            minBidAmount = highestBid.getAmount().add(new BigDecimal("0.5"));
        } else {
            // 如果没有出价，最低出价 = 起拍价
            minBidAmount = auction.getStartingPrice();
        }
        
        if (amount.compareTo(minBidAmount) < 0) {
            throw new RuntimeException("出价必须至少为 " + minBidAmount);
        }

        // 检查加价幅度：每次加价最少0.5
        if (highestBid != null) {
            BigDecimal increment = amount.subtract(highestBid.getAmount());
            if (increment.compareTo(new BigDecimal("0.5")) < 0) {
                throw new RuntimeException("每次加价最少0.5");
            }
        }

        Bid bid = new Bid();
        bid.setAuctionId(auctionId);
        bid.setTeamId(teamId);
        bid.setCaptainId(captainId);
        bid.setAmount(amount);
        bid.setBidTime(LocalDateTime.now());
        bid.setIsWinner(false);

        bidMapper.insert(bid);
        
        // 如果出到最高价，直接获得该队员
        if (auction.getMaxPrice() != null && amount.compareTo(auction.getMaxPrice()) >= 0) {
            // 立即结束拍卖，该队伍获胜
            finishAuction(auctionId);
        }
        
        return bid;
    }

    @Override
    @Transactional
    public Auction finishAuction(Long auctionId) {
        Auction auction = auctionMapper.selectById(auctionId);
        if (auction == null || "FINISHED".equals(auction.getStatus()) || "CANCELLED".equals(auction.getStatus())) {
            throw new RuntimeException("拍卖不存在或已结束");
        }

        // 找到最高竞价
        Bid highestBid = bidMapper.selectHighestByAuctionId(auctionId);
        
        if (highestBid != null) {
            // 有竞价，标记获胜者
            bidMapper.updateIsWinner(auctionId, highestBid.getId(), true);
            auction.setWinningBidId(highestBid.getId());
            auction.setWinningTeamId(highestBid.getTeamId());
            
            // 将队员分配给获胜队伍
            Player player = playerMapper.selectById(auction.getPlayerId());
            if (player != null) {
                player.setStatus("SOLD");
                player.setTeamId(highestBid.getTeamId());
                player.setCurrentAuctionId(null);
                playerMapper.update(player);
                
                // 增加队伍队员数量
                teamMapper.incrementPlayerCount(highestBid.getTeamId());
                
                // 减少队伍剩余费用（减去获胜出价）
                teamMapper.decreaseNowCost(highestBid.getTeamId(), highestBid.getAmount());
            }
        } else {
            // 没有竞价
            if ("FIRST_PHASE".equals(auction.getStatus())) {
                // 第一阶段没有出价，进入捡漏环节
                return enterPickupPhase(auctionId);
            } else {
                // 捡漏环节也没有出价，将队员放回待拍卖池
                Player player = playerMapper.selectById(auction.getPlayerId());
                if (player != null) {
                    player.setStatus("POOL");
                    player.setCurrentAuctionId(null);
                    playerMapper.update(player);
                }
                auction.setStatus("FINISHED");
                auctionMapper.update(auction);
            }
            return auction;
        }

        auction.setStatus("FINISHED");
        auction.setPhase(null);
        auctionMapper.update(auction);
        
        return auction;
    }

    @Override
    public List<Auction> getActiveAuctions() {
        return auctionMapper.selectByStatus("ACTIVE");
    }

    @Override
    public Bid getHighestBid(Long auctionId) {
        return bidMapper.selectHighestByAuctionId(auctionId);
    }

    @Override
    public List<Bid> getRecentBids(Long auctionId, int limit) {
        return bidMapper.selectRecentByAuctionId(auctionId, limit);
    }
}
