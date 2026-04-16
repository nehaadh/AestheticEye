# Aesthetic Eye – Android App

> AI-powered interior design assistant that visualizes product placement in real rooms, delivers expert design critiques, and learns how to improve your space: all from your phone.

Upload a room photo, add products (from your camera, gallery, or **any product URL**), and Aesthetic Eye generates a photorealistic composite scene scored by a simulated Design Director. Keep adding products one by one until your room is complete. Then tap "See AI's Improvement" and watch the AI redesign your scene with a full list of changes.

---

## Table of Contents

- [Features](#features)
- [How It Works](#how-it-works)
- [Tech Stack](#tech-stack)
- [AI Pipeline](#ai-pipeline)
- [Project Structure](#project-structure)
- [Setup Guide](#setup-guide)
- [File Reference](#file-reference)
- [Troubleshooting](#troubleshooting)
- [Authors](#authors)

---

## Features

### Core Workflow
| Step | Action |
|------|--------|
| 1 | Upload a room photo |
| 2 | Pick a product — camera, gallery, or paste any product URL |
| 3 | Optionally add a texture or detail image |
| 4 | Write a placement instruction (or leave it for AI to decide) |
| 5 | AI generates a photorealistic composite scene |
| 6 | Room style card, size fit check, and Design Director critique appear progressively |
| 7 | Tap "See AI's Improvement" for an AI redesign with a change log |
| 8 | Add more products iteratively, or save to your collection |

### Highlights

- **URL Product Import** — Paste any furniture or decor URL. Gemini extracts the name, description, price, dimensions, material, color, and image automatically. Supported sites include IKEA, Amazon, Wayfair, Target, and most retailer pages.
- **Smart Placement Understanding** — Casual instructions like *"lamp next to the couch"* or *"add texture to the wall"* are interpreted intelligently. Textures apply to the full wall surface; placement hints resolve to the most logical spatial position.
- **Progressive Reveal** — The generated scene appears immediately after image generation completes. Room style, size check, design critique, and mood board load in progressively while you view the scene — no waiting at a spinner.
- **AI Design Improvement** — After the design critique loads, tap "See AI's Improvement". Gemini analyses the full scene, identifies specific improvements to placement, composition, and balance, generates an improved version, and lists every change made (additions, replacements, removals) with Google Shopping links.
- **Iterative Placement** — Add products one at a time. Each generation uses the previous scene as the room base, preserving all previously placed items.
- **Design Director Critique** — Holistic 0–100 score covering placement, scale, lighting coherence, colour harmony, and style cohesion. Judges both the product and any applied textures as equal design decisions.
- **Room Style Card** — Detected interior design style, dominant colour palette (with hex codes), and key design traits.
- **Size Fit Check** — Uses visible furniture as reference points to flag if a product is too large or too small before you commit to the placement.
- **Mood Board** — 4 curated complementary product suggestions with Google Shopping links, tailored to the detected room style.
- **Saved Collections** — Save scenes to a local Room database. The detail view shows the full design story: detected style, products used, critique, and — for multi-stage projects — every intermediate scene with its AI comparison if one was run.

---

## How It Works

### Single Product Flow
```
Select room + product (+ optional detail/texture)
        ↓
GeminiService pipeline:
  1. analyseProduct()          → Product name + description
  2. describeImage(room)       → Room description
  3. getRoomStyleCard()        → Style name, colours, traits
  4. getSizeWarning()          → PERFECT FIT / TOO LARGE / TOO SMALL
  5. generateCompositeImage()  → Photorealistic scene
        ↓ (scene shown immediately)
  6. getDirectorCritique()     → Score + feedback (shown while user views scene)
  7. getMoodBoard()            → 4 complementary product suggestions
        ↓
User optionally taps "See AI's Improvement"
  → analyseAndPlanImprovements() → What to change + generation instruction
  → generateImprovedScene()      → AI redesigned scene
  → changedProducts tracked      → ADDED / REPLACED / REMOVED with shop links
```

### Iterative Product Flow
```
User taps "Add Another Product" (or "Use AI's Version")
        ↓
Chosen scene becomes the new room base
Product slot clears, stage history recorded
        ↓
Same pipeline runs with new product
        ↓
New scene has all products from all stages
        ↓
Repeat as many times as needed
```

### URL Import
```
User taps "Paste URL" → enters a product URL
        ↓
ProductUrlExtractor:
  Fetch HTML → trim to 15k chars → Gemini extracts structured JSON
  (name, description, price, dimensions, material, colour, image URL, brand)
        ↓
Product image downloaded → preview card shown → user confirms
        ↓
Image + name feed into the main pipeline
```

---

## Tech Stack

| Layer | Technology | Purpose |
|-------|-----------|---------|
| UI | Jetpack Compose + Material 3 | Declarative UI |
| State | ViewModel + StateFlow | Reactive state, progressive reveal |
| Networking | OkHttp | Gemini REST API + HTML fetching |
| JSON | kotlinx.serialization | API payloads + stage history serialisation |
| Database | Room (v8) + KSP | Favourites persistence |
| Animation | Compose Infinite Transition | Grid tile loading animation, AeDots |
| AI | Gemini 2.5 Flash (text) | Analysis, critique, improvement planning, URL extraction |
| AI | Gemini 2.5 Flash Image | Scene generation, AI improvement generation |

---

## AI Pipeline

### Per-Product Pipeline

| Stage | Function | Model | Input | Output |
|-------|----------|-------|-------|--------|
| 1 | `analyseProduct()` | gemini-2.5-flash | Product image | Short name + description |
| 2 | `describeImage()` | gemini-2.5-flash | Room image | Style, lighting, furniture |
| 3 | `getRoomStyleCard()` | gemini-2.5-flash | Room image | Style name, hex colours, traits |
| 4 | `getSizeWarning()` | gemini-2.5-flash | Room + product | PERFECT FIT / TOO LARGE / TOO SMALL |
| 5 | `generateCompositeImage()` | gemini-2.5-flash-image | Room + product (+ detail) | Photorealistic scene |
| 6 | `getDirectorCritique()` | gemini-2.5-flash | Generated scene | JSON score, status, flaws, suggestion |
| 7 | `getMoodBoard()` | gemini-2.5-flash | Generated scene | 4 product suggestions |

### AI Improvement Pipeline

| Stage | Function | Model | Output |
|-------|----------|-------|--------|
| 1 | `analyseAndPlanImprovements()` | gemini-2.5-flash | Bullet improvements, generation instruction, changed products JSON |
| 2 | `generateImprovedScene()` | gemini-2.5-flash-image | Redesigned scene image |

### URL Extraction Pipeline

| Stage | Tool | Action |
|-------|------|--------|
| 1 | OkHttp | Fetch page HTML with browser-like headers |
| 2 | Gemini 2.5 Flash | Extract structured JSON from trimmed HTML |
| 3 | OkHttp | Download product image from extracted URL |
| 4 | UI | Show preview card → user confirms |

---

## Project Structure

```
app/src/main/java/com/example/aestheticeye/
├── MainActivity.kt
├── data/
│   ├── Models.kt                  ← All data classes: UiState, AestheticResult,
│   │                                 ImprovementState, SceneStage, FavoriteScene,
│   │                                 Gemini wire types
│   └── FavoritesDatabase.kt       ← Room database v8 + DAO
├── network/
│   ├── GeminiService.kt           ← Full AI pipeline
│   └── ProductUrlExtractor.kt     ← URL → product data extraction
├── viewmodel/
│   └── MainViewModel.kt           ← State, generation, stages, URL import
└── ui/
    ├── theme/
    │   ├── Theme.kt               ← Material 3 colours + dark/light schemes
    │   └── Type.kt                ← Typography
    ├── screens/
    │   ├── MainScreen.kt          ← Main UI + iterative mode
    │   └── FavoritesScreen.kt     ← Collections list + detail with stage history
    └── components/
        ├── ImageUploadCard.kt     ← Image picker card with "Paste URL"
        ├── LoadingScreen.kt       ← Grid tile animation + AeDots
        └── ResultDisplay.kt       ← Scene + critique + AI improvement + mood board
```

---

## Setup Guide

### 1. Install Android Studio
Download from https://developer.android.com/studio. Install with default settings.

### 2. Create the Project
- Open Android Studio → **New Project → Empty Activity**
- Name: `AestheticEye`
- Package: `com.example.aestheticeye`
- Language: **Kotlin**
- Minimum SDK: **API 26** (Android 8.0)

### 3. Replace Gradle Files
| File | Action |
|------|--------|
| `gradle/libs.versions.toml` | Replace with provided file |
| `build.gradle.kts` (root) | Replace with provided file |
| `app/build.gradle.kts` | Replace with provided file |

Click **Sync Now** when prompted.

### 4. Add Your Gemini API Key
In `local.properties` (project root — never commit this file):
```
sdk.dir=/Users/YourName/Library/Android/sdk
GEMINI_API_KEY=YOUR_ACTUAL_KEY_HERE
```
Get a free key at https://aistudio.google.com/apikey.

### 5. Copy Source Files

| File | Destination |
|------|-------------|
| `MainActivity.kt` | `…/com/example/aestheticeye/` |
| `Models.kt` | `…/data/` |
| `FavoritesDatabase.kt` | `…/data/` |
| `GeminiService.kt` | `…/network/` |
| `ProductUrlExtractor.kt` | `…/network/` |
| `MainViewModel.kt` | `…/viewmodel/` |
| `Theme.kt` + `Type.kt` | `…/ui/theme/` |
| `MainScreen.kt` | `…/ui/screens/` |
| `FavoritesScreen.kt` | `…/ui/screens/` |
| `ImageUploadCard.kt` | `…/ui/components/` |
| `LoadingScreen.kt` | `…/ui/components/` |
| `ResultDisplay.kt` | `…/ui/components/` |

### 6. Update `res/values/themes.xml`
```xml
<resources>
    <style name="Theme.AestheticEye" parent="android:Theme.Material.Light.NoActionBar" />
</resources>
```

### 7. Run the App
Connect an Android device (USB Debugging on) or launch the built-in emulator. Click the green **Run** button.

---

## File Reference

| File | Purpose |
|------|---------|
| `MainActivity.kt` | Entry point. Initialises the Compose host and ViewModel. |
| `Models.kt` | All data classes: `UiState`, `ImprovementState`, `AestheticResult`, `AiImprovementResult`, `ChangedProduct`, `SceneStage`, `FavoriteScene`, `DesignDirectorFeedback`, Gemini wire types. |
| `FavoritesDatabase.kt` | Room database v8, `FavoritesDao`, `fallbackToDestructiveMigration`. |
| `GeminiService.kt` | Full 7-stage per-product pipeline, 2-stage AI improvement pipeline, URL-aware composite image generation, director critique (product + detail, holistic), mood board, product URL extraction helper. |
| `ProductUrlExtractor.kt` | OkHttp + Gemini HTML → structured product JSON extraction. Handles product pages and direct image URLs. |
| `MainViewModel.kt` | Image slots, URL import flow, stage tracking, bitmap stash, progressive `onSceneReady` callbacks, `addAnotherProduct`, `buildFinalStages`, favourites CRUD. |
| `Theme.kt` / `Type.kt` | Warm editorial palette (AeGold, AeGreen, AeAmber, AeRed, AeCream), dark + light Material 3 schemes, typography with valid letter spacing. |
| `MainScreen.kt` | Three equal image upload cards, placement instruction field, generate button with `AeDots`, `UiState.SceneReady` progressive banner, save dialog, start-over button. |
| `FavoritesScreen.kt` | Collection card grid (photo-first with score/style overlay), detail screen showing style card, products added, director critique, and full stage history (user scene + AI redesign per stage). |
| `ImageUploadCard.kt` | Tappable card (`aspectRatio(0.82f)`) with gradient scrim label, clear button, and "Paste URL" link. |
| `LoadingScreen.kt` | 3×3 animated tile grid with staggered pulse. Public `AeDots` composable (3 gold dots) used throughout the app instead of spinners. |
| `ResultDisplay.kt` | Progressive scene reveal: style card → size badge → generated scene → design critique → AI improvement button (shown only after critique loads) → mood board. AI improvement section shows both scenes with chosen indicator, changes list, and product change cards (ADDED/REPLACED/REMOVED). |

---

## Troubleshooting

| Problem | Solution |
|---------|----------|
| `GEMINI_API_KEY not found` | Check `local.properties`, then **File → Sync Project with Gradle Files** |
| API error 400 | Image may exceed limits — service auto-scales to 1024px max |
| API error 429 | Rate limit — wait a moment and retry |
| Image generation returns no image | The image model (`gemini-2.5-flash-image`) is in preview — check AI Studio for availability |
| URL extraction fails | Some sites block scraping. Try a different URL or use camera/gallery |
| Scene missing product | Ensure the product image is clear and well-lit against a neutral background |
| App crashes on launch | Check **Logcat** in Android Studio for the stack trace |
| Database crash after update | Room version bumped to 8 with `fallbackToDestructiveMigration` — uninstall and reinstall the app |

---

## Authors

**Bibhusa Rawal** and **Neha Adhikari**
Stony Brook University — Application Development — Spring 2026
