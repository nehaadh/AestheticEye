# Aesthetic Eye

Android app for AI-powered interior design visualization. Upload a room photo, add products one at a time, and get a photorealistic composite scene with a design critique for each addition.

---

## Table of Contents

- [Features](#features)
- [How to Use](#how-to-use)
- [Tech Stack](#tech-stack)
- [AI Pipeline](#ai-pipeline)
- [Project Structure](#project-structure)
- [Setup](#setup)
- [File Reference](#file-reference)
- [Troubleshooting](#troubleshooting)
- [Authors](#authors)

---

## Features

- Upload room photos and product images from camera, gallery, or any product URL
- Optional detail/texture image for wall or surface treatments
- Photorealistic scene generation using Gemini image model
- Room style detection with colour palette and design traits
- Product size fit check before generation
- Design Director critique: 0-100 score, detected style, issues, and suggestions
- AI design improvement: analyses your scene and generates an improved version with a list of changes
- Iterative placement: each generated scene becomes the base for the next product
- Mood board with 4 complementary product suggestions and Google Shopping links
- Save scenes to a local collection with full stage history

---

## How to Use

### Basic flow

1. Open the app. You will see three image slots: **Room Photo**, **Product**, and **Detail (Optional)**.
2. Tap a slot to pick an image from your camera or gallery, or tap **Paste URL** below any slot to import from a web link.
3. Write a placement instruction in the text field, for example: `place lamp next to the sofa` or `add texture to the wall`. You can also leave it blank and the app will choose a placement.
4. Tap **Generate & Review**.

The app runs through several steps and shows them as they complete:
- Room style card (detected style, colour palette, traits)
- Size fit check (whether the product is proportionally appropriate)
- Generated scene image
- Design Director report (score, critique, issues, suggestion)
- Mood board (4 complementary product picks)

### Placement instructions

Write naturally. The app interprets short or vague instructions using interior design context:

| Instruction | Interpretation |
|-------------|---------------|
| `by the window` | Placed directly beside or flanking the window frame |
| `next to the sofa` | On the floor beside the sofa |
| `in the corner` | Most natural visible corner of the room |
| `add texture to the wall` | Material applied across the entire main wall, floor to ceiling |
| `against the back wall` | Pushed up against the back wall |

### Importing from a URL

Tap **Paste URL** under any image slot. Enter a product page URL from any retailer (IKEA, Amazon, Wayfair, Target, etc.). The app will:

1. Fetch the page and extract the product name, description, price, dimensions, material, colour, and image
2. Show a preview card with the extracted details
3. Let you confirm before using it in the scene

For room or texture slots, paste a direct image URL or a page that contains the image you want.

### AI design improvement

After the Design Director report loads, a **See AI's Improvement** button appears. Tap it to:

1. Have the AI analyse your scene and identify what to change
2. Generate an improved version of the scene
3. See both versions side by side with a list of changes

Each change is labelled as **ADDED**, **REPLACED**, or **REMOVED**. Added and replaced items include a Google Shopping link.

You can choose to continue with your version or the AI's version for the next product stage.

### Adding more products

Tap **Add Another Product** at the bottom of the results screen. The current scene becomes the new room base. The product and detail slots clear. Add your next product and repeat the same flow.

Each stage is recorded independently. When you save the scene, the full history is stored, including which scene you chose at each stage and any AI comparison that was run.

### Saving to your collection

Tap the heart icon in the top bar at any point after a scene is generated. Give the scene a name and tap Save. You can view saved scenes from the heart icon on the main screen.

The detail view for each saved scene shows:
- Final scene image with score overlay
- Detected style, colour palette, traits
- Products added (numbered list)
- Design Director score, critique, and suggestions
- Per-stage history: your scene and AI redesign side by side for each product addition, with a marker showing which one you continued with

### Starting over

Tap **Start New Scene** at the bottom of the results to reset everything and start from scratch.

---

## Tech Stack

| | |
|---|---|
| UI | Jetpack Compose, Material 3 |
| State | ViewModel, StateFlow |
| Networking | OkHttp |
| JSON | kotlinx.serialization |
| Database | Room v8, KSP |
| AI (text) | Gemini 2.5 Flash |
| AI (image gen) | Gemini 2.5 Flash Image |

---

## AI Pipeline

### Per product (7 stages)

| Stage | Function | Input | Output |
|-------|----------|-------|--------|
| 1 | `analyseProduct()` | Product image | Short name + description |
| 2 | `describeImage()` | Room image | Room description |
| 3 | `getRoomStyleCard()` | Room image | Style, hex colours, traits |
| 4 | `getSizeWarning()` | Room + product | PERFECT FIT / TOO LARGE / TOO SMALL |
| 5 | `generateCompositeImage()` | Room + product + optional detail | Generated scene |
| 6 | `getDirectorCritique()` | Generated scene | Score, status, flaws, suggestion |
| 7 | `getMoodBoard()` | Generated scene | 4 product suggestions |

Stages 1-4 run before generation. Stage 5 runs and the scene is shown immediately. Stages 6-7 run in the background while the user views the scene.

### AI improvement (2 stages)

| Stage | Function | Output |
|-------|----------|--------|
| 1 | `analyseAndPlanImprovements()` | Improvement list, generation instruction, changed products |
| 2 | `generateImprovedScene()` | Redesigned scene image |

### URL import

1. OkHttp fetches page HTML
2. HTML trimmed to 15k chars, sent to Gemini 2.5 Flash
3. Gemini returns: name, description, price, dimensions, material, colour, image URL
4. Product image downloaded, preview shown, user confirms

---

## Project Structure

```
app/src/main/java/com/example/aestheticeye/
├── MainActivity.kt
├── data/
│   ├── Models.kt
│   └── FavoritesDatabase.kt
├── network/
│   ├── GeminiService.kt
│   └── ProductUrlExtractor.kt
├── viewmodel/
│   └── MainViewModel.kt
└── ui/
    ├── theme/
    │   ├── Theme.kt
    │   └── Type.kt
    ├── screens/
    │   ├── MainScreen.kt
    │   └── FavoritesScreen.kt
    └── components/
        ├── ImageUploadCard.kt
        ├── LoadingScreen.kt
        └── ResultDisplay.kt
```

---

## Setup

### 1. Install Android Studio
https://developer.android.com/studio

### 2. Create project
- New Project > Empty Activity
- Package: `com.example.aestheticeye`
- Language: Kotlin
- Min SDK: API 26

### 3. Replace Gradle files
Replace `gradle/libs.versions.toml`, root `build.gradle.kts`, and `app/build.gradle.kts` with the provided files. Sync when prompted.

### 4. Add API key
In `local.properties` (do not commit this file):
```
GEMINI_API_KEY=your_key_here
```
Get a free key at https://aistudio.google.com/apikey.

### 5. Copy source files

| File | Path |
|------|------|
| `MainActivity.kt` | `…/com/example/aestheticeye/` |
| `Models.kt` | `…/data/` |
| `FavoritesDatabase.kt` | `…/data/` |
| `GeminiService.kt` | `…/network/` |
| `ProductUrlExtractor.kt` | `…/network/` |
| `MainViewModel.kt` | `…/viewmodel/` |
| `Theme.kt`, `Type.kt` | `…/ui/theme/` |
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

### 7. Run
Connect a device with USB debugging enabled, or use the emulator. Click Run.

---

## File Reference

| File | Description |
|------|-------------|
| `MainActivity.kt` | App entry point |
| `Models.kt` | All data classes: UiState, AestheticResult, ImprovementState, AiImprovementResult, ChangedProduct, SceneStage, FavoriteScene, DesignDirectorFeedback, Gemini API wire types |
| `FavoritesDatabase.kt` | Room database v8, FavoritesDao, fallbackToDestructiveMigration |
| `GeminiService.kt` | Full AI pipeline: scene generation, improvement, critique, mood board, product analysis |
| `ProductUrlExtractor.kt` | Fetches product page HTML, extracts structured product data via Gemini |
| `MainViewModel.kt` | Image slots, URL import, stage tracking, progressive scene callbacks, favourites CRUD |
| `Theme.kt` / `Type.kt` | Material 3 colour scheme and typography |
| `MainScreen.kt` | Main UI: image cards, prompt input, generate button, progressive result banner |
| `FavoritesScreen.kt` | Collections list and detail view with per-stage scene history |
| `ImageUploadCard.kt` | Image picker card with camera/gallery/URL support |
| `LoadingScreen.kt` | Animated tile grid loading screen. Exports `AeDots` composable used app-wide |
| `ResultDisplay.kt` | Scene display, design critique, AI improvement section, mood board |

---

## Troubleshooting

| Problem | Fix |
|---------|-----|
| `GEMINI_API_KEY not found` | Check `local.properties`, run File > Sync Project with Gradle Files |
| API 400 | Image too large. Service auto-scales to 1024px max |
| API 429 | Rate limit. Wait and retry |
| No image returned | `gemini-2.5-flash-image` is in preview. Check availability at AI Studio |
| URL import fails | Some sites block scraping. Use camera or gallery instead |
| App crashes on launch | Check Logcat for stack trace |
| Database crash after update | Room version bumped. Uninstall and reinstall the app |

---

## Demo

See Demo: https://drive.google.com/file/d/1wVgmXwf1rzmmqF4ZeYyMYP9_IyMeAafC/view?usp=sharing

## Authors

Researcher: Bibhusa Rawal and Developer: Neha Adhikari  
Stony Brook University, Application Development, Spring 2026
