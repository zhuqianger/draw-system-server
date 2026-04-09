package org.example.drawsystemserver.task;

import org.example.drawsystemserver.entity.Auction;
import org.example.drawsystemserver.entity.Bid;
import org.example.drawsystemserver.mapper.AuctionMapper;
import org.example.drawsystemserver.mapper.BidMapper;
import org.example.drawsystemserver.service.AuctionService;
import org.example.drawsystemserver.service.WebSocketService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 拍卖定时任务
 * 定期检查过期的拍卖并自动处理
 */
@Component
public class AuctionScheduledTask {

    @Autowired
    private AuctionMapper auctionMapper;

    @Autowired
    private BidMapper bidMapper;

    @Autowired
    private AuctionService auctionService;

    @Autowired
    private WebSocketService webSocketService;

    /**
     * 内存级超时通知去重：
     * key: auctionId
     * value: status|phase|endTime(秒级)
     *
     * 不改数据库时，用它避免同一场拍卖在“等待管理员确认”期间每秒重复推送 AUCTION_FINISHED。
     */
    private final Map<Long, String> timeoutNotifiedToken = new ConcurrentHashMap<>();

    private String buildTimeoutToken(Auction auction) {
        if (auction == null || auction.getId() == null || auction.getEndTime() == null) {
            return null;
        }
        LocalDateTime endTime = auction.getEndTime().withNano(0);
        return auction.getStatus() + "|" + auction.getPhase() + "|" + endTime;
    }

    private boolean shouldSkipByToken(Auction auction) {
        String token = buildTimeoutToken(auction);
        if (token == null || auction.getId() == null) {
            return false;
        }
        String notified = timeoutNotifiedToken.get(auction.getId());
        return token.equals(notified);
    }

    private void markTokenNotified(Auction auction) {
        String token = buildTimeoutToken(auction);
        if (token != null && auction.getId() != null) {
            timeoutNotifiedToken.put(auction.getId(), token);
        }
    }

    /**
     * 每1秒检查一次过期的拍卖
     * 处理逻辑：
     * 1. 第一阶段无人出价：自动进入捡漏环节
     * 2. 其他情况：只停止倒计时，等待管理员手动确认
     */
    @Scheduled(fixedRate = 1000) // 每1秒执行一次
    public void checkExpiredAuctions() {
        try {
            // 查询已过期但未结束的拍卖（endTime <= NOW()）
            List<Auction> expiredAuctions = auctionMapper.selectExpiredAuctions();

            // 清理不再需要的内存去重token：
            // 当拍卖被管理员手动结束后，不会再出现在 expiredAuctions 中，及时移除可避免长期运行时map累积。
            Set<Long> expiredIds = new HashSet<>();
            for (Auction auction : expiredAuctions) {
                if (auction != null && auction.getId() != null) {
                    expiredIds.add(auction.getId());
                }
            }
            timeoutNotifiedToken.keySet().removeIf(id -> !expiredIds.contains(id));
            
            for (Auction auction : expiredAuctions) {
                try {
                    if (shouldSkipByToken(auction)) {
                        continue;
                    }

                    LocalDateTime now = LocalDateTime.now();
                    
                    // 优先处理第一阶段无人出价的情况
                    if ("FIRST_PHASE".equals(auction.getStatus())) {
                        Bid highestBid = bidMapper.selectHighestByAuctionId(auction.getId());
                        if (highestBid == null) {
                            // 第一阶段无人出价，自动进入捡漏环节
                            auctionService.enterPickupPhase(auction.getId());
                            
                            // 推送WebSocket消息
                            webSocketService.broadcastAuctionFinished(auction.getId());
                            webSocketService.broadcastAuctionStart(auction.getId());
                            markTokenNotified(auction);
                            continue; // 处理完成，继续下一个
                        }
                    }
                    
                    // 其他情况（包括第一阶段有出价、捡漏环节）：只停止倒计时，不自动结束拍卖
                    // 将endTime设置为当前时间，确保倒计时显示为0
                    // 但保持拍卖状态不变，等待管理员手动确认
                    if (auction.getEndTime() == null || auction.getEndTime().isAfter(now)) {
                        auction.setEndTime(now);
                        auctionMapper.update(auction);
                    }
                    // 推送WebSocket消息，通知前端倒计时已结束，等待管理员确认
                    webSocketService.broadcastAuctionFinished(auction.getId());
                    markTokenNotified(auction);
                } catch (Exception e) {
                    // 静默处理错误，继续处理其他拍卖
                    // 避免一个拍卖的错误影响其他拍卖的处理
                }
            }
        } catch (Exception e) {
            // 静默处理错误，不影响定时任务继续运行
        }
    }
}
