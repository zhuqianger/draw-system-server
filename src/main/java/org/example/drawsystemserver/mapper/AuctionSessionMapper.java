package org.example.drawsystemserver.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.example.drawsystemserver.entity.AuctionSession;

import java.util.List;

@Mapper
public interface AuctionSessionMapper {
    AuctionSession selectById(Long id);
    List<AuctionSession> selectAll();
    List<AuctionSession> selectByStatus(String status);
    int insert(AuctionSession session);
    int update(AuctionSession session);
    int updateStatus(Long id, String status);
}
