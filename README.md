# Aesthetic Eye – Android App

AI-powered interior design assistant that visualizes product placement in real rooms and delivers expert design critiques — all from your phone.

Upload a room photo, pick a product (from your camera or the built-in catalog), and Aesthetic Eye generates a photorealistic composite scene scored by a simulated Design Director.

---

## What It Does

1. **Upload a room photo** — take or select a photo of the space you want to design.
2. **Pick a product** — upload a product image from your camera roll, or browse the built-in furniture catalog.
3. **AI generates the scene** — Gemini creates a photorealistic composite of the product placed naturally in the room.
4. **Get a Design Director critique** — the AI scores the placement 0–100, detects the room's style, lists flaws, and suggests improvements.

---

## Tech Stack

| Layer | Technology | Purpose |
|-------|-----------|---------|
| UI | Jetpack Compose + Material 3 | Declarative UI, no XML layouts |
| State | ViewModel + StateFlow | Reactive state management |
| Networking | OkHttp | REST calls to Gemini and DummyJSON |
| JSON | kotlinx.serialization | Encode/decode API payloads |
| Images | Coil (display) + Bitmap (decode) | Async loading from URLs and camera |
| AI | Gemini 2.0 Flash (REST API) | Text, vision, and image generation |
| Catalog | DummyJSON API | Free product browsing with images |

---

## The 4-Stage Gemini AI Pipeline

| Stage | Name | Model | What It Does | Output |
|-------|------|-------|-------------|--------|
| 1 | Room Analysis | gemini-2.0-flash | Describes the room's style, lighting, colors, furniture, and surfaces | Room description text |
| 2 | Product Analysis | gemini-2.0-flash | Extracts the product's shape, color, material, and finish | Product description text |
| 3 | Scene Generation | gemini-2.0-flash-image | Combines descriptions + images into a photorealistic composite | Generated scene bitmap |
| 4 | Design Critique | gemini-2.0-flash | Scores the placement 0–100 with status, style, flaws, and suggestions | Structured JSON critique |

---

## Project Structure

```
app/src/main/java/com/aestheticeye/
├── MainActivity.kt                    ← App entry point
├── data/
│   ├── Models.kt                      ← Gemini request/response data classes
│   └── FurnitureCatalogModels.kt      ← Product, dimensions, filter models
├── network/
│   ├── GeminiService.kt               ← All 4 Gemini API calls + image encoding
│   └── FurnitureApiService.kt         ← DummyJSON product fetching + image download
├── viewmodel/
│   ├── MainViewModel.kt               ← Room/product state, generation, navigation
│   └── CatalogViewModel.kt            ← Catalog filtering, selection, image download
└── ui/
    ├── theme/
    │   └── Theme.kt                   ← Colors + Material theme
    ├── screens/
    │   ├── MainScreen.kt              ← Main UI with upload cards + prompt + catalog button
    │   └── CatalogScreen.kt           ← Browsable product grid with filters + detail sheet
    └── components/
        ├── ImageUploadCard.kt         ← Reusable image picker card
        ├── LoadingScreen.kt           ← Loading spinner + step text
        └── ResultDisplay.kt           ← Generated image + Design Director critique
```

---

## Step-by-Step Setup

### 1. Install Android Studio
Download from: https://developer.android.com/studio
Install with default settings (includes the Android SDK).

### 2. Create the Project
- Open Android Studio → **New Project** → **Empty Activity**
- Name: `AestheticEye`
- Package name: `com.aestheticeye`
- Language: Kotlin
- Minimum SDK: API 26 (Android 8.0)
- Click **Finish** and let Gradle sync.

### 3. Replace the Gradle Files
- `gradle/libs.versions.toml` — replace with the provided `libs.versions.toml`
- `build.gradle.kts` (project root) — replace with `project_build.gradle.kts`
- `app/build.gradle.kts` — replace with `app_build.gradle.kts`

Click **Sync Now** when prompted.

### 4. Add Dependencies
Make sure `app/build.gradle.kts` includes Coil for async image loading:

```kotlin
implementation("io.coil-kt:coil-compose:2.6.0")
```

### 5. Add Your Gemini API Key
Open (or create) `local.properties` in the project root:

```
sdk.dir=/Users/YourName/Library/Android/sdk
GEMINI_API_KEY=YOUR_ACTUAL_KEY_HERE
```

Get a free key at: https://aistudio.google.com/apikey

> Never commit `local.properties` to Git — it's already in `.gitignore`.

### 6. Copy the Source Files

| File | Location |
|------|----------|
| `MainActivity.kt` | `app/src/main/java/com/aestheticeye/` |
| `Models.kt` | `app/src/main/java/com/aestheticeye/data/` |
| `FurnitureCatalogModels.kt` | `app/src/main/java/com/aestheticeye/data/` |
| `GeminiService.kt` | `app/src/main/java/com/aestheticeye/network/` |
| `FurnitureApiService.kt` | `app/src/main/java/com/aestheticeye/network/` |
| `MainViewModel.kt` | `app/src/main/java/com/aestheticeye/viewmodel/` |
| `CatalogViewModel.kt` | `app/src/main/java/com/aestheticeye/viewmodel/` |
| `Theme.kt` | `app/src/main/java/com/aestheticeye/ui/theme/` |
| `MainScreen.kt` | `app/src/main/java/com/aestheticeye/ui/screens/` |
| `CatalogScreen.kt` | `app/src/main/java/com/aestheticeye/ui/screens/` |
| `ImageUploadCard.kt` | `app/src/main/java/com/aestheticeye/ui/components/` |
| `LoadingScreen.kt` | `app/src/main/java/com/aestheticeye/ui/components/` |
| `ResultDisplay.kt` | `app/src/main/java/com/aestheticeye/ui/components/` |
| `AndroidManifest.xml` | `app/src/main/` (replace existing) |

To create folders: right-click `com/aestheticeye` → **New → Package**.

### 7. Update res/values/themes.xml

```xml
<resources>
    <style name="Theme.AestheticEye" parent="android:Theme.Material.Light.NoActionBar" />
</resources>
```

### 8. Run the App
- Connect an Android phone via USB (enable Developer Mode + USB Debugging)
- Or use the built-in emulator: **Device Manager → Create Device**
- Click the green **Run** button.

---

## How It Works Internally

```
User selects images (camera or catalog)
        ↓
MainViewModel.generateScene()
        ↓
GeminiService pipeline:
  1. describeImage(room)            → roomDescription (text)
  2. describeImage(product)         → productDescription (text)
  3. generateCompositeImage()       → generatedBitmap (image)
  4. getDirectorCritique()          → DesignDirectorFeedback (JSON)
        ↓
UiState.Success → ResultDisplay shows image + critique
```

### Catalog Flow

```
User taps "Browse Catalog"
        ↓
MainViewModel.openCatalog() → CatalogScreen renders
        ↓
CatalogViewModel → FurnitureApiService.getHomeProducts()
        ↓
DummyJSON API returns products with images
        ↓
User taps product → bottom sheet → "Use This Product"
        ↓
FurnitureApiService.downloadProductImage() → Bitmap
        ↓
MainViewModel.setProductFromCatalog(bitmap, name)
        ↓
Returns to MainScreen with product loaded → normal pipeline
```

---

## Furniture Catalog API

- **API**: [DummyJSON](https://dummyjson.com/products) — free, no API key required
- **Categories**: Furniture, Home Decoration, Kitchen Accessories
- **Features**: Product images, prices, ratings, brands, dimensions
- **Rate limit**: No hard limit for reasonable use
- **Fallback**: If the API is unreachable, the upload-from-camera flow still works independently

---

## What Each File Does

| File | Purpose |
|------|---------|
| `MainActivity.kt` | App entry point. Tells Android to start here and sets up the Compose screen. |
| `Models.kt` | Data classes for Gemini API requests/responses and the Design Director feedback structure. |
| `FurnitureCatalogModels.kt` | Data classes for DummyJSON products, dimensions, filter state, and category options. |
| `GeminiService.kt` | Makes all 4 HTTP calls to Gemini. Sends images as base64, gets back text and images. |
| `FurnitureApiService.kt` | Fetches products from DummyJSON by category. Downloads product images as Bitmaps. |
| `MainViewModel.kt` | Holds app state: selected images, prompt, UI state, product source (camera vs. catalog), catalog navigation. |
| `CatalogViewModel.kt` | Manages catalog state: product list, filters, selected product, image download status. |
| `Theme.kt` | Colors and fonts for the whole app using Material 3 theming. |
| `MainScreen.kt` | Full main UI: image upload cards, "Browse Catalog" button, prompt field, generate button, result display. |
| `CatalogScreen.kt` | Product browsing grid with category filter chips, product cards, and a detail bottom sheet. |
| `ImageUploadCard.kt` | A tappable card component that opens the system photo picker and shows a live preview. |
| `LoadingScreen.kt` | Spinning animation with real-time step text shown while the Gemini pipeline runs. |
| `ResultDisplay.kt` | Shows the generated scene image and the full Design Director report (score, style, flaws, suggestions). |

---

## Troubleshooting

| Problem | Solution |
|---------|----------|
| `GEMINI_API_KEY` not found | Make sure `local.properties` has the key and you did **Sync Project** |
| API error 400 | The image might be too large. The service auto-scales to 1024px max. |
| API error 429 | Rate limit hit — wait a moment and try again. |
| Image generation returns no image | The image generation model is in preview — check AI Studio for availability. |
| Catalog shows "Failed to load" | Check your internet connection. DummyJSON may be temporarily down — the retry button will try again. |
| App crashes on launch | Check **Logcat** in Android Studio for the stack trace. |

---

## Upcoming Features

These features are planned for the next development phase:

- **AI vs. User Comparison** — After generating the user's placement, the AI suggests its own optimized placement. Both scenes display side-by-side with scores so users can compare and learn.
- **Room Style Auto-Detect** — Surfaces the room analysis as a visible card showing detected style name, dominant color palette, and key design traits.
- **AI Size Reality Check** — Uses visible furniture as reference points to estimate relative scale. Flags proportional mismatches before generating the scene.
- **Mood Board Generator** — After analyzing the room, the AI suggests 3–4 complementary products that match the detected style.
- **Favorites and Collections** — Local storage using Room database. Users save past generations into named project folders and revisit results across sessions.

---

## Authors

Bibhusa Rawal and Neha Adhikari
Stony Brook University — Application Development — Spring 2026
