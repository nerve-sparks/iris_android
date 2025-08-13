# IRIS Star Changelog

This file tracks notable changes and completed work. For active items, see ROADMAP.

## Recent Achievements
- 14 files changed, 769 insertions(+), 345 deletions(-)
- New components: MarkdownText.kt, MemoryManager.kt
- UI/UX improvements: cleaner interface, consistent theming, statusBarsPadding()
- Performance fixes: TPS, TTFT, latency, memory usage metrics restored
- Features: local model import, default models, markdown rendering
- Async operations: improved error handling and build optimization
- Fixed: SettingsRepositoryImplTest
- Added: exportConfiguration()/importConfiguration() to UserPreferencesRepository
- Implemented: async search with HuggingFace API; default models in UI
- Enhanced: real-time memory usage monitoring; model management; error feedback

## Native Runtime & Stability
- Backend switching safety; plugin load hardening; batch allocation/free via upstream helpers
- GPU benchmark skip when offload zero; diagnostics exporter
- Token log gating; perf stats exposed (KV size)

## Network & Manifest
- OkHttp timeouts + UA + debug-logging (debug only)
- Request de-duplication interceptor
- Manifest cleanup; removed hardcoded splash colors

## UI Overhaul
- Material 3 migration, dark theme consistency, dynamic color support
- Modern chat input, message bubbles, quick actions, top app bar
- Thinking UI with collapsible reasoning and robust parsing

Older entries moved during consolidation. See repository history for further details.

