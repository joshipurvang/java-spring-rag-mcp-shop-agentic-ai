package com.demo.agent;

import com.demo.tools.ShopTools;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

/**
 * ShopAgent — the agentic AI loop.
 *
 * .defaultTools(shopTools) receives the @Service bean directly.
 * Spring AI scans it internally for @Tool annotated methods.
 *
 * CORRECT:
 *   .defaultTools(shopTools)
 *   // Spring AI inspects the bean, finds @Tool methods, registers them
 */
@Service
@Slf4j
public class ShopAgent {

    private final ChatClient chatClient;

    public ShopAgent(ChatClient.Builder builder, ShopTools shopTools) {
        this.chatClient = builder
            .defaultSystem("""
                You are ShopBot, an intelligent shopping assistant for ShopSmart electronics.

                You help customers:
                - Find products using natural language descriptions
                - Check stock availability and pricing
                - Place orders
                - Track existing orders

                RULES:
                - Always use tools to get real data — never invent prices, stock, or product names
                - For product search, always use semanticSearch — it understands natural language
                - Before placing any order, always call checkStock first
                - If the user wants to buy something and hasn't given their email, ask for it
                - Be concise and helpful
                - If stock is 0, tell the user and do NOT place the order
                """)
            // Pass bean directly — Spring AI finds @Tool methods automatically
            .defaultTools(shopTools)
            .build();
    }

    public String chat(String userMessage) {
        log.info("=== Agent received: \"{}\"", userMessage);
        String response = chatClient.prompt()
            .user(userMessage)
            .call()
            .content();
        log.info("=== Agent responded: \"{}\"", response);
        return response;
    }
}
