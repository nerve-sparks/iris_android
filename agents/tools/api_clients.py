"""
Thin wrappers for external creative APIs.

All functions return either a local file path (str) or a URL (str).
They raise RuntimeError on failure so callers can decide whether to retry
or fall back gracefully.
"""

from __future__ import annotations
import base64
import io
import os
import time
import requests

from ..config import cfg


# ═══════════════════════════════════════════════════════════════════════════
#  Image generation
# ═══════════════════════════════════════════════════════════════════════════

def generate_image(prompt: str, filename: str = "image.png") -> str:
    """
    Generate an image from a text prompt.
    Backend is determined by cfg.image_backend.
    Returns the local file path where the image is saved.
    """
    if cfg.image_backend == "dall-e-3":
        return _dalle3(prompt, filename)
    elif cfg.image_backend == "banana":
        return _banana_image(prompt, filename)
    else:
        raise RuntimeError(f"Unknown image backend: {cfg.image_backend}")


def _dalle3(prompt: str, filename: str) -> str:
    import openai
    client = openai.OpenAI(api_key=cfg.openai_api_key)
    response = client.images.generate(
        model="dall-e-3",
        prompt=prompt,
        size="1024x1024",
        quality="standard",
        n=1,
        response_format="b64_json",
    )
    image_bytes = base64.b64decode(response.data[0].b64_json)
    path = os.path.join(cfg.output_dir, "design", filename)
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "wb") as f:
        f.write(image_bytes)
    return path


def _banana_image(prompt: str, filename: str) -> str:
    """Banana.dev / Potassium stable-diffusion endpoint."""
    import banana_dev as banana
    out = banana.run(
        cfg.banana_api_key,
        cfg.banana_model_key,
        {"prompt": prompt, "num_inference_steps": 30},
    )
    image_b64 = out["modelOutputs"][0]["image_base64"]
    image_bytes = base64.b64decode(image_b64)
    path = os.path.join(cfg.output_dir, "design", filename)
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "wb") as f:
        f.write(image_bytes)
    return path


# ═══════════════════════════════════════════════════════════════════════════
#  Text-to-Speech  (ElevenLabs)
# ═══════════════════════════════════════════════════════════════════════════

def tts_elevenlabs(text: str, filename: str = "speech.mp3", voice_id: str = "Rachel") -> str:
    """
    Convert text to speech via ElevenLabs API.
    Returns local file path.
    """
    url = f"https://api.elevenlabs.io/v1/text-to-speech/{voice_id}"
    headers = {
        "Accept": "audio/mpeg",
        "Content-Type": "application/json",
        "xi-api-key": cfg.elevenlabs_api_key,
    }
    body = {
        "text": text,
        "model_id": "eleven_multilingual_v2",
        "voice_settings": {"stability": 0.5, "similarity_boost": 0.75},
    }
    resp = requests.post(url, json=body, headers=headers, timeout=60)
    if resp.status_code != 200:
        raise RuntimeError(f"ElevenLabs error {resp.status_code}: {resp.text[:200]}")
    path = os.path.join(cfg.output_dir, "audio", filename)
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "wb") as f:
        f.write(resp.content)
    return path


# ═══════════════════════════════════════════════════════════════════════════
#  Music generation  (Suno AI — unofficial API wrapper)
# ═══════════════════════════════════════════════════════════════════════════

def generate_music_suno(prompt: str, filename: str = "music.mp3") -> str:
    """
    Generate music via Suno's unofficial API.
    Returns local file path after polling for completion.

    NOTE: Suno does not have an official public API.
    This uses the community-maintained endpoint (suno-api).
    You need to run the suno-api service locally or use a hosted instance
    and set SUNO_API_KEY + SUNO_API_BASE_URL in your environment.
    """
    base_url = os.getenv("SUNO_API_BASE_URL", "http://localhost:3000")
    headers = {"Authorization": f"Bearer {cfg.suno_api_key}"}

    # Submit generation job
    resp = requests.post(
        f"{base_url}/api/generate",
        json={"prompt": prompt, "make_instrumental": False, "wait_audio": False},
        headers=headers,
        timeout=30,
    )
    if resp.status_code != 200:
        raise RuntimeError(f"Suno submit error {resp.status_code}: {resp.text[:200]}")
    job_id = resp.json()["id"]

    # Poll until done (max 5 min)
    for _ in range(60):
        time.sleep(5)
        poll = requests.get(
            f"{base_url}/api/get?ids={job_id}",
            headers=headers,
            timeout=10,
        )
        data = poll.json()[0]
        if data.get("status") == "complete":
            audio_url = data["audio_url"]
            audio_bytes = requests.get(audio_url, timeout=60).content
            path = os.path.join(cfg.output_dir, "audio", filename)
            os.makedirs(os.path.dirname(path), exist_ok=True)
            with open(path, "wb") as f:
                f.write(audio_bytes)
            return path
    raise RuntimeError("Suno generation timed out after 5 minutes.")


# ═══════════════════════════════════════════════════════════════════════════
#  Offline TTS fallback (gTTS — no API key required)
# ═══════════════════════════════════════════════════════════════════════════

def tts_gtts(text: str, filename: str = "speech.mp3", lang: str = "ru") -> str:
    """Google Text-to-Speech (offline-ish, no key). Returns local file path."""
    from gtts import gTTS
    tts = gTTS(text=text, lang=lang, slow=False)
    path = os.path.join(cfg.output_dir, "audio", filename)
    os.makedirs(os.path.dirname(path), exist_ok=True)
    tts.save(path)
    return path
