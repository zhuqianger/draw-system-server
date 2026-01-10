package org.example.drawsystemserver.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.example.drawsystemserver.entity.Bid;

import java.math.BigDecimal;
import java.util.List;

@Mapper
public interface BidMapper {
    Bid selectById(Long id);
    List<Bid> selectByAuctionId(Long auctionId);
    List<Bid> selectByTeamId(Long teamId);
    Bid selectHighestByAuctionId(Long auctionId);
    List<Bid> selectRecentByAuctionId(@Param("auctionId") Long auctionId, @Param("limit") int limit);
    int insert(Bid bid);
    int update(Bid bid);
    int updateIsWinner(@Param("auctionId") Long auctionId, @Param("bidId") Long bidId, @Param("isWinner") Boolean isWinner);
    BigDecimal selectMaxAmountByAuctionId(Long auctionId);
}
