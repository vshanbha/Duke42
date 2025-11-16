# Duke42 🚀

*Duke42 – your Hitchhiker’s Guide to the Java AI Galaxy*

Welcome aboard! I’m **Duke**, your trusty Java mascot and pilot, here to navigate you through the far reaches of the **Java + GenAI universe**. From running **local LLMs** to **polyglot Java + Python libraries** and **AI workflow orchestration**, we’ll explore it all—no towels required… unless you’re feeling particularly intergalactic.


                        .       ☆
          .      *                .
    .          .        *
                 .
        *              .
                 ✦
          .          *
                       .
             ☆

                 🛸
                / \
               | D |   <- Duke, piloting the ship
                \_/

       ~ Edge Planet ~         ~ Polyglot Planet ~         ~ Protocol Planet ~
           (💻)                     (🐍)                       (⚙️)
              o----------------------o---------------------------o
               \                     |                           /
                \                    |                          /
                 \                   |                         /
                  \                  |                        /
                   *-----------------*-----------------------*
                       <--- Warp lanes / AI pipelines --->

---

## 🌌 Galaxy Overview

Duke42 is organized into **planets**, each showcasing a different part of the Java GenAI ecosystem:

| Planet     | Mission                  | Key Features |
|-----------|--------------------------|--------------|
| **Edge**   | Local LLM inference       | Run LLMs offline (Ollama), privacy-friendly for enterprises |
| **Polyglot** | Java ↔ Python pipelines | GraalVM polyglot integration, sentiment analysis, more |
| **Protocol** | AI workflow orchestration | Connect Java services using MCP/LangChain4j for full pipelines |

Supporting modules and notable folders:

- `AnomalyDetector/` – Python anomaly detection project (data, model, tests)
- `backend/` – Quarkus-based backend with REST endpoints and server logic
- `polyglot/` – experimental GraalVM polyglot integration (Python from Java)
- `ui/` – JavaFX frontend where Duke guides you interactively

---

## ⚡ Why Duke42?

- Fully **Java-native**: UI + backend + AI services  
- Modular design: planets can be explored independently  
- **GraalVM native image-ready**: lightning-fast startup for demos  
- Offline-first, privacy-conscious **Edge LLM integration**  
- Polyglot support via **GraalVM Context** for Python AI libraries  
- AI workflow orchestration through **LangChain4j / MCP**  
- And of course… **Duke pilots the galaxy!** 🪐  

---

## 🛠️ Getting Started

### Prerequisites

- Java 21+ (LTS recommended)  
- Maven 4+  
- GraalVM 21+ (for native image and polyglot features)  
- Ollama CLI / local LLM setup (for Edge planet)  
- Optional: Python environment for polyglot demos  

### 📂 Repository Structure

```
Duke42/
├── AnomalyDetector/   # Python anomaly detection project
├── backend/           # Quarkus backend services
├── polyglot/          # GraalVM polyglot (experimental)
├── ui/                # JavaFX frontend
├── pom.xml            # Maven parent POM
├── LICENSE
└── README.md
```

### Build & Run

**1. Clone the repo**

```bash
git clone git@github.com:vshanbha/Duke42.git
cd duke42
```

**2. Build all modules**
```bash
mvn clean install
```

**3. Run the Polyglot module**

The Polyglot module uses Python code and libraries through GraalPy. During development we have observed that GraalPy does not initialize properly in quarkus dev mode. 

```bash
cd polyglot
mvn clean install
java -jar target/polyglot-runner.jar
```

**4. Run the backend in dev mode**

The backend module can use the Polyglot module as an MCP server running on the port 9000. By default the `application.properties` has the MCP configuration commented to allow unit tests to run without MCP. 

If Unit tests need to be run with MCP configuration, then ensure that the Polyglot module is up and running.

```bash
cd backend
mvn quarkus:dev
```

**5. Run the JavaFX UI**

```bash
cd ui
mvn javafx:run
```

**6. Optional: Build native image for ultra-fast startup**

This works for the backend and polyglot modules.
e.g. for the backend:
```bash
cd backend
mvn package -Pnative
./target/duke42-runner
```

## 🗺️ Workshop Learning Outcomes

Participants exploring Duke42 will:

- Understand the Java + GenAI galaxy
- Run local LLMs securely and offline
- Extend Java apps with Python AI libraries via GraalVM
- Connect Java services into AI workflows
- Gain a mental map of Java GenAI possibilities for enterprise applications

## 📚 References

- [Quarkus Langchain4j extension](https://docs.quarkiverse.io/quarkus-langchain4j/dev/)
- [Langchain4j](https://docs.langchain4j.dev/)
- [Smollm2 models](https://ollama.com/library/smollm2)
- [Qwen3 models](https://ollama.com/library/qwen3)
- [LLama3.2 models](https://ollama.com/library/llama3.2)
- [Baeldung](https://www.baeldung.com/langchain4j-quarkus-mcp)
- [Using Multiple models](https://www.the-main-thread.com/p/agentic-java-multi-model-ai-quarkus)
- [Bank Transaction Dataset for Fraud Detection](https://www.kaggle.com/datasets/valakhorasani/bank-transaction-dataset-for-fraud-detection/data)

## 💡 Contributing

Duke42 thrives on curiosity and collaboration. Contributions welcome:
- New AI workflow demos
- Extended Edge / Polyglot capabilities
- UI improvements or visualizations

Fork, code, and submit pull requests—we’ll navigate the galaxy together!

## 🪐 License
MIT License – explore, adapt, and share freely!
