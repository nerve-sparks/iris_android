"""
ProducerAgent  —  🎬  The top-level orchestrator / planner.

Responsibilities
----------------
1. Take a raw user idea (free-form text, any language).
2. Deeply analyse it: intent, domain, complexity, required capabilities.
3. Decompose into a structured Project Plan.
4. Dispatch sub-tasks to specialist agents (ArchitectAgent first, then
   the parallel specialists).
5. Collect all results and produce the final report.

The ProducerAgent does NOT hard-code which specialists run — it decides
dynamically based on what the idea needs.  For example, a pure back-end
service might skip DesignAgent and AudioAgent.
"""

from __future__ import annotations
import json
import os
from typing import Any

from .base_agent import BaseAgent
from .config import cfg


SYSTEM_PROMPT = """
You are the ProducerAgent — the chief orchestrator of an autonomous AI development team.

Your team members:
  • ArchitectAgent   — system design, technology choices, module breakdown
  • CodeAgent        — writes all source code (Android/Kotlin, Python, web, etc.)
  • DesignAgent      — UI mockups, icons, animations, brand assets
  • AudioAgent       — voice-overs, music, sound effects, audio recognition
  • SecurityAgent    — threat modelling, encryption design, auth flows
  • QAAgent          — test plans, review criteria, risk assessment

Your job:
1. Understand the user idea precisely (even if vague or in any language).
2. Determine which agents are needed and in what order.
3. For each agent produce a crisp, unambiguous subtask description in English.
4. Return a structured JSON project plan.

Rules:
- Be concrete.  Vague ideas must be clarified and given a precise scope.
- Always include ArchitectAgent first.
- Always include QAAgent last.
- Only include agents that are actually needed.
- Identify external APIs/SDKs required (audio recognition, encryption, etc.).
"""


class ProducerAgent(BaseAgent):
    name = "ProducerAgent"

    def execute(self, task: dict) -> dict:
        goal = task.get("goal", "")

        # ── Step 1: deep-analyse the idea ─────────────────────────────────
        self._log("Analysing user idea …")
        analysis = self._analyse_idea(goal)

        # ── Step 2: build the project plan ────────────────────────────────
        self._log("Building project plan …")
        plan = self._build_plan(goal, analysis)

        # ── Step 3: save artefacts ────────────────────────────────────────
        plan_path = self.save_artifact(
            "plan.json", json.dumps({"analysis": analysis, "plan": plan}, indent=2, ensure_ascii=False)
        )
        self._log(f"Plan saved → {plan_path}")

        # ── Step 4: pretty-print plan to console ──────────────────────────
        self._print_plan(plan)

        return self._ok(
            summary=f"Project plan created: {plan.get('title', goal[:60])}",
            artifacts=[plan_path],
            analysis=analysis,
            plan=plan,
        )

    # ── Private helpers ────────────────────────────────────────────────────

    def _analyse_idea(self, goal: str) -> dict:
        """Ask Claude to deeply understand the idea and return structured JSON."""
        return self.chat_json(
            system=SYSTEM_PROMPT,
            user=f"""
Analyse this user idea and return JSON with these exact keys:

{{
  "title":           "Short project title (5-8 words)",
  "language":        "Detected language of the idea",
  "domain":          "Android app | Web app | Desktop | CLI | API service | Game | Other",
  "summary":         "1-paragraph precise description of what we are building",
  "key_features":    ["feature 1", "feature 2", ...],
  "tech_stack":      ["Kotlin/Android", "llama.cpp", ...],
  "external_apis":   ["ElevenLabs TTS", "DALL-E 3", ...],
  "complexity":      "Low | Medium | High | Very High",
  "agents_needed":   ["ArchitectAgent", "CodeAgent", "DesignAgent", "AudioAgent", "SecurityAgent", "QAAgent"],
  "risks":           ["risk 1", "risk 2", ...]
}}

User idea:
\"\"\"{goal}\"\"\"
""",
        )

    def _build_plan(self, goal: str, analysis: dict) -> dict:
        """Build a full project plan with per-agent subtasks."""
        agents_needed = analysis.get("agents_needed", [
            "ArchitectAgent", "CodeAgent", "DesignAgent", "AudioAgent", "SecurityAgent", "QAAgent"
        ])

        plan_raw = self.chat_json(
            system=SYSTEM_PROMPT,
            user=f"""
Create a detailed project plan based on this analysis and return JSON.

Analysis:
{json.dumps(analysis, indent=2, ensure_ascii=False)}

Original goal:
\"\"\"{goal}\"\"\"

Return JSON with this exact structure:
{{
  "title":       "Project title",
  "version":     "1.0",
  "phases": [
    {{
      "phase":   1,
      "name":    "Architecture & Design",
      "agents":  ["ArchitectAgent"],
      "tasks": [
        {{
          "agent":    "ArchitectAgent",
          "subtask":  "Design the full system architecture for ...",
          "inputs":   [],
          "outputs":  ["architecture.md", "module_list.json"]
        }}
      ]
    }},
    {{
      "phase":   2,
      "name":    "Implementation",
      "agents":  ["CodeAgent", "DesignAgent", "AudioAgent", "SecurityAgent"],
      "tasks":   [...]
    }},
    {{
      "phase":   3,
      "name":    "Quality & Delivery",
      "agents":  ["QAAgent"],
      "tasks":   [...]
    }}
  ],
  "deliverables": ["APK file", "source code", "design assets", ...]
}}

Only include agents from this list: {agents_needed}
Each subtask must be a concrete, self-contained instruction — imagine you
are delegating to a human expert who knows nothing else about the project.
""",
        )
        return plan_raw

    def _print_plan(self, plan: dict):
        """Pretty-print the plan to stdout."""
        print()
        print("=" * 70)
        print(f"  PROJECT: {plan.get('title', '?')}")
        print("=" * 70)
        for phase in plan.get("phases", []):
            print(f"\n  Phase {phase['phase']}: {phase['name']}")
            print(f"  Agents: {', '.join(phase.get('agents', []))}")
            for t in phase.get("tasks", []):
                print(f"    • [{t['agent']}] {t['subtask'][:90]}")
        print()
        deliverables = plan.get("deliverables", [])
        if deliverables:
            print("  Deliverables:")
            for d in deliverables:
                print(f"    - {d}")
        print("=" * 70)
        print()
