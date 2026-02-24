"""
AudioAgent  —  🔊  Audio assets, TTS, and audio recognition.

Responsibilities:
  1. Identify all audio needs from the project context.
  2. Generate voice-overs via ElevenLabs (or gTTS as fallback).
  3. Generate background music / sound effects via Suno.
  4. For audio-recognition features (e.g. whistle detection):
       a. Define the recognition algorithm & Android implementation.
       b. Generate the Kotlin/DSP code for pitch detection.
  5. Save all audio files and code.
"""

from __future__ import annotations
import json

from .base_agent import BaseAgent
from .config import cfg


SYSTEM_PROMPT = """
You are the AudioAgent — an expert in:
  • Mobile audio engineering (Android AudioRecord, AudioTrack, MediaPlayer)
  • Digital Signal Processing (FFT, pitch detection, autocorrelation, YIN algorithm)
  • Voice synthesis APIs (ElevenLabs, Google TTS)
  • Music generation (Suno AI)
  • TarsosDSP, Aubio, librosa

When generating recognition code, prefer robust, tested algorithms.
For whistle / melody detection, use YIN pitch estimation + DTW (Dynamic Time Warping)
for melody matching.
"""


class AudioAgent(BaseAgent):
    name = "AudioAgent"

    def execute(self, task: dict) -> dict:
        goal = task.get("goal", "")
        context = task.get("context", {})
        subtask = task.get("subtask", "Handle all audio requirements")
        modules = context.get("modules", [])

        # ── Analyse audio requirements ────────────────────────────────────
        self._log("Analysing audio requirements …")
        audio_plan = self._analyse_requirements(goal, modules, subtask)

        artifacts: list[str] = []

        # ── Generate voice assets ─────────────────────────────────────────
        voice_lines: list[dict] = audio_plan.get("voice_lines", [])
        if voice_lines:
            self._log(f"Generating {len(voice_lines)} voice line(s) …")
            for item in voice_lines:
                text = item.get("text", "")
                filename = item.get("filename", "speech.mp3")
                try:
                    if cfg.tts_backend == "elevenlabs" and cfg.elevenlabs_api_key:
                        from .tools.api_clients import tts_elevenlabs
                        path = tts_elevenlabs(text, filename=f"audio/{filename}")
                    else:
                        from .tools.api_clients import tts_gtts
                        path = tts_gtts(text, filename=f"audio/{filename}")
                    artifacts.append(path)
                    self._log(f"  ✓ {filename}")
                except Exception as e:
                    self._log(f"  ✗ {filename}: {e} (skipped)")

        # ── Generate music / sound effects ────────────────────────────────
        music_items: list[dict] = audio_plan.get("music", [])
        if music_items and cfg.suno_api_key:
            self._log(f"Generating {len(music_items)} music track(s) via Suno …")
            for item in music_items:
                prompt = item.get("prompt", "")
                filename = item.get("filename", "music.mp3")
                try:
                    from .tools.api_clients import generate_music_suno
                    path = generate_music_suno(prompt, filename=f"audio/{filename}")
                    artifacts.append(path)
                    self._log(f"  ✓ {filename}")
                except Exception as e:
                    self._log(f"  ✗ {filename}: {e} (skipped)")

        # ── Generate audio recognition code ──────────────────────────────
        recognition = audio_plan.get("recognition_features", [])
        for feature in recognition:
            self._log(f"Generating recognition code: {feature.get('name', '?')} …")
            code_file = self._generate_recognition_code(feature, goal)
            if code_file:
                artifacts.append(code_file)

        # ── Save audio plan ───────────────────────────────────────────────
        plan_path = self.save_artifact(
            "audio/audio_plan.json",
            json.dumps(audio_plan, indent=2, ensure_ascii=False),
        )
        artifacts.append(plan_path)

        return self._ok(
            summary=(
                f"Audio: {len(voice_lines)} voice lines, "
                f"{len(music_items)} music tracks, "
                f"{len(recognition)} recognition feature(s)"
            ),
            artifacts=artifacts,
            audio_plan=audio_plan,
        )

    # ── Private helpers ────────────────────────────────────────────────────

    def _analyse_requirements(self, goal: str, modules: list, subtask: str) -> dict:
        return self.chat_json(
            system=SYSTEM_PROMPT,
            user=f"""
Project: {goal}
Audio subtask: {subtask}
Modules: {[m['name'] for m in modules]}

Analyse and return JSON with all audio requirements:
{{
  "voice_lines": [
    {{
      "purpose":  "Welcome message shown on first launch",
      "text":     "...",
      "language": "ru",
      "filename": "welcome.mp3",
      "voice":    "Rachel"
    }}
  ],
  "music": [
    {{
      "purpose":  "Background ambient music for the main screen",
      "prompt":   "calm, mysterious, ambient electronic, 90 BPM, no lyrics",
      "filename": "bg_main.mp3"
    }}
  ],
  "recognition_features": [
    {{
      "name":        "WhistleUnlock",
      "description": "Detect a specific whistled melody to unlock the app",
      "algorithm":   "YIN pitch detection + DTW melody matching",
      "android_api": "AudioRecord",
      "library":     "TarsosDSP",
      "class_name":  "WhistleDetector",
      "filename":    "WhistleDetector.kt"
    }}
  ],
  "sound_effects": [
    {{
      "name":     "unlock_success",
      "filename": "unlock_success.mp3",
      "description": "played when vault opens"
    }}
  ]
}}

Only include items that are actually needed for this project.
""",
        )

    def _generate_recognition_code(self, feature: dict, goal: str) -> str | None:
        name = feature.get("name", "AudioRecognizer")
        class_name = feature.get("class_name", name)
        algorithm = feature.get("algorithm", "")
        library = feature.get("library", "TarsosDSP")
        description = feature.get("description", "")
        filename = feature.get("filename", f"{class_name}.kt")

        code = self.chat(
            system=SYSTEM_PROMPT,
            user=f"""
Project: {goal}
Feature: {name}
Description: {description}
Algorithm: {algorithm}
Library: {library}
Class name: {class_name}

Write a complete, production-ready Kotlin implementation of {class_name}.

Requirements:
- Use Android AudioRecord for audio capture.
- Use {library} for signal processing.
- Implement {algorithm}.
- Expose a clean API: start(), stop(), onMelodyMatched(callback).
- Include coroutines for background processing.
- Handle permissions (RECORD_AUDIO) gracefully.
- Include brief inline comments for the DSP logic.

Write the complete Kotlin file content only.
""",
            model=cfg.claude_model,
            max_tokens=8192,
        )
        return self.save_artifact(f"code/audio/{filename}", code)
