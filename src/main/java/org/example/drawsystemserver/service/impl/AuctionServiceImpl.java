package org.example.drawsystemserver.service.impl;

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

    @Autowired
    private AuctionPickRecordMapper auctionPickRecordMapper;

    /**
     * 根据当前队伍的 totalCost、队长费用以及选人纪录，重新计算队伍的
     * 剩余费用(nowCost) 和 队员数量(playerCount)。
     * 公式：
     *   nowCost = totalCost - captainCost - sum(pickRecord.amount for this team)
     */
    private void recalcTeamCost(Long teamId) {
        if (teamId == null) {
            return;
        }
        Team team = teamMapper.selectByIdForUpdate(teamId);
        if (team == null) {
            return;
        }

        // 统计该队已拍下队员的总费用
        BigDecimal usedCost = auctionPickRecordMapper.sumAmountByTeamId(teamId);
        if (usedCost == null) {
            usedCost = BigDecimal.ZERO;
        }

        // 队长费用
        BigDecimal captainCost = BigDecimal.ZERO;
        if (team.getCaptainId() != null) {
            Player captain = playerMapper.selectById(team.getCaptainId());
            if (captain != null && captain.getCost() != null) {
                captainCost = captain.getCost();
            }
        }

        BigDecimal totalCost = team.getTotalCost() != null ? team.getTotalCost() : BigDecimal.ZERO;
        BigDecimal nowCost = totalCost.subtract(captainCost).subtract(usedCost);
        if (nowCost.compareTo(BigDecimal.ZERO) < 0) {
            nowCost = BigDecimal.ZERO;
        }
        team.setNowCost(nowCost);

        // 按当前数据库中的实际队员数（不含队长）同步 playerCount
        List<Player> actualMembers = playerMapper.selectByTeamIdExcludingCaptain(teamId, team.getCaptainId());
        int actualCount = actualMembers != null ? actualMembers.size() : 0;
        team.setPlayerCount(actualCount);

        teamMapper.update(team);
    }

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
        
        // 第一梯度（>=3费）：基础定价-1，第二梯度（<3费）：基础定价-1.5
        BigDecimal startingPrice = isFirstTier 
            ? baseCost.subtract(new BigDecimal("1"))
            : baseCost.subtract(new BigDecimal("1.5"));
        
        // 最高出价 = 基础定价 + 2.5
        BigDecimal maxPrice = baseCost.add(new BigDecimal("2.5"));

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
        
        // 确保价格已正确设置
        if (auction.getStartingPrice() == null || auction.getMaxPrice() == null) {
            throw new RuntimeException("拍卖价格计算失败：startingPrice=" + auction.getStartingPrice() + ", maxPrice=" + auction.getMaxPrice());
        }

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
        
        // 捡漏环节：第一梯度（>=3费）：基础定价-1.5，第二梯度（<3费）：基础定价-2
        BigDecimal startingPrice = isFirstTier 
            ? baseCost.subtract(new BigDecimal("1.5"))
            : baseCost.subtract(new BigDecimal("2"));

        LocalDateTime now = LocalDateTime.now();
        // 捡漏环节30秒
        auction.setStartTime(now);
        auction.setEndTime(now.plusSeconds(30));
        auction.setDuration(30);
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
        
        // 检查是否已经过了5秒的等待期
        // 第一阶段：30秒，需要开始后至少5秒才能出价（剩余时间<=25秒）
        // 捡漏阶段：30秒，需要开始后至少5秒才能出价（剩余时间<=25秒）
        LocalDateTime now = LocalDateTime.now();
        long secondsSinceStart = java.time.Duration.between(auction.getStartTime(), now).getSeconds();
        int minWaitSeconds = 5; // 等待期5秒
        
        if (secondsSinceStart < minWaitSeconds) {
            long remainingWait = minWaitSeconds - secondsSinceStart;
            throw new RuntimeException("拍卖开始后需要等待" + minWaitSeconds + "秒才能出价（还需等待" + remainingWait + "秒）");
        }

        // 检查队伍是否已满员（总共5人：1个队长+4个队员）
        Team team = teamMapper.selectById(teamId);
        if (team == null) {
            throw new RuntimeException("队伍不存在");
        }
        
        // 验证队伍是否属于当前拍卖的session
        if (!auction.getSessionId().equals(team.getSessionId())) {
            throw new RuntimeException("队伍不属于当前拍卖流程（sessionId不匹配：拍卖sessionId=" + auction.getSessionId() + "，队伍sessionId=" + team.getSessionId() + "）");
        }
        
        // playerCount表示队员数量（不包括队长），所以满员是4个队员（加上队长共5人）
        if (team.getPlayerCount() == null || team.getPlayerCount() >= 4) {
            throw new RuntimeException("队伍已满员（最多5人：1个队长+4个队员，当前队员数：" + (team.getPlayerCount() != null ? team.getPlayerCount() : "null") + "）");
        }
        
        // 检查出价是否是0.5的倍数
        BigDecimal half = new BigDecimal("0.5");
        // 将出价乘以2，然后检查是否能被1整除
        BigDecimal multiplied = amount.multiply(new BigDecimal("2"));
        BigDecimal remainder = multiplied.remainder(BigDecimal.ONE);
        if (remainder.compareTo(BigDecimal.ZERO) != 0) {
            throw new RuntimeException("出价必须是0.5的倍数（当前出价：" + amount.toPlainString() + "）");
        }
        
        // 检查出价是否超过队伍剩余费用（向下取整到0.5的倍数）
        if (team.getNowCost() == null) {
            throw new RuntimeException("队伍剩余费用未设置，无法出价");
        }
        // 将剩余费用向下取整到0.5的倍数
        BigDecimal maxAllowedBid = team.getNowCost().divide(half, 0, java.math.RoundingMode.DOWN).multiply(half);
        if (amount.compareTo(maxAllowedBid) > 0) {
            throw new RuntimeException("出价不能超过队伍剩余费用（剩余：¥" + team.getNowCost().toPlainString() + "，最高可出：¥" + maxAllowedBid.toPlainString() + "，出价：¥" + amount.toPlainString() + "）");
        }
        
        // 检查出价后剩余费用是否足够：出价后剩余费用必须 >= 还差的队员数-1（因为出价后要减去这个出价，还要再招remainingSlots-1个队员）
        // 总共需要4个队员（不包括队长），还差 remainingSlots 个队员
        int remainingSlots = 4 - team.getPlayerCount(); // 还差几个队员（不包括队长）
        BigDecimal remainingCostAfterBid = team.getNowCost().subtract(amount);
        if (remainingSlots > 1 && remainingCostAfterBid.compareTo(new BigDecimal(remainingSlots - 1)) < 0) {
            throw new RuntimeException("出价后剩余费用不足，无法出价（出价后剩余：¥" + remainingCostAfterBid.toPlainString() + "，还需：" + (remainingSlots - 1) + "个队员，每个至少需要¥1.00）");
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
        
        // 确保最低出价不低于起拍价（费用下限）
        if (auction.getStartingPrice() != null && minBidAmount.compareTo(auction.getStartingPrice()) < 0) {
            minBidAmount = auction.getStartingPrice();
        }
        
        // 检查出价是否低于最低出价（起拍价或当前最高价+0.5）
        if (amount.compareTo(minBidAmount) < 0) {
            throw new RuntimeException("出价不能低于最低出价（最低出价：" + minBidAmount.toPlainString() + "，起拍价：" + auction.getStartingPrice().toPlainString() + "）");
        }
        
        // 检查出价是否低于起拍价（费用下限）
        if (auction.getStartingPrice() != null && amount.compareTo(auction.getStartingPrice()) < 0) {
            throw new RuntimeException("出价不能低于起拍价（起拍价：" + auction.getStartingPrice().toPlainString() + "）");
        }

        // 检查出价是否超过最高价（费用上限：基础定价 + 2.5）
        if (auction.getMaxPrice() != null && amount.compareTo(auction.getMaxPrice()) > 0) {
            throw new RuntimeException("出价不能超过最高价（最高价：" + auction.getMaxPrice().toPlainString() + "，基础定价 + 2.5）");
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
        
        // 如果出到最高价，停止倒计时（设置endTime为当前时间），但仍需要管理员手动点击结束拍卖确认
        if (auction.getMaxPrice() != null && amount.compareTo(auction.getMaxPrice()) >= 0) {
            LocalDateTime nowTime = LocalDateTime.now();
            auction.setEndTime(nowTime); // 停止倒计时
            auctionMapper.update(auction);
        }
        
        return bid;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void assignPlayerDirect(Long playerId, Long teamId, BigDecimal amount) {
        if (amount == null) {
            throw new RuntimeException("费用不能为空");
        }
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("费用必须大于0");
        }

        Player player = playerMapper.selectById(playerId);
        if (player == null) {
            throw new RuntimeException("队员不存在");
        }
        if (!"POOL".equals(player.getStatus())) {
            throw new RuntimeException("只有待拍卖池中的队员才能直接分配（当前状态：" + player.getStatus() + "）");
        }

        Team team = teamMapper.selectByIdForUpdate(teamId);
        if (team == null) {
            throw new RuntimeException("队伍不存在");
        }

        // 验证队伍与队员在同一拍卖流程
        if (player.getSessionId() == null || team.getSessionId() == null ||
                !player.getSessionId().equals(team.getSessionId())) {
            throw new RuntimeException("队伍与队员不在同一拍卖流程，无法分配");
        }

        // 队伍未满员（最多4个队员，不含队长）
        Integer currentCount = team.getPlayerCount() != null ? team.getPlayerCount() : 0;
        if (currentCount >= 4) {
            throw new RuntimeException("目标队伍已满员（最多4名队员）");
        }

        // 费用必须是0.5的倍数
        BigDecimal half = new BigDecimal("0.5");
        BigDecimal multiplied = amount.multiply(new BigDecimal("2"));
        BigDecimal remainder = multiplied.remainder(BigDecimal.ONE);
        if (remainder.compareTo(BigDecimal.ZERO) != 0) {
            throw new RuntimeException("费用必须是0.5的倍数（当前金额：" + amount.toPlainString() + "）");
        }

        if (team.getNowCost() == null) {
            throw new RuntimeException("队伍剩余费用未设置，无法分配队员");
        }
        // 不能超过队伍剩余费用（向下取整到0.5的倍数）
        BigDecimal maxAllowed = team.getNowCost().divide(half, 0, java.math.RoundingMode.DOWN).multiply(half);
        if (amount.compareTo(maxAllowed) > 0) {
            throw new RuntimeException("分配费用不能超过队伍剩余费用（剩余：¥" + team.getNowCost().toPlainString()
                    + "，最高可分配：¥" + maxAllowed.toPlainString() + "，当前：¥" + amount.toPlainString() + "）");
        }

        // 分配后剩余费用必须足够再补齐剩余空位（每个至少1）
        int remainingSlots = 4 - currentCount;
        BigDecimal remainingAfter = team.getNowCost().subtract(amount);
        if (remainingSlots > 1 && remainingAfter.compareTo(new BigDecimal(remainingSlots - 1)) < 0) {
            throw new RuntimeException("分配后剩余费用不足以补齐剩余队员（分配后剩余：¥" + remainingAfter.toPlainString()
                    + "，还需：" + (remainingSlots - 1) + "名队员，每个至少¥1.00）");
        }

        // 更新队员：标记为 SOLD，归属该队伍
        playerMapper.updateStatus(playerId, "SOLD");
        playerMapper.updateTeamId(playerId, teamId);
        playerMapper.updateCurrentAuctionId(playerId, null);

        // 写入选人纪录（没有真实拍卖，auctionId 置为0，仅用于回退）
        AuctionPickRecord record = new AuctionPickRecord();
        record.setSessionId(team.getSessionId());
        record.setAuctionId(0L);
        record.setPlayerId(playerId);
        record.setTeamId(teamId);
        record.setAmount(amount);
        Integer maxSeq = auctionPickRecordMapper.selectMaxSequenceBySessionId(team.getSessionId());
        int nextSeq = (maxSeq == null ? 1 : maxSeq + 1);
        record.setSequence(nextSeq);
        auctionPickRecordMapper.insert(record);

        // 统一重算该队伍的剩余费用与队员数量
        recalcTeamCost(teamId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateTeamCost(Long teamId, BigDecimal newNowCost) {
        if (newNowCost == null) {
            throw new RuntimeException("剩余费用不能为空");
        }
        if (newNowCost.compareTo(BigDecimal.ZERO) < 0) {
            throw new RuntimeException("剩余费用不能为负数");
        }

        Team team = teamMapper.selectByIdForUpdate(teamId);
        if (team == null) {
            throw new RuntimeException("队伍不存在");
        }

        // 已拍下队员的总费用来自选人纪录
        BigDecimal usedCost = auctionPickRecordMapper.sumAmountByTeamId(teamId);
        if (usedCost == null) {
            usedCost = BigDecimal.ZERO;
        }

        // 队长费用来自 player 表
        BigDecimal captainCost = BigDecimal.ZERO;
        if (team.getCaptainId() != null) {
            Player captain = playerMapper.selectById(team.getCaptainId());
            if (captain != null && captain.getCost() != null) {
                captainCost = captain.getCost();
            }
        }

        // 总费用 = 队长费用 + 已花费用 + 剩余费用
        BigDecimal totalCost = newNowCost.add(usedCost).add(captainCost);
        team.setNowCost(newNowCost);
        team.setTotalCost(totalCost);
        teamMapper.update(team);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void removePlayerFromTeam(Long teamId, Long playerId) {
        Team team = teamMapper.selectByIdForUpdate(teamId);
        if (team == null) {
            throw new RuntimeException("队伍不存在");
        }

        Player player = playerMapper.selectById(playerId);
        if (player == null) {
            throw new RuntimeException("队员不存在");
        }
        if (team.getCaptainId() != null && team.getCaptainId().equals(playerId)) {
            throw new RuntimeException("不能移除队长");
        }
        if (player.getTeamId() == null || !player.getTeamId().equals(teamId)) {
            throw new RuntimeException("该队员不在当前队伍中");
        }

        // 删除该队员的选人纪录（如果存在）
        List<AuctionPickRecord> records = auctionPickRecordMapper.selectByTeamIdAndPlayerId(teamId, playerId);
        if (!records.isEmpty()) {
            auctionPickRecordMapper.deleteByTeamIdAndPlayerId(teamId, playerId);
        }

        // 将队员放回待拍卖池（重置 teamId / currentAuctionId）
        playerMapper.updateStatus(playerId, "POOL");
        playerMapper.updateTeamId(playerId, null);
        playerMapper.updateCurrentAuctionId(playerId, null);

        // 重新根据选人纪录计算该队伍已用费用，并据此反推出最新剩余费用
        BigDecimal usedCost = auctionPickRecordMapper.sumAmountByTeamId(teamId);
        if (usedCost == null) {
            usedCost = BigDecimal.ZERO;
        }
        BigDecimal totalCost = team.getTotalCost() != null ? team.getTotalCost() : BigDecimal.ZERO;

        // 队长费用
        BigDecimal captainCost = BigDecimal.ZERO;
        if (team.getCaptainId() != null) {
            Player captain = playerMapper.selectById(team.getCaptainId());
            if (captain != null && captain.getCost() != null) {
                captainCost = captain.getCost();
            }
        }

        // 剩余费用 = 总费用 - 队长费用 - 已花费用
        BigDecimal newNowCost = totalCost.subtract(captainCost).subtract(usedCost);
        if (newNowCost.compareTo(BigDecimal.ZERO) < 0) {
            newNowCost = BigDecimal.ZERO;
        }
        team.setNowCost(newNowCost);

        // 同步队伍队员数量（不包括队长），防止累计误差
        List<Player> actualMembers = playerMapper.selectByTeamIdExcludingCaptain(teamId, team.getCaptainId());
        int actualCount = actualMembers != null ? actualMembers.size() : 0;
        team.setPlayerCount(actualCount);

        teamMapper.update(team);
    }

    @Override
    @Transactional
    public Auction finishAuction(Long auctionId) {
        // 默认是管理员手动结束
        return finishAuction(auctionId, false);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Auction finishAuction(Long auctionId, boolean autoFinish) {
        Auction auction = auctionMapper.selectById(auctionId);
        if (auction == null || "FINISHED".equals(auction.getStatus()) || "CANCELLED".equals(auction.getStatus())) {
            throw new RuntimeException("拍卖不存在或已结束");
        }

        // 找到最高竞价（检查是否有队长出价）
        Bid highestBid = bidMapper.selectHighestByAuctionId(auctionId);
        
        if (highestBid != null) {
            // 有竞价，标记获胜者
            bidMapper.updateIsWinner(auctionId, highestBid.getId(), true);
            auction.setWinningBidId(highestBid.getId());
            auction.setWinningTeamId(highestBid.getTeamId());
            
            // 验证获胜队伍是否属于当前拍卖的session
            Team winningTeam = teamMapper.selectById(highestBid.getTeamId());
            if (winningTeam == null) {
                throw new RuntimeException("获胜队伍不存在（teamId=" + highestBid.getTeamId() + "）");
            }
            if (!auction.getSessionId().equals(winningTeam.getSessionId())) {
                throw new RuntimeException("获胜队伍不属于当前拍卖流程（拍卖sessionId=" + auction.getSessionId() + "，队伍sessionId=" + winningTeam.getSessionId() + "）");
            }
            
            // 将队员分配给获胜队伍
            Player player = playerMapper.selectById(auction.getPlayerId());
            if (player != null) {
                // 验证队员是否属于当前拍卖的session
                if (!auction.getSessionId().equals(player.getSessionId())) {
                    throw new RuntimeException("队员不属于当前拍卖流程（拍卖sessionId=" + auction.getSessionId() + "，队员sessionId=" + player.getSessionId() + "）");
                }
                
                player.setStatus("SOLD");
                player.setTeamId(highestBid.getTeamId());
                player.setCurrentAuctionId(null);
                playerMapper.update(player);
                
                // 不再在这里手动维护队伍的 playerCount / nowCost，
                // 统一在插入选人纪录后通过 recalcTeamCost 进行重算，避免多路径导致的数据不一致。
            }

            // 记录本次成功拍卖的选人纪录（无人拍到的不记录）
            AuctionPickRecord record = new AuctionPickRecord();
            record.setSessionId(auction.getSessionId());
            record.setAuctionId(auction.getId());
            record.setPlayerId(auction.getPlayerId());
            record.setTeamId(highestBid.getTeamId());
            record.setAmount(highestBid.getAmount());
            Integer maxSeq = auctionPickRecordMapper.selectMaxSequenceBySessionId(auction.getSessionId());
            int nextSeq = (maxSeq == null ? 1 : maxSeq + 1);
            record.setSequence(nextSeq);
            auctionPickRecordMapper.insert(record);
            
            // 统一根据 totalCost / 队长费用 / 所有选人纪录重算该队伍的剩余费用与队员数量
            recalcTeamCost(highestBid.getTeamId());
            
            // 有出价时，直接结束拍卖
            auction.setStatus("FINISHED");
            auction.setPhase(null);
            auctionMapper.update(auction);
            return auction;
        } else {
            // 没有竞价（没有队长出价）
            // 第一阶段自动结束：如果第一阶段30s倒计时结束且无人出价，进入捡漏环节
            if (autoFinish && "FIRST_PHASE".equals(auction.getStatus())) {
                // 自动结束且是第一阶段，检查是否有出价，如果没有则进入捡漏环节
                return enterPickupPhase(auctionId);
            } else {
                // 管理员手动结束，或者捡漏环节自动结束，直接结束拍卖，将队员放回待拍卖池
                Player player = playerMapper.selectById(auction.getPlayerId());
                if (player != null) {
                    player.setStatus("POOL");
                    player.setCurrentAuctionId(null);
                    playerMapper.update(player);
                }
                auction.setStatus("FINISHED");
                auction.setPhase(null);
                auctionMapper.update(auction);
                return auction;
            }
        }
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

    @Override
    public List<AuctionPickRecord> getPickRecordsBySession(Long sessionId) {
        return auctionPickRecordMapper.selectBySessionIdOrderBySequence(sessionId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void rollbackToPickRecord(Long recordId) {
        AuctionPickRecord target = auctionPickRecordMapper.selectById(recordId);
        if (target == null) {
            throw new RuntimeException("选人纪录不存在，无法回退");
        }
        Long sessionId = target.getSessionId();
        Integer targetSeq = target.getSequence();
        if (targetSeq == null) {
            throw new RuntimeException("选人纪录缺少顺序号，无法回退");
        }

        // 查询该流程下的全部选人纪录，按顺序区分需保留和需回退的部分
        List<AuctionPickRecord> allRecords = auctionPickRecordMapper.selectBySessionIdOrderBySequence(sessionId);
        if (allRecords == null || allRecords.isEmpty()) {
            return;
        }

        java.util.List<AuctionPickRecord> keptRecords = new java.util.ArrayList<>();
        java.util.List<AuctionPickRecord> revertedRecords = new java.util.ArrayList<>();
        for (AuctionPickRecord r : allRecords) {
            if (r.getSequence() != null && r.getSequence() < targetSeq) {
                keptRecords.add(r);
            } else if (r.getSequence() != null && r.getSequence() >= targetSeq) {
                revertedRecords.add(r);
            }
        }

        // 删除需要回退的选人纪录（从目标节点开始及之后的所有纪录全部删除）
        auctionPickRecordMapper.deleteBySessionIdAndSequenceGte(sessionId, targetSeq);

        // 获取该流程下的队伍与队员
        List<Team> teams = teamMapper.selectBySessionId(sessionId);
        List<Player> players = playerMapper.selectBySessionId(sessionId);

        java.util.Set<Long> captainPlayerIds = new java.util.HashSet<>();
        for (Team team : teams) {
            if (team.getCaptainId() != null) {
                captainPlayerIds.add(team.getCaptainId());
            }
        }

        // 构建：保留记录中每个队伍对应的成交列表、每个队员对应的保留记录
        java.util.Map<Long, java.util.List<AuctionPickRecord>> teamKeptRecords = new java.util.HashMap<>();
        java.util.Map<Long, AuctionPickRecord> playerKeptRecord = new java.util.HashMap<>();
        for (AuctionPickRecord r : keptRecords) {
            if (r.getTeamId() != null) {
                teamKeptRecords.computeIfAbsent(r.getTeamId(), k -> new java.util.ArrayList<>()).add(r);
            }
            if (r.getPlayerId() != null) {
                playerKeptRecord.put(r.getPlayerId(), r);
            }
        }

        // 1）同步队员归属：目标节点及之后的队员从队伍中移除、重新进入待拍卖池；保留记录中的队员留在对应队伍
        for (Player player : players) {
            Long playerId = player.getId();
            if (playerId == null) continue;
            if (captainPlayerIds.contains(playerId)) continue; // 队长不参与回退

            AuctionPickRecord kept = playerKeptRecord.get(playerId);
            String newStatus;
            Long newTeamId;
            if (kept != null) {
                player.setStatus("SOLD");
                player.setTeamId(kept.getTeamId());
                player.setCurrentAuctionId(null);
                newStatus = "SOLD";
                newTeamId = kept.getTeamId();
            } else {
                // 该节点及之后被选中的队员：从队伍中删除，放回待拍卖池
                player.setStatus("POOL");
                player.setTeamId(null);
                player.setCurrentAuctionId(null);
                newStatus = "POOL";
                newTeamId = null;
            }
            // 注意：不能用通用 update，因为其中 teamId 的动态 SQL 不会在为 null 时更新列
            playerMapper.updateStatus(playerId, newStatus);
            playerMapper.updateTeamId(playerId, newTeamId);
            playerMapper.updateCurrentAuctionId(playerId, null);
        }

        // 2）同步队伍费用与成员数：按保留记录重算 nowCost，并按实际队员数同步 playerCount
        for (Team team : teams) {
            java.util.List<AuctionPickRecord> list = teamKeptRecords.get(team.getId());

            // 重新计算该队已用费用（仅根据选人纪录）
            java.math.BigDecimal usedCost = java.math.BigDecimal.ZERO;
            if (list != null) {
                for (AuctionPickRecord r : list) {
                    if (r.getAmount() != null) {
                        usedCost = usedCost.add(r.getAmount());
                    }
                }
            }

            java.math.BigDecimal totalCost = team.getTotalCost() != null
                    ? team.getTotalCost()
                    : java.math.BigDecimal.ZERO;

            // 队长费用
            java.math.BigDecimal captainCost = java.math.BigDecimal.ZERO;
            if (team.getCaptainId() != null) {
                Player captainPlayer = playerMapper.selectById(team.getCaptainId());
                if (captainPlayer != null && captainPlayer.getCost() != null) {
                    captainCost = captainPlayer.getCost();
                }
            }

            // 剩余费用 = 总费用 - 队长费用 - 已花费用
            java.math.BigDecimal newNowCost = totalCost.subtract(captainCost).subtract(usedCost);
            if (newNowCost.compareTo(java.math.BigDecimal.ZERO) < 0) {
                newNowCost = java.math.BigDecimal.ZERO;
            }
            team.setNowCost(newNowCost);

            // 按当前数据库中的实际队员数（不含队长）同步 playerCount
            List<Player> actualMembers = playerMapper.selectByTeamIdExcludingCaptain(team.getId(), team.getCaptainId());
            int actualCount = actualMembers != null ? actualMembers.size() : 0;
            team.setPlayerCount(actualCount);

            teamMapper.update(team);
        }
    }
}
