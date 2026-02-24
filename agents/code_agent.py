"""
CodeAgent  —  👨‍💻  Source code generation.

Receives module specs from ArchitectAgent and generates complete,
production-quality source files.  Each file is saved to the output
directory under code/<relative_path>.

Strategy:
  1. For each module in the spec, generate its source files one by one.
  2. Use Claude Sonnet (fast) for individual files.
  3. Use Claude Opus (powerful) for complex modules (security, audio DSP).
  4. After all files, generate a root build file (build.gradle.kts / CMakeLists, etc.).
"""

from __future__ import annotations
import json
import os

from .base_agent import BaseAgent
from .config import cfg


SYSTEM_PROMPT = """
You are the CodeAgent — an expert software engineer.

Rules:
- Write complete, working source code.  NO placeholders, NO "TODO: implement".
- Follow the architecture spec exactly.
- Add only the comments needed to understand non-obvious logic.
- Use modern idioms (Kotlin Coroutines, Flow, Compose, etc.).
- Every file must compile (or run) standalone as much as possible.
- For Android: target SDK 34, minSdk 26.
- For security-sensitive code: use Android Keystore + AES-256-GCM, never roll your own crypto.
"""

# Modules whose complexity warrants the Opus model
HIGH_COMPLEXITY_TYPES = {"Service", "Repository", "API", "Library"}


class CodeAgent(BaseAgent):
    name = "CodeAgent"

    def execute(self, task: dict) -> dict:
        goal = task.get("goal", "")
        context = task.get("context", {})
        subtask = task.get("subtask", "Generate all source code")
        modules: list[dict] = context.get("modules", [])
        data_model: list[dict] = context.get("data_model", [])
        arch_doc: str = context.get("arch_doc", "")

        if not modules:
            return self._err("No module spec received from ArchitectAgent.")

        self._log(f"Generating code for {len(modules)} modules …")
        generated: list[str] = []

        for mod in modules:
            mod_name = mod.get("name", "Unknown")
            self._log(f"  Coding module: {mod_name}")
            files = self._generate_module(mod, goal, arch_doc, data_model)
            generated.extend(files)

        # Generate project build file
        self._log("Generating build configuration …")
        build_file = self._generate_build_file(goal, modules)
        if build_file:
            generated.append(build_file)

        # Generate README for the code directory
        readme = self._generate_code_readme(goal, modules, generated)
        readme_path = self.save_artifact("code/README.md", readme)
        generated.append(readme_path)

        return self._ok(
            summary=f"Generated {len(generated)} source files for {len(modules)} modules",
            artifacts=generated,
            file_count=len(generated),
            module_count=len(modules),
        )

    # ── Private helpers ────────────────────────────────────────────────────

    def _generate_module(
        self,
        mod: dict,
        goal: str,
        arch_doc: str,
        data_model: list,
    ) -> list[str]:
        """Generate all source files for a single module."""
        mod_name = mod.get("name", "")
        mod_type = mod.get("type", "")
        language = mod.get("language", "Kotlin")
        description = mod.get("description", "")
        key_classes = mod.get("key_classes", [])
        deps = mod.get("dependencies", [])
        file_paths = mod.get("files", [])

        # Pick model based on complexity
        model = cfg.claude_model if mod_type in HIGH_COMPLEXITY_TYPES else cfg.claude_code_model

        # Generate each file declared in the spec
        saved: list[str] = []
        for rel_path in file_paths:
            code = self.chat(
                system=SYSTEM_PROMPT,
                user=f"""
Project: {goal}

You are generating the file: {rel_path}

Module: {mod_name} ({mod_type})
Language: {language}
Purpose: {description}
Key classes/functions in this file: {', '.join(key_classes)}
Dependencies: {', '.join(deps)}

Data model context:
{json.dumps(data_model[:5], indent=2, ensure_ascii=False)}

Architecture notes (excerpt):
{arch_doc[:1500]}

Write the COMPLETE file content for {rel_path}.
Start directly with the file content — no preamble, no explanation.
""",
                model=model,
                max_tokens=8192,
            )
            out_path = self.save_artifact(f"code/{rel_path}", code)
            saved.append(out_path)
            self._log(f"    ✓ {rel_path}")

        # If no files were declared, generate a single class file
        if not file_paths and key_classes:
            first_class = key_classes[0]
            ext = self._ext(language)
            rel = f"{mod_name}/{first_class}{ext}"
            code = self.chat(
                system=SYSTEM_PROMPT,
                user=f"""
Project: {goal}
Module: {mod_name}
Generate class {first_class} in {language}.
Purpose: {description}
Dependencies: {', '.join(deps)}
Write the complete file.
""",
                model=model,
                max_tokens=8192,
            )
            out_path = self.save_artifact(f"code/{rel}", code)
            saved.append(out_path)

        return saved

    def _generate_build_file(self, goal: str, modules: list) -> str | None:
        """Generate a build.gradle.kts (or equivalent) for the project."""
        languages = {m.get("language", "Kotlin") for m in modules}
        if "Kotlin" not in languages and "Java" not in languages:
            return None

        deps_needed = set()
        for m in modules:
            for d in m.get("dependencies", []):
                deps_needed.add(d)

        content = self.chat(
            system=SYSTEM_PROMPT,
            user=f"""
Generate a complete Android build.gradle.kts (app-level) for this project.

Project: {goal}
External dependencies mentioned: {', '.join(deps_needed)}

Include:
- compileSdk 34, minSdk 26, targetSdk 34
- Jetpack Compose (BOM latest)
- Coroutines, ViewModel, Room with encryption if security is needed
- Any other deps implied by the project

Write ONLY the file content. Start with `plugins {{`.
""",
            model=cfg.claude_code_model,
        )
        return self.save_artifact("code/build.gradle.kts", content)

    def _generate_code_readme(self, goal: str, modules: list, files: list) -> str:
        return self.chat(
            system=SYSTEM_PROMPT,
            user=f"""
Write a short README.md for the generated code of this project.

Project: {goal}
Modules: {[m['name'] for m in modules]}
Files generated: {len(files)}

Cover: setup instructions, how to build, module overview (one line each).
Keep it under 60 lines.
""",
        )

    @staticmethod
    def _ext(language: str) -> str:
        return {
            "Kotlin": ".kt",
            "Java": ".java",
            "Python": ".py",
            "Swift": ".swift",
            "Dart": ".dart",
            "JavaScript": ".js",
            "TypeScript": ".ts",
        }.get(language, ".txt")
