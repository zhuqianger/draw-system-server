package org.example.drawsystemserver.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.example.drawsystemserver.entity.AuctionPickRecord;

import java.math.BigDecimal;
import java.util.List;

@Mapper
public interface AuctionPickRecordMapper {
    AuctionPickRecord selectById(Long id);

    /**
     * 按流程查询所有选人纪录，按 sequence 正序排列。
     */
    List<AuctionPickRecord> selectBySessionIdOrderBySequence(Long sessionId);

    /**
     * 查询该流程当前的最大 sequence，用于生成下一条纪录的顺序号。
     */
    Integer selectMaxSequenceBySessionId(Long sessionId);

    int insert(AuctionPickRecord record);

    int deleteById(Long id);

    /**
     * 删除某流程中 sequence 大于等于指定值的所有纪录，
     * 用于回退到某条纪录之前。
     */
    int deleteBySessionIdAndSequenceGte(@Param("sessionId") Long sessionId,
                                        @Param("sequence") Integer sequence);

    int deleteBySessionId(Long sessionId);

    /**
     * 统计某个队伍已拍下队员的总费用。
     */
    BigDecimal sumAmountByTeamId(Long teamId);

    /**
     * 查询某队伍下某个队员的选人纪录（通常只有一条）。
     */
    List<AuctionPickRecord> selectByTeamIdAndPlayerId(@Param("teamId") Long teamId,
                                                      @Param("playerId") Long playerId);

    /**
     * 删除某队伍下某个队员的所有选人纪录。
     */
    int deleteByTeamIdAndPlayerId(@Param("teamId") Long teamId,
                                  @Param("playerId") Long playerId);
}

