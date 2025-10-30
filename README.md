# Duke42 🚀

*Duke42 – your Hitchhiker’s Guide to the Java AI Galaxy*

Welcome aboard! I’m **Duke**, your trusty Java mascot and pilot, here to navigate you through the far reaches of the **Java + GenAI universe**. From running **local LLMs** to **polyglot pipelines** and **AI workflow orchestration**, we’ll explore it all—no towels required… unless you’re feeling particularly intergalactic.


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

Supporting modules:

- **common** – shared models, DTOs, and utilities  
- **backend** – Quarkus-based backend with REST endpoints for each planet  
- **ui** – JavaFX frontend where Duke guides you interactively  

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
- GraalVM 23+ (for native image and polyglot features)  
- Ollama CLI / local LLM setup (for Edge planet)  
- Optional: Python environment for polyglot demos  

### 📂 Repository Structure

```
duke42/
│
├── common/       # Shared models, DTOs, utilities
├── backend/      # Quarkus backend services
├── edge/         # Local LLM handling
├── polyglot/     # GraalVM polyglot services
├── protocol/     # Workflow orchestration services
├── ui/           # JavaFX frontend
└── pom.xml       # Maven parent POM
```

### Build & Run

**1. Clone the repo**

```bash
git clone https://github.com/<your-org>/duke42.git
cd duke42
```

**2. Build all modules**
```bash
mvn clean install
```

**3. Run the backend in dev mode**

```bash
cd backend
mvn quarkus:dev
```

**4. Run the JavaFX UI**

```bash
cd ui
mvn javafx:run
```

**5. Optional: Build native image for ultra-fast startup**

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

## 📖 Resources

- Workshop slides and diagrams: docs/
- Example polyglot scripts: resources/polyglot/
- GitHub gists for each demo snippet

## 💡 Contributing

Duke42 thrives on curiosity and collaboration. Contributions welcome:
- New AI workflow demos
- Extended Edge / Polyglot capabilities
- UI improvements or visualizations

Fork, code, and submit pull requests—we’ll navigate the galaxy together!

## 🪐 License
MIT License – explore, adapt, and share freely!

