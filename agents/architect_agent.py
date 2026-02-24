"""
ArchitectAgent  —  🧱  System design & technology decisions.

Given a project plan from ProducerAgent, it:
1. Selects the best tech stack for each component.
2. Defines the module/package structure.
3. Designs data flow and key interfaces.
4. Documents architectural decisions and trade-offs.
5. Produces machine-readable specs that CodeAgent, DesignAgent, etc. use.
"""

from __future__ import annotations
import json

from .base_agent import BaseAgent


SYSTEM_PROMPT = """
You are the ArchitectAgent — a senior software architect with expertise in:
  • Android (Kotlin, Jetpack Compose, MVVM, Room, CameraX, ML Kit)
  • Audio / DSP (Android AudioRecord, TarsosDSP, Whisper.cpp)
  • Security (AES-256-GCM, Android Keystore, BiometricPrompt)
  • Backend (Python FastAPI, Node.js, Firebase, Supabase)
  • Cross-platform (Flutter, KMM, React Native)
  • AI/ML on-device (llama.cpp, TFLite, ONNX)

Your output is always precise, structured, and actionable.
You produce markdown documentation AND JSON specs for other agents.
"""


class ArchitectAgent(BaseAgent):
    name = "ArchitectAgent"

    def execute(self, task: dict) -> dict:
        goal = task.get("goal", "")
        context = task.get("context", {})
        subtask = task.get("subtask", "Design full system architecture")
        plan = context.get("plan", {})

        # ── Step 1: module breakdown ──────────────────────────────────────
        self._log("Designing module structure …")
        module_spec = self._design_modules(goal, plan, subtask)

        # ── Step 2: data model ────────────────────────────────────────────
        self._log("Defining data models …")
        data_model = self._design_data_model(goal, module_spec)

        # ── Step 3: architecture doc ──────────────────────────────────────
        self._log("Writing architecture document …")
        arch_doc = self._write_arch_doc(goal, module_spec, data_model)

        # ── Save artefacts ────────────────────────────────────────────────
        doc_path = self.save_artifact("architecture.md", arch_doc)
        spec_path = self.save_artifact(
            "module_spec.json",
            json.dumps({"modules": module_spec, "data_model": data_model}, indent=2, ensure_ascii=False),
        )

        return self._ok(
            summary=f"Architecture designed: {len(module_spec)} modules",
            artifacts=[doc_path, spec_path],
            modules=module_spec,
            data_model=data_model,
            arch_doc=arch_doc,
        )

    # ── Private helpers ────────────────────────────────────────────────────

    def _design_modules(self, goal: str, plan: dict, subtask: str) -> list[dict]:
        return self.chat_json(
            system=SYSTEM_PROMPT,
            user=f"""
Project goal: {goal}

Subtask: {subtask}

Plan overview:
{json.dumps(plan, indent=2, ensure_ascii=False)[:3000]}

Return a JSON array of modules. Each module object:
{{
  "name":        "ModuleName",
  "type":        "Android Screen | Service | Repository | ViewModel | Util | API | Library",
  "language":    "Kotlin | Python | Java | ...",
  "description": "What this module does",
  "key_classes": ["ClassName1", "ClassName2"],
  "dependencies": ["OtherModule", "external-lib"],
  "files":       ["path/to/File.kt", ...]
}}

Be exhaustive. Think through every component needed.
""",
        )

    def _design_data_model(self, goal: str, modules: list) -> list[dict]:
        return self.chat_json(
            system=SYSTEM_PROMPT,
            user=f"""
Project: {goal}

Modules: {json.dumps([m["name"] for m in modules])}

Design the data model. Return a JSON array of entity objects:
{{
  "entity":  "EntityName",
  "storage": "Room DB | SharedPrefs | EncryptedFile | InMemory | Remote",
  "fields": [
    {{"name": "field_name", "type": "String | Int | ByteArray | ...", "description": "..."}}
  ],
  "encrypted": true,
  "relationships": ["OtherEntity (1:N)", ...]
}}
""",
        )

    def _write_arch_doc(self, goal: str, modules: list, data_model: list) -> str:
        return self.chat(
            system=SYSTEM_PROMPT,
            user=f"""
Write a comprehensive Architecture Document in Markdown for this project.

Project: {goal}

Modules ({len(modules)} total):
{json.dumps([{"name": m["name"], "description": m["description"]} for m in modules], indent=2, ensure_ascii=False)}

Data model ({len(data_model)} entities):
{json.dumps([{"entity": e["entity"], "storage": e["storage"]} for e in data_model], indent=2, ensure_ascii=False)}

The document must cover:
1. Executive summary
2. Tech stack & rationale
3. Module dependency diagram (ASCII art)
4. Component interactions / data flow
5. Security architecture
6. Key technical decisions & trade-offs
7. Development phases

Write in English. Be precise and professional.
""",
        )
