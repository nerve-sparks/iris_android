"""
events_bus.py — Thread-safe event publisher for the agent system.

Agents call emit() from background threads.
The Studio server sets a publisher function that routes events
to the SSE stream for the active run.
"""
from __future__ import annotations
import threading
from typing import Callable, Optional

_local = threading.local()          # per-thread run_id
_publisher: Optional[Callable] = None  # set once by studio.runner


def configure(publisher: Callable) -> None:
    """Called once at startup. publisher(run_id, **kwargs) must be thread-safe."""
    global _publisher
    _publisher = publisher


def set_run_context(run_id: str) -> None:
    """Call from the worker thread before starting agents."""
    _local.run_id = run_id


def clear_run_context() -> None:
    _local.run_id = None


def emit(agent: str, **kwargs) -> None:
    """Emit an event for the current thread's run. No-op if no context."""
    run_id = getattr(_local, "run_id", None)
    if _publisher and run_id:
        _publisher(run_id, agent=agent, **kwargs)
