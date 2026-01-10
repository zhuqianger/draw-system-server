package org.example.drawsystemserver.controller;

import org.example.drawsystemserver.dto.LoginDTO;
import org.example.drawsystemserver.dto.ResponseDTO;
import org.example.drawsystemserver.entity.User;
import org.example.drawsystemserver.service.TeamService;
import org.example.drawsystemserver.service.UserService;
import org.example.drawsystemserver.util.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin
public class AuthController {

    @Autowired
    private UserService userService;

    @Autowired
    private TeamService teamService;

    @PostMapping("/login")
    public ResponseDTO<Map<String, Object>> login(@RequestBody LoginDTO loginDTO) {
        User user = userService.login(loginDTO.getUsername(), loginDTO.getPassword());
        if (user == null) {
            return ResponseDTO.error("用户名或密码错误");
        }

        // 如果是队长，确保有队伍
        if ("CAPTAIN".equals(user.getUserType())) {
            teamService.createTeam(user.getId(), user.getUsername() + "的队伍");
        }

        // 生成JWT token
        String token = JwtUtil.generateToken(user.getId(), user.getUsername(), user.getUserType());

        Map<String, Object> result = new HashMap<>();
        result.put("token", token);
        result.put("userId", user.getId());
        result.put("username", user.getUsername());
        result.put("userType", user.getUserType());

        return ResponseDTO.success("登录成功", result);
    }
}
