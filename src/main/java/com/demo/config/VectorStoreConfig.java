package com.demo.config;

import com.demo.model.ProductCatalog;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * VectorStoreConfig — RAG indexing on startup.
 *
 * HOW RAG WORKS HERE:
 *
 * 1. At startup: each product is converted to rich descriptive text,
 *    embedded using nomic-embed-text (Ollama), and stored as a vector in Redis.
 *
 * 2. At query time (in ShopTools.semanticSearch):
 *    - User's question is embedded with the same model
 *    - Redis finds the most semantically similar product vectors
 *    - Top results are returned as context to the LLM
 *    - LLM generates a grounded answer using real product data
 *
 * Result: "Best headphones for travel?" returns Sony XM5 even if the user
 * didn't type "Sony" or "XM5" — semantic understanding, not keyword matching.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class VectorStoreConfig implements CommandLineRunner {

    private final VectorStore vectorStore;
    private final ProductCatalog catalog;

    @Override
    public void run(String... args) {
        log.info("=== Indexing products into Redis Vector Store for RAG ===");

        List<Document> documents = catalog.all().stream()
            .map(p -> new Document(
                p.toEmbeddingText(),
                Map.of(
                    "productId",  p.id(),
                    "name",       p.name(),
                    "category",   p.category(),
                    "price",      String.valueOf(p.price()),
                    "stock",      String.valueOf(p.stock())
                )
            ))
            .collect(Collectors.toList());

        vectorStore.add(documents);

        log.info("=== {} products indexed — RAG ready ===", documents.size());
    }
}
