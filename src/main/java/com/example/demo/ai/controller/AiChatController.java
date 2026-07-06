package com.example.demo.ai.controller;

import com.example.demo.ai.dto.ChatRequest;
import com.example.demo.ai.service.AiChatService;
import com.example.demo.ai.service.RateLimitingService;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/ai")
@CrossOrigin(origins = "*") // Hoặc chỉ định frontend URL
public class AiChatController {

    private final AiChatService aiChatService;
    private final RateLimitingService rateLimitingService;

    public AiChatController(AiChatService aiChatService, RateLimitingService rateLimitingService) {
        this.aiChatService = aiChatService;
        this.rateLimitingService = rateLimitingService;
    }

    @PostMapping(value = "/chat", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> chat(@RequestBody ChatRequest request, HttpServletRequest httpRequest) {
        // Lấy IP của client
        String clientIp = httpRequest.getRemoteAddr();
        
        // Kiểm tra Rate Limiting
        Bucket bucket = rateLimitingService.resolveBucket(clientIp);
        ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);
        
        if (!probe.isConsumed()) {
            return Flux.just("Quá nhiều yêu cầu. Vui lòng thử lại sau.");
        }

        // Tự động tạo chatId nếu chưa có
        String chatId = request.getChatId();
        if (chatId == null || chatId.isEmpty()) {
            chatId = UUID.randomUUID().toString();
        }

        try {
            return aiChatService.chat(chatId, request.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            return Flux.just("Có lỗi xảy ra: " + e.getMessage());
        }
    }
    
    @ExceptionHandler(Exception.class)
    public ResponseEntity<?> handleException(Exception e) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "AI Service Error: " + e.getMessage()));
    }
}
