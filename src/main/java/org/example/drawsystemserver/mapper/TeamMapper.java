package org.example.drawsystemserver.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.example.drawsystemserver.entity.Team;

import java.math.BigDecimal;
import java.util.List;

@Mapper
public interface TeamMapper {
    Team selectById(Long id);
    Team selectByCaptainId(Long captainId);
    Team selectByUserId(Long userId);
    Team selectBySessionIdAndCaptainId(@Param("sessionId") Long sessionId, @Param("captainId") Long captainId);
    Team selectBySessionIdAndUserId(@Param("sessionId") Long sessionId, @Param("userId") Long userId);
    List<Team> selectBySessionId(Long sessionId);
    List<Team> selectAll();
    int insert(Team team);
    int update(Team team);
    int incrementPlayerCount(Long id);
    int decrementPlayerCount(Long id);
    int decreaseNowCost(@Param("id") Long id, @Param("amount") BigDecimal amount);
    Team selectByIdForUpdate(Long id); // 使用SELECT FOR UPDATE锁定行
    int deleteBySessionId(Long sessionId);
}
