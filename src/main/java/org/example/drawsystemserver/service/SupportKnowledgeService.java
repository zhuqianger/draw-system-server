package org.example.drawsystemserver.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import org.example.drawsystemserver.dto.SupportAnswerDTO;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class SupportKnowledgeService {

    private static final double MATCH_THRESHOLD = 14.0;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private List<Entry> entries = List.of();

    @PostConstruct
    public void load() throws Exception {
        ClassPathResource resource = new ClassPathResource("support/knowledge-base.json");
        try (InputStream in = resource.getInputStream()) {
            entries = objectMapper.readValue(in, new TypeReference<List<Entry>>() {});
        }
        if (entries == null) {
            entries = List.of();
        }
    }

    public List<String> suggestions() {
        List<String> result = new ArrayList<>();
        for (Entry entry : entries) {
            if (entry.featured && entry.questions != null && !entry.questions.isEmpty()) {
                result.add(entry.questions.get(0));
            }
        }
        return result;
    }

    public SupportAnswerDTO ask(String question) {
        SupportAnswerDTO dto = new SupportAnswerDTO();
        dto.setSuggestions(suggestions());

        String normalized = normalize(question);
        if (normalized.length() < 2) {
            dto.setMatched(false);
            dto.setAnswer("请用一句话描述你想了解的操作，例如「队长怎么出价」或「普通池和流拍池有什么区别」。");
            return dto;
        }

        Set<String> questionGrams = bigrams(normalized);
        List<Scored> ranked = new ArrayList<>();
        for (Entry entry : entries) {
            ranked.add(new Scored(entry, score(entry, normalized, questionGrams)));
        }
        ranked.sort(Comparator.comparingDouble((Scored s) -> s.score).reversed());

        Scored best = ranked.get(0);
        if (best.score < MATCH_THRESHOLD) {
            dto.setMatched(false);
            dto.setAnswer("没有在使用说明里找到对应内容。可以换个说法，或点下面的常见问题。我目前只能回答本系统的操作和规则，不能查询某一场拍卖的实时数据。");
            return dto;
        }

        dto.setMatched(true);
        dto.setTitle(best.entry.title);
        dto.setAnswer(best.entry.answer);
        return dto;
    }

    private double score(Entry entry, String normalizedQuestion, Set<String> questionGrams) {
        double score = 0;
        if (entry.keywords != null) {
            for (String keyword : entry.keywords) {
                String normalizedKeyword = normalize(keyword);
                if (normalizedKeyword.length() >= 2 && normalizedQuestion.contains(normalizedKeyword)) {
                    score += 12 + Math.min(normalizedKeyword.length(), 6);
                }
            }
        }
        score += overlap(questionGrams, bigrams(normalize(entry.title))) * 24;
        double bestExample = 0;
        if (entry.questions != null) {
            for (String example : entry.questions) {
                bestExample = Math.max(bestExample, overlap(questionGrams, bigrams(normalize(example))));
            }
        }
        score += bestExample * 36;
        return score;
    }

    private double overlap(Set<String> left, Set<String> right) {
        if (left.isEmpty() || right.isEmpty()) {
            return 0;
        }
        int hit = 0;
        for (String gram : left) {
            if (right.contains(gram)) {
                hit++;
            }
        }
        return (double) hit / left.size();
    }

    private Set<String> bigrams(String text) {
        Set<String> grams = new HashSet<>();
        if (text == null || text.length() < 2) {
            return grams;
        }
        for (int i = 0; i < text.length() - 1; i++) {
            grams.add(text.substring(i, i + 2));
        }
        return grams;
    }

    private String normalize(String text) {
        if (text == null) {
            return "";
        }
        StringBuilder builder = new StringBuilder(text.length());
        for (int i = 0; i < text.length(); i++) {
            char ch = Character.toLowerCase(text.charAt(i));
            if (Character.isLetterOrDigit(ch) || isCjk(ch)) {
                builder.append(ch);
            }
        }
        return builder.toString();
    }

    private boolean isCjk(char ch) {
        return ch >= 0x4E00 && ch <= 0x9FFF;
    }

    public static class Entry {
        public String id;
        public String title;
        public boolean featured;
        public List<String> keywords;
        public List<String> questions;
        public String answer;
    }

    private static class Scored {
        private final Entry entry;
        private final double score;

        private Scored(Entry entry, double score) {
            this.entry = entry;
            this.score = score;
        }
    }
}
