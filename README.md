# 📱 PAN Card OCR Scanner

An Android application built with Kotlin and Jetpack Compose that scans Indian PAN cards and extracts essential personal details using Google ML Kit OCR.

## ✨ Features

- 📷 **Camera Capture with CameraX**: Full camera preview with high-quality image capture using MediaImage
- 🖼️ **Gallery Selection**: Select existing PAN card images from device gallery
- 🤖 **ML Kit OCR**: Powered by Google's ML Kit for accurate text recognition
- 🎨 **Modern UI**: Built entirely with Jetpack Compose for a clean, material design interface
- 🔒 **URI-based Processing**: Secure image handling using URI-based access

## 📋 Extracted Data

The app extracts and displays the following details from the PAN card:

- ✅ **PAN Number** (Format: ABCDE1234F)
- ✅ **Date of Birth** (Format: DD/MM/YYYY or DD-MM-YYYY)
- ✅ **Name**
- ✅ **Father's Name**

## 🛠️ Technology Stack

- **Language**: Kotlin
- **UI Framework**: Jetpack Compose with Material Design 3
- **Camera**: CameraX with ImageCapture
- **OCR Engine**: Google ML Kit Text Recognition
- **Min SDK**: 24 (Android 7.0)
- **Target SDK**: 36

## 📦 Key Dependencies

```kotlin
// ML Kit Text Recognition
implementation("com.google.mlkit:text-recognition:16.0.0")

// CameraX
implementation("androidx.camera:camera-camera2:1.3.1")
implementation("androidx.camera:camera-lifecycle:1.3.1")
implementation("androidx.camera:camera-view:1.3.1")

// Jetpack Compose
implementation("androidx.compose.ui:ui:1.6.8")
implementation("androidx.compose.material3:material3:1.2.1")
implementation("androidx.activity:activity-compose:1.9.2")
```

## 🚀 Installation

```bash
# Clone the repository
git clone <repository-url>
cd panOcr

# Build and install
./gradlew installDebug
```

## 📱 Usage

1. **Launch the app** on your Android device
2. **Choose an option**:
   - **Camera**: Tap "📷 Scan PAN using Camera" 
     - Grant camera permission if requested
     - Point camera at PAN card
     - Tap the camera button to capture
     - Wait for processing
   - **Gallery**: Tap "🖼 Select PAN from Gallery"
     - Select a PAN card image from your gallery
     - Wait for processing
3. **View Results**: Extracted details appear in a card below

## 🎯 How It Works

### Camera Flow (CameraX + MediaImage)
1. User taps camera button → Permission check
2. CameraX preview opens with live camera feed
3. User captures image → ImageProxy with MediaImage
4. MediaImage converted to InputImage for ML Kit
5. OCR processing with enhanced extraction logic
6. Results displayed on main screen

### Gallery Flow
1. User selects image from gallery
2. URI converted to InputImage using `fromFilePath()`
3. Multi-pass OCR processing:
   - **Pass 1**: Extract PAN number and DOB using regex
   - **Pass 2**: Find "NAME" and "FATHER'S NAME" labels
   - **Pass 3**: Extract data from lines following labels
   - **Pass 4**: Fallback pattern-based extraction
4. Results displayed with loading indicator

## 🔐 Permissions

Required permissions in AndroidManifest.xml:

```xml
<uses-permission android:name="android.permission.CAMERA" />
<uses-permission android:name="android.permission.READ_MEDIA_IMAGES" />
<uses-feature android:name="android.hardware.camera" android:required="false" />
```

## 🏗️ Architecture

### Main Components

- **MainActivity**: Activity with lifecycle management for camera executor
- **PanOCRScreen**: Main composable with state management
- **CameraPreviewScreen**: CameraX preview composable
- **captureAndProcessImage**: Image capture function using MediaImage
- **processImage**: Enhanced OCR processing with multi-pass extraction
- **isValidNameCandidate**: Helper for pattern-based name detection

### OCR Processing Strategy

1. **Text Extraction**: Extract all text blocks and lines from ML Kit
2. **Regex Matching**: Match PAN number (`[A-Z]{5}[0-9]{4}[A-Z]`) and DOB
3. **Label Detection**: Find "NAME" and "FATHER'S NAME" labels
4. **Data Extraction**: Extract from lines following labels
5. **Fallback Logic**: Pattern-based extraction if labels not found
6. **Validation**: Filter out common header/footer text

## 🔍 Debugging

All OCR operations are logged with tag `PAN_OCR`:

```bash
# View logs in terminal
adb logcat -s PAN_OCR:D

# Or in Android Studio: View → Tool Windows → Logcat
# Filter: PAN_OCR
```

Log output includes:
- All extracted text lines
- PAN number detection
- DOB detection
- Name/Father's name extraction steps
- Final results

## 💡 Tips for Best Results

### Camera Capture
- Use good lighting without shadows or glare
- Hold camera steady and wait for focus
- Capture from directly above the PAN card
- Ensure entire card is visible in frame
- Avoid using flash

### Gallery Images
- Use high-resolution images
- Ensure text is clear and sharp
- Full PAN card should be visible
- Good contrast between text and background

## ✅ Build Status

```
BUILD SUCCESSFUL
✅ No compilation errors
✅ CameraX with MediaImage integration
✅ Enhanced OCR accuracy
✅ Ready for testing
```

## 📄 License

This project is open source and available under the MIT License.

---

**Built with ❤️ using Kotlin, Jetpack Compose, and CameraX**

