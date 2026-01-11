package org.example.drawsystemserver.service;

import org.example.drawsystemserver.entity.AuctionSession;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface AuctionSessionService {
    AuctionSession createSession(String sessionName, MultipartFile excelFile, List<Integer> captainIndices, Long adminId) throws Exception;
    List<AuctionSession> getAllSessions();
    AuctionSession getSessionById(Long sessionId);
    AuctionSession activateSession(Long sessionId);
    AuctionSession deleteSession(Long sessionId);
    boolean canUserAccessSession(Long sessionId, Long userId, String userType);
}
