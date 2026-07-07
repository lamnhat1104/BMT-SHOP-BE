package com.example.demo.ai.service;

import com.example.demo.product.repository.ProductRepository;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

@Service
public class AiChatService {

    private final ChatClient chatClient;
    private final ChatMemory chatMemory;

    public AiChatService(ChatClient.Builder chatClientBuilder,
                         ProductRepository productRepository,
                         ChatMemory chatMemory) {
        this.chatClient = chatClientBuilder.build();
        this.chatMemory = chatMemory;
    }

    public Flux<String> chat(String chatId, String userMessage) {
        String systemPrompt = """
                Bạn là nhân viên tư vấn bán hàng nhiệt tình và chuyên nghiệp của cửa hàng BMT-SHOP, chuyên bán phụ kiện cầu lông (vợt, cầu, giày, túi...).
                Quy tắc trả lời:
                1. Luôn trả lời bằng tiếng Việt, tự nhiên và thân thiện.
                2. Tư vấn sản phẩm phù hợp dựa trên ngân sách, trình độ và nhu cầu của khách.
                3. Nếu không có đủ thông tin, hãy hỏi lại khách để tư vấn tốt hơn.
                """;

        return chatClient.prompt()
                .system(systemPrompt)
                .user(userMessage)
                // Nhớ lịch sử tối đa 10 tin nhắn gần nhất
                .advisors(new MessageChatMemoryAdvisor(chatMemory, chatId, 10))
                .stream()
                .content();
    }
}

