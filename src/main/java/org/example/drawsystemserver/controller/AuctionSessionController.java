package org.example.drawsystemserver.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.example.drawsystemserver.dto.ResponseDTO;
import org.example.drawsystemserver.entity.AuctionSession;
import org.example.drawsystemserver.service.AuctionSessionService;
import org.example.drawsystemserver.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/session")
@CrossOrigin
public class AuctionSessionController {

    @Autowired
    private AuctionSessionService sessionService;

    @Autowired
    private UserService userService;

    /**
     * 创建新的拍卖流程（仅管理员）
     */
    @PostMapping("/create")
    public ResponseDTO<AuctionSession> createSession(
            @RequestParam String sessionName,
            @RequestParam MultipartFile excelFile,
            @RequestParam String captainIndices, // 逗号分隔的队长序号，如 "1,2,3"
            HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        if (!userService.isAdmin(userId)) {
            return ResponseDTO.error(403, "只有管理员可以创建拍卖流程");
        }

        try {
            // 解析队长序号
            List<Integer> indices = Arrays.stream(captainIndices.split(","))
                    .map(String::trim)
                    .map(Integer::parseInt)
                    .collect(Collectors.toList());

            AuctionSession session = sessionService.createSession(sessionName, excelFile, indices, userId);
            return ResponseDTO.success("拍卖流程创建成功", session);
        } catch (Exception e) {
            return ResponseDTO.error("创建失败：" + e.getMessage());
        }
    }

    /**
     * 获取所有拍卖流程
     */
    @GetMapping("/list")
    public ResponseDTO<List<AuctionSession>> getAllSessions() {
        List<AuctionSession> sessions = sessionService.getAllSessions();
        return ResponseDTO.success(sessions);
    }

    /**
     * 获取指定拍卖流程详情
     */
    @GetMapping("/{sessionId}")
    public ResponseDTO<AuctionSession> getSession(@PathVariable Long sessionId, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        String userType = (String) request.getAttribute("userType");

        if (!sessionService.canUserAccessSession(sessionId, userId, userType)) {
            return ResponseDTO.error(403, "无权限访问该拍卖流程");
        }

        AuctionSession session = sessionService.getSessionById(sessionId);
        if (session == null) {
            return ResponseDTO.error("拍卖流程不存在");
        }
        return ResponseDTO.success(session);
    }

    /**
     * 激活拍卖流程（开始拍卖）
     */
    @PostMapping("/{sessionId}/activate")
    public ResponseDTO<AuctionSession> activateSession(@PathVariable Long sessionId, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        if (!userService.isAdmin(userId)) {
            return ResponseDTO.error(403, "只有管理员可以激活拍卖流程");
        }

        try {
            AuctionSession session = sessionService.activateSession(sessionId);
            return ResponseDTO.success("拍卖流程已激活", session);
        } catch (Exception e) {
            return ResponseDTO.error("激活失败：" + e.getMessage());
        }
    }
}
