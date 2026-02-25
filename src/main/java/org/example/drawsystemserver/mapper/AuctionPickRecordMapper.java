package org.example.drawsystemserver.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.example.drawsystemserver.entity.AuctionPickRecord;

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
}

