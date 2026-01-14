package org.example.drawsystemserver.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.drawsystemserver.entity.*;
import org.example.drawsystemserver.mapper.*;
import org.example.drawsystemserver.service.AuctionSessionService;
import org.example.drawsystemserver.service.ExcelService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class AuctionSessionServiceImpl implements AuctionSessionService {

    @Autowired
    private AuctionSessionMapper sessionMapper;

    @Autowired
    private PlayerMapper playerMapper;

    @Autowired
    private TeamMapper teamMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private ExcelService excelService;

    @Autowired
    private AuctionMapper auctionMapper;

    @Autowired
    private BidMapper bidMapper;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AuctionSession createSession(String sessionName, MultipartFile excelFile, List<Integer> captainIndices, Long adminId) throws Exception {
        // 先保存Excel文件（避免多次读取MultipartFile的InputStream）
        String filePath = excelService.saveFile(excelFile);
        
        try {
            // 创建拍卖流程
            AuctionSession session = new AuctionSession();
            session.setSessionName(sessionName);
            session.setDataSourceFile(filePath);
            session.setStatus("CREATED");
            
            // 将队长序号列表转为JSON字符串
            String captainIdsJson = objectMapper.writeValueAsString(captainIndices);
            session.setCaptainIds(captainIdsJson);
            
            sessionMapper.insert(session);
            
            // 从保存的文件路径解析并导入队员数据（避免重复读取MultipartFile）
            List<Player> players = excelService.parsePlayersFromFilePath(filePath, session.getId());
            for (Player player : players) {
                playerMapper.insert(player);
            }
            
            // 计算所有队员费用的平均值（用于计算nowCost，但totalCost固定为18）
            List<BigDecimal> validCosts = players.stream()
                .filter(p -> p.getCost() != null)
                .map(Player::getCost)
                .collect(Collectors.toList());
            
            BigDecimal averageCost;
            if (validCosts.isEmpty()) {
                // 如果没有有效费用，使用默认值3
                averageCost = new BigDecimal("3");
            } else {
                averageCost = validCosts.stream()
                    .reduce(BigDecimal.ZERO, BigDecimal::add)
                    .divide(new BigDecimal(validCosts.size()), 2, RoundingMode.HALF_UP);
            }
            
            // 队伍总费用固定为18（包括队长和4个队员的费用总和）
            BigDecimal totalCost = new BigDecimal("18");
            
            // 获取该session下所有已插入的player（用于查找队长player）
            List<Player> allPlayers = playerMapper.selectBySessionId(session.getId());
            
            // 遍历队长序号列表，顺序对应userId（第1个序号→userId=1，第2个序号→userId=2，以此类推）
            for (int i = 0; i < captainIndices.size(); i++) {
                Integer captainGroupId = captainIndices.get(i); // 队长序号（groupId）
                Long captainUserId = (long) (i + 1); // 对应的userId（根据输入的序号在列表中的位置）
                
                // 验证该userId是否存在且为队长类型
                User captainUser = userMapper.selectById(captainUserId);
                if (captainUser == null || !"CAPTAIN".equals(captainUser.getUserType())) {
                    throw new RuntimeException("队长用户ID " + captainUserId + " 不存在或不是队长类型");
                }
                
                // 从已插入的player中查找groupId匹配的队长player
                Player captainPlayer = allPlayers.stream()
                    .filter(p -> captainGroupId.equals(p.getGroupId()))
                    .findFirst()
                    .orElse(null);
                
                if (captainPlayer == null) {
                    throw new RuntimeException("未找到groupId为 " + captainGroupId + " 的player");
                }
                
                // 计算当前剩余费用（总费用 - 队长费用）
                BigDecimal captainCost = captainPlayer.getCost() != null ? captainPlayer.getCost() : BigDecimal.ZERO;
                BigDecimal nowCost = totalCost.subtract(captainCost);
                
                Team team = new Team();
                team.setSessionId(session.getId());
                team.setCaptainId(captainPlayer.getId()); // captainId设置为player表中的id
                team.setUserId(captainUserId); // userId设置为对应的userId（根据输入的序号位置）
                team.setCaptainName(captainPlayer.getGroupName()); // captainName从player表的groupName获取
                team.setTeamName((i + 1) + "号队伍"); // teamName按照下标设置为"1号队伍"、"2号队伍"等
                team.setPlayerCount(0);
                team.setTotalCost(totalCost);
                team.setNowCost(nowCost);
                teamMapper.insert(team);
                
                // 更新队长player的teamId
                captainPlayer.setTeamId(team.getId());
                playerMapper.update(captainPlayer);
            }
            
            return session;
        } catch (Exception e) {
            // 如果发生异常，记录日志
            e.printStackTrace();
            throw e; // 重新抛出异常，让事务回滚
        }
    }

    @Override
    public List<AuctionSession> getAllSessions() {
        // 获取所有session，过滤掉DELETED状态的
        List<AuctionSession> allSessions = sessionMapper.selectAll();
        return allSessions.stream()
            .filter(session -> session.getStatus() == null || !"DELETED".equals(session.getStatus()))
            .collect(java.util.stream.Collectors.toList());
    }

    @Override
    public AuctionSession getSessionById(Long sessionId) {
        return sessionMapper.selectById(sessionId);
    }

    @Override
    @Transactional
    public AuctionSession activateSession(Long sessionId) {
        AuctionSession session = sessionMapper.selectById(sessionId);
        if (session != null) {
            session.setStatus("ACTIVE");
            sessionMapper.update(session);
        }
        return session;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AuctionSession deleteSession(Long sessionId) {
        AuctionSession session = sessionMapper.selectById(sessionId);
        if (session == null) {
            return null;
        }
        
        // 删除顺序：按照外键依赖关系，从子表到父表
        // 1. 删除bid表（依赖auction）
        bidMapper.deleteBySessionId(sessionId);
        
        // 2. 删除auction表（依赖player和team）
        auctionMapper.deleteBySessionId(sessionId);
        
        // 3. 删除player表（依赖session）
        playerMapper.deleteBySessionId(sessionId);
        
        // 4. 删除team表（依赖session）
        teamMapper.deleteBySessionId(sessionId);
        
        // 5. 最后删除auction_session表
        sessionMapper.deleteById(sessionId);
        
        return session;
    }

    @Override
    public boolean canUserAccessSession(Long sessionId, Long userId, String userType) {
        if ("ADMIN".equals(userType)) {
            return true; // 管理员可以访问所有流程
        }
        
        AuctionSession session = sessionMapper.selectById(sessionId);
        if (session == null) {
            return false;
        }
        
        try {
            // 解析队长ID列表
            List<Integer> captainIndices = objectMapper.readValue(session.getCaptainIds(), new TypeReference<List<Integer>>() {});
            // 检查当前用户是否在该流程的队长列表中
            // 这里简化处理，实际需要根据队长序号和用户ID的映射关系判断
            // 可以通过查找该session下的team来判断
            Team team = teamMapper.selectBySessionIdAndCaptainId(sessionId, userId);
            return team != null;
        } catch (Exception e) {
            return false;
        }
    }
}
