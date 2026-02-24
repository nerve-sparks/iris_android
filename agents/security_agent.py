"""
SecurityAgent  —  🔐  Threat modelling, encryption, and auth design.

Responsibilities:
  1. Threat model: identify attack surfaces, assets, threats.
  2. Encryption design: choose algorithms, key management strategy.
  3. Auth flow: design and generate authentication/unlock code.
  4. Produce a Security Report (Markdown).
  5. Generate hardened implementation for security-critical modules.
"""

from __future__ import annotations
import json

from .base_agent import BaseAgent
from .config import cfg


SYSTEM_PROMPT = """
You are the SecurityAgent — an application security expert specialising in:
  • Mobile security (Android Keystore, EncryptedSharedPreferences, EncryptedFile)
  • Cryptography (AES-256-GCM, Argon2, PBKDF2, Ed25519)
  • Biometric authentication (BiometricPrompt API)
  • Secure coding (OWASP Mobile Top 10, CWE)
  • Threat modelling (STRIDE, attack trees)

You produce threat models, security architectures, and hardened code.
You NEVER recommend insecure practices and always explain WHY.
"""


class SecurityAgent(BaseAgent):
    name = "SecurityAgent"

    def execute(self, task: dict) -> dict:
        goal = task.get("goal", "")
        context = task.get("context", {})
        subtask = task.get("subtask", "Design security architecture")
        modules = context.get("modules", [])
        data_model = context.get("data_model", [])

        # ── Threat model ──────────────────────────────────────────────────
        self._log("Building threat model …")
        threat_model = self._build_threat_model(goal, modules, data_model)

        # ── Security architecture ─────────────────────────────────────────
        self._log("Designing security architecture …")
        sec_arch = self._design_security_arch(goal, threat_model)

        # ── Generate security code ────────────────────────────────────────
        artifacts: list[str] = []
        self._log("Generating encryption & auth code …")
        sec_files = self._generate_security_code(goal, sec_arch, modules)
        artifacts.extend(sec_files)

        # ── Security report ───────────────────────────────────────────────
        self._log("Writing security report …")
        report = self._write_report(goal, threat_model, sec_arch)
        report_path = self.save_artifact("security_report.md", report)
        artifacts.append(report_path)

        arch_path = self.save_artifact(
            "security_arch.json",
            json.dumps({"threat_model": threat_model, "architecture": sec_arch}, indent=2, ensure_ascii=False),
        )
        artifacts.append(arch_path)

        threats_count = len(threat_model.get("threats", []))
        controls_count = len(sec_arch.get("controls", []))

        return self._ok(
            summary=f"Security: {threats_count} threats identified, {controls_count} controls, {len(sec_files)} code files",
            artifacts=artifacts,
            threat_model=threat_model,
            security_arch=sec_arch,
        )

    # ── Private helpers ────────────────────────────────────────────────────

    def _build_threat_model(self, goal: str, modules: list, data_model: list) -> dict:
        return self.chat_json(
            system=SYSTEM_PROMPT,
            user=f"""
Project: {goal}
Modules: {[m['name'] for m in modules]}
Data entities: {[e['entity'] for e in data_model]}

Build a STRIDE threat model. Return JSON:
{{
  "assets": [
    {{"name": "User passwords", "sensitivity": "Critical", "location": "EncryptedFile"}}
  ],
  "attack_surfaces": ["unlock screen", "exported activities", "backup data"],
  "threats": [
    {{
      "id":       "T1",
      "category": "Spoofing | Tampering | Repudiation | Info Disclosure | DoS | Elevation",
      "description": "Attacker extracts encryption key from memory",
      "likelihood": "Medium",
      "impact":     "Critical",
      "mitigation": "Use Android Keystore hardware-backed key, never load key into app memory"
    }}
  ],
  "compliance_notes": ["GDPR: all PII stored locally", "OWASP M9: insecure data storage addressed by ..."]
}}
""",
        )

    def _design_security_arch(self, goal: str, threat_model: dict) -> dict:
        threats = threat_model.get("threats", [])
        return self.chat_json(
            system=SYSTEM_PROMPT,
            user=f"""
Project: {goal}
Threats to mitigate:
{json.dumps(threats, indent=2, ensure_ascii=False)}

Design the security architecture. Return JSON:
{{
  "encryption": {{
    "algorithm":    "AES-256-GCM",
    "key_management": "Android Keystore (hardware-backed where available)",
    "key_derivation": "PBKDF2WithHmacSHA256, 310000 iterations",
    "iv_strategy":  "random 12-byte IV per encryption, stored with ciphertext",
    "at_rest":      "EncryptedFile (Jetpack Security Crypto)",
    "in_transit":   "TLS 1.3 (if network used)"
  }},
  "authentication": {{
    "primary":   "Biometric (BiometricPrompt) + fallback PIN",
    "secondary": "Custom challenge (e.g. whistle melody)",
    "lockout":   "5 failed attempts → 30s exponential back-off"
  }},
  "controls": [
    {{
      "id":          "C1",
      "name":        "Hardware-backed key storage",
      "mitigates":   ["T1"],
      "android_api": "KeyStore.getInstance(\"AndroidKeyStore\")",
      "notes":       "..."
    }}
  ],
  "secure_coding_checklist": [
    "Disable app backup (android:allowBackup=false)",
    "Clear clipboard after copy",
    "..."
  ],
  "classes_to_generate": [
    {{
      "class_name": "CryptoManager",
      "filename":   "CryptoManager.kt",
      "description": "AES-256-GCM encrypt/decrypt using Android Keystore"
    }}
  ]
}}
""",
        )

    def _generate_security_code(self, goal: str, sec_arch: dict, modules: list) -> list[str]:
        classes = sec_arch.get("classes_to_generate", [])
        enc = sec_arch.get("encryption", {})
        auth = sec_arch.get("authentication", {})
        saved: list[str] = []

        for cls in classes:
            class_name = cls.get("class_name", "SecurityClass")
            filename = cls.get("filename", f"{class_name}.kt")
            description = cls.get("description", "")

            code = self.chat(
                system=SYSTEM_PROMPT,
                user=f"""
Project: {goal}
Generate class: {class_name}
Purpose: {description}

Encryption details: {json.dumps(enc, indent=2)}
Auth details: {json.dumps(auth, indent=2)}

Requirements:
- Android Keystore for key management.
- AES-256-GCM for symmetric encryption.
- No keys or plaintext ever leave the Keystore boundary.
- Include error handling with sealed class Result<T>.
- Kotlin, coroutines-compatible.
- Complete file, no placeholders.

Write only the Kotlin file content.
""",
                model=cfg.claude_model,
                max_tokens=8192,
            )
            path = self.save_artifact(f"code/security/{filename}", code)
            saved.append(path)
            self._log(f"  ✓ {filename}")

        return saved

    def _write_report(self, goal: str, threat_model: dict, sec_arch: dict) -> str:
        threats = threat_model.get("threats", [])
        controls = sec_arch.get("controls", [])
        checklist = sec_arch.get("secure_coding_checklist", [])

        return self.chat(
            system=SYSTEM_PROMPT,
            user=f"""
Write a professional Security Report in Markdown for this project.

Project: {goal}
Threats: {len(threats)} identified
Controls: {len(controls)} implemented
Secure coding checklist: {len(checklist)} items

Key threats:
{json.dumps(threats[:5], indent=2, ensure_ascii=False)}

Key controls:
{json.dumps(controls[:5], indent=2, ensure_ascii=False)}

Secure coding checklist:
{json.dumps(checklist, indent=2, ensure_ascii=False)}

Format:
# Security Report: [Project Name]
## Executive Summary
## Threat Model
## Security Architecture
## Encryption Design
## Authentication Design
## Secure Coding Checklist
## Residual Risks
""",
        )
