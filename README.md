# 📱 PAN Card OCR Scanner

An Android application built with Kotlin and Jetpack Compose that scans Indian PAN cards and extracts essential personal details using Google ML Kit OCR.

## ✨ Features

- 📷 **Camera Capture**: Take a photo of a PAN card directly using your phone's camera
- 🖼️ **Gallery Selection**: Select an existing PAN card image from your device gallery
- 🤖 **ML Kit OCR**: Powered by Google's ML Kit for accurate text recognition
- 🎨 **Modern UI**: Built entirely with Jetpack Compose for a clean, material design interface
- 🔒 **URI-based Processing**: Secure image handling using URI-based access (no file-path based access)

## 📋 Extracted Data

The app extracts and displays the following details from the PAN card:

- ✅ **Name**
- ✅ **Father's Name**
- ✅ **Date of Birth**
- ✅ **PAN Card Number**

## 🛠️ Technology Stack

- **Language**: Kotlin
- **UI Framework**: Jetpack Compose
- **OCR Engine**: Google ML Kit Text Recognition
- **Min SDK**: 24 (Android 7.0)
- **Target SDK**: 36

## 📦 Dependencies

```kotlin
// ML Kit Text Recognition
implementation("com.google.mlkit:text-recognition:16.0.0")

// Jetpack Compose
implementation("androidx.compose.ui:ui:1.6.8")
implementation("androidx.compose.material3:material3:1.2.1")
implementation("androidx.activity:activity-compose:1.9.2")
```

## 🔧 Setup & Installation

1. **Clone the repository**
   ```bash
   git clone <repository-url>
   cd panOcr
   ```

2. **Open in Android Studio**
   - Open Android Studio
   - File → Open → Select the project directory

3. **Build the project**
   ```bash
   ./gradlew assembleDebug
   ```

4. **Run the app**
   - Connect an Android device or start an emulator
   - Click Run in Android Studio or use:
   ```bash
   ./gradlew installDebug
   ```

## 📱 Usage

1. **Launch the app** on your Android device
2. **Choose an option**:
   - Tap "📷 Scan PAN using Camera" to capture a photo
   - Tap "🖼 Select PAN from Gallery" to choose from gallery
3. **View extracted details** displayed in a card below the buttons

## 🎯 How It Works

1. **Image Capture/Selection**: User captures or selects a PAN card image
2. **Permission Handling**: App requests camera permission if needed
3. **URI Processing**: Image is processed using URI-based `InputImage.fromFilePath()` or `InputImage.fromBitmap()`
4. **ML Kit OCR**: Google ML Kit processes the image and extracts text
5. **Data Parsing**: Custom regex patterns identify:
   - PAN Number: `[A-Z]{5}[0-9]{4}[A-Z]`
   - Date of Birth: `DD/MM/YYYY` or `DD-MM-YYYY`
   - Name and Father's Name: Text following "NAME" and "FATHER" labels
6. **Display Results**: Extracted data is shown in a Material Design card

## 🔐 Permissions

The app requires the following permissions:

```xml
<uses-permission android:name="android.permission.CAMERA" />
<uses-permission android:name="android.permission.READ_MEDIA_IMAGES" />
<uses-feature android:name="android.hardware.camera" android:required="false" />
```

- **CAMERA**: For capturing PAN card photos
- **READ_MEDIA_IMAGES**: For accessing images from gallery (Android 13+)
- **Camera Hardware Feature**: Marked as optional for device compatibility

## 📸 Screenshots

The app features:
- Clean title "PAN Card Scanner"
- Two prominent buttons for camera and gallery
- Material Design card displaying extracted details
- Responsive layout with proper spacing

## 🏗️ Project Structure

```
app/src/main/
├── AndroidManifest.xml          # App configuration & permissions
└── java/com/example/panocr/
    └── MainActivity.kt           # Main activity with Compose UI
```

### Key Components

**MainActivity.kt**:
- `MainActivity`: Main activity class
- `PanCardData`: Data class for PAN card information
- `PanOCRScreen()`: Main Compose UI screen
- `DetailRow()`: Reusable UI component for displaying key-value pairs
- `processImage()`: OCR processing and data extraction logic

## 🔍 OCR Accuracy Tips

For best results:
- ✅ Ensure good lighting
- ✅ Keep the PAN card flat and fully visible
- ✅ Avoid shadows and glare
- ✅ Capture in high resolution
- ✅ Keep text clear and in focus

## 🚀 Future Enhancements

Potential improvements:
- [ ] Add image preview before processing
- [ ] Support for cropping/rotating images
- [ ] Enhanced error handling and user feedback
- [ ] Save/export extracted data
- [ ] Support for multiple document types
- [ ] Batch processing
- [ ] Offline mode improvements

## 📄 License

This project is open source and available under the MIT License.

## 🤝 Contributing

Contributions are welcome! Please feel free to submit a Pull Request.

## 📞 Support

For issues or questions, please open an issue in the repository.

---

**Built with ❤️ using Kotlin and Jetpack Compose**

