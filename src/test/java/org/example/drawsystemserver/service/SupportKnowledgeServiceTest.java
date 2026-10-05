package org.example.drawsystemserver.service;

import org.example.drawsystemserver.dto.SupportAnswerDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SupportKnowledgeServiceTest {

    private SupportKnowledgeService service;

    @BeforeEach
    void setUp() throws Exception {
        service = new SupportKnowledgeService();
        service.load();
    }

    @Test
    void answersHowCaptainsBid() {
        SupportAnswerDTO answer = service.ask("队长怎么出价？");
        assertTrue(answer.isMatched());
        assertTrue(answer.getAnswer().contains("0.5"));
    }

    @Test
    void answersPoolDifference() {
        SupportAnswerDTO answer = service.ask("普通池和流拍池有什么区别");
        assertTrue(answer.isMatched());
        assertTrue(answer.getTitle().contains("流拍"));
    }

    @Test
    void rejectsUnrelatedQuestion() {
        SupportAnswerDTO answer = service.ask("今天天气怎么样");
        assertFalse(answer.isMatched());
    }
}
