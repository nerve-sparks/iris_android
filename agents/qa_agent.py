"""
QAAgent  —  🧪  Quality assurance & critical review.

Responsibilities:
  1. Review the full project output (plan, architecture, code, assets).
  2. Identify gaps, bugs, and inconsistencies.
  3. Generate test plans (unit, integration, E2E, security).
  4. Generate sample unit tests (Kotlin JUnit4 + Mockk).
  5. Produce a final project report.
"""

from __future__ import annotations
import json
import os

from .base_agent import BaseAgent
from .config import cfg


SYSTEM_PROMPT = """
You are the QAAgent — a senior quality engineer and code reviewer.

You are rigorous, constructive, and thorough.
You identify:
  • Missing requirements or edge cases
  • Security flaws
  • UX problems
  • Performance bottlenecks
  • Test coverage gaps

You produce actionable, prioritised findings.
"""


class QAAgent(BaseAgent):
    name = "QAAgent"

    def execute(self, task: dict) -> dict:
        goal = task.get("goal", "")
        context = task.get("context", {})
        subtask = task.get("subtask", "Full QA review and test generation")

        plan = context.get("plan", {})
        modules = context.get("modules", [])
        threat_model = context.get("threat_model", {})
        ui_spec = context.get("ui_spec", {})
        audio_plan = context.get("audio_plan", {})

        # ── Step 1: critical review ───────────────────────────────────────
        self._log("Running critical review …")
        review = self._critical_review(goal, plan, modules, threat_model, ui_spec)

        # ── Step 2: test plan ─────────────────────────────────────────────
        self._log("Generating test plan …")
        test_plan = self._generate_test_plan(goal, modules, review)

        # ── Step 3: generate sample unit tests ───────────────────────────
        self._log("Writing sample unit tests …")
        artifacts: list[str] = []
        test_files = self._generate_unit_tests(goal, modules)
        artifacts.extend(test_files)

        # ── Step 4: final report ──────────────────────────────────────────
        self._log("Writing final project report …")
        final_report = self._write_final_report(
            goal, plan, review, test_plan, artifacts, context
        )
        report_path = self.save_artifact("FINAL_REPORT.md", final_report)
        artifacts.append(report_path)

        qa_path = self.save_artifact(
            "qa_review.json",
            json.dumps({"review": review, "test_plan": test_plan}, indent=2, ensure_ascii=False),
        )
        artifacts.append(qa_path)

        issues = review.get("issues", [])
        critical = [i for i in issues if i.get("severity") == "Critical"]

        return self._ok(
            summary=(
                f"QA: {len(issues)} issues found ({len(critical)} critical), "
                f"{len(test_files)} test files generated"
            ),
            artifacts=artifacts,
            review=review,
            test_plan=test_plan,
        )

    # ── Private helpers ────────────────────────────────────────────────────

    def _critical_review(
        self,
        goal: str,
        plan: dict,
        modules: list,
        threat_model: dict,
        ui_spec: dict,
    ) -> dict:
        return self.chat_json(
            system=SYSTEM_PROMPT,
            user=f"""
Project: {goal}

Plan overview:
{json.dumps(plan, indent=2, ensure_ascii=False)[:2000]}

Modules ({len(modules)}):
{json.dumps([{"name": m["name"], "type": m.get("type")} for m in modules], indent=2)}

Threats identified: {len(threat_model.get("threats", []))}
UI screens: {len(ui_spec.get("screens", []))}

Perform a critical review. Return JSON:
{{
  "issues": [
    {{
      "id":          "QA-001",
      "severity":    "Critical | High | Medium | Low",
      "category":    "Security | UX | Performance | Architecture | Missing Feature | Bug Risk",
      "description": "Precise description of the problem",
      "location":    "ModuleName or component",
      "recommendation": "How to fix it"
    }}
  ],
  "missing_requirements": ["..."],
  "positive_findings": ["..."],
  "overall_quality_score": 7,
  "ready_for_development": true
}}
""",
        )

    def _generate_test_plan(self, goal: str, modules: list, review: dict) -> dict:
        issues = review.get("issues", [])
        return self.chat_json(
            system=SYSTEM_PROMPT,
            user=f"""
Project: {goal}
Modules: {[m['name'] for m in modules]}
Known issues: {len(issues)}

Create a test plan. Return JSON:
{{
  "unit_tests": [
    {{
      "module":    "CryptoManager",
      "test_class": "CryptoManagerTest",
      "cases":     ["encrypt and decrypt returns original plaintext",
                    "different IVs produce different ciphertext",
                    "tampering with ciphertext throws AEADBadTagException"]
    }}
  ],
  "integration_tests": [
    {{
      "name":      "VaultUnlock flow",
      "steps":     ["Open app", "Record whistle melody", "App detects match", "Vault opens"],
      "expected":  "Vault content visible within 2 seconds"
    }}
  ],
  "e2e_scenarios": [
    {{
      "name":      "First-time setup",
      "steps":     ["..."],
      "acceptance": "..."
    }}
  ],
  "performance_benchmarks": [
    {{"metric": "App cold start", "target": "< 2s on mid-range device"}},
    {{"metric": "Whistle recognition latency", "target": "< 500ms"}}
  ],
  "security_tests": [
    "Verify key not extractable from Keystore",
    "Verify app fails open backup attempt",
    "Verify brute-force lockout after 5 attempts"
  ]
}}
""",
        )

    def _generate_unit_tests(self, goal: str, modules: list) -> list[str]:
        """Generate sample unit test files for the most important modules."""
        saved: list[str] = []
        # Only test the first 3 most important modules to keep output manageable
        testable = [m for m in modules if m.get("type") in {
            "Repository", "ViewModel", "Service", "Util", "API", "Library"
        }][:3]

        for mod in testable:
            class_name = (mod.get("key_classes") or [mod.get("name", "Unknown")])[0]
            test_class = f"{class_name}Test"
            filename = f"{test_class}.kt"

            code = self.chat(
                system=SYSTEM_PROMPT,
                user=f"""
Project: {goal}
Module: {mod.get("name")}  ({mod.get("description", "")})
Class under test: {class_name}

Write a complete JUnit4 + Mockk unit test class in Kotlin.

Requirements:
- Use @RunWith(MockitoJUnitRunner::class) OR JUnit4 runner with Mockk.
- Cover happy path, edge cases, and error cases.
- Use runTest {{ }} for coroutine tests (kotlinx-coroutines-test).
- At least 5 meaningful test cases.
- Include proper @Before setup.

Write only the Kotlin file content.
""",
                model=cfg.claude_code_model,
                max_tokens=4096,
            )
            path = self.save_artifact(f"code/test/{filename}", code)
            saved.append(path)
            self._log(f"  ✓ {filename}")

        return saved

    def _write_final_report(
        self,
        goal: str,
        plan: dict,
        review: dict,
        test_plan: dict,
        artifacts: list,
        context: dict,
    ) -> str:
        issues = review.get("issues", [])
        critical = [i for i in issues if i.get("severity") == "Critical"]
        high = [i for i in issues if i.get("severity") == "High"]
        score = review.get("overall_quality_score", "?")
        ready = review.get("ready_for_development", False)

        return self.chat(
            system=SYSTEM_PROMPT,
            user=f"""
Write a comprehensive Final Project Report in Markdown.

Project: {goal}
Title: {plan.get("title", "?")}

Summary statistics:
- Modules designed: {len(context.get("modules", []))}
- Files generated: {len(artifacts)}
- Critical issues: {len(critical)}
- High issues: {len(high)}
- Quality score: {score}/10
- Ready for development: {"YES" if ready else "NO — fix critical issues first"}

Critical issues:
{json.dumps(critical, indent=2, ensure_ascii=False)[:2000]}

Test plan summary:
- Unit test suites: {len(test_plan.get("unit_tests", []))}
- Integration tests: {len(test_plan.get("integration_tests", []))}
- E2E scenarios: {len(test_plan.get("e2e_scenarios", []))}
- Security tests: {len(test_plan.get("security_tests", []))}

Format:
# Final Report: [Project Title]
## What was built
## Architecture summary
## Generated artefacts
## Quality assessment
## Critical issues & fixes required
## Test plan overview
## Next steps for the development team
## Deployment checklist
""",
        )
