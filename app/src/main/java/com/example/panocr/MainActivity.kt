package com.example.panocr

import android.Manifest
import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import java.io.IOException
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class MainActivity : ComponentActivity() {

    private lateinit var cameraExecutor: ExecutorService

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        cameraExecutor = Executors.newSingleThreadExecutor()
        setContent {
            PanOCRScreen()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }
}

data class PanCardData(
    val name: String? = null,
    val fatherName: String? = null,
    val dob: String? = null,
    val panNumber: String? = null
)

@Composable
fun PanOCRScreen() {

    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var panData by remember { mutableStateOf<PanCardData?>(null) }
    var isProcessing by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showCamera by remember { mutableStateOf(false) }
    var hasCameraPermission by remember { mutableStateOf(false) }

    val recognizer =
        remember { TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS) }

    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }

    // Camera permission launcher
    val cameraPermissionLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            hasCameraPermission = granted
            if (granted) {
                showCamera = true
            } else {
                errorMessage = "Camera permission denied"
            }
        }

    // Gallery launcher (URI)
    val galleryLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            uri?.let {
                try {
                    isProcessing = true
                    errorMessage = null
                    val image = InputImage.fromFilePath(context, it)
                    processImage(image, recognizer) { result ->
                        panData = result
                        isProcessing = false
                    }
                } catch (e: IOException) {
                    e.printStackTrace()
                    errorMessage = "Failed to load image: ${e.message}"
                    isProcessing = false
                }
            } ?: run {
                errorMessage = "No image selected"
                isProcessing = false
            }
        }

    DisposableEffect(Unit) {
        onDispose {
            cameraExecutor.shutdown()
        }
    }

    MaterialTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            if (showCamera && hasCameraPermission) {
                // Show CameraX preview
                CameraPreviewScreen(
                    context = context,
                    lifecycleOwner = lifecycleOwner,
                    cameraExecutor = cameraExecutor,
                    recognizer = recognizer,
                    onImageCaptured = { result ->
                        panData = result
                        showCamera = false
                        isProcessing = false
                    },
                    onClose = {
                        showCamera = false
                    },
                    onProcessing = { processing ->
                        isProcessing = processing
                    },
                    onError = { error ->
                        errorMessage = error
                        showCamera = false
                        isProcessing = false
                    }
                )
            } else {
                // Show main screen with buttons
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {

                    Text(
                        text = "PAN Card Scanner",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(vertical = 16.dp)
                    )

                    Button(
                        onClick = {
                            errorMessage = null
                            panData = null
                            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("📷 Scan PAN using Camera")
                    }

                    Button(
                        onClick = {
                            errorMessage = null
                            panData = null
                            galleryLauncher.launch("image/*")
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("🖼 Select PAN from Gallery")
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                    // Loading indicator
                    if (isProcessing) {
                        CircularProgressIndicator(
                            modifier = Modifier.padding(16.dp)
                        )
                        Text("Processing image...", fontSize = 16.sp)
                    }

                    // Error message
                    errorMessage?.let { error ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer
                            )
                        ) {
                            Text(
                                text = error,
                                modifier = Modifier.padding(16.dp),
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }

                    panData?.let {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Text(
                                    text = "Extracted Details",
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold
                                )

                                DetailRow("Name", it.name)
                                DetailRow("Father's Name", it.fatherName)
                                DetailRow("Date of Birth", it.dob)
                                DetailRow("PAN Number", it.panNumber)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DetailRow(label: String, value: String?) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = "$label:",
            fontWeight = FontWeight.Medium,
            fontSize = 16.sp
        )
        Text(
            text = value ?: "-",
            fontSize = 16.sp
        )
    }
}

@Composable
fun CameraPreviewScreen(
    context: Context,
    lifecycleOwner: androidx.lifecycle.LifecycleOwner,
    cameraExecutor: ExecutorService,
    recognizer: com.google.mlkit.vision.text.TextRecognizer,
    onImageCaptured: (PanCardData) -> Unit,
    onClose: () -> Unit,
    onProcessing: (Boolean) -> Unit,
    onError: (String) -> Unit
) {
    val previewView = remember { PreviewView(context) }
    var isCapturing by remember { mutableStateOf(false) }

    // Store imageCapture as state so we can access it from the button
    var imageCapture by remember { mutableStateOf<ImageCapture?>(null) }

    DisposableEffect(Unit) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)

        cameraProviderFuture.addListener({
            try {
                val cameraProvider = cameraProviderFuture.get()

                // Build preview
                val preview = Preview.Builder()
                    .build()
                    .also {
                        it.setSurfaceProvider(previewView.surfaceProvider)
                    }

                // Build image capture use case
                val imageCaptureUseCase = ImageCapture.Builder()
                    .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
                    .setTargetRotation(previewView.display.rotation)
                    .build()

                // Store it in state
                imageCapture = imageCaptureUseCase

                val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                // Unbind all use cases before rebinding
                cameraProvider.unbindAll()

                // Bind use cases to camera
                cameraProvider.bindToLifecycle(
                    lifecycleOwner,
                    cameraSelector,
                    preview,
                    imageCaptureUseCase
                )

                Log.d("PAN_OCR", "Camera bound successfully")

            } catch (exc: Exception) {
                Log.e("PAN_OCR", "Camera binding failed", exc)
                onError("Failed to start camera: ${exc.message}")
            }
        }, ContextCompat.getMainExecutor(context))

        onDispose {
            try {
                val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
                cameraProviderFuture.get().unbindAll()
            } catch (e: Exception) {
                Log.e("PAN_OCR", "Error unbinding camera", e)
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            factory = { previewView },
            modifier = Modifier.fillMaxSize()
        )

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (isCapturing) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                Text(
                    text = "Processing...",
                    color = MaterialTheme.colorScheme.onPrimary,
                    fontSize = 16.sp
                )
            } else {
                Button(
                    onClick = {
                        if (!isCapturing && imageCapture != null) {
                            isCapturing = true
                            onProcessing(true)

                            Log.d("PAN_OCR", "Capture button clicked, taking picture...")

                            // Take picture using the stored imageCapture
                            imageCapture!!.takePicture(
                                cameraExecutor,
                                object : ImageCapture.OnImageCapturedCallback() {
                                    @androidx.annotation.OptIn(androidx.camera.core.ExperimentalGetImage::class)
                                    override fun onCaptureSuccess(imageProxy: ImageProxy) {
                                        Log.d("PAN_OCR", "Image captured successfully")

                                        val mediaImage = imageProxy.image
                                        if (mediaImage != null) {
                                            val image = InputImage.fromMediaImage(
                                                mediaImage,
                                                imageProxy.imageInfo.rotationDegrees
                                            )

                                            Log.d("PAN_OCR", "Starting OCR processing...")
                                            processImage(image, recognizer) { result ->
                                                Log.d("PAN_OCR", "OCR completed with result: $result")
                                                onImageCaptured(result)
                                                imageProxy.close()
                                            }
                                        } else {
                                            Log.e("PAN_OCR", "Media image is null")
                                            isCapturing = false
                                            onProcessing(false)
                                            onError("Failed to capture image")
                                            imageProxy.close()
                                        }
                                    }

                                    override fun onError(exception: ImageCaptureException) {
                                        Log.e("PAN_OCR", "Image capture failed", exception)
                                        isCapturing = false
                                        onProcessing(false)
                                        onError("Capture failed: ${exception.message}")
                                    }
                                }
                            )
                        } else if (imageCapture == null) {
                            Log.e("PAN_OCR", "ImageCapture is null, camera not ready")
                            onError("Camera not ready, please try again")
                        }
                    },
                    modifier = Modifier.size(80.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    ),
                    enabled = !isCapturing && imageCapture != null
                ) {
                    Text("📷", fontSize = 32.sp)
                }

                Button(
                    onClick = onClose,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    ),
                    enabled = !isCapturing
                ) {
                    Text("Cancel")
                }
            }
        }
    }
}

fun processImage(
    image: InputImage,
    recognizer: com.google.mlkit.vision.text.TextRecognizer,
    onResult: (PanCardData) -> Unit
) {
    recognizer.process(image)
        .addOnSuccessListener { visionText ->

            // Extract all text lines with better organization
            val lines = mutableListOf<String>()
            val allText = StringBuilder()

            visionText.textBlocks.forEach { block ->
                block.lines.forEach { line ->
                    val cleanedText = line.text.trim()
                    if (cleanedText.isNotEmpty()) {
                        lines.add(cleanedText)
                        allText.append(cleanedText).append(" ")
                    }
                }
            }

            // Log all extracted text for debugging
            Log.d("PAN_OCR", "=== Extracted Text ===")
            lines.forEachIndexed { index, line ->
                Log.d("PAN_OCR", "Line $index: $line")
            }
            Log.d("PAN_OCR", "Combined: ${allText.toString()}")
            Log.d("PAN_OCR", "=====================")

            // Enhanced regex patterns
            val panRegex = Regex("[A-Z]{5}[0-9]{4}[A-Z]")
            // More flexible DOB pattern
            val dobRegex = Regex("\\b(0?[1-9]|[12][0-9]|3[01])[/-](0?[1-9]|1[0-2])[/-]((19|20)?\\d{2})\\b")

            var name: String? = null
            var fatherName: String? = null
            var dob: String? = null
            var panNumber: String? = null

            // First pass: Extract PAN number and DOB using regex from all text
            val combinedText = allText.toString()

            // Extract PAN number
            panRegex.find(combinedText.uppercase())?.let {
                panNumber = it.value
                Log.d("PAN_OCR", "Found PAN in combined text: $panNumber")
            }

            // Also check individual lines for PAN
            if (panNumber == null) {
                for (line in lines) {
                    val found = panRegex.find(line.uppercase())
                    if (found != null) {
                        panNumber = found.value
                        Log.d("PAN_OCR", "Found PAN in line: $panNumber")
                        break
                    }
                }
            }

            // Extract Date of Birth
            dobRegex.find(combinedText)?.let {
                dob = it.value
                Log.d("PAN_OCR", "Found DOB in combined text: $dob")
            }

            if (dob == null) {
                for (line in lines) {
                    val found = dobRegex.find(line)
                    if (found != null) {
                        dob = found.value
                        Log.d("PAN_OCR", "Found DOB in line: $dob")
                        break
                    }
                }
            }

            // Second pass: Extract Name and Father's Name using multiple strategies
            var nameIndex = -1
            var fatherIndex = -1

            // Strategy 1: Look for exact label matches
            for (i in lines.indices) {
                val line = lines[i]
                val upperLine = line.uppercase().trim()

                // Look for NAME label - check if the name is on the same line
                if (nameIndex == -1 && upperLine.contains("NAME") &&
                    !upperLine.contains("FATHER") &&
                    !upperLine.contains("SURNAME") &&
                    !upperLine.contains("SUR NAME")) {

                    nameIndex = i
                    Log.d("PAN_OCR", "Found NAME label at line $i: $line")

                    // Check if name is on the same line after "NAME"
                    val namePattern = Regex("NAME\\s*:?\\s*([A-Z\\s]+)", RegexOption.IGNORE_CASE)
                    namePattern.find(line)?.let { match ->
                        val extractedName = match.groupValues[1].trim()
                        if (extractedName.length > 2 &&
                            !extractedName.contains("FATHER") &&
                            !extractedName.contains("DOB")) {
                            name = extractedName
                            Log.d("PAN_OCR", "Found name on same line: $name")
                        }
                    }
                }

                // Look for Father's Name label
                if (fatherIndex == -1 &&
                    (upperLine.contains("FATHER") && upperLine.contains("NAME"))) {
                    fatherIndex = i
                    Log.d("PAN_OCR", "Found FATHER label at line $i: $line")

                    // Check if father's name is on the same line
                    val fatherPattern = Regex("FATHER'?S?\\s*NAME\\s*:?\\s*([A-Z\\s]+)", RegexOption.IGNORE_CASE)
                    fatherPattern.find(line)?.let { match ->
                        val extractedFather = match.groupValues[1].trim()
                        if (extractedFather.length > 2 &&
                            !extractedFather.contains("DOB") &&
                            !extractedFather.contains("DATE")) {
                            fatherName = extractedFather
                            Log.d("PAN_OCR", "Found father's name on same line: $fatherName")
                        }
                    }
                }
            }

            // Extract name from the line after NAME label (if not found on same line)
            if (name == null && nameIndex != -1 && nameIndex + 1 < lines.size) {
                val nextLine = lines[nameIndex + 1].trim()
                val upperNext = nextLine.uppercase()

                if (nextLine.isNotEmpty() &&
                    !upperNext.contains("FATHER") &&
                    !upperNext.contains("DOB") &&
                    !upperNext.contains("DATE") &&
                    !upperNext.contains("BIRTH") &&
                    !upperNext.contains("PERMANENT") &&
                    !panRegex.matches(upperNext) &&
                    nextLine.length > 2 &&
                    nextLine.count { it.isLetter() || it.isWhitespace() } > nextLine.length * 0.7) {
                    name = nextLine
                    Log.d("PAN_OCR", "Extracted Name from next line: $name")
                }
            }

            // Extract father's name from the line after FATHER'S NAME label (if not found on same line)
            if (fatherName == null && fatherIndex != -1 && fatherIndex + 1 < lines.size) {
                val nextLine = lines[fatherIndex + 1].trim()
                val upperNext = nextLine.uppercase()

                if (nextLine.isNotEmpty() &&
                    !upperNext.contains("DOB") &&
                    !upperNext.contains("DATE") &&
                    !upperNext.contains("BIRTH") &&
                    !upperNext.contains("PERMANENT") &&
                    !panRegex.matches(upperNext) &&
                    nextLine.length > 2 &&
                    nextLine.count { it.isLetter() || it.isWhitespace() } > nextLine.length * 0.7) {
                    fatherName = nextLine
                    Log.d("PAN_OCR", "Extracted Father's Name from next line: $fatherName")
                }
            }

            // Strategy 2: Pattern-based extraction if labels not found
            if (name == null) {
                for (i in lines.indices) {
                    val line = lines[i].trim()
                    val upperLine = line.uppercase()

                    if (isValidNameCandidate(upperLine)) {
                        name = line
                        Log.d("PAN_OCR", "Pattern-based Name: $name")
                        break
                    }
                }
            }

            val result = PanCardData(
                name = name,
                fatherName = fatherName,
                dob = dob,
                panNumber = panNumber
            )

            Log.d("PAN_OCR", "Final Result: $result")
            onResult(result)
        }
        .addOnFailureListener { e ->
            Log.e("PAN_OCR", "OCR failed: ${e.message}", e)
            onResult(PanCardData())
        }
}

// Helper function to validate name candidates
fun isValidNameCandidate(text: String): Boolean {
    return text.length > 4 &&
            text.matches(Regex("[A-Z\\s]+")) &&
            !text.contains("INCOME") &&
            !text.contains("TAX") &&
            !text.contains("DEPARTMENT") &&
            !text.contains("GOVT") &&
            !text.contains("GOVERNMENT") &&
            !text.contains("INDIA") &&
            !text.contains("PERMANENT") &&
            !text.contains("ACCOUNT") &&
            !text.contains("NUMBER") &&
            !text.contains("CARD") &&
            !text.contains("FATHER") &&
            !text.contains("NAME") &&
            !text.contains("DOB") &&
            !text.contains("DATE") &&
            !text.contains("BIRTH") &&
            !text.contains("SIGNATURE") &&
            !text.contains("PAN") &&
            text.split(" ").size >= 2 // At least two words for a full name
}   