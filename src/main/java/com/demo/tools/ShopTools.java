package com.demo.tools;

import com.demo.model.ProductCatalog;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * ShopTools — the heart of the system.
 *
 * DUAL ROLE:
 *
 * Role 1 — Agent Tools (used by ShopAgent):
 *   The ChatClient registers these via MethodToolCallbackProvider.
 *   When a user asks a question, the LLM reads tool descriptions and
 *   autonomously decides which tool(s) to call and in what order.
 *   This autonomous think→call→observe loop is what makes it AGENTIC.
 *
 * Role 2 — MCP Server (used by external clients):
 *   The same methods are exposed via HTTP/SSE at /sse and /mcp/message.
 *   Claude Desktop, Cursor IDE, or any MCP client can call these tools
 *   without going through our ShopAgent at all.
 *
 * KEY INSIGHT: the @Tool annotation does BOTH jobs.
 * One codebase. Two modes of invocation.
 *
 * WHAT MAKES semanticSearch DIFFERENT from keyword search:
 *   Old way: products.stream().filter(p -> p.name().contains(keyword))
 *   This way: embed(query) → cosine_similarity(query_vec, product_vecs) → top-K
 *   User says "headphones for long flights" → finds Sony XM5 (noise cancellation)
 *   even though the user never said "Sony", "XM5", or "noise cancellation".
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ShopTools {

    private final VectorStore vectorStore;
    private final ProductCatalog catalog;

    // ── Tool 1: RAG Semantic Search ───────────────────────────────────────────

    @Tool(description = """
        Search the product catalog using semantic similarity (RAG).
        Understands natural language — "wireless audio for commuting" finds
        headphones with noise cancellation even without exact keyword match.
        Use this for any product discovery or recommendation query.
        Returns product IDs, names, prices, stock, and descriptions.
        """)
    public String semanticSearch(String query) {
        log.info(">>> TOOL: semanticSearch(\"{}\")", query);

        List<Document> results = vectorStore.similaritySearch(
            SearchRequest.builder()
                .query(query)
                .topK(4)
                .similarityThreshold(0.4)
                .build()
        );

        if (results.isEmpty()) {
            return "No products found matching: " + query;
        }

        return "Found " + results.size() + " relevant product(s):\n" +
            results.stream()
                .map(doc -> doc.getText())
                .collect(Collectors.joining("\n---\n"));
    }

    // ── Tool 2: Check exact stock for a product ID ────────────────────────────

    @Tool(description = """
        Check exact real-time stock availability for a specific product by its ID (e.g. P001).
        Always call this BEFORE placing an order to confirm availability.
        Returns current stock count and availability status.
        """)
    public String checkStock(String productId) {
        log.info(">>> TOOL: checkStock(\"{}\")", productId);
        return catalog.findById(productId)
            .map(p -> String.format(
                "Product: %s (ID: %s) | Stock: %d units | Price: $%.2f | %s",
                p.name(), p.id(), p.stock(), p.price(),
                p.stock() == 0 ? "OUT OF STOCK" :
                p.stock() < 5  ? "LOW STOCK — order soon!" : "IN STOCK"))
            .orElse("Product ID not found: " + productId);
    }

    // ── Tool 3: Place an order ────────────────────────────────────────────────

    @Tool(description = """
        Place an order for a product. Requires product ID, quantity, and customer email.
        IMPORTANT: Always call checkStock first to verify availability.
        Only call this when the user explicitly says they want to buy or order.
        Returns order confirmation number and total cost.
        """)
    public String placeOrder(String productId, int quantity, String customerEmail) {
        log.info(">>> TOOL: placeOrder(\"{}\", {}, \"{}\")", productId, quantity, customerEmail);
        return catalog.placeOrder(productId, quantity, customerEmail);
    }

    // ── Tool 4: Track an order ────────────────────────────────────────────────

    @Tool(description = """
        Get the status of an existing order using its order ID (format: ORD-XXXX).
        Use when the user asks about their order, wants tracking info, or order confirmation.
        """)
    public String getOrderStatus(String orderId) {
        log.info(">>> TOOL: getOrderStatus(\"{}\")", orderId);
        return catalog.getOrderStatus(orderId);
    }

    // ── Tool 5: List all products ─────────────────────────────────────────────

    @Tool(description = """
        List every product in the catalog with IDs, prices, and stock levels.
        Use when the user asks what's available, wants to browse everything,
        or needs to pick from the full catalog.
        """)
    public String listAllProducts() {
        log.info(">>> TOOL: listAllProducts()");
        return "Full catalog:\n" +
            catalog.all().stream()
                .map(p -> String.format("%-8s | %-30s | $%7.2f | Stock: %d",
                    p.id(), p.name(), p.price(), p.stock()))
                .collect(Collectors.joining("\n"));
    }
}
