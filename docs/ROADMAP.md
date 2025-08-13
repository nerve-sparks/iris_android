# IRIS Star Roadmap

This roadmap is the single source of truth for current and upcoming work. It replaces fragmented plans elsewhere.

Legend: [ ] Pending · [x] Completed

## Goals
- [ ] Preserve all current features and UX capabilities
- [ ] Reduce redundancy and centralize shared logic
- [ ] Improve security, privacy, stability, and performance
- [ ] Modernize build/tooling for maintainability

## Foundations: Build & Toolchain
- [ ] Move to Java 17 toolchain for AGP 8.5
- [ ] Bump Kotlin, Compose BOM, Material3, Hilt, Room, Retrofit, Moshi to latest stable compatible matrix
- [ ] Align app and NDK toolchains for reproducible builds (CMake/NDK versions)
- [ ] Expand R8/proguard rules for Hilt, Room, Moshi, ML Kit, JNI reflection

## Architecture & Code Organization
- [ ] Split MainViewModel into feature-scoped ViewModels (Chat/Search/Voice/Model/Metrics) with Hilt scopes
- [ ] Introduce PromptComposer service: centralize prompt assembly, token-reserve enforcement, context trimming
- [ ] Centralize default model catalog in ModelRepository; remove duplicates in MainViewModel and MainActivity
- [ ] Keep dev-only screens (ModernTestScreen) behind debug build type

## Networking
- [ ] Use DI-provided OkHttpClient everywhere (WebSearchService, ModelUpdateWorker)
- [ ] Add SSL pinning for HuggingFace; toggle by build type
- [ ] Respect caching headers (ETag, Last-Modified) via OkHttp cache; leverage dedupe interceptor already present
- [ ] Remove hardcoded Google API key/CSE ID; store via encrypted preferences and Settings

## Data & Storage
- [ ] Enable Room schema export (exportSchema=true) and add schema/ directory
- [ ] Replace fallbackToDestructiveMigration with real migrations; add migration tests
- [ ] Review data_extraction_rules.xml to exclude sensitive data by default (tokens, secrets), add explicit user opt-in controls
- [ ] Settings export/import: redact secrets by default; require explicit include for tokens

## Native Layer (llama.cpp)
- [x] Persist user-chosen GPU layers; reload model on change; reflect offload N/N in UI consistently
- [x] Harden plugin load logic order and limit search to app-controlled paths post-registry discovery
- [x] Expose additional perf stats (KV size) to UI metrics and diagnostics
- [x] Safe backend switching (defer free when contexts exist)
- [x] Correct batch allocation/free via upstream helpers
- [x] GPU benchmark skip when offload is zero/CPU session forced; clear JSON error

## Downloads & Model Management
- [ ] Unify downloads flow: share integrity verification and model registration across DownloadManager and WorkManager codepaths
- [ ] Add SHA-256 verification and optional signature checks; persist model metadata in DB
- [ ] Curate default/suggested models with labels (Fast/Quality/Reasoning) and known sizes

## UI/UX
- [ ] Clean Manifest splash/theme; remove hardcoded colors and use theme-based splash
- [ ] Ensure thinking tokens toggle honored consistently between TemplateRegistry and JNI strip logic
- [ ] Improve long-operation UX: progress, cancel, resume for quantize/download/verify
- [ ] Accessibility: content descriptions, min touch targets, dynamic type paths

## Performance
- [ ] Memoize token counts for static segments and repeated prompts
- [ ] Add stable keys to chat lists; reduce recompositions via scoped state reads
- [ ] Offload heavy summarization/trimming to WorkManager when exceeding UI budgets
- [x] Keep UI thread free of any blocking network/disk work (reviewed runLoop usage for JNI)

## Security & Privacy
- [ ] Remove hardcoded secrets from source; add CI secrets scanning
- [ ] SSL pinning for Retrofit; consider CT enforcement if feasible
- [ ] Disable Timber DebugTree in release; gate verbose logs by build type
- [ ] Gate sensitive actions (export/import, token view) behind biometrics (optional)
- [ ] Whitelist approved model download hosts; reject unknown domains

## Testing & QA
- [ ] Unit tests: repositories, PromptComposer, TemplateRegistry, ReasoningParser, settings import/export
- [ ] Instrumentation: chat flow (load, send, stream), downloads, navigation/back
- [ ] Integration: model registry + UI flows; WorkManager periodic updates (TestDriver)
- [ ] Room migration tests and data retention checks
- [ ] JNI smoke tests (manual/device): load/unload cycles and short generation

## Build & Release
- [ ] Product flavors: dev (looser security/logging) vs prod (pinning, no debug logging)
- [ ] CI: assemble, tests, lint/ktlint, dependency updates, signing
- [x] Consistent branding: unify rootProject.name to IRISStar

## Conflicts & Duplicates To Resolve
- [ ] Default model lists consolidated into repository (remove in MainViewModel/MainActivity)
- [ ] Use AppNavigation.kt as source of truth; remove leftover Navigation helpers if not used
- [ ] WebSearchService uses DI OkHttpClient; remove private client
- [ ] Consolidate prompt/context trimming to PromptComposer
- [ ] Reuse constants in tests via shared fixture or public constants

## Milestones
1. Consolidation & Security Start (Architecture, Networking, Data, Duplicates)
2. Downloads + Model Mgmt Unification
3. Performance Pass
4. UI/UX Polish + Accessibility
5. Native Layer Enhancements
6. Testing & CI Maturity
7. Feature Backlog Items

Refer to CHANGELOG for completed work and detailed history.


