package org.example.drawsystemserver.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.example.drawsystemserver.entity.Player;

import java.util.List;

@Mapper
public interface PlayerMapper {
    Player selectById(Long id);
    List<Player> selectByStatus(String status);
    List<Player> selectBySessionId(Long sessionId);
    List<Player> selectBySessionIdAndStatus(Long sessionId, String status);
    List<Player> selectByTeamId(Long teamId);
    List<Player> selectByTeamIdExcludingCaptain(@Param("teamId") Long teamId, @Param("captainId") Long captainId);
    List<Player> selectAll();
    int insert(Player player);
    int update(Player player);
    int updateStatus(Long id, String status);
    int updateCurrentAuctionId(Long id, Long auctionId);
    int updateTeamId(Long id, Long teamId);
}
