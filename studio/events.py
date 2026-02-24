"""
events.py — Asyncio-based SSE event store.

Background threads push events via push() (thread-safe).
The FastAPI SSE endpoint reads via subscribe() (async generator).

Each run has:
  - _history[run_id]: full list of all events (for replay on reconnect)
  - _subscribers[run_id]: list of asyncio.Queue for live subscribers
"""
from __future__ import annotations
import asyncio
from collections import defaultdict
from datetime import datetime, timezone
from typing import AsyncGenerator

_history: dict[str, list[dict]] = defaultdict(list)
_subscribers: dict[str, list[asyncio.Queue]] = defaultdict(list)
_loop: asyncio.AbstractEventLoop | None = None


def init_loop(loop: asyncio.AbstractEventLoop) -> None:
    global _loop
    _loop = loop


def create_run(run_id: str) -> None:
    _history[run_id] = []


def push(run_id: str, **kwargs) -> None:
    """Thread-safe. Called from background worker threads."""
    event = {"ts": datetime.now(timezone.utc).isoformat(), **kwargs}
    _history[run_id].append(event)
    if _loop and _loop.is_running():
        asyncio.run_coroutine_threadsafe(_broadcast(run_id, event), _loop)


async def _broadcast(run_id: str, event: dict) -> None:
    for q in list(_subscribers[run_id]):
        await q.put(event)


async def subscribe(run_id: str) -> AsyncGenerator[dict, None]:
    """
    Async generator: replays all history, then streams live events.
    Stops when a {"type": "done"} event is received.
    """
    q: asyncio.Queue = asyncio.Queue()
    _subscribers[run_id].append(q)
    try:
        # Replay full history first
        for event in list(_history.get(run_id, [])):
            yield event
            if event.get("type") == "done":
                return
        # Stream new events
        while True:
            try:
                event = await asyncio.wait_for(q.get(), timeout=120)
            except asyncio.TimeoutError:
                # Keepalive ping
                yield {"type": "ping"}
                continue
            yield event
            if event.get("type") == "done":
                return
    finally:
        try:
            _subscribers[run_id].remove(q)
        except ValueError:
            pass


def get_history(run_id: str) -> list[dict]:
    return list(_history.get(run_id, []))
