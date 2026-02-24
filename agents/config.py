"""
Central configuration for the Autonomous AI Agent System.
Set API keys via environment variables or a .env file.
"""
import os
from dataclasses import dataclass, field
from typing import Optional


@dataclass
class Config:
    # ── Orchestration ──────────────────────────────────────────────────────
    # Primary LLM used for reasoning, planning, and code (Claude)
    anthropic_api_key: str = field(
        default_factory=lambda: os.getenv("ANTHROPIC_API_KEY", "")
    )
    claude_model: str = "claude-opus-4-6"          # top reasoning model
    claude_code_model: str = "claude-sonnet-4-6"   # fast code generation

    # ── Design / Image generation ──────────────────────────────────────────
    openai_api_key: str = field(
        default_factory=lambda: os.getenv("OPENAI_API_KEY", "")
    )
    # Supported: "dall-e-3" | "stable-diffusion" | "banana" (Banana.dev)
    image_backend: str = "dall-e-3"
    banana_api_key: str = field(
        default_factory=lambda: os.getenv("BANANA_API_KEY", "")
    )
    banana_model_key: str = field(
        default_factory=lambda: os.getenv("BANANA_MODEL_KEY", "")
    )

    # ── Audio / Voice ──────────────────────────────────────────────────────
    elevenlabs_api_key: str = field(
        default_factory=lambda: os.getenv("ELEVENLABS_API_KEY", "")
    )
    suno_api_key: str = field(
        default_factory=lambda: os.getenv("SUNO_API_KEY", "")
    )
    # Supported: "elevenlabs" | "suno" | "gtts" (offline fallback)
    tts_backend: str = "elevenlabs"
    music_backend: str = "suno"

    # ── Output ────────────────────────────────────────────────────────────
    output_dir: str = field(
        default_factory=lambda: os.getenv("AGENT_OUTPUT_DIR", "./agent_output")
    )
    max_iterations: int = 10          # safety limit for agent loops
    verbose: bool = True


# Singleton
cfg = Config()
