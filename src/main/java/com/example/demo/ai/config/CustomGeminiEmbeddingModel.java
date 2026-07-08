package com.example.demo.ai.config;

import org.springframework.ai.embedding.Embedding;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.EmbeddingRequest;
import org.springframework.ai.embedding.EmbeddingResponse;
import org.springframework.ai.embedding.EmbeddingResponseMetadata;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class CustomGeminiEmbeddingModel implements EmbeddingModel {

    private final String apiKey;
    private final RestTemplate restTemplate = new RestTemplate();

    public CustomGeminiEmbeddingModel(String apiKey) {
        this.apiKey = apiKey;
    }

    @Override
    public float[] embed(org.springframework.ai.document.Document document) {
        return embed(document.getContent());
    }

    @Override
    public float[] embed(String text) {
        return call(new EmbeddingRequest(List.of(text), null)).getResult().getOutput();
    }

    @Override
    public EmbeddingResponse call(EmbeddingRequest request) {
        String url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-embedding-2:batchEmbedContents?key=" + apiKey;

        List<String> instructions = request.getInstructions();
        List<Embedding> allEmbeddings = new java.util.ArrayList<>();
        int batchSize = 50;

        for (int i = 0; i < instructions.size(); i += batchSize) {
            List<String> chunk = instructions.subList(i, Math.min(instructions.size(), i + batchSize));
            
            List<Map<String, Object>> requests = chunk.stream().map(text -> Map.of(
                    "model", "models/gemini-embedding-2",
                    "content", Map.of("parts", List.of(Map.of("text", text)))
            )).collect(Collectors.toList());

            Map<String, Object> body = Map.of("requests", requests);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

            try {
                Map response = restTemplate.postForObject(url, entity, Map.class);
                List<Map<String, Object>> embeddingsList = (List<Map<String, Object>>) response.get("embeddings");
                
                for (int j = 0; j < embeddingsList.size(); j++) {
                    Map<String, Object> embeddingData = embeddingsList.get(j);
                    List<Double> values = (List<Double>) embeddingData.get("values");

                    float[] floatValues = new float[values.size()];
                    for (int k = 0; k < values.size(); k++) {
                        floatValues[k] = values.get(k).floatValue();
                    }
                    
                    allEmbeddings.add(new Embedding(floatValues, i + j));
                }
                
                // Sleep to avoid rate limits
                if (i + batchSize < instructions.size()) {
                    Thread.sleep(4500);
                }
            } catch (Exception e) {
                throw new RuntimeException("Lỗi khi gọi Gemini Embedding Batch: " + e.getMessage(), e);
            }
        }

        return new EmbeddingResponse(allEmbeddings, new EmbeddingResponseMetadata());
    }
}
