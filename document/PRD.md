# agentShell — Product Requirements Document (PRD)

## 1. App Identity & Core Goal
**agentShell** is an autonomous, on-device AI agent for Android. It operates as a bridge between a Large Language Model (LLM) and the Android operating system, allowing the user to control their phone entirely via natural language (voice or text). It does not rely on cloud processing by default, ensuring absolute privacy.

**Tagline:** "Your phone. Your agent. No cloud required."

## 2. Core Features & Capabilities
- **Universal App Control:** The agent can interact with installed apps (e.g., WhatsApp, Chrome) via Accessibility Services (tap, scroll, read, type).
- **Terminal/Shell Access:** Direct execution of Linux commands on the device via Termux bridge integration.
- **Dynamic Voice Interface:** Minimalist voice UI dock allowing hands-free command execution with real-time feedback of the agent's thought process.
- **Mini-App Generation:** The agent can dynamically generate standalone "Mini Apps" (HTML/JS) and save them to the App Hub for immediate user interaction.
- **On-Device LLM Support:** Full integration with local models (e.g., Gemma via MediaPipe) and self-hosted models (Ollama, LM Studio) to maintain privacy.

## 3. Long-Term Vision & Ecosystem (Future Features)
agentShell aims to evolve from a simple mobile app into a **fully automated, cross-device ecosystem**.

### 3.1 The "Everywhere" Agent Ecosystem
- **Multi-Device Sync:** Install agentShell on phone, PC, tablet, and smart home hubs. 
- **Remote Automation:** The user can speak to their phone to control their PC. For example, the agent can connect to the PC over the internet, access files, execute scripts, and perform tasks without the user ever opening the laptop.

### 3.2 Plugin & Add-on Marketplace
- **Developer Plugins:** Third-party developers can build and list "Mini Features" or APIs that agentShell can integrate with.
- **Hardware Integration:** With the right plugins, the agent can interface with IoT devices (Home Automation), initiate 3D prints, or orchestrate complex multi-step workflows across different services.
- **Ethical Monetization:** Core features remain free and private. Advanced integrations, premium cloud-fallback credits, and specialized marketplace plugins will be available via subscription or one-time ethical charges.

## 4. Developer Needs & Architecture
- **Tech Stack:** 100% Kotlin, Jetpack Compose, Room DB, Hilt DI.
- **Architecture:** MVI + Clean Architecture for maximum maintainability.
- **Agent Loop:** Employs a robust PLAN → ACT → OBSERVE loop. Developers must adhere strictly to the established `AgentLoopManager` and `ToolDispatcher` patterns when adding new capabilities.
