package org.example.drawsystemserver.task;

import org.example.drawsystemserver.entity.Auction;
import org.example.drawsystemserver.mapper.AuctionMapper;
import org.example.drawsystemserver.service.AuctionService;
import org.example.drawsystemserver.service.WebSocketService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 拍卖定时任务
 * 定期检查过期的拍卖并自动处理
 */
@Component
public class AuctionScheduledTask {

    @Autowired
    private AuctionMapper auctionMapper;

    @Autowired
    private AuctionService auctionService;

    @Autowired
    private WebSocketService webSocketService;

    /**
     * 每1秒检查一次过期的拍卖
     * 当第一阶段30s倒计时结束，检查是否有队长出价：
     * - 如果有出价，分配队员给最高出价的队长
     * - 如果没有出价，进入捡漏环节
     */
    @Scheduled(fixedRate = 1000) // 每1秒执行一次
    public void checkExpiredAuctions() {
        try {
            // 查询已过期但未结束的拍卖
            List<Auction> expiredAuctions = auctionMapper.selectExpiredAuctions();
            
            for (Auction auction : expiredAuctions) {
                try {
                    // 自动结束拍卖（autoFinish=true）
                    // finishAuction方法会根据是否有出价自动判断：
                    // - 如果有出价：分配队员给最高出价的队长
                    // - 如果没有出价且是第一阶段：进入捡漏环节
                    // - 如果没有出价且是捡漏环节：放回待拍卖池
                    Auction result = auctionService.finishAuction(auction.getId(), true);
                    
                    // 推送WebSocket消息
                    webSocketService.broadcastAuctionFinished(auction.getId());
                    
                    // 如果进入捡漏环节，推送消息
                    if ("PICKUP_PHASE".equals(result.getStatus())) {
                        webSocketService.broadcastAuctionStart(auction.getId());
                    }
                    
                    // 如果队员被分配，推送队员分配消息
                    if (result.getWinningTeamId() != null && result.getPlayerId() != null) {
                        webSocketService.broadcastPlayerAssigned(result.getPlayerId(), result.getWinningTeamId());
                    }
                } catch (Exception e) {
                    // 记录错误但继续处理其他拍卖
                    // 避免一个拍卖的错误影响其他拍卖的处理
                }
            }
        } catch (Exception e) {
            // 记录错误但不影响定时任务继续运行
        }
    }
}
