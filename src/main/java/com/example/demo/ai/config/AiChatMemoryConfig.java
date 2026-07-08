package com.example.demo.ai.config;

import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.InMemoryChatMemory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AiChatMemoryConfig {

    @Value("${groq.api-key}")
    private String groqApiKey;

    @Value("${spring.ai.openai.api-key}")
    private String geminiApiKey;

    @Bean
    public ChatMemory chatMemory() {
        return new InMemoryChatMemory();
    }

    @Bean
    public CustomGeminiEmbeddingModel customEmbeddingModel() {
        return new CustomGeminiEmbeddingModel(geminiApiKey);
    }

    @Bean
    public VectorStore vectorStore(CustomGeminiEmbeddingModel customEmbeddingModel) {
        return new CustomVectorStore(customEmbeddingModel);
    }

    @Bean
    public ChatModel groqChatModel() {
        OpenAiApi groqApi = new OpenAiApi("https://api.groq.com/openai/v1", groqApiKey);
        return new OpenAiChatModel(groqApi, OpenAiChatOptions.builder()
                .withModel("llama3-8b-8192")
                .withTemperature(0.3f)
                .build());
    }
}
