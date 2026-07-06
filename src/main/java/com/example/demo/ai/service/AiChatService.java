package com.example.demo.ai.service;

import com.example.demo.product.entity.Product;
import com.example.demo.product.repository.ProductRepository;
import jakarta.annotation.PostConstruct;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.QuestionAnswerAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class AiChatService {

    private final ChatClient chatClient;
    private final ProductRepository productRepository;
    private final ChatMemory chatMemory;
    private final VectorStore vectorStore;

    public AiChatService(ChatClient.Builder chatClientBuilder, 
                         ProductRepository productRepository,
                         ChatMemory chatMemory,
                         VectorStore vectorStore) {
        this.chatClient = chatClientBuilder.build();
        this.productRepository = productRepository;
        this.chatMemory = chatMemory;
        this.vectorStore = vectorStore;
    }

    @PostConstruct
    public void loadProductsToVectorStore() {
        // Lấy danh sách sản phẩm active
        List<Product> products = productRepository.findAll().stream()
                .filter(p -> "available".equals(p.getStatus()) && (p.getIsDeleted() == null || !p.getIsDeleted()))
                .collect(Collectors.toList());

        // Chuyển đổi thành các Document để nhúng (embedding)
        List<Document> documents = products.stream()
                .map(p -> {
                    String content = String.format("Tên sản phẩm: %s\nGiá: %,.0f VNĐ\nHãng: %s\nMô tả: %s", 
                                                    p.getName(), p.getPrice(), p.getBrand(), p.getDescription() != null ? p.getDescription() : "");
                    return new Document(content, Map.of("productId", p.getId()));
                })
                .collect(Collectors.toList());

        if (!documents.isEmpty()) {
            try {
                vectorStore.add(documents);
            } catch (Exception e) {
                System.err.println("Warning: Failed to load products to vector store. " + e.getMessage());
            }
        }
    }

    public Flux<String> chat(String chatId, String userMessage) {
        String systemPrompt = "Bạn là nhân viên tư vấn bán hàng nhiệt tình và chuyên nghiệp của cửa hàng BMT-SHOP chuyên bán vợt cầu lông.\n" +
                "Quy tắc trả lời:\n" +
                "1. Nếu khách hàng cần tư vấn mua vợt, hãy gợi ý từ thông tin sản phẩm (documents) được cung cấp sao cho phù hợp với yêu cầu (ngân sách, trình độ, hãng) và báo giá.\n" +
                "2. Nếu khách hàng hỏi về đơn hàng (ví dụ: 'đơn hàng #DH001 của tôi đang ở đâu?'), hãy gọi hàm 'getOrderStatus' để lấy thông tin, sau đó trả lời lịch sự cho khách.\n" +
                "3. Luôn trả lời bằng tiếng Việt một cách tự nhiên, thân thiện.";

        return chatClient.prompt()
                .system(systemPrompt)
                .user(userMessage)
                .functions("getOrderStatus")
                // Thêm RAG Advisor để tự động chèn ngữ cảnh (thông tin sản phẩm liên quan) vào prompt
                .advisors(new QuestionAnswerAdvisor(vectorStore, SearchRequest.defaults().withTopK(5)))
                // Thêm Chat Memory Advisor để nhớ lịch sử cuộc trò chuyện
                .advisors(new MessageChatMemoryAdvisor(chatMemory, chatId, 10))
                .stream()
                .content();
    }
}
