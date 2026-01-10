package org.example.drawsystemserver.service;

import org.example.drawsystemserver.entity.Player;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

public interface ExcelService {
    List<Player> parsePlayersFromExcel(MultipartFile file, Long sessionId) throws Exception;
    Map<Integer, String> parseCaptainsFromExcel(MultipartFile file, List<Integer> captainIndices) throws Exception;
    String saveFile(MultipartFile file) throws Exception;
}
