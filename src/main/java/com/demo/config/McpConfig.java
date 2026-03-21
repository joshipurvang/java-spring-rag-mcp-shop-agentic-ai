package com.demo.config;

import com.demo.tools.ShopTools;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * McpConfig — registers ShopTools with the MCP server.
 *
 * MethodToolCallbackProvider IS the right place to use here.
 * The MCP server infrastructure bean (auto-configured by Spring AI)
 * injects ToolCallbackProvider and uses it to advertise tools to MCP clients.
 * This is different from ChatClient.defaultTools() which takes the raw bean.
 *
 * Summary of correct usage:
 *   ChatClient.defaultTools(shopTools)          ← raw bean, Spring AI scans for @Tool
 *   McpConfig: MethodToolCallbackProvider(...)  ← provider pattern for MCP server wiring
 */
@Configuration
public class McpConfig {

    @Bean
    public ToolCallbackProvider shopToolsProvider(ShopTools shopTools) {
        return MethodToolCallbackProvider.builder()
            .toolObjects(shopTools)
            .build();
    }
}
