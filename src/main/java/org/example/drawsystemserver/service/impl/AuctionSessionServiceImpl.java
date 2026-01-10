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
    @Transactional
    public AuctionSession createSession(String sessionName, MultipartFile excelFile, List<Integer> captainIndices, Long adminId) throws Exception {
        // 保存Excel文件
        String filePath = excelService.saveFile(excelFile);
        
        // 创建拍卖流程
        AuctionSession session = new AuctionSession();
        session.setSessionName(sessionName);
        session.setDataSourceFile(filePath);
        session.setStatus("CREATED");
        
        // 将队长序号列表转为JSON字符串
        String captainIdsJson = objectMapper.writeValueAsString(captainIndices);
        session.setCaptainIds(captainIdsJson);
        
        sessionMapper.insert(session);
        
        // 解析并导入队员数据
        List<Player> players = excelService.parsePlayersFromExcel(excelFile, session.getId());
        for (Player player : players) {
            playerMapper.insert(player);
        }
        
        // 解析队长信息并创建队伍
        Map<Integer, String> captains = excelService.parseCaptainsFromExcel(excelFile, captainIndices);
        for (Integer captainIndex : captainIndices) {
            String captainName = captains.getOrDefault(captainIndex, "队长" + captainIndex);
            // 这里假设队长序号对应userId，实际可能需要根据业务调整
            // 创建队伍时，captainId可能需要在后续步骤中关联实际用户
            Team team = new Team();
            team.setSessionId(session.getId());
            team.setCaptainId(null); // 暂时为空，后续关联
            team.setCaptainName(captainName);
            team.setTeamName(captainName + "的队伍");
            team.setPlayerCount(0);
            teamMapper.insert(team);
        }
        
        return session;
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
