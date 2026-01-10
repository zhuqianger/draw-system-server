package org.example.drawsystemserver.controller;

import org.example.drawsystemserver.dto.ResponseDTO;
import org.example.drawsystemserver.dto.SystemStatusDTO;
import org.example.drawsystemserver.service.SystemService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/system")
@CrossOrigin
public class SystemController {

    @Autowired
    private SystemService systemService;

    /**
     * 获取系统状态（用于掉线后重新登录同步数据）
     */
    @GetMapping("/status")
    public ResponseDTO<SystemStatusDTO> getSystemStatus() {
        SystemStatusDTO status = systemService.getSystemStatus();
        return ResponseDTO.success("同步成功", status);
    }
}
