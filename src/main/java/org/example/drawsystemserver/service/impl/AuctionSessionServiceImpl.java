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
            
            // 从保存的文件路径解析队长信息并创建队伍
            Map<Integer, String> captains = excelService.parseCaptainsFromFilePath(filePath, captainIndices);
            // 遍历队长序号列表，顺序对应userId（第1个序号→userId=1，第2个序号→userId=2，以此类推）
            for (int i = 0; i < captainIndices.size(); i++) {
                Integer captainIndex = captainIndices.get(i);
                Long captainUserId = (long) (i + 1); // 队长序号在列表中的位置（从1开始）对应userId
                String captainName = captains.getOrDefault(captainIndex, "队长" + captainIndex);
                
                // 验证该userId是否存在且为队长类型
                User captainUser = userMapper.selectById(captainUserId);
                if (captainUser == null || !"CAPTAIN".equals(captainUser.getUserType())) {
                    throw new RuntimeException("队长用户ID " + captainUserId + " 不存在或不是队长类型");
                }
                
                Team team = new Team();
                team.setSessionId(session.getId());
                team.setCaptainId(captainUserId); // 设置对应的userId
                team.setCaptainName(captainName);
                team.setTeamName(captainName + "的队伍");
                team.setPlayerCount(0);
                teamMapper.insert(team);
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
        return sessionMapper.selectAll();
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
