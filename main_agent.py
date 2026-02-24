#!/usr/bin/env python3
"""
Autonomous AI Agent — CLI Entry Point

Usage:
    python main_agent.py "your idea in any language"

    python main_agent.py  # interactive mode

Environment variables (set in .env or shell):
    ANTHROPIC_API_KEY      — required
    OPENAI_API_KEY         — for DALL-E 3 image generation
    ELEVENLABS_API_KEY     — for TTS voice synthesis
    SUNO_API_KEY           — for AI music generation
    BANANA_API_KEY         — for Banana.dev stable diffusion
    AGENT_OUTPUT_DIR       — default: ./agent_output
"""

import sys
import os

# Allow running from the repo root
sys.path.insert(0, os.path.dirname(__file__))

# Load .env if python-dotenv is installed
try:
    from dotenv import load_dotenv
    load_dotenv()
except ImportError:
    pass  # .env support is optional


def check_api_key():
    key = os.getenv("ANTHROPIC_API_KEY", "")
    if not key:
        print("ERROR: ANTHROPIC_API_KEY is not set.")
        print("  export ANTHROPIC_API_KEY=your_key_here")
        sys.exit(1)


def main():
    check_api_key()

    from agents import Orchestrator

    if len(sys.argv) > 1:
        # Goal passed as CLI argument
        goal = " ".join(sys.argv[1:])
    else:
        # Interactive mode
        print("=" * 60)
        print("  MATRIX AI AGENT")
        print("=" * 60)
        print("  Describe your idea (any language, any detail level).")
        print("  The agent will plan, design, code, and create assets.")
        print("  Press Ctrl+D or leave blank + Enter to exit.")
        print()
        try:
            goal = input("Your idea: ").strip()
        except (EOFError, KeyboardInterrupt):
            print("\nBye.")
            return

    if not goal:
        print("No idea provided. Exiting.")
        return

    orch = Orchestrator()
    orch.run(goal)


if __name__ == "__main__":
    main()
