package com.example.demo.ai.service;

import com.example.demo.ai.dto.ChatRequest;
import com.example.demo.product.dto.ProductResponse;
import com.example.demo.product.service.ProductService;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.SystemPromptTemplate;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class AiChatService {

    private final ChatModel chatModel;
    private final VectorStore vectorStore;
    private final ProductService productService;
    private final com.example.demo.ai.config.CustomGeminiEmbeddingModel embeddingModel;

    public AiChatService(ChatModel groqChatModel, VectorStore vectorStore, ProductService productService, com.example.demo.ai.config.CustomGeminiEmbeddingModel embeddingModel) {
        this.chatModel = groqChatModel;
        this.vectorStore = vectorStore;
        this.productService = productService;
        this.embeddingModel = embeddingModel;
    }

    @org.springframework.beans.factory.annotation.Value("${groq.api-key}")
    private String groqApiKey;

    public Map<String, Object> chat(ChatRequest request) {
        String message = request.getMessage();
        if (message == null || message.trim().isEmpty()) {
            return Map.of("response", "Vui lòng nhập câu hỏi.", "related_products", List.of());
        }

        // Search similar products in vector store
        List<Document> similarDocuments = vectorStore.similaritySearch(SearchRequest.query(message).withTopK(5));
        
        String context = similarDocuments.stream()
                .map(Document::getContent)
                .collect(Collectors.joining("\n\n"));

        List<Map<String, Object>> relatedProducts = similarDocuments.stream()
                .map(doc -> {
                    Map<String, Object> meta = doc.getMetadata();
                    Map<String, Object> productMap = new java.util.LinkedHashMap<>();
                    productMap.put("id", meta.get("id"));
                    productMap.put("title", meta.get("name"));
                    productMap.put("brand", meta.get("brand"));
                    productMap.put("price", meta.get("price"));
                    productMap.put("link", "/products/" + meta.get("id"));
                    return productMap;
                })
                .distinct()
                .collect(Collectors.toList());

        String systemPrompt = """
                Bạn là nhân viên tư vấn bán hàng chuyên nghiệp của BMT Shop - cửa hàng đồ cầu lông.

                NHIỆM VỤ:
                1. Tư vấn vợt, giày, quần áo, túi, phụ kiện cầu lông dựa trên Context sản phẩm được cung cấp.
                2. Nếu khách hỏi về giá, hãng, tồn kho, màu sắc, thông số hoặc biến thể, chỉ trả lời theo Context.
                3. Nếu khách cần gợi ý mua hàng, hãy nêu rõ lý do sản phẩm phù hợp với nhu cầu, ngân sách, trình độ và thương hiệu nếu có.
                4. Nếu khách chào hỏi hoặc hỏi xã giao, trả lời ngắn gọn, lịch sự và thân thiện.

                QUY TẮC:
                - Tuyệt đối không bịa đặt giá cả, tồn kho, thương hiệu, biến thể hoặc thông số kỹ thuật.
                - Nếu Context không có thông tin cần thiết, hãy nói rằng hiện chưa có dữ liệu sản phẩm phù hợp trong hệ thống BMT Shop.
                - Luôn trả lời bằng tiếng Việt tự nhiên, nhiệt tình, không quá dài.
                - Khi gợi ý sản phẩm, nên nhắc lại tên sản phẩm và giá nếu Context có giá.
                
                Context sản phẩm:
                """ + context;

        // Bypassing Spring AI ChatModel due to OpenAI Usage null bug with Groq. Using direct RestTemplate call.
        org.springframework.web.client.RestTemplate restTemplate = new org.springframework.web.client.RestTemplate();
        org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
        headers.setContentType(org.springframework.http.MediaType.APPLICATION_JSON);
        headers.setBearerAuth(groqApiKey);

        Map<String, Object> body = Map.of(
            "model", "llama-3.1-8b-instant",
            "messages", List.of(
                Map.of("role", "system", "content", systemPrompt),
                Map.of("role", "user", "content", message)
            ),
            "temperature", 0.7
        );

        org.springframework.http.HttpEntity<Map<String, Object>> entity = new org.springframework.http.HttpEntity<>(body, headers);
        String url = "https://api.groq.com/openai/v1/chat/completions";
        
        String responseContent = "Xin lỗi, không thể trả lời lúc này.";
        try {
            java.util.Map<String, Object> groqResponse = restTemplate.postForObject(url, entity, java.util.Map.class);
            if (groqResponse != null && groqResponse.containsKey("choices")) {
                List<Map<String, Object>> choices = (List<Map<String, Object>>) groqResponse.get("choices");
                if (!choices.isEmpty()) {
                    Map<String, Object> firstChoice = choices.get(0);
                    Map<String, Object> msg = (Map<String, Object>) firstChoice.get("message");
                    responseContent = (String) msg.get("content");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            responseContent = "Lỗi khi gọi Groq API: " + e.getMessage();
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("response", responseContent);
        result.put("answer", responseContent);
        result.put("related_products", relatedProducts);
        result.put("citations", relatedProducts.stream().map(p -> "product_id:" + p.get("id")).collect(Collectors.toList()));
        return result;
    }

    @jakarta.annotation.PostConstruct
    public void init() {
        if (vectorStore instanceof com.example.demo.ai.config.CustomVectorStore) {
            java.io.File file = new java.io.File("vector_store.json");
            if (file.exists()) {
                try {
                    ((com.example.demo.ai.config.CustomVectorStore) vectorStore).load(file);
                    System.out.println("Loaded vector store from file: " + file.getAbsolutePath());
                } catch (Exception e) {
                    System.err.println("Failed to load vector store: " + e.getMessage());
                }
            }
        }
    }

    public Map<String, Object> syncProducts() {
        try {
            List<ProductResponse> products = productService.getAllProducts();
            
            // Check if we need to sync (if store already has products, skip for now to save quota)
            // A simple check is to see if similaritySearch returns anything for a generic query
            List<Document> existing = vectorStore.similaritySearch(SearchRequest.query("a").withTopK(1));
            if (!existing.isEmpty()) {
                return Map.of("message", "Dữ liệu đã được đồng bộ trước đó. Bỏ qua để tiết kiệm quota.");
            }

            // Giới hạn 90 sản phẩm để tránh rate limit của Gemini (100 req/min)
            List<Document> documents = products.stream().limit(90).map(product -> {
                String content = String.format("ID: %d\\nTên: %s\\nThương hiệu: %s\\nGiá: %f\\nMô tả: %s",
                        product.getId(), product.getName(), product.getBrand(), product.getPrice(), product.getDescription());
                Map<String, Object> metadata = Map.of(
                        "id", product.getId(),
                        "name", product.getName() != null ? product.getName() : "",
                        "brand", product.getBrand() != null ? product.getBrand() : "",
                        "price", product.getPrice() != null ? product.getPrice() : 0.0
                );
                return new Document(content, metadata);
            }).collect(Collectors.toList());

            List<String> contents = documents.stream().map(Document::getContent).collect(Collectors.toList());
            org.springframework.ai.embedding.EmbeddingResponse response = embeddingModel.call(
                new org.springframework.ai.embedding.EmbeddingRequest(contents, org.springframework.ai.embedding.EmbeddingOptionsBuilder.builder().build())
            );
            List<org.springframework.ai.embedding.Embedding> embeddings = response.getResults();
            for (int i = 0; i < documents.size(); i++) {
                documents.get(i).setEmbedding(embeddings.get(i).getOutput());
            }

            vectorStore.add(documents);
            
            if (vectorStore instanceof com.example.demo.ai.config.CustomVectorStore) {
                ((com.example.demo.ai.config.CustomVectorStore) vectorStore).save(new java.io.File("vector_store.json"));
            }

            return Map.of("message", "Đồng bộ thành công " + documents.size() + " sản phẩm.");
        } catch (Exception e) {
            e.printStackTrace();
            return Map.of("message", "Lỗi đồng bộ: " + e.getMessage());
        }
    }
}
