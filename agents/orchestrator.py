"""
Orchestrator  —  Ties all agents together and runs the pipeline.

Pipeline:
  Phase 1 (sequential):  ProducerAgent → ArchitectAgent
  Phase 2 (sequential):  CodeAgent | DesignAgent | AudioAgent | SecurityAgent
  Phase 3 (sequential):  QAAgent
  Final:                 Consolidated report

The orchestrator passes context forward between phases so each agent
has full access to prior results.
"""

from __future__ import annotations
import json
import os
import time
from typing import Any

from .config import cfg
from .producer_agent import ProducerAgent
from .architect_agent import ArchitectAgent
from .code_agent import CodeAgent
from .design_agent import DesignAgent
from .audio_agent import AudioAgent
from .security_agent import SecurityAgent
from .qa_agent import QAAgent


# Map agent name → class
AGENT_REGISTRY = {
    "ArchitectAgent": ArchitectAgent,
    "CodeAgent":      CodeAgent,
    "DesignAgent":    DesignAgent,
    "AudioAgent":     AudioAgent,
    "SecurityAgent":  SecurityAgent,
    "QAAgent":        QAAgent,
}


class Orchestrator:
    """
    Run the full autonomous pipeline for a given user goal.

    Usage:
        orch = Orchestrator()
        result = orch.run("Build a vault Android app unlocked by whistling")
    """

    def __init__(self):
        os.makedirs(cfg.output_dir, exist_ok=True)

    def run(self, goal: str) -> dict:
        """Execute the full pipeline. Returns consolidated results dict."""
        print()
        print("╔══════════════════════════════════════════════════════════════════╗")
        print("║          AUTONOMOUS AI AGENT  —  PIPELINE START                 ║")
        print("╚══════════════════════════════════════════════════════════════════╝")
        print(f"  Goal: {goal[:90]}")
        print()

        context: dict = {}   # shared knowledge base, grows after each phase
        all_artifacts: list[str] = []
        results: dict[str, dict] = {}

        t_start = time.time()

        # ══════════════════════════════════════════════════════════════════
        # PHASE 0 — ProducerAgent  (always runs first)
        # ══════════════════════════════════════════════════════════════════
        print("▶ Phase 0 — Planning")
        producer = ProducerAgent()
        prod_result = producer.run({"goal": goal, "context": context})
        results["ProducerAgent"] = prod_result
        context.update(prod_result.get("raw", {}))
        all_artifacts.extend(prod_result.get("artifacts", []))

        if prod_result["status"] != "ok":
            return self._final(goal, results, all_artifacts, t_start, failed="ProducerAgent")

        plan: dict = context.get("plan", {})

        # ══════════════════════════════════════════════════════════════════
        # PHASE 1 — ArchitectAgent
        # ══════════════════════════════════════════════════════════════════
        print("▶ Phase 1 — Architecture")
        arch = ArchitectAgent()
        arch_task_desc = self._find_subtask(plan, "ArchitectAgent", default="Design full system architecture")
        arch_result = arch.run({
            "goal":    goal,
            "context": context,
            "subtask": arch_task_desc,
        })
        results["ArchitectAgent"] = arch_result
        context.update(arch_result.get("raw", {}))
        all_artifacts.extend(arch_result.get("artifacts", []))

        if arch_result["status"] != "ok":
            return self._final(goal, results, all_artifacts, t_start, failed="ArchitectAgent")

        # ══════════════════════════════════════════════════════════════════
        # PHASE 2 — Specialist agents
        # ══════════════════════════════════════════════════════════════════
        agents_in_plan: list[str] = self._agents_in_plan(plan)

        # Determine order: Code first, then Design + Audio + Security
        specialist_order = ["CodeAgent", "DesignAgent", "AudioAgent", "SecurityAgent"]
        specialists_to_run = [a for a in specialist_order if a in agents_in_plan]

        print(f"▶ Phase 2 — Specialist agents: {', '.join(specialists_to_run)}")

        for agent_name in specialists_to_run:
            AgentClass = AGENT_REGISTRY.get(agent_name)
            if not AgentClass:
                continue
            subtask = self._find_subtask(plan, agent_name, default=f"Handle {agent_name} responsibilities")
            agent = AgentClass()
            result = agent.run({
                "goal":    goal,
                "context": context,
                "subtask": subtask,
            })
            results[agent_name] = result
            # Merge raw data into shared context (prefix to avoid collisions)
            raw = result.get("raw", {})
            context.update(raw)
            all_artifacts.extend(result.get("artifacts", []))

        # ══════════════════════════════════════════════════════════════════
        # PHASE 3 — QAAgent  (always runs last)
        # ══════════════════════════════════════════════════════════════════
        print("▶ Phase 3 — QA & Final Report")
        qa = QAAgent()
        qa_result = qa.run({
            "goal":    goal,
            "context": context,
            "subtask": self._find_subtask(plan, "QAAgent", default="Full QA review and test generation"),
        })
        results["QAAgent"] = qa_result
        all_artifacts.extend(qa_result.get("artifacts", []))

        return self._final(goal, results, all_artifacts, t_start)

    # ── Helpers ───────────────────────────────────────────────────────────

    def _agents_in_plan(self, plan: dict) -> list[str]:
        """Collect all agent names referenced in the plan phases."""
        agents: list[str] = []
        for phase in plan.get("phases", []):
            for agent in phase.get("agents", []):
                if agent not in agents:
                    agents.append(agent)
        return agents

    def _find_subtask(self, plan: dict, agent_name: str, default: str) -> str:
        """Extract the subtask description for a specific agent from the plan."""
        for phase in plan.get("phases", []):
            for task in phase.get("tasks", []):
                if task.get("agent") == agent_name:
                    return task.get("subtask", default)
        return default

    def _final(
        self,
        goal: str,
        results: dict,
        artifacts: list,
        t_start: float,
        failed: str | None = None,
    ) -> dict:
        elapsed = round(time.time() - t_start, 1)
        ok_agents = [k for k, v in results.items() if v.get("status") == "ok"]
        err_agents = [k for k, v in results.items() if v.get("status") != "ok"]

        # Save consolidated manifest
        manifest = {
            "goal":      goal,
            "elapsed_s": elapsed,
            "agents_ok": ok_agents,
            "agents_err": err_agents,
            "artifacts": artifacts,
            "failed_at": failed,
        }
        manifest_path = os.path.join(cfg.output_dir, "manifest.json")
        with open(manifest_path, "w") as f:
            json.dump(manifest, f, indent=2, ensure_ascii=False)

        print()
        print("╔══════════════════════════════════════════════════════════════════╗")
        print("║                    PIPELINE COMPLETE                             ║")
        print("╚══════════════════════════════════════════════════════════════════╝")
        print(f"  Time      : {elapsed}s")
        print(f"  Agents OK : {', '.join(ok_agents) or 'none'}")
        if err_agents:
            print(f"  Agents ERR: {', '.join(err_agents)}")
        print(f"  Artifacts : {len(artifacts)} files in {cfg.output_dir}/")
        print(f"  Manifest  : {manifest_path}")
        print()

        return {
            "status":    "ok" if not failed else "partial",
            "manifest":  manifest,
            "results":   results,
            "artifacts": artifacts,
        }
