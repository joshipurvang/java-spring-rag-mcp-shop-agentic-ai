# ShopSmart AI Agent

> **Agentic AI in pure Java/Spring — RAG · Redis Vector DB · MCP Server · Tool Calling**
> Built with Spring AI 1.0.0 GA · Spring Boot 3.4.3 · Java 21 · Ollama (local, free)

---

A e-commerce shopping assistant POC that demonstrates three enterprise AI patterns in a single plain Spring Boot application — **no cloud dependency, no paid API key, no framework lock-in.**

Every AI component (LLM, embedding model) runs locally through Ollama.

---

## The three patterns demonstrated

### 1. Agentic AI loop
The LLM autonomously decides which tools to call, in what order, and when it has enough information to answer. No routing code. No switch statements. No if-else chains.

When a user says *"Find a laptop for ML work, check if it's in stock, and order one for sam@example.com"*, the LLM chains:
```
semanticSearch() → checkStock() → placeOrder()
```
entirely on its own — we wrote zero routing logic for this sequence.

### 2. RAG with Redis Vector DB
Products are embedded at startup using `nomic-embed-text` and stored in Redis as vectors. Queries use cosine similarity search — not keyword matching.

```
User: "headphones for long-haul flights"
Keyword search: 0 results
Vector search:  Sony WH-1000XM5 (matched on: noise cancellation, long battery, travel)
```

### 3. MCP Server (Model Context Protocol)
The same `@Tool` annotated methods are automatically exposed over HTTP/SSE. Claude Desktop or Cursor IDE can connect directly to `http://localhost:8080/sse` and call your business tools — without going through the agent at all.

---

## Architecture

```
┌─────────────────────────────────────────────────────────────┐
│  Clients                                                    │
│  REST client          Claude Desktop        Cursor IDE      │
│  POST /api/chat       MCP · GET /sse        MCP · GET /sse  │
└──────────┬────────────────────┬──────────────────┬───────────┘
           │                    │                  │
┌──────────▼────────────────────▼──────────────────▼───────────┐
│  Spring Boot 3.4.3 + Spring AI 1.0.0 GA                      │
│                                                              │
│  ShopController          MCP Server (spring-ai-mcp-webmvc)   │
│  /api/chat               /sse  /mcp/message                   │
│         │                        │                            │
│         └────────────┬───────────┘                            │
│                      ▼                                        │
│         ShopAgent — ChatClient + Ollama llama3.2              │
│         ┌── reads user message                                 │
│         ├── LLM picks tools autonomously        AGENTIC        │
│         └── loops until answer ready            LOOP           │
│                      │                                         │
│    ┌─────────────────┼──────────────────┐                      │
│    ▼                 ▼                  ▼                      │
│  semanticSearch()  checkStock()     placeOrder()               │
│  @Tool · RAG       @Tool · stock    @Tool · orders             │
│    │                                                           │
│    ▼                              ▼                            │
│  Redis Vector Store :6379       Ollama :11434                  │
│  cosine similarity · RAG        llama3.2 + nomic-embed-text    │
└───────────────────────────────────────────────────────────────┘
```

---

## Project structure

```
shop-rag-mcp/
├── pom.xml                          3 Spring AI deps: ollama, redis-vector, mcp-server
├── src/main/resources/
│   └── application.properties
└── src/main/java/com/demo/
    ├── ShopRagMcpApplication.java
    ├── model/ProductCatalog.java    10 products, order store (in-memory for demo)
    ├── config/
    │   ├── VectorStoreConfig.java   embeds products into Redis at startup (RAG indexing)
    │   └── McpConfig.java           registers ShopTools as MCP server (1 @Bean)
    ├── tools/ShopTools.java         5 @Tool methods — semanticSearch, checkStock,
    │                                placeOrder, getOrderStatus, listAllProducts
    ├── agent/ShopAgent.java         ChatClient + tools = agentic loop
    └── controller/ShopController.java  REST endpoints
```

---

## Quick start

### Prerequisites
- Java 21+
- Maven 3.9+
- Docker
- [Ollama](https://ollama.ai)

### 1. Start Redis Stack (includes vector search)
```bash
docker run -d -p 6379:6379 redis/redis-stack:latest
```

### 2. Pull Ollama models
```bash
ollama pull llama3.2          # chat LLM
ollama pull nomic-embed-text  # embedding model for RAG
```

### 3. Run the application
```bash
cd shop-rag-mcp
mvn spring-boot:run
```

Watch for:
```
=== Indexing products into Redis Vector Store for RAG ===
=== 10 products indexed — RAG ready ===
```

### 4. Try it
```bash
# RAG semantic search (not keyword matching)
curl -X POST http://localhost:8080/api/chat \
  -H "Content-Type: application/json" \
  -d '{"message": "Find headphones for long-haul flights"}'

# Multi-step agentic chain
curl -X POST http://localhost:8080/api/chat \
  -H "Content-Type: application/json" \
  -d '{"message": "Find a laptop for ML work, check if in stock, order 1 for sam@example.com"}'

# See all demo scenarios
curl http://localhost:8080/api/demo
```

---

## Connect Claude Desktop (MCP)

Add to `~/Library/Application Support/Claude/claude_desktop_config.json`:

```json
{
  "mcpServers": {
    "shop-agent": {
      "url": "http://localhost:8080/sse"
    }
  }
}
```

Restart Claude Desktop. Your `semanticSearch`, `checkStock`, `placeOrder` tools will appear in Claude's tool list.

---

## API reference

| Method | Path | Description |
|--------|------|-------------|
| POST | `/api/chat` | Send natural language message to agent |
| GET  | `/api/products` | Raw product catalog |
| GET  | `/api/orders` | All placed orders |
| GET  | `/api/demo` | Example queries + MCP config |
| GET  | `/sse` | MCP SSE stream (for external clients) |
| POST | `/mcp/message` | MCP message endpoint |

---

## Key insight: `@Tool` description text is the routing logic

```java
@Tool(description = """
    Search the product catalog using semantic similarity (RAG).
    Use this for any product discovery or recommendation query.
    """)
public String semanticSearch(String query) { ... }
```

The LLM reads this description to decide when to call the tool. Precise descriptions → correct autonomous tool chains. Vague descriptions → wrong choices. **The description is your routing logic.**

---

## Tech stack

| Layer | Technology |
|-------|-----------|
| Language | Java 21 |
| Framework | Spring Boot 3.4.3 |
| AI framework | Spring AI 1.0.0 GA |
| LLM | Ollama llama3.2 (local, free) |
| Embeddings | Ollama nomic-embed-text |
| Vector store | Redis Stack (cosine similarity) |
| MCP server | spring-ai-starter-mcp-server-webmvc |

---

## What's not included (intentionally)

This is a demo. Production additions would include:

- Database persistence (PostgreSQL + JPA)
- Authentication (Spring Security + JWT)
- Conversation memory (Redis-backed chat history)
- Retry/resilience (Resilience4j)
- Streaming responses (`Flux<String>` from ChatClient)

---
