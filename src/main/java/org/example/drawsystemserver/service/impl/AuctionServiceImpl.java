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

    @Override
    @Transactional
    public Auction startAuction(Long sessionId, Long playerId, Integer durationSeconds) {
        // 检查该流程是否已有进行中的拍卖
        Auction currentAuction = auctionMapper.selectCurrentActiveBySessionId(sessionId);
        if (currentAuction != null) {
            throw new RuntimeException("该流程已有进行中的拍卖，请先结束当前拍卖");
        }

        Player player = playerMapper.selectById(playerId);
        if (player == null || !"POOL".equals(player.getStatus())) {
            throw new RuntimeException("队员不存在或不在待拍卖池中");
        }

        LocalDateTime now = LocalDateTime.now();
        Auction auction = new Auction();
        auction.setSessionId(sessionId);
        auction.setPlayerId(playerId);
        auction.setStartTime(now);
        auction.setEndTime(now.plusSeconds(durationSeconds));
        auction.setDuration(durationSeconds);
        auction.setStatus("ACTIVE");

        auctionMapper.insert(auction);

        // 更新队员状态
        player.setStatus("AUCTIONING");
        player.setCurrentAuctionId(auction.getId());
        playerMapper.update(player);

        return auction;
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
        if (auction == null || !"ACTIVE".equals(auction.getStatus())) {
            throw new RuntimeException("拍卖不存在或已结束");
        }

        if (LocalDateTime.now().isAfter(auction.getEndTime())) {
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

        // 检查出价是否高于当前最高价
        Bid highestBid = bidMapper.selectHighestByAuctionId(auctionId);
        if (highestBid != null && amount.compareTo(highestBid.getAmount()) <= 0) {
            throw new RuntimeException("出价必须高于当前最高价");
        }

        Bid bid = new Bid();
        bid.setAuctionId(auctionId);
        bid.setTeamId(teamId);
        bid.setCaptainId(captainId);
        bid.setAmount(amount);
        bid.setBidTime(LocalDateTime.now());
        bid.setIsWinner(false);

        bidMapper.insert(bid);
        return bid;
    }

    @Override
    @Transactional
    public Auction finishAuction(Long auctionId) {
        Auction auction = auctionMapper.selectById(auctionId);
        if (auction == null || !"ACTIVE".equals(auction.getStatus())) {
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
            // 没有竞价，将队员放回待拍卖池
            Player player = playerMapper.selectById(auction.getPlayerId());
            if (player != null) {
                player.setStatus("POOL");
                player.setCurrentAuctionId(null);
                playerMapper.update(player);
            }
        }

        auction.setStatus("FINISHED");
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
