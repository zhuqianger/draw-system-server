package org.example.drawsystemserver.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.example.drawsystemserver.entity.Auction;

import java.util.List;

@Mapper
public interface AuctionMapper {
    Auction selectById(Long id);
    Auction selectActiveByPlayerId(Long playerId);
    Auction selectCurrentActive();
    Auction selectCurrentActiveBySessionId(Long sessionId);
    List<Auction> selectByStatus(String status);
    List<Auction> selectBySessionId(Long sessionId);
    List<Auction> selectAll();
    List<Auction> selectExpiredAuctions(); // 查询已过期但未结束的拍卖（endTime已过但状态为FIRST_PHASE或PICKUP_PHASE）
    int insert(Auction auction);
    int update(Auction auction);
    int updateStatus(Long id, String status);
    int updateWinning(Long id, Long winningBidId, Long winningTeamId);
    int deleteBySessionId(Long sessionId);
}
