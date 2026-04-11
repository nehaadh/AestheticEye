Aesthetic Eye – Android App
AI-powered interior design assistant that visualizes product placement in real rooms and delivers expert design critiques — all from your phone.

Upload a room photo, add products (from your camera, gallery, or any product URL), and Aesthetic Eye generates a photorealistic composite scene scored by a simulated Design Director. Keep adding products one by one until your room is complete.

What It Does
Upload a room photo — take or select a photo of the space you want to design.
Pick a product — upload a product image from your camera roll, or paste any product URL (IKEA, Amazon, Wayfair, Target, etc.) and the AI extracts the product details and image automatically.
AI generates the scene — Gemini creates a photorealistic composite of the product placed naturally in the room.
Get a Design Director critique — the AI scores the placement 0–100, detects the room's style, lists flaws, and suggests improvements.
Add more products — after each generation, tap "Add Another Product" to keep building your scene iteratively. The generated scene becomes the new room base.
Start over anytime — reset to a fresh canvas with one tap.

Key Features
URL Product Import — Paste any furniture product URL and the AI extracts the product name, description, price, dimensions, material, color, and image automatically. Works with IKEA, Amazon, Wayfair, Target, and most furniture websites.
Iterative Product Placement — Add products one at a time. Each generation preserves the previous scene, so you can layer multiple products into the same room naturally.
Smart Placement Understanding — Casual placement instructions like "lamp next to the couch" or "texture on the wall" are interpreted intelligently by the AI using interior design expertise.
Holistic Design Critique — The Design Director evaluates how each new product fits within the overall room composition, not just in isolation.
Camera + Gallery + URL — Every image slot supports uploading from camera/gallery OR pasting a URL. Room photos, product images, and detail/texture references can all come from any source.

Tech Stack
Layer           Technology                  Purpose
UI              Jetpack Compose + Material 3    Declarative UI, no XML layouts
State           ViewModel + StateFlow           Reactive state management
Networking      OkHttp                          REST calls to Gemini APIs and web pages
JSON            kotlinx.serialization           Encode/decode API payloads
Images          Coil (display) + Bitmap (decode) Async loading from URLs and camera
AI              Gemini 2.5 Flash (REST API)     Text, vision, and image generation
URL Extraction  Gemini 2.5 Flash + OkHttp       Fetches product pages, AI extracts structured data

The 4-Stage Gemini AI Pipeline
Stage  Name               Model                    What It Does                                                    Output
1      Room Analysis      gemini-2.5-flash         Describes the room's style, lighting, colors, furniture          Room description text
2      Product Analysis   gemini-2.5-flash         Extracts the product's shape, color, material, and finish       Product description text
3      Scene Generation   gemini-2.5-flash-image   Combines descriptions + images into a photorealistic composite  Generated scene bitmap
4      Design Critique    gemini-2.5-flash         Scores placement 0–100 with status, style, flaws, suggestions   Structured JSON critique

For iterative placement (adding more products), Stages 1–3 run again using the previous generated scene as the new room input. Stage 4 evaluates the new product primarily while considering the overall composition.

URL Product Extraction Pipeline
When a user pastes a product URL:
1. OkHttp fetches the page HTML with browser-like headers
2. HTML is trimmed to 15k characters and sent to Gemini 2.5 Flash
3. Gemini extracts structured JSON: name, description, price, dimensions, material, color, image URL, brand
4. The product image is downloaded and displayed in a preview card
5. User confirms → product image feeds into the main generation pipeline

For room/detail URLs, a simpler extraction finds the main image (via og:image tags or hero images) and downloads it directly.

Project Structure
app/src/main/java/com/aestheticeye/
├── MainActivity.kt                    ← App entry point
├── data/
│   └── Models.kt                      ← Gemini request/response data classes
├── network/
│   ├── GeminiService.kt               ← All 4 Gemini API calls + image encoding
│   └── ProductUrlExtractor.kt         ← URL fetching + Gemini product extraction
├── viewmodel/
│   └── MainViewModel.kt               ← Room/product state, generation, URL import, iteration
└── ui/
    ├── theme/
    │   └── Theme.kt                   ← Colors + Material theme
    ├── screens/
    │   └── MainScreen.kt              ← Main UI with upload cards + URL import + prompt + results
    └── components/
        ├── ImageUploadCard.kt         ← Reusable image picker card with URL option
        ├── UrlImportSheet.kt          ← Bottom sheet for URL input + product preview
        ├── LoadingScreen.kt           ← Loading spinner + step text
        └── ResultDisplay.kt           ← Generated image + Design Director critique

Step-by-Step Setup
1. Install Android Studio
Download from: https://developer.android.com/studio
Install with default settings (includes the Android SDK).

2. Create the Project
Open Android Studio → New Project → Empty Activity
Name: AestheticEye
Package name: com.aestheticeye
Language: Kotlin
Minimum SDK: API 26 (Android 8.0)
Click Finish and let Gradle sync.

3. Replace the Gradle Files
gradle/libs.versions.toml — replace with the provided libs.versions.toml
build.gradle.kts (project root) — replace with project_build.gradle.kts
app/build.gradle.kts — replace with app_build.gradle.kts
Click Sync Now when prompted.

4. Add Dependencies
Make sure app/build.gradle.kts includes Coil for async image loading:

implementation("io.coil-kt:coil-compose:2.6.0")

5. Add Your Gemini API Key
Open (or create) local.properties in the project root:

sdk.dir=/Users/YourName/Library/Android/sdk
GEMINI_API_KEY=YOUR_ACTUAL_KEY_HERE

Get a free key at: https://aistudio.google.com/apikey
Never commit local.properties to Git — it's already in .gitignore.

6. Copy the Source Files
File                    Location
MainActivity.kt        app/src/main/java/com/aestheticeye/
Models.kt              app/src/main/java/com/aestheticeye/data/
GeminiService.kt       app/src/main/java/com/aestheticeye/network/
ProductUrlExtractor.kt app/src/main/java/com/aestheticeye/network/
MainViewModel.kt       app/src/main/java/com/aestheticeye/viewmodel/
Theme.kt               app/src/main/java/com/aestheticeye/ui/theme/
MainScreen.kt          app/src/main/java/com/aestheticeye/ui/screens/
ImageUploadCard.kt     app/src/main/java/com/aestheticeye/ui/components/
UrlImportSheet.kt      app/src/main/java/com/aestheticeye/ui/components/
LoadingScreen.kt       app/src/main/java/com/aestheticeye/ui/components/
ResultDisplay.kt       app/src/main/java/com/aestheticeye/ui/components/
AndroidManifest.xml    app/src/main/ (replace existing)

To create folders: right-click com/aestheticeye → New → Package.

7. Update res/values/themes.xml
<resources>
    <style name="Theme.AestheticEye" parent="android:Theme.Material.Light.NoActionBar" />
</resources>

8. Run the App
Connect an Android phone via USB (enable Developer Mode + USB Debugging)
Or use the built-in emulator: Device Manager → Create Device
Click the green Run button.

How It Works Internally

First Product Flow:
User selects room photo + product (camera/gallery/URL)
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
        ↓
User can tap "Add Another Product" or "Done — Start New Scene"

Iterative Product Flow (Adding More Products):
User taps "Add Another Product"
        ↓
Generated scene becomes the new "room" base
Product slot clears, prompt resets
        ↓
User picks next product (camera/gallery/URL)
        ↓
Same 4-stage pipeline runs with the previous scene as input
        ↓
New scene generated with both products visible
        ↓
User can keep adding more products or finish

URL Import Flow:
User taps "Paste URL" under any image slot
        ↓
UrlImportSheet opens → user pastes URL
        ↓
For product slots:
  ProductUrlExtractor.extractProductFromUrl()
    → Fetches HTML → Gemini extracts product details
    → Downloads product image → Shows preview card
    → User confirms → product loaded into slot
        ↓
For room/detail slots:
  ProductUrlExtractor.extractImageFromUrl()
    → Detects direct image URLs (jpg/png) → downloads directly
    → Or fetches HTML → Gemini finds main image URL
    → Downloads image → Shows preview
    → User confirms → image loaded into slot

What Each File Does
File                    Purpose
MainActivity.kt        App entry point. Sets up the Compose screen.
Models.kt              Data classes for Gemini API requests/responses and Design Director feedback.
GeminiService.kt       Makes all 4 Gemini API calls. Sends images as base64, returns text and images. Smart placement interpretation.
ProductUrlExtractor.kt Fetches product page HTML, sends to Gemini for structured extraction. Also handles simple image extraction for room/detail URLs.
MainViewModel.kt       Holds app state: image slots, prompt, UI state, URL import, iterative placement flow. Manages the "add another product" loop.
Theme.kt               Colors and fonts for the whole app using Material 3 theming.
MainScreen.kt          Full main UI: image upload cards with URL option, prompt field, generate button, result display with "Add Another Product" flow.
ImageUploadCard.kt     Tappable card that opens the system photo picker and shows a live preview. Includes "Paste URL" link below each card.
UrlImportSheet.kt      Bottom sheet for URL input with paste button, loading states, product detail preview (for product URLs), and image preview (for room/detail URLs).
LoadingScreen.kt       Spinning animation with real-time step text shown while the Gemini pipeline runs.
ResultDisplay.kt       Shows the generated scene image and the full Design Director report (score, style, flaws, suggestions).

Troubleshooting
Problem                             Solution
GEMINI_API_KEY not found            Make sure local.properties has the key and you did Sync Project
API error 400                       The image might be too large. The service auto-scales to 1024px max.
API error 429                       Rate limit hit — wait a moment and try again.
Image generation returns no image   The image generation model is in preview — check AI Studio for availability.
URL extraction fails                Some sites block scraping. Try a different product URL or use camera/gallery instead.
App crashes on launch               Check Logcat in Android Studio for the stack trace.

Upcoming Features
These features are planned for the next development phase:

AI vs. User Comparison — After generating the user's placement, the AI suggests its own optimized placement. Both scenes display side-by-side with scores so users can compare and learn.
Room Style Auto-Detect — Surfaces the room analysis as a visible card showing detected style name, dominant color palette, and key design traits.
AI Size Reality Check — Uses visible furniture as reference points to estimate relative scale. Flags proportional mismatches before generating the scene.
Mood Board Generator — After analyzing the room, the AI suggests 3–4 complementary products that match the detected style.
Favorites and Collections — Local storage using Room database. Users save past generations into named project folders and revisit results across sessions.

Authors
Bibhusa Rawal and Neha Adhikari
Stony Brook University — Application Development — Spring 2026
