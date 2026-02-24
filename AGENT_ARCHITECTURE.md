# Autonomous AI Agent — Architecture

## Overview

This system takes a raw user idea (in any language) and autonomously:
1. Understands and clarifies the idea
2. Plans the full project
3. Designs the architecture
4. Generates all source code
5. Creates UI/visual assets
6. Produces audio / voice / music
7. Designs the security model
8. Reviews quality and generates tests

---

## Pipeline

```
User Idea (any language)
        │
        ▼
┌───────────────────┐
│   ProducerAgent   │  🎬  Planner
│                   │  • Understands goal
│                   │  • Analyses domain & complexity
│                   │  • Builds structured project plan
│                   │  • Decides which agents to run
└────────┬──────────┘
         │  plan.json
         ▼
┌───────────────────┐
│  ArchitectAgent   │  🧱  System Design
│                   │  • Module breakdown
│                   │  • Data model
│                   │  • Tech stack selection
│                   │  • Architecture doc (Markdown)
└────────┬──────────┘
         │  module_spec.json
         ▼
    ┌────┴──────────────────────────────────────┐
    │                                           │
    ▼             ▼           ▼         ▼       │
┌──────────┐ ┌─────────┐ ┌────────┐ ┌────────┐│
│CodeAgent │ │DesignAgt│ │AudioAgt│ │SecuAgt ││
│👨‍💻       │ │🎨       │ │🔊      │ │🔐      ││
│          │ │         │ │        │ │        ││
│• Kotlin  │ │• UI spec│ │• TTS   │ │• Threat│
│• Python  │ │• DALL-E │ │  EL11  │ │  model │
│• Compose │ │• colors │ │• Music │ │• AES   │
│• Tests   │ │• themes │ │  Suno  │ │• Keystr│
│• Gradle  │ │• assets │ │• DSP   │ │• Auth  │
└──────────┘ └─────────┘ └────────┘ └────────┘
         │                                      │
         └──────────────────┬───────────────────┘
                            │
                            ▼
                   ┌─────────────────┐
                   │    QAAgent      │  🧪  Quality
                   │                 │  • Critical review
                   │                 │  • Test plan
                   │                 │  • Unit tests
                   │                 │  • Final report
                   └────────┬────────┘
                            │
                            ▼
                   📦  agent_output/
                       ├── plan.json
                       ├── architecture.md
                       ├── module_spec.json
                       ├── security_report.md
                       ├── FINAL_REPORT.md
                       ├── manifest.json
                       ├── code/
                       │   ├── *.kt  (Android source)
                       │   ├── build.gradle.kts
                       │   └── test/*.kt
                       ├── design/
                       │   ├── icon.png
                       │   ├── splash_bg.png
                       │   └── ui_spec.json
                       └── audio/
                           ├── welcome.mp3
                           └── audio_plan.json
```

---

## Quick Start

```bash
# 1. Install dependencies
pip install -r requirements_agent.txt

# 2. Set your API keys
cp .env.example .env
nano .env   # fill in ANTHROPIC_API_KEY (required) + others

# 3. Run with your idea
python main_agent.py "create a vault Android app that opens when you whistle a melody"

# Or interactive mode
python main_agent.py
```

---

## Configuration (`agents/config.py`)

| Variable | Default | Description |
|---|---|---|
| `ANTHROPIC_API_KEY` | — | **Required.** Claude API key |
| `OPENAI_API_KEY` | — | For DALL-E 3 image generation |
| `ELEVENLABS_API_KEY` | — | For ElevenLabs TTS |
| `SUNO_API_KEY` | — | For Suno music generation |
| `AGENT_OUTPUT_DIR` | `./agent_output` | Where all outputs are saved |
| `image_backend` | `dall-e-3` | `dall-e-3` \| `banana` |
| `tts_backend` | `elevenlabs` | `elevenlabs` \| `gtts` |
| `music_backend` | `suno` | `suno` |

---

## Example: Whistle-Unlock Vault App

**Input:**
> "Приложение сейф на андроид — логины, пароли, фотки, файлы. Открыть только если правильно насвистеть песенку."

**What the agents produce:**

| Agent | Output |
|---|---|
| ProducerAgent | Project plan with 4 phases |
| ArchitectAgent | 12 modules, Room DB schema, architecture.md |
| CodeAgent | ~20 Kotlin files (screens, ViewModel, Repository, etc.) |
| DesignAgent | UI spec, dark theme colors.xml, icon + splash images |
| AudioAgent | `WhistleDetector.kt` (YIN + DTW), unlock sound, welcome TTS |
| SecurityAgent | AES-256-GCM `CryptoManager.kt`, threat model, security report |
| QAAgent | 3 unit test files, test plan, `FINAL_REPORT.md` |

---

## Adding a New Agent

1. Create `agents/my_agent.py` extending `BaseAgent`
2. Implement `execute(task: dict) -> dict`
3. Register in `agents/orchestrator.py` → `AGENT_REGISTRY`

That's it. The orchestrator reads agent names from the plan and dispatches automatically.
