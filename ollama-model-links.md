# Ollama Model Comparison — Edge General Purpose CLI Agent

> **Single source of truth** for all model sizes, tool support and Ollama library links in this repo.
> `TUTORIAL.md`, `BLUEPRINT-CLI-Agent.md` and `application.properties` link here instead of duplicating tables.

**Goal**: Build a general-purpose CLI agent that runs locally on edge hardware
**Constraints**: Under 10 GB download, tools + thinking support, no cloud dependency
**Stack**: Spring Boot + Spring AI + Ollama (local, free, private)
**Default**: `lfm2.5` (5.2 GB, fastest, MoE 8B/1B active, 125K). Alternatives under 10 GB: `qwen3.5:9b` (6.6 GB, 256K) and `gemma4:e4b` (9.6 GB, vision+audio) — both with `tools`.

---

## QUICK PICK

| Your Device | RAM | Model | Size | Command |
|---|---|---|---|---|
| **IoT / MCU** | < 1GB | lfm2.5-thinking | 731MB | `ollama pull lfm2.5-thinking` |
| **Mobile** | 2-4GB | nemotron-3-nano:4b | 2.8GB | `ollama pull nemotron-3-nano:4b` |
| **Raspberry Pi** | 4-8GB | lfm2.5 | 5.2GB | `ollama pull lfm2.5` |
| **Laptop** | 8GB | qwen3.5:4b | 3.4GB | `ollama pull qwen3.5:4b` |
| **Desktop** | 16GB | gemma4:26b | 18GB | `ollama pull gemma4:26b` |

---

## EDGE MODELS — PRIMARY CANDIDATES

### Tier 1: Ultra-Light (< 3GB)

| Model | Size | Params | Active | Context | Architecture | Vision | Thinking | Speed | Best For |
|---|---|---|---|---|---|---|---|---|---|
| **lfm2.5-thinking** | 731MB | 1.2B | 1.2B | 125K | Hybrid | No | Yes | Fastest | Absolute smallest with tools+thinking |
| **qwen3.5:0.8b** | 1GB | 0.8B | 0.8B | 256K | Dense | Yes | Yes | Fast | Vision at sub-1B |
| **qwen3.5:2b** | 2.7GB | 2B | 2B | 256K | Dense | Yes | Yes | Medium | Vision, good quality |
| **nemotron-3-nano:4b** | 2.8GB | 30B | 3.5B | 256K | MoE (Mamba) | No | Yes | Fastest | Hybrid Mamba, fastest inference |

### Tier 2: Light (3-6GB)

| Model | Size | Params | Active | Context | Architecture | Vision | Thinking | Speed | Best For |
|---|---|---|---|---|---|---|---|---|---|
| **qwen3.5:4b** | 3.4GB | 4B | 4B | 256K | Dense | Yes | Yes | Medium | Best quality at 4B, vision |
| **olmo-3:7b** | 4.5GB | 7B | 7B | 64K | Dense | No | Yes | Medium | Strong math, instruction following |
| **lfm2.5** | 5.2GB | 8B | 1B | 125K | MoE | No | Yes | Fastest | Fastest 8B class, edge-optimized |
| **granite4.1:8b** | 5.3GB | 8B | 8B | 128K | Dense | No | No | Medium | IBM, Apache 2.0, enterprise |
| **ornith:9b** | 5.6GB | 9B | 9B | 256K | Dense | No | No | Medium | Self-improving RL, MIT license |

### Tier 3: Multimodal Edge (6-10GB)

| Model | Size | Params | Active | Context | Vision | Audio | Thinking | Best For |
|---|---|---|---|---|---|---|---|---|
| **qwen3.5:9b** | 6.6GB | 9B | 9B | 256K | Yes | No | Yes | Best vision quality, 201 languages |
| **gemma4:e2b** | 7.2GB | 2.3B eff | 2.3B | 128K | Yes | Yes | Yes | Audio+vision, edge devices |
| **gemma4:12b** | 7.6GB | 12B | 12B | 256K | Yes | No | Yes | Strong reasoning, vision |
| **gemma4:e4b** | 9.6GB | 4.5B eff | 4.5B | 128K | Yes | Yes | Yes | Best multimodal edge model |

---

## EDGE DEPLOYMENT GUIDE

### By Device

| Device | RAM | Primary Pick | Alternative | Why |
|---|---|---|---|---|
| **IoT / MCU** | < 1GB | lfm2.5-thinking (731MB) | — | Only option at this size |
| **Mobile** | 2-4GB | nemotron-3-nano:4b (2.8GB) | qwen3.5:2b (2.7GB) | Speed vs vision |
| **Mobile + Vision** | 4GB | qwen3.5:4b (3.4GB) | qwen3.5:2b (2.7GB) | Quality vs size |
| **Raspberry Pi** | 4-8GB | lfm2.5 (5.2GB) | qwen3.5:4b (3.4GB) | Speed vs quality |
| **Laptop** | 8GB | qwen3.5:9b (6.6GB) | gemma4:e4b (9.6GB) | Quality vs multimodal |
| **Desktop** | 16GB | gemma4:26b (18GB) | qwen3.5:27b (17GB) | Reasoning vs instruction |

### By Use Case

| Use Case | Model | Size | Notes |
|---|---|---|---|
| **CLI Agent (text only)** | lfm2.5 | 5.2GB | Fastest inference, tools+thinking |
| **CLI Agent (with vision)** | qwen3.5:4b | 3.4GB | See images, tools+thinking |
| **Voice Assistant** | gemma4:e4b | 9.6GB | Audio input, tools+thinking |
| **Code Helper** | ornith:9b | 5.6GB | Self-improving, MIT license |
| **Enterprise RAG** | granite4.1:8b | 5.3GB | IBM, Apache 2.0, RAG focus |
| **Minimal Agent** | nemotron-3-nano:4b | 2.8GB | Fastest possible, tools+thinking |

---

## SPRING AI CONFIGURATION

> Config file is `src/main/resources/application.properties` (project moved from `application.yaml`/`application.yml` to `.properties`).

### Text-Only Edge Agent (lfm2.5)

```properties
spring.ai.ollama.base-url=http://localhost:11434
spring.ai.ollama.chat.model=lfm2.5
```

### Vision-Capable Edge Agent (qwen3.5:4b)

```properties
spring.ai.ollama.base-url=http://localhost:11434
spring.ai.ollama.chat.model=qwen3.5:4b
```

### Minimal Memory Agent (nemotron-3-nano:4b)

```properties
spring.ai.ollama.base-url=http://localhost:11434
spring.ai.ollama.chat.model=nemotron-3-nano:4b
```

### Multimodal Agent (gemma4:e4b)

```properties
spring.ai.ollama.base-url=http://localhost:11434
spring.ai.ollama.chat.model=gemma4:e4b
```

---

## lfm2.5 DEEP DIVE

**Why lfm2.5 is the edge champion:**

| Spec | Value |
|---|---|
| Total Parameters | 8B |
| Active Parameters | 1B (MoE) |
| Size on Disk | 5.2GB |
| Context Window | 125K tokens |
| Tools Support | Yes |
| Thinking Support | Yes |
| Vision Support | No |
| Architecture | Hybrid (Conv + Attention) |
| License | Apache 2.0 |

**Advantages:**
- Only 1B active params = fastest inference in its class
- Lowest power consumption (battery-friendly)
- 125K context (sufficient for most CLI tasks)
- Tools + thinking for agentic workflows
- Hybrid architecture optimized for on-device

**Disadvantages:**
- No vision support
- 128K context (others offer 256K+)
- Weaker on complex reasoning vs larger models

**Compare with same-size alternatives:**

| Model | Size | Active | Context | Thinking | Vision | Speed |
|---|---|---|---|---|---|---|
| **lfm2.5** | 5.2GB | 1B | 125K | Yes | No | Fastest |
| ornith:9b | 5.6GB | 9B | 256K | No | No | Medium |
| granite4.1:8b | 5.3GB | 8B | 128K | No | No | Medium |
| qwen3.5:4b | 3.4GB | 4B | 256K | Yes | Yes | Medium |

---

## UPGRADE PATH (When You Need More Power)

### Tier 4: Desktop (10-20GB)

| Model | Size | Params | Active | Context | Vision | Best For |
|---|---|---|---|---|---|---|
| **gemma4:26b** | 18GB | 25.2B | 3.8B | 256K | Yes | Best reasoning at this size |
| **qwen3.5:27b** | 17GB | 27B | 27B | 256K | Yes | Best instruction following |
| **qwen3.6:27b** | 17GB | 27B | 27B | 256K | Yes | Strong agentic coding |
| **glm-4.7-flash** | 19GB | 30B | 3B | 198K | No | Strongest 30B class |
| **north-mini-code-1.0** | 19GB | 30B | 3B | 488K | No | Longest context |
| **gemma4:31b** | 20GB | 30.7B | 30.7B | 256K | Yes | Top benchmarks |
| **olmo-3.1** | 19GB | 32B | 32B | 64K | No | Strong math |
| **gpt-oss:20b** | 14GB | 20B | 20B | 128K | No | Baseline reference |

### Tier 5: Workstation (>20GB)

| Model | Size | Notes |
|---|---|---|
| laguna-xs-2.1:nvfp4 | 19GB | 33B MoE, 3B active, agentic coding |
| nemotron-cascade-2 | 24GB | 30B MoE, 3B active, IMO gold medal |
| mistral-medium-3.5 | 80GB | 128B dense, flagship |

### Tier 6: Cloud Only

| Model | Params | Context | Notes |
|---|---|---|---|
| glm-5.2 | 756B | 976K | Flagship, 81.0 Terminal-Bench 2.1 |
| deepseek-v4-flash | 304B (13B active) | 1M | MoE, efficient reasoning |
| kimi-k3 | 2.8T | 1M | Requires Pro/Max subscription |
| kimi-k2.7-code | 1.04T | 256K | Coding-focused |
| nemotron-3-ultra | 550B (55B active) | 256K | Agent orchestration |

---

## BENCHMARKS (Edge Models Only)

### MMLU-Pro (Knowledge & Reasoning)

| Model | Size | Score |
|---|---|---|
| gemma4:e4b | 9.6GB | **69.4%** |
| gemma4:e2b | 7.2GB | 60.0% |
| lfm2.5-thinking | 731MB | 49.65% |

### GPQA Diamond (Science Reasoning)

| Model | Size | Score |
|---|---|---|
| gemma4:e4b | 9.6GB | **58.6%** |
| gemma4:e2b | 7.2GB | 43.4% |
| lfm2.5-thinking | 731MB | 37.86% |

### IFEval (Instruction Following)

| Model | Size | Score |
|---|---|---|
| lfm2.5-thinking | 731MB | **88.42%** |
| olmo-3:7b | 4.5GB | 86.3% |

### AIME 2025 (Math Reasoning)

| Model | Size | Score |
|---|---|---|
| lfm2.5-thinking | 731MB | **31.73** |
| gemma4:e4b | 9.6GB | 42.5 |
| gemma4:e2b | 7.2GB | 37.5 |

---

## MODEL LINKS

### Edge Models (Primary)
1. https://ollama.com/library/lfm2.5
2. https://ollama.com/library/lfm2.5-thinking
3. https://ollama.com/library/nemotron-3-nano
4. https://ollama.com/library/qwen3.5
5. https://ollama.com/library/gemma4
6. https://ollama.com/library/granite4.1
7. https://ollama.com/library/ornith
8. https://ollama.com/library/olmo-3

### Desktop Models (Upgrade Path)
9. https://ollama.com/library/glm-4.7-flash
10. https://ollama.com/library/qwen3.6
11. https://ollama.com/library/olmo-3.1
12. https://ollama.com/library/north-mini-code-1.0
13. https://ollama.com/library/laguna-xs-2.1
14. https://ollama.com/library/gpt-oss

### Cloud Models (Reference)
15. https://ollama.com/library/glm-5.2
16. https://ollama.com/library/deepseek-v4-flash
17. https://ollama.com/library/kimi-k3
18. https://ollama.com/library/kimi-k2.7-code
19. https://ollama.com/library/nemotron-3-ultra
20. https://ollama.com/library/minimax-m3
21. https://ollama.com/library/glm-5.1
22. https://ollama.com/library/kimi-k2.6
23. https://ollama.com/library/minimax-m2.7
24. https://ollama.com/library/mistral-medium-3.5
25. https://ollama.com/library/nemotron3
26. https://ollama.com/library/laguna-s-2.1
27. https://ollama.com/library/laguna-xs.2
28. https://ollama.com/library/nemotron-cascade-2
29. https://ollama.com/library/granite4.1-guardian
