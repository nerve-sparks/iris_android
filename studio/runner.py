"""
runner.py — Manages background pipeline runs.

Each call to start_run() launches the full agent pipeline in a daemon
thread, publishes events via events_bus → events store, and records
the final result.
"""
from __future__ import annotations
import threading
import uuid
from typing import Optional

from agents import events_bus
from agents.orchestrator import Orchestrator
from . import events as ev_store

# Wire up the event bus publisher once at import time
events_bus.configure(ev_store.push)

# run_id → {id, goal, status, result?}
_runs: dict[str, dict] = {}
_lock = threading.Lock()


def start_run(goal: str) -> str:
    run_id = uuid.uuid4().hex[:8]
    ev_store.create_run(run_id)
    with _lock:
        _runs[run_id] = {"id": run_id, "goal": goal, "status": "running"}
    t = threading.Thread(target=_worker, args=(run_id, goal), daemon=True)
    t.start()
    return run_id


def _worker(run_id: str, goal: str) -> None:
    events_bus.set_run_context(run_id)
    try:
        orch = Orchestrator()
        result = orch.run(goal)
        with _lock:
            _runs[run_id]["status"] = "done"
            _runs[run_id]["result"] = result
        ev_store.push(
            run_id,
            type="done",
            status="ok",
            artifacts=result.get("artifacts", []),
        )
    except Exception as exc:
        with _lock:
            _runs[run_id]["status"] = "error"
            _runs[run_id]["error"] = str(exc)
        ev_store.push(run_id, type="done", status="error", error=str(exc))
    finally:
        events_bus.clear_run_context()


def get_runs() -> list[dict]:
    with _lock:
        return [
            {"id": r["id"], "goal": r["goal"][:80], "status": r["status"]}
            for r in _runs.values()
        ]


def get_run(run_id: str) -> Optional[dict]:
    with _lock:
        return _runs.get(run_id)
