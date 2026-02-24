"""
BaseAgent — shared interface for all specialist agents.

Every agent receives a *task* dict (produced by the upstream agent) and
returns a *result* dict that is passed downstream.

Standard task keys
------------------
{
  "goal":        str,   # original user idea (always present)
  "context":     dict,  # accumulated knowledge from previous agents
  "subtask":     str,   # what THIS agent specifically has to do
  "artifacts":   list,  # files / URLs produced by upstream agents
}

Standard result keys
--------------------
{
  "agent":       str,        # agent name
  "status":      "ok"|"err", # outcome
  "summary":     str,        # human-readable summary
  "artifacts":   list,       # new files / URLs produced
  "next_tasks":  list,       # optional: sub-tasks for downstream agents
  "raw":         dict,       # agent-specific data
}
"""

from __future__ import annotations
import json
import os
import time
import anthropic
from abc import ABC, abstractmethod
from typing import Any

from .config import cfg


class BaseAgent(ABC):
    """Abstract base for all agents."""

    name: str = "BaseAgent"

    def __init__(self):
        self._client: anthropic.Anthropic | None = None
        os.makedirs(cfg.output_dir, exist_ok=True)

    # ── Public entry point ─────────────────────────────────────────────────

    def run(self, task: dict) -> dict:
        """Run the agent. Wraps execute() with logging & error handling."""
        from .events_bus import emit
        emit(self.name, type="agent_start")
        self._log(f"Starting on: {task.get('subtask', task.get('goal', '?'))[:120]}")
        t0 = time.time()
        try:
            result = self.execute(task)
        except Exception as exc:
            result = self._err(str(exc))
        elapsed = round(time.time() - t0, 1)
        status_icon = "✓" if result.get("status") == "ok" else "✗"
        self._log(f"{status_icon} Done in {elapsed}s — {result.get('summary', '')[:100]}")
        emit(
            self.name,
            type="agent_done",
            status=result.get("status"),
            summary=result.get("summary", ""),
            elapsed=elapsed,
            artifacts=result.get("artifacts", []),
        )
        return result

    # ── Subclasses implement this ──────────────────────────────────────────

    @abstractmethod
    def execute(self, task: dict) -> dict:
        """Core logic. Must return a result dict."""

    # ── Helpers ───────────────────────────────────────────────────────────

    @property
    def llm(self) -> anthropic.Anthropic:
        if self._client is None:
            self._client = anthropic.Anthropic(api_key=cfg.anthropic_api_key)
        return self._client

    def chat(
        self,
        system: str,
        user: str,
        model: str | None = None,
        max_tokens: int = 8192,
    ) -> str:
        """Single-turn Claude call. Returns the assistant text."""
        model = model or cfg.claude_model
        resp = self.llm.messages.create(
            model=model,
            max_tokens=max_tokens,
            system=system,
            messages=[{"role": "user", "content": user}],
        )
        return resp.content[0].text.strip()

    def chat_json(self, system: str, user: str, **kwargs) -> Any:
        """Claude call that expects JSON back. Returns parsed object."""
        full_system = (
            system
            + "\n\nIMPORTANT: Reply ONLY with valid JSON — no markdown, no prose."
        )
        raw = self.chat(full_system, user, **kwargs)
        # strip possible ```json ... ``` fences
        raw = raw.strip()
        if raw.startswith("```"):
            raw = raw.split("```", 2)[1]
            if raw.startswith("json"):
                raw = raw[4:]
            raw = raw.rsplit("```", 1)[0]
        return json.loads(raw.strip())

    def save_artifact(self, filename: str, content: str | bytes) -> str:
        """Save a file under cfg.output_dir and return its path."""
        from .events_bus import emit
        path = os.path.join(cfg.output_dir, filename)
        os.makedirs(os.path.dirname(path), exist_ok=True)
        mode = "wb" if isinstance(content, bytes) else "w"
        with open(path, mode) as f:
            f.write(content)
        emit(self.name, type="artifact", path=path, filename=filename)
        return path

    # ── Result constructors ────────────────────────────────────────────────

    def _ok(self, summary: str, artifacts: list | None = None, **raw) -> dict:
        return {
            "agent": self.name,
            "status": "ok",
            "summary": summary,
            "artifacts": artifacts or [],
            "raw": raw,
        }

    def _err(self, message: str) -> dict:
        return {
            "agent": self.name,
            "status": "err",
            "summary": f"ERROR: {message}",
            "artifacts": [],
            "raw": {"error": message},
        }

    # ── Logging ───────────────────────────────────────────────────────────

    def _log(self, msg: str):
        if cfg.verbose:
            print(f"  [{self.name}] {msg}")
        from .events_bus import emit
        emit(self.name, type="log", message=msg)
