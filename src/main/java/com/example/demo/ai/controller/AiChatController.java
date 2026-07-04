package com.example.demo.ai.controller;

import com.example.demo.ai.dto.ChatRequest;
import com.example.demo.ai.service.AiChatService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/ai")
@CrossOrigin(origins = "*") // Hoặc chỉ định frontend URL
public class AiChatController {

    private final AiChatService aiChatService;

    public AiChatController(AiChatService aiChatService) {
        this.aiChatService = aiChatService;
    }

    @PostMapping("/chat")
    public ResponseEntity<?> chat(@RequestBody ChatRequest request) {
        try {
            String response = aiChatService.chat(request.getMessage());
            return ResponseEntity.ok(Map.of("response", response));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(Map.of("error", "Có lỗi xảy ra: " + e.getMessage()));
        }
    }
}
