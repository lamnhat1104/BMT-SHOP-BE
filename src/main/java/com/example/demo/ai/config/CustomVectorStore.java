package com.example.demo.ai.config;

import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.SimpleVectorStore;

import java.util.List;

public class CustomVectorStore extends SimpleVectorStore {

    public CustomVectorStore(EmbeddingModel embeddingModel) {
        super(embeddingModel);
    }

    @Override
    public void doAdd(List<Document> documents) {
        for (Document document : documents) {
            if (document.getEmbedding() == null || document.getEmbedding().length == 0) {
                document.setEmbedding(this.embeddingModel.embed(document));
            }
            this.store.put(document.getId(), document);
        }
    }
}
