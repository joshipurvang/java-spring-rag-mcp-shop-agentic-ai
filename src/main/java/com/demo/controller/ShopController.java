package com.demo.controller;

import com.demo.agent.ShopAgent;
import com.demo.model.ProductCatalog;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Collection;
import java.util.Map;

/**
 * REST API
 *
 * POST /api/chat           — talk to the AI agent
 * GET  /api/products       — view raw product catalog
 * GET  /api/orders         — view all orders placed
 * GET  /api/demo           — example queries to try
 *
 * MCP Server (auto-configured by Spring AI):
 * GET  /sse                — connect MCP client here (SSE stream)
 * POST /mcp/message        — send MCP messages
 */
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class ShopController {

    private final ShopAgent shopAgent;
    private final ProductCatalog catalog;

    @PostMapping("/chat")
    public Map<String, String> chat(@RequestBody Map<String, String> body) {
        String message = body.getOrDefault("message", "").trim();
        if (message.isEmpty()) return Map.of("error", "'message' field is required");
        return Map.of("message", message, "response", shopAgent.chat(message));
    }

    @GetMapping("/products")
    public Collection<ProductCatalog.Product> products() {
        return catalog.all();
    }

    @GetMapping("/orders")
    public Collection<ProductCatalog.Order> orders() {
        return catalog.allOrders();
    }

    @GetMapping("/demo")
    public Map<String, Object> demo() {
        return Map.of(
            "endpoint",  "POST /api/chat  body: {\"message\": \"...\"}",
            "mcpServer", Map.of(
                "sseEndpoint",     "GET  http://localhost:8080/sse",
                "messageEndpoint", "POST http://localhost:8080/mcp/message",
                "claudeDesktopConfig", Map.of(
                    "mcpServers", Map.of(
                        "shop-agent", Map.of("url", "http://localhost:8080/sse")
                    )
                )
            ),
            "tryTheseQueries", Map.of(
                "1_rag_search",   "What headphones are good for long-haul flights?",
                "2_rag_natural",  "I need something for video editing on the go",
                "3_stock_check",  "Is the MacBook Pro available?",
                "4_order",        "Buy 1 Sony headphones for alex@example.com",
                "5_out_of_stock", "Order the Philips Hue Kit for me",
                "6_track",        "What is the status of order ORD-1001?",
                "7_full_agent",   "Find me a portable gaming device, check stock, and order 1 for sam@example.com"
            ),
            "whatsNew", Map.of(
                "RAG",        "semanticSearch uses Redis Vector Store — natural language understood",
                "MCP_server", "Tools exposed at /sse — connect Claude Desktop or Cursor directly",
                "agentic",    "LLM chains tool calls autonomously — no routing code needed"
            )
        );
    }
}
