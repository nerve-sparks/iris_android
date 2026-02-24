"""
DesignAgent  —  🎨  UI/UX & visual assets.

Responsibilities:
  1. Analyse project and produce a UI/UX specification (screens, colors, typography).
  2. Generate image prompts for icons, splash screens, illustrations.
  3. Call the configured image backend (DALL-E 3 / Banana.dev / Stable Diffusion).
  4. Save all assets and return their paths.
  5. Generate an XML color/theme resource file for Android projects.
"""

from __future__ import annotations
import json
import os

from .base_agent import BaseAgent
from .config import cfg


SYSTEM_PROMPT = """
You are the DesignAgent — a senior UI/UX designer and Android design specialist.

You excel at:
  • Material Design 3 (Material You)
  • Creating detailed, precise image generation prompts
  • Defining color palettes, typography, and spacing systems
  • Android XML resources (colors.xml, themes.xml, styles.xml)
  • Accessibility and dark mode support

Your prompts for image generation must be vivid, specific, and optimised
for AI image models (DALL-E 3 / Stable Diffusion).
"""


class DesignAgent(BaseAgent):
    name = "DesignAgent"

    def execute(self, task: dict) -> dict:
        goal = task.get("goal", "")
        context = task.get("context", {})
        subtask = task.get("subtask", "Create all UI/UX assets")
        modules = context.get("modules", [])

        # ── Step 1: UI specification ──────────────────────────────────────
        self._log("Creating UI/UX specification …")
        ui_spec = self._create_ui_spec(goal, modules, subtask)

        # ── Step 2: generate image assets ────────────────────────────────
        artifacts: list[str] = []
        image_prompts: list[dict] = ui_spec.get("image_prompts", [])
        self._log(f"Generating {len(image_prompts)} image asset(s) …")
        for item in image_prompts:
            name = item.get("filename", "asset.png")
            prompt = item.get("prompt", "")
            try:
                from .tools.api_clients import generate_image
                path = generate_image(prompt, filename=f"design/{name}")
                artifacts.append(path)
                self._log(f"  ✓ {name}")
            except Exception as e:
                self._log(f"  ✗ {name}: {e} (skipped)")

        # ── Step 3: Android theme resources ──────────────────────────────
        self._log("Generating Android theme resources …")
        colors_xml = self._generate_colors_xml(ui_spec)
        theme_xml = self._generate_theme_xml(ui_spec)
        typography_kt = self._generate_typography_kt(ui_spec)

        colors_path = self.save_artifact("code/app/src/main/res/values/colors.xml", colors_xml)
        theme_path = self.save_artifact("code/app/src/main/res/values/themes.xml", theme_xml)
        typo_path = self.save_artifact("code/app/src/main/java/ui/theme/Type.kt", typography_kt)
        spec_path = self.save_artifact(
            "design/ui_spec.json",
            json.dumps(ui_spec, indent=2, ensure_ascii=False),
        )

        artifacts += [colors_path, theme_path, typo_path, spec_path]

        return self._ok(
            summary=f"UI spec created, {len(image_prompts)} images generated, theme resources ready",
            artifacts=artifacts,
            ui_spec=ui_spec,
        )

    # ── Private helpers ────────────────────────────────────────────────────

    def _create_ui_spec(self, goal: str, modules: list, subtask: str) -> dict:
        screens = [m["name"] for m in modules if "Screen" in m.get("type", "")]
        return self.chat_json(
            system=SYSTEM_PROMPT,
            user=f"""
Project: {goal}
Subtask: {subtask}
Screens detected: {screens or ['Main', 'Detail', 'Settings']}

Create a complete UI/UX specification. Return JSON:
{{
  "app_name":  "...",
  "theme":     "dark | light | auto",
  "color_palette": {{
    "primary":     "#RRGGBB",
    "secondary":   "#RRGGBB",
    "background":  "#RRGGBB",
    "surface":     "#RRGGBB",
    "error":       "#RRGGBB",
    "on_primary":  "#RRGGBB",
    "on_background":"#RRGGBB"
  }},
  "typography": {{
    "headline_font": "font name",
    "body_font":     "font name",
    "font_scale":    "normal"
  }},
  "screens": [
    {{
      "name":        "ScreenName",
      "description": "What the user does here",
      "components":  ["TopAppBar", "LazyColumn", "FAB", ...]
    }}
  ],
  "image_prompts": [
    {{
      "purpose":  "App icon",
      "filename": "icon.png",
      "prompt":   "Ultra detailed app icon for [description], Material Design 3 style, ..."
    }},
    {{
      "purpose":  "Splash screen background",
      "filename": "splash_bg.png",
      "prompt":   "..."
    }}
  ],
  "animations": [
    {{
      "name":   "unlock_success",
      "type":   "Lottie | ObjectAnimator | Compose",
      "description": "..."
    }}
  ]
}}

Choose colors and assets appropriate for the app's purpose and tone.
""",
        )

    def _generate_colors_xml(self, ui_spec: dict) -> str:
        palette = ui_spec.get("color_palette", {})
        lines = ['<?xml version="1.0" encoding="utf-8"?>', "<resources>"]
        color_map = {
            "primary":       "colorPrimary",
            "secondary":     "colorSecondary",
            "background":    "colorBackground",
            "surface":       "colorSurface",
            "error":         "colorError",
            "on_primary":    "colorOnPrimary",
            "on_background": "colorOnBackground",
        }
        for key, xml_name in color_map.items():
            val = palette.get(key, "#000000")
            lines.append(f'    <color name="{xml_name}">{val}</color>')
        lines.append("</resources>")
        return "\n".join(lines)

    def _generate_theme_xml(self, ui_spec: dict) -> str:
        palette = ui_spec.get("color_palette", {})
        return f"""<?xml version="1.0" encoding="utf-8"?>
<resources>
    <style name="Theme.App" parent="Theme.Material3.DayNight.NoActionBar">
        <item name="colorPrimary">{palette.get("primary", "#6200EE")}</item>
        <item name="colorSecondary">{palette.get("secondary", "#03DAC6")}</item>
        <item name="android:colorBackground">{palette.get("background", "#121212")}</item>
        <item name="colorSurface">{palette.get("surface", "#1E1E1E")}</item>
        <item name="colorError">{palette.get("error", "#CF6679")}</item>
        <item name="colorOnPrimary">{palette.get("on_primary", "#FFFFFF")}</item>
    </style>
</resources>"""

    def _generate_typography_kt(self, ui_spec: dict) -> str:
        typo = ui_spec.get("typography", {})
        headline = typo.get("headline_font", "Poppins")
        body = typo.get("body_font", "Inter")
        return f"""package ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// Headline: {headline}  |  Body: {body}
val AppTypography = Typography(
    headlineLarge = TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize = 32.sp,
        lineHeight = 40.sp,
    ),
    headlineMedium = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 24.sp,
        lineHeight = 32.sp,
    ),
    bodyLarge = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
    ),
    bodyMedium = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
    ),
    labelSmall = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 16.sp,
    ),
)
"""
