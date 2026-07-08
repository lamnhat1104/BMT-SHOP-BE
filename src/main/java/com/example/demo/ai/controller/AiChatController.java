package com.example.demo.ai.controller;

import com.example.demo.ai.dto.ChatRequest;
import com.example.demo.ai.service.AiChatService;
import com.example.demo.ai.service.RateLimitingService;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/ai")
@CrossOrigin(origins = "*")
public class AiChatController {

    private final AiChatService aiChatService;
    private final RateLimitingService rateLimitingService;

    public AiChatController(AiChatService aiChatService, RateLimitingService rateLimitingService) {
        this.aiChatService = aiChatService;
        this.rateLimitingService = rateLimitingService;
    }

    @PostMapping("/chat")
    public ResponseEntity<?> chat(@RequestBody ChatRequest request, HttpServletRequest httpRequest) {
        String clientIp = httpRequest.getRemoteAddr();
        Bucket bucket = rateLimitingService.resolveBucket(clientIp);
        ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);

        if (!probe.isConsumed()) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .body(Map.of("response", "Quá nhiều yêu cầu. Vui lòng thử lại sau."));
        }

        try {
            return ResponseEntity.ok(aiChatService.chat(request));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                    .body(Map.of("response", "Lỗi xử lý AI: " + e.getMessage()));
        }
    }

    @PostMapping("/sync")
    public ResponseEntity<?> syncProducts() {
        try {
            return ResponseEntity.ok(aiChatService.syncProducts());
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                    .body(Map.of("message", "Lỗi đồng bộ AI: " + e.getMessage()));
        }
    }
}
