package com.example.panocr

import android.Manifest
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import java.io.IOException

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            PanOCRScreen()
        }
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
    var panData by remember { mutableStateOf<PanCardData?>(null) }
    var isProcessing by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val recognizer =
        remember { TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS) }

    // ✅ Camera launcher (returns Bitmap directly)
    val cameraLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.TakePicturePreview()) { bitmap ->
            bitmap?.let {
                isProcessing = true
                errorMessage = null
                val image = InputImage.fromBitmap(it, 0)
                processImage(image, recognizer) { result ->
                    panData = result
                    isProcessing = false
                }
            } ?: run {
                errorMessage = "Failed to capture image"
                isProcessing = false
            }
        }

    // ✅ Camera permission
    val permissionLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) {
                cameraLauncher.launch(null)
            } else {
                errorMessage = "Camera permission denied"
            }
        }

    // ✅ Gallery launcher (URI)
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

    MaterialTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
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
                        permissionLauncher.launch(Manifest.permission.CAMERA)
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("📷 Scan PAN using Camera")
                }

                Button(
                    onClick = {
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



fun processImage(
    image: InputImage,
    recognizer: com.google.mlkit.vision.text.TextRecognizer,
    onResult: (PanCardData) -> Unit
) {
    recognizer.process(image)
        .addOnSuccessListener { visionText ->

            // Extract all text lines
            val lines = mutableListOf<String>()
            visionText.textBlocks.forEach { block ->
                block.lines.forEach { line ->
                    lines.add(line.text.trim())
                }
            }

            // Log all extracted text for debugging
            Log.d("PAN_OCR", "=== Extracted Text ===")
            lines.forEachIndexed { index, line ->
                Log.d("PAN_OCR", "Line $index: $line")
            }
            Log.d("PAN_OCR", "=====================")

            // Enhanced regex patterns
            val panRegex = Regex("[A-Z]{5}[0-9]{4}[A-Z]")
            val dobRegex = Regex("\\b(0[1-9]|[12][0-9]|3[01])[/-](0[1-9]|1[0-2])[/-](19|20)?\\d{2}\\b")

            var name: String? = null
            var fatherName: String? = null
            var dob: String? = null
            var panNumber: String? = null

            // First pass: Extract PAN number and DOB using regex
            for (line in lines) {
                val upperLine = line.uppercase()

                // Extract PAN number
                if (panNumber == null && panRegex.containsMatchIn(upperLine)) {
                    panNumber = panRegex.find(upperLine)?.value
                    Log.d("PAN_OCR", "Found PAN: $panNumber")
                }

                // Extract Date of Birth
                if (dob == null && dobRegex.containsMatchIn(line)) {
                    dob = dobRegex.find(line)?.value
                    Log.d("PAN_OCR", "Found DOB: $dob")
                }
            }

            // Second pass: Extract Name and Father's Name
            var foundNameLabel = false
            var foundFatherLabel = false
            var nameIndex = -1
            var fatherIndex = -1

            for (i in lines.indices) {
                val line = lines[i]
                val upperLine = line.uppercase()

                // Look for NAME label (but not "FATHER'S NAME" or "SURNAME")
                if (!foundNameLabel && upperLine == "NAME" &&
                    (lines.getOrNull(i - 1)?.uppercase()?.contains("FATHER") != true) &&
                    (lines.getOrNull(i - 1)?.uppercase()?.contains("SUR") != true)) {
                    foundNameLabel = true
                    nameIndex = i
                    Log.d("PAN_OCR", "Found NAME label at line $i")
                }

                // Look for Father's Name label
                if (!foundFatherLabel && (
                    upperLine.contains("FATHER") && upperLine.contains("NAME") ||
                    upperLine == "FATHER'S NAME" ||
                    upperLine == "FATHERS NAME")) {
                    foundFatherLabel = true
                    fatherIndex = i
                    Log.d("PAN_OCR", "Found FATHER label at line $i")
                }
            }

            // Extract name from the line after NAME label
            if (nameIndex != -1 && nameIndex + 1 < lines.size) {
                val nextLine = lines[nameIndex + 1].trim()
                // Skip if it's a label or PAN number
                if (!nextLine.uppercase().contains("FATHER") &&
                    !nextLine.uppercase().contains("DOB") &&
                    !nextLine.uppercase().contains("DATE") &&
                    !panRegex.matches(nextLine.uppercase()) &&
                    nextLine.length > 2) {
                    name = nextLine
                    Log.d("PAN_OCR", "Extracted Name: $name")
                }
            }

            // Extract father's name from the line after FATHER'S NAME label
            if (fatherIndex != -1 && fatherIndex + 1 < lines.size) {
                val nextLine = lines[fatherIndex + 1].trim()
                // Skip if it's a label or PAN number
                if (!nextLine.uppercase().contains("DOB") &&
                    !nextLine.uppercase().contains("DATE") &&
                    !nextLine.uppercase().contains("BIRTH") &&
                    !panRegex.matches(nextLine.uppercase()) &&
                    nextLine.length > 2) {
                    fatherName = nextLine
                    Log.d("PAN_OCR", "Extracted Father's Name: $fatherName")
                }
            }

            // Alternative: If labels not found, try pattern-based extraction
            if (name == null) {
                // Look for a line that appears to be a name (typically all caps, alphabetic)
                for (i in lines.indices) {
                    val line = lines[i].trim()
                    val upperLine = line.uppercase()

                    if (upperLine.length > 4 &&
                        upperLine.matches(Regex("[A-Z\\s]+")) &&
                        !upperLine.contains("INCOME") &&
                        !upperLine.contains("TAX") &&
                        !upperLine.contains("DEPARTMENT") &&
                        !upperLine.contains("GOVT") &&
                        !upperLine.contains("INDIA") &&
                        !upperLine.contains("PERMANENT") &&
                        !upperLine.contains("ACCOUNT") &&
                        !upperLine.contains("NUMBER") &&
                        !upperLine.contains("CARD") &&
                        !upperLine.contains("FATHER") &&
                        !upperLine.contains("NAME") &&
                        !upperLine.contains("DOB") &&
                        !upperLine.contains("DATE") &&
                        name == null) {
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
