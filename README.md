# Aesthetic Eye – Android App

> AI-powered interior design assistant that visualizes product placement in real rooms and delivers expert design critiques — all from your phone.

Upload a room photo, add products (from your camera, gallery, or **any product URL**), and Aesthetic Eye generates a photorealistic composite scene scored by a simulated Design Director. Keep adding products one by one until your room is complete.

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
- [Upcoming Features](#upcoming-features)
- [Authors](#authors)

---

## Features

### Core Workflow
| Step | Action |
|------|--------|
| 1. Upload a room photo | Take or select a photo of the space you want to design |
| 2. Pick a product | Upload from camera/gallery **or** paste any product URL (IKEA, Amazon, Wayfair, Target, etc.) |
| 3. AI generates the scene | Gemini creates a photorealistic composite of the product placed naturally in the room |
| 4. Get a Design Director critique | AI scores the placement 0–100, detects room style, lists flaws, and suggests improvements |
| 5. Add more products | Tap "Add Another Product" to keep building your scene iteratively |
| 6. Start over anytime | Reset to a fresh canvas with one tap |

### Highlights

- **URL Product Import** — Paste any furniture product URL and the AI extracts the name, description, price, dimensions, material, color, and image automatically.
- **Iterative Product Placement** — Add products one at a time. Each generation preserves the previous scene so you can layer multiple products naturally.
- **Smart Placement Understanding** — Casual instructions like *"lamp next to the couch"* or *"texture on the wall"* are interpreted intelligently using interior design expertise.
- **Holistic Design Critique** — The Design Director evaluates how each product fits within the overall room composition, not just in isolation.
- **Camera + Gallery + URL** — Every image slot supports uploading from camera/gallery OR pasting a URL.

---

## How It Works

### First Product
```
User selects room + product (camera/gallery/URL)
        ↓
GeminiService pipeline:
  1. describeImage(room)         → Room description
  2. describeImage(product)      → Product description
  3. generateCompositeImage()    → Generated scene
  4. getDirectorCritique()       → Score + feedback
        ↓
Result: generated scene + Design Director report
        ↓
User taps "Add Another Product" or "Done"
```

### Adding More Products (Iterative)
```
Generated scene becomes the new "room" base
Product slot clears, prompt resets
        ↓
User picks next product (camera/gallery/URL)
        ↓
Same 4-stage pipeline runs
        ↓
New scene with all products visible
        ↓
Repeat as many times as desired
```

### URL Import
```
User taps "Paste URL" → enters a product URL
        ↓
For product URLs:
  Fetch HTML → Gemini extracts structured data → Download image → Preview card → Confirm
        ↓
For room/detail URLs:
  Detect direct image → download, OR fetch HTML → find main image → download → Confirm
```

---

## Tech Stack

| Layer | Technology | Purpose |
|-------|-----------|---------|
| UI | Jetpack Compose + Material 3 | Declarative UI, no XML layouts |
| State | ViewModel + StateFlow | Reactive state management |
| Networking | OkHttp | REST calls to Gemini APIs and web pages |
| JSON | kotlinx.serialization | Encode/decode API payloads |
| Images | Coil (display) + Bitmap (decode) | Async loading from URLs and camera |
| AI | Gemini 2.5 Flash (REST API) | Text, vision, and image generation |
| URL Extraction | Gemini 2.5 Flash + OkHttp | Fetch product pages → AI extracts structured data |

---

## AI Pipeline

### 4-Stage Gemini Pipeline (per product)

| Stage | Name | Model | Input | Output |
|-------|------|-------|-------|--------|
| 1 | Room Analysis | `gemini-2.5-flash` | Room photo | Style, lighting, colors, furniture description |
| 2 | Product Analysis | `gemini-2.5-flash` | Product photo | Shape, color, material, finish description |
| 3 | Scene Generation | `gemini-2.5-flash-image` | Room + product images + descriptions + prompt | Photorealistic composite scene |
| 4 | Design Critique | `gemini-2.5-flash` | Generated scene | JSON: score 0–100, status, style, flaws, suggestion |

For iterative placement, Stages 1–4 run again using the **previous generated scene** as the room input.

### URL Extraction Pipeline

1. OkHttp fetches the page HTML with browser-like headers
2. HTML trimmed to 15k characters → sent to Gemini 2.5 Flash
3. Gemini returns structured JSON: name, description, price, dimensions, material, color, image URL, brand
4. Product image downloaded → displayed in preview card
5. User confirms → image feeds into the main pipeline

---

## Project Structure

```
app/src/main/java/com/aestheticeye/
├── MainActivity.kt                      ← App entry point
├── data/
│   └── Models.kt                        ← Gemini request/response data classes
├── network/
│   ├── GeminiService.kt                 ← 4-stage Gemini pipeline + smart placement
│   └── ProductUrlExtractor.kt           ← URL fetching + AI product extraction
├── viewmodel/
│   └── MainViewModel.kt                 ← State, generation, URL import, iteration
└── ui/
    ├── theme/
    │   └── Theme.kt                     ← Colors + Material 3 theme
    ├── screens/
    │   └── MainScreen.kt                ← Main UI: image cards, prompt, results, iteration
    └── components/
        ├── ImageUploadCard.kt           ← Reusable image picker with "Paste URL" option
        ├── UrlImportSheet.kt            ← Bottom sheet: URL input → preview → confirm
        ├── LoadingScreen.kt             ← Spinner + step text during generation
        └── ResultDisplay.kt             ← Generated scene + Design Director report
```

---

## Setup Guide

### 1. Install Android Studio
Download from https://developer.android.com/studio. Install with default settings.

### 2. Create the Project
- Open Android Studio → **New Project → Empty Activity**
- Name: `AestheticEye`
- Package: `com.aestheticeye`
- Language: **Kotlin**
- Minimum SDK: **API 26** (Android 8.0)

### 3. Replace Gradle Files
| File | Action |
|------|--------|
| `gradle/libs.versions.toml` | Replace with provided `libs.versions.toml` |
| `build.gradle.kts` (root) | Replace with `project_build.gradle.kts` |
| `app/build.gradle.kts` | Replace with `app_build.gradle.kts` |

Click **Sync Now** when prompted.

### 4. Add Dependencies
Ensure `app/build.gradle.kts` includes:
```kotlin
implementation("io.coil-kt:coil-compose:2.6.0")
```

### 5. Add Your Gemini API Key
In `local.properties` (project root):
```
sdk.dir=/Users/YourName/Library/Android/sdk
GEMINI_API_KEY=YOUR_ACTUAL_KEY_HERE
```
Get a free key at https://aistudio.google.com/apikey. Never commit this file to Git.

### 6. Copy Source Files

| File | Location |
|------|----------|
| `MainActivity.kt` | `app/src/main/java/com/aestheticeye/` |
| `Models.kt` | `app/src/main/java/com/aestheticeye/data/` |
| `GeminiService.kt` | `app/src/main/java/com/aestheticeye/network/` |
| `ProductUrlExtractor.kt` | `app/src/main/java/com/aestheticeye/network/` |
| `MainViewModel.kt` | `app/src/main/java/com/aestheticeye/viewmodel/` |
| `Theme.kt` | `app/src/main/java/com/aestheticeye/ui/theme/` |
| `MainScreen.kt` | `app/src/main/java/com/aestheticeye/ui/screens/` |
| `ImageUploadCard.kt` | `app/src/main/java/com/aestheticeye/ui/components/` |
| `UrlImportSheet.kt` | `app/src/main/java/com/aestheticeye/ui/components/` |
| `LoadingScreen.kt` | `app/src/main/java/com/aestheticeye/ui/components/` |
| `ResultDisplay.kt` | `app/src/main/java/com/aestheticeye/ui/components/` |
| `AndroidManifest.xml` | `app/src/main/` |

### 7. Update `res/values/themes.xml`
```xml
<resources>
    <style name="Theme.AestheticEye" parent="android:Theme.Material.Light.NoActionBar" />
</resources>
```

### 8. Run the App
Connect an Android phone (USB Debugging enabled) or use the built-in emulator. Click the green **Run** button.

---

## File Reference

| File | Purpose |
|------|---------|
| `MainActivity.kt` | App entry point. Sets up the Compose screen. |
| `Models.kt` | Data classes for Gemini API requests/responses and Design Director feedback. |
| `GeminiService.kt` | 4-stage Gemini pipeline. Sends images as base64, returns text and generated images. Includes smart placement interpretation for casual user instructions. |
| `ProductUrlExtractor.kt` | Fetches product page HTML with browser headers, sends to Gemini for structured extraction (name, price, dimensions, image URL, etc.). Also handles simple image extraction for room/detail URLs. |
| `MainViewModel.kt` | Central state holder: image slots, prompt, UI state, URL import flow, iterative product placement loop, start over / reset logic. |
| `Theme.kt` | Material 3 theming: colors and typography. |
| `MainScreen.kt` | Main UI: image upload cards with URL option, placement prompt, generate button, result display with "Add Another Product" and "Done" actions. Adapts UI for iterative mode. |
| `ImageUploadCard.kt` | Reusable tappable card that opens the system photo picker and shows a live preview. Includes a "Paste URL" link below each card. |
| `UrlImportSheet.kt` | Bottom sheet with URL input field, paste button, loading states. Shows full product detail preview (for product URLs) or image preview (for room/detail URLs). |
| `LoadingScreen.kt` | Spinning animation with real-time step text during pipeline execution. |
| `ResultDisplay.kt` | Generated scene image, save-to-gallery button, and full Design Director report (score, status, style, flaws, suggestions). |

---

## Troubleshooting

| Problem | Solution |
|---------|----------|
| `GEMINI_API_KEY not found` | Ensure `local.properties` has the key, then **Sync Project** |
| API error 400 | Image may be too large — the service auto-scales to 1024px max |
| API error 429 | Rate limit hit — wait a moment and retry |
| Image generation returns no image | The image model is in preview — check AI Studio for availability |
| URL extraction fails | Some sites block scraping. Try a different URL or use camera/gallery instead |
| App crashes on launch | Check **Logcat** in Android Studio for the stack trace |

---

## Upcoming Features

- **AI vs. User Comparison** — After generating the user's placement, the AI suggests its own optimized placement. Both scenes display side-by-side with scores so users can compare and learn.
- **Room Style Auto-Detect** — Surfaces the room analysis as a visible card showing detected style name, dominant color palette, and key design traits.
- **AI Size Reality Check** — Uses visible furniture as reference points to estimate relative scale. Flags proportional mismatches before generating the scene.
- **Mood Board Generator** — After analyzing the room, the AI suggests 3–4 complementary products that match the detected style.
- **Favorites and Collections** — Local storage using Room database. Users save past generations into named project folders and revisit results across sessions.

---

## Authors

**Bibhusa Rawal** and **Neha Adhikari**
Stony Brook University — Application Development — Spring 2026
