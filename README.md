# Aesthetic Eye – Android App

A native Android version of the Aesthetic Eye web app.  
Upload a room photo + product photo → Gemini AI places the product in the room  
and gives you a professional design director critique.

---

## Tech Stack

| Layer        | Technology                        |
|--------------|-----------------------------------|
| UI           | Jetpack Compose + Material 3      |
| State        | ViewModel + StateFlow             |
| Networking   | OkHttp (REST calls to Gemini)     |
| JSON         | kotlinx.serialization             |
| Images       | Coil (display) + Bitmap (decode)  |
| AI           | Gemini 2.0 Flash (REST API)       |

---

## Project Structure

```
app/src/main/
├── java/com/aestheticeye/
│   ├── MainActivity.kt                  ← App entry point
│   ├── data/
│   │   └── Models.kt                    ← Data classes + API models
│   ├── network/
│   │   └── GeminiService.kt             ← All Gemini API calls
│   ├── viewmodel/
│   │   └── MainViewModel.kt             ← State management
│   └── ui/
│       ├── theme/
│       │   └── Theme.kt                 ← Colors + Material theme
│       ├── screens/
│       │   └── MainScreen.kt            ← Main UI screen
│       └── components/
│           ├── ImageUploadCard.kt       ← Reusable image picker card
│           ├── LoadingScreen.kt         ← Loading spinner + step text
│           └── ResultDisplay.kt         ← Generated image + critique
└── res/
    └── AndroidManifest.xml
```

---

## Step-by-Step Setup

### 1. Install Android Studio

Download from: https://developer.android.com/studio  
Install with default settings (includes the Android SDK).

---

### 2. Create the Project

1. Open Android Studio
2. Click **New Project**
3. Choose **Empty Activity**
4. Fill in:
   - **Name:** `AestheticEye`
   - **Package name:** `com.aestheticeye`
   - **Language:** Kotlin
   - **Minimum SDK:** API 26 (Android 8.0)
5. Click **Finish** and let Gradle sync

---

### 3. Replace the Gradle Files

**`gradle/libs.versions.toml`** — replace entire file with: `libs.versions.toml`  
**`build.gradle.kts` (project root)** — replace with: `project_build.gradle.kts`  
**`app/build.gradle.kts`** — replace with: `app_build.gradle.kts`

Click **Sync Now** when prompted.

---

### 4. Add Your Gemini API Key

Open (or create) `local.properties` in the project root:

```
sdk.dir=/Users/YourName/Library/Android/sdk
GEMINI_API_KEY=YOUR_ACTUAL_KEY_HERE
```

Get a free key at: https://aistudio.google.com/apikey

> ⚠️ Never commit `local.properties` to Git — it's already in `.gitignore`.

---

### 5. Copy the Source Files

Place each file in the correct location:

| File provided          | Where it goes in Android Studio                                      |
|------------------------|----------------------------------------------------------------------|
| `MainActivity.kt`      | `app/src/main/java/com/aestheticeye/`                                |
| `Models.kt`            | `app/src/main/java/com/aestheticeye/data/`                           |
| `GeminiService.kt`     | `app/src/main/java/com/aestheticeye/network/`                        |
| `MainViewModel.kt`     | `app/src/main/java/com/aestheticeye/viewmodel/`                      |
| `Theme.kt`             | `app/src/main/java/com/aestheticeye/ui/theme/`                       |
| `MainScreen.kt`        | `app/src/main/java/com/aestheticeye/ui/screens/`                     |
| `ImageUploadCard.kt`   | `app/src/main/java/com/aestheticeye/ui/components/`                  |
| `LoadingScreen.kt`     | `app/src/main/java/com/aestheticeye/ui/components/`                  |
| `ResultDisplay.kt`     | `app/src/main/java/com/aestheticeye/ui/components/`                  |
| `AndroidManifest.xml`  | `app/src/main/` (replace existing)                                   |

To create folders: right-click `com/aestheticeye` → **New → Package**.

---

### 6. Update `res/values/themes.xml`

Replace the content with:

```xml
<resources>
    <style name="Theme.AestheticEye" parent="android:Theme.Material.Light.NoActionBar" />
</resources>
```

---

### 7. Run the App

- Connect an Android phone via USB (enable Developer Mode + USB Debugging)
- Or use the built-in emulator: **Device Manager → Create Device**
- Click the green ▶ **Run** button

---

## How the App Works

```
User selects images
        ↓
MainViewModel.generateScene()
        ↓
GeminiService pipeline:
  1. describeImage(room)       → roomDescription (text)
  2. describeImage(product)    → productDescription (text)
  3. generateCompositeImage()  → generatedBitmap (image)
  4. getDirectorCritique()     → DesignDirectorFeedback (JSON)
        ↓
UiState.Success → ResultDisplay shows image + critique
```

---

## Troubleshooting

| Problem | Solution |
|---------|----------|
| `GEMINI_API_KEY` not found | Make sure `local.properties` has the key and you did **Sync Project** |
| API error 400 | The image might be too large. The service auto-scales to 1024px max |
| API error 429 | You've hit the rate limit — wait a moment and try again |
| Image generation returns no image | The `gemini-2.0-flash-preview-image-generation` model is in preview — check AI Studio for availability |
| App crashes on launch | Check Logcat in Android Studio for the stack trace |

---

## What Each File Does (Plain English)

- **MainActivity.kt** – The front door. Tells Android "start here" and sets up the screen.
- **Models.kt** – Data shapes: what a result looks like, what JSON we send/receive.
- **GeminiService.kt** – The worker. Makes HTTP calls to Gemini, sends images, gets back text + images.
- **MainViewModel.kt** – The manager. Holds state (which images are selected, is it loading, what's the result).
- **Theme.kt** – Colors and fonts for the whole app.
- **MainScreen.kt** – The full UI screen with image pickers, prompt box, and generate button.
- **ImageUploadCard.kt** – A single tappable card that lets you pick and preview an image.
- **LoadingScreen.kt** – The spinning animation shown while Gemini is working.
- **ResultDisplay.kt** – Shows the generated image and the design director's score/critique.
