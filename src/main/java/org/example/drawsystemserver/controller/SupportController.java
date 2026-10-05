package org.example.drawsystemserver.controller;

import org.example.drawsystemserver.dto.ResponseDTO;
import org.example.drawsystemserver.dto.SupportAnswerDTO;
import org.example.drawsystemserver.dto.SupportAskRequest;
import org.example.drawsystemserver.service.SupportKnowledgeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/support")
@CrossOrigin
public class SupportController {

    @Autowired
    private SupportKnowledgeService supportKnowledgeService;

    @GetMapping("/suggestions")
    public ResponseDTO<List<String>> suggestions() {
        return ResponseDTO.success(supportKnowledgeService.suggestions());
    }

    @PostMapping("/ask")
    public ResponseDTO<SupportAnswerDTO> ask(@RequestBody SupportAskRequest request) {
        if (request == null || request.getQuestion() == null || request.getQuestion().isBlank()) {
            return ResponseDTO.error("请输入问题");
        }
        if (request.getQuestion().length() > 200) {
            return ResponseDTO.error("问题请控制在 200 字以内");
        }
        return ResponseDTO.success(supportKnowledgeService.ask(request.getQuestion().trim()));
    }
}
