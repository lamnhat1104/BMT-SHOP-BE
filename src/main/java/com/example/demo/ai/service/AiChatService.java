package com.example.demo.ai.service;

import com.example.demo.product.entity.Product;
import com.example.demo.product.repository.ProductRepository;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class AiChatService {

    private final ChatClient chatClient;
    private final ProductRepository productRepository;

    public AiChatService(ChatClient.Builder chatClientBuilder, ProductRepository productRepository) {
        this.chatClient = chatClientBuilder.build();
        this.productRepository = productRepository;
    }

    public String chat(String userMessage) {
        // Lấy danh sách sản phẩm active
        List<Product> products = productRepository.findAll().stream()
                .filter(p -> "available".equals(p.getStatus()) && (p.getIsDeleted() == null || !p.getIsDeleted()))
                .collect(Collectors.toList());

        String productContext = products.stream()
                .map(p -> "- " + p.getName() + ": " + String.format("%,.0f", p.getPrice()) + " VNĐ (Hãng: " + p.getBrand() + ")")
                .collect(Collectors.joining("\n"));

        String systemPrompt = "Bạn là nhân viên tư vấn bán hàng nhiệt tình và chuyên nghiệp của cửa hàng BMT-SHOP chuyên bán vợt cầu lông.\n" +
                "Dưới đây là danh sách sản phẩm hiện có tại cửa hàng:\n" +
                productContext + "\n\n" +
                "Quy tắc trả lời:\n" +
                "1. Nếu khách hàng cần tư vấn mua vợt, hãy gợi ý từ danh sách trên sao cho phù hợp với yêu cầu (ngân sách, trình độ, hãng) và báo giá.\n" +
                "2. Nếu khách hàng hỏi về đơn hàng (ví dụ: 'đơn hàng #DH001 của tôi đang ở đâu?'), hãy gọi hàm 'getOrderStatus' để lấy thông tin, sau đó trả lời lịch sự cho khách (ví dụ: 'Đơn hàng của bạn đang được giao' nếu status là shipping).\n" +
                "3. Luôn trả lời bằng tiếng Việt một cách tự nhiên, thân thiện.";

        return chatClient.prompt()
                .system(systemPrompt)
                .user(userMessage)
                .functions("getOrderStatus")
                .call()
                .content();
    }
}
