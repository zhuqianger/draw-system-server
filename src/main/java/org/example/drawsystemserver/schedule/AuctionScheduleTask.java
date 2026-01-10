package org.example.drawsystemserver.schedule;

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
 * 每10秒检查一次过期的拍卖并自动结束
 */
@Component
public class AuctionScheduleTask {

    @Autowired
    private AuctionService auctionService;

    @Autowired
    private WebSocketService webSocketService;

    @Autowired
    private AuctionMapper auctionMapper;

    @Scheduled(fixedRate = 10000) // 每10秒执行一次
    public void checkExpiredAuctions() {
        try {
            List<Auction> expiredAuctions = auctionMapper.selectExpiredActive();
            for (Auction auction : expiredAuctions) {
                auctionService.finishAuction(auction.getId());
                webSocketService.broadcastAuctionFinished(auction.getId());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
