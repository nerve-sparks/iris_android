"""
server.py — FastAPI application for AI Studio.

Endpoints:
  GET  /                      → index.html
  POST /api/run               → {"goal": "..."} → {"run_id": "..."}
  GET  /api/runs              → list of runs
  GET  /api/run/{run_id}      → run detail
  GET  /events/{run_id}       → SSE stream (text/event-stream)
  GET  /api/artifacts/{run_id}→ list artifact paths
  GET  /api/artifact          → ?path=... → file content
"""
from __future__ import annotations
import asyncio
import json
import os

from fastapi import FastAPI, HTTPException, Query
from fastapi.responses import StreamingResponse, FileResponse, JSONResponse
from fastapi.staticfiles import StaticFiles
from pydantic import BaseModel

from . import events as ev_store
from . import runner

app = FastAPI(title="Matrix AI", docs_url="/docs")

_STATIC = os.path.join(os.path.dirname(__file__), "static")
app.mount("/static", StaticFiles(directory=_STATIC), name="static")


# ── Lifecycle ──────────────────────────────────────────────────────────────

@app.on_event("startup")
async def _startup():
    ev_store.init_loop(asyncio.get_event_loop())


# ── Pages ──────────────────────────────────────────────────────────────────

@app.get("/", include_in_schema=False)
async def index():
    return FileResponse(os.path.join(_STATIC, "index.html"))


# ── Run management ─────────────────────────────────────────────────────────

class RunRequest(BaseModel):
    goal: str


@app.post("/api/run")
async def start_run(req: RunRequest):
    if not req.goal.strip():
        raise HTTPException(400, "goal must not be empty")
    run_id = runner.start_run(req.goal.strip())
    return {"run_id": run_id}


@app.get("/api/runs")
async def list_runs():
    return runner.get_runs()


@app.get("/api/run/{run_id}")
async def get_run(run_id: str):
    run = runner.get_run(run_id)
    if not run:
        raise HTTPException(404, "Run not found")
    return run


# ── SSE event stream ───────────────────────────────────────────────────────

@app.get("/events/{run_id}")
async def sse_stream(run_id: str):
    """Server-Sent Events stream for a specific run."""
    if runner.get_run(run_id) is None:
        raise HTTPException(404, "Run not found")

    async def generate():
        async for event in ev_store.subscribe(run_id):
            if event.get("type") == "ping":
                yield ": keepalive\n\n"
            else:
                yield f"data: {json.dumps(event, ensure_ascii=False)}\n\n"

    return StreamingResponse(
        generate(),
        media_type="text/event-stream",
        headers={
            "Cache-Control": "no-cache",
            "X-Accel-Buffering": "no",
            "Connection": "keep-alive",
        },
    )


# ── Artifacts ─────────────────────────────────────────────────────────────

@app.get("/api/artifacts/{run_id}")
async def list_artifacts(run_id: str):
    run = runner.get_run(run_id)
    if not run:
        raise HTTPException(404, "Run not found")
    result = run.get("result", {})
    artifacts: list[str] = result.get("artifacts", [])
    # Build a simple tree structure
    tree: list[dict] = []
    for path in artifacts:
        if os.path.isfile(path):
            tree.append({
                "path": path,
                "name": os.path.basename(path),
                "size": os.path.getsize(path),
                "ext": os.path.splitext(path)[1].lower(),
            })
    return {"artifacts": tree}


@app.get("/api/artifact")
async def get_artifact(path: str = Query(...)):
    if not os.path.isfile(path):
        raise HTTPException(404, "File not found")
    # Restrict to output dir
    from agents.config import cfg
    output_dir = os.path.abspath(cfg.output_dir)
    abs_path = os.path.abspath(path)
    if not abs_path.startswith(output_dir):
        raise HTTPException(403, "Access denied")

    ext = os.path.splitext(path)[1].lower()
    # Return binary assets directly
    if ext in {".png", ".jpg", ".jpeg", ".gif", ".webp"}:
        return FileResponse(path)
    if ext in {".mp3", ".mp4", ".wav", ".ogg"}:
        return FileResponse(path)

    try:
        with open(path, encoding="utf-8", errors="replace") as f:
            content = f.read()
    except Exception as e:
        raise HTTPException(500, str(e))

    return {"path": path, "name": os.path.basename(path), "content": content, "ext": ext}
