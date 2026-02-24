#!/usr/bin/env python3
"""
AI Studio — startup script.

Usage:
    python run_studio.py           # default port 7860
    STUDIO_PORT=8080 python run_studio.py
"""
import sys
import os

sys.path.insert(0, os.path.dirname(__file__))

try:
    from dotenv import load_dotenv
    load_dotenv()
except ImportError:
    pass

if not os.getenv("ANTHROPIC_API_KEY"):
    print("ERROR: ANTHROPIC_API_KEY is not set.")
    print("  export ANTHROPIC_API_KEY=sk-ant-...")
    sys.exit(1)

import uvicorn

port = int(os.getenv("STUDIO_PORT", "7860"))
print(f"")
print(f"  ╔══════════════════════════════════════╗")
print(f"  ║          AI STUDIO                   ║")
print(f"  ╚══════════════════════════════════════╝")
print(f"  Open: http://localhost:{port}")
print(f"")

uvicorn.run(
    "studio.server:app",
    host="0.0.0.0",
    port=port,
    log_level="warning",
    reload=False,
)
