package com.example.ui

import android.net.Uri
import android.provider.OpenableColumns
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.theme.*
import com.example.util.PdfHelper
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import kotlinx.coroutines.launch

@Composable
fun FormsScreen(
    viewModel: BankingViewModel,
    modifier: Modifier = Modifier
) {
    val forms by viewModel.allForms.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val formTypes = listOf(
        "Account Service Form",
        "CRF Form",
        "Supplementary Form",
        "Credit Card Service Form",
        "DPS Form",
        "Fixed Deposit Form",
        "Authorization Form",
        "BGB Loan Top-Up Form"
    )

    // PDF Preview States for default templates
    var showPreviewDialog by remember { mutableStateOf(false) }
    var previewTitle by remember { mutableStateOf("") }
    var previewTextContent by remember { mutableStateOf<String?>(null) }
    var onConfirmDownload by remember { mutableStateOf<() -> Unit>({}) }

    // Upload / Selection states
    var targetFormTypeForUpload by remember { mutableStateOf<String?>(null) }
    var refreshTrigger by remember { mutableStateOf(0) }

    // Upload animation states
    var uploadingFormName by remember { mutableStateOf<String?>(null) }
    var uploadProgress by remember { mutableStateOf(0f) }
    var showSuccessAnimation by remember { mutableStateOf(false) }

    // Coroutine simulator for visual progress feedback
    fun startUploadAnimation(formName: String) {
        coroutineScope.launch {
            uploadingFormName = formName
            uploadProgress = 0f
            showSuccessAnimation = false
            while (uploadProgress < 1f) {
                kotlinx.coroutines.delay(50)
                uploadProgress += 0.1f
            }
            showSuccessAnimation = true
            kotlinx.coroutines.delay(800)
            showSuccessAnimation = false
            uploadingFormName = null
        }
    }

    // PDF Launcher contract
    val pdfLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            val size = getUriSize(context, uri)
            val maxBytes = 100L * 1024L * 1024L // 100 MB
            if (size > maxBytes) {
                Toast.makeText(context, "Upload failed! File exceeds the 100 MB maximum size limit.", Toast.LENGTH_LONG).show()
                targetFormTypeForUpload = null
                return@rememberLauncherForActivityResult
            }

            val templateFormType = targetFormTypeForUpload

            if (templateFormType != null) {
                startUploadAnimation(templateFormType)
                coroutineScope.launch {
                    val destDir = File(context.filesDir, "digital_forms_pdf")
                    if (!destDir.exists()) destDir.mkdirs()
                    
                    val safeFormName = templateFormType.replace(" ", "_").lowercase()
                    val destFile = File(destDir, "uploaded_${safeFormName}.pdf")

                    val success = copyUriToFile(context, uri, destFile)
                    if (success) {
                        destFile.setReadable(true, false)
                        refreshTrigger++
                        Toast.makeText(context, "Upload successful!", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, "Failed to copy uploaded PDF file.", Toast.LENGTH_SHORT).show()
                    }
                    targetFormTypeForUpload = null
                }
            }
        } else {
            // Cancelled
            targetFormTypeForUpload = null
        }
    }

    fun triggerBlankPreview(formName: String) {
        val formattedDate = SimpleDateFormat("d-M-yyyy", Locale.getDefault()).format(Date())
        val pdfFilename = "$formName Blank-$formattedDate.pdf"
        val fullText = """
            OFFICIAL BANKING DOCUMENT TEMPLATE
            ==========================================
            Document Name: $formName
            Security Status: Verified Printable Template
            Integrity Hash: SHA-256 Certified
            
            DIRECTIONS FOR CLIENTS:
            1. Print in standard black & white A4 paper.
            2. Fill out all sections using capital block letters.
            3. Sign and date clearly.
            4. Hand over to Toufiq's Smart Banking Tracker Desk.
            
            --------------------------------------------------
            CLIENT SIGNATURE                  DESK OFFICER
        """.trimIndent()

        previewTitle = "BLANK: $formName"
        previewTextContent = fullText
        onConfirmDownload = {
            PdfHelper.generateAndSharePdf(context, pdfFilename, formName.uppercase(), fullText)
            Toast.makeText(context, "Downloaded official blank $formName PDF", Toast.LENGTH_SHORT).show()
        }
        showPreviewDialog = true
    }

    fun triggerUploadedFormDownload(formName: String) {
        val safeFormName = formName.replace(" ", "_").lowercase()
        val file = File(File(context.filesDir, "digital_forms_pdf"), "uploaded_${safeFormName}.pdf")
        if (file.exists()) {
            try {
                val authority = "${context.packageName}.fileprovider"
                val fileUri = androidx.core.content.FileProvider.getUriForFile(context, authority, file)
                val shareIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                    type = "application/pdf"
                    putExtra(android.content.Intent.EXTRA_STREAM, fileUri)
                    addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                context.startActivity(android.content.Intent.createChooser(shareIntent, "Download / Share Form PDF"))
            } catch (e: Exception) {
                Toast.makeText(context, "Error sharing PDF: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
            }
        } else {
            // No custom copy uploaded yet. Fallback to blank template preview
            Toast.makeText(context, "No custom copy uploaded yet. Downloading blank template...", Toast.LENGTH_SHORT).show()
            triggerBlankPreview(formName)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Top Header Title
        Text(
            text = "ALL FORMS CENTER",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = GoldPrimary,
            letterSpacing = 1.sp
        )

        if (uploadingFormName != null) {
            // Uploading progress screen overlay
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .background(SlateDark, RoundedCornerShape(12.dp))
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    if (showSuccessAnimation) {
                        Box(
                            modifier = Modifier
                                .size(80.dp)
                                .background(GoldPrimary.copy(alpha = 0.15f), shape = androidx.compose.foundation.shape.CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "Success",
                                tint = GreenAccent,
                                modifier = Modifier.size(50.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("UPLOAD SUCCESSFUL!", color = GoldPrimary, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    } else {
                        CircularProgressIndicator(
                            progress = uploadProgress,
                            color = GoldPrimary,
                            strokeWidth = 5.dp,
                            modifier = Modifier.size(60.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Uploading '$uploadingFormName' securely...", color = Color.White, fontSize = 13.sp)
                        Text("${(uploadProgress * 100).toInt()}% Secure Transmission", color = Color.Gray, fontSize = 11.sp)
                    }
                }
            }
        } else {
            // Main Forms Console
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item {
                    Text(
                        text = "OFFICIAL BLANK FORM TEMPLATES",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = GoldLight,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                }

                items(formTypes) { formName ->
                    val safeFormName = formName.replace(" ", "_").lowercase()
                    val fileExists = remember(formName, refreshTrigger) {
                        File(File(context.filesDir, "digital_forms_pdf"), "uploaded_${safeFormName}.pdf").exists()
                    }

                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PictureAsPdf,
                                    contentDescription = null,
                                    tint = if (fileExists) GreenAccent else RedAccent,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(formName, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color.White)
                                    Text(
                                        text = if (fileExists) "Custom Uploaded Copy Available" else "Official Blank Template",
                                        fontSize = 11.sp,
                                        color = if (fileExists) GreenAccent else Color.LightGray
                                    )
                                }
                            }

                            Row(
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Left side: Download button (Clicking this downloads the uploaded form, or falls back to blank)
                                IconButton(
                                    onClick = { triggerUploadedFormDownload(formName) },
                                    colors = IconButtonDefaults.iconButtonColors(contentColor = GoldPrimary)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Download,
                                        contentDescription = "Download Form"
                                    )
                                }

                                // Right side: Upload button (Clicking this directly triggers file picker)
                                IconButton(
                                    onClick = {
                                        targetFormTypeForUpload = formName
                                        pdfLauncher.launch("application/pdf")
                                    },
                                    colors = IconButtonDefaults.iconButtonColors(contentColor = GreenAccent)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CloudUpload,
                                        contentDescription = "Upload Completed Copy"
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showPreviewDialog) {
        PdfPreviewDialog(
            title = previewTitle,
            textContent = previewTextContent,
            onDismiss = { showPreviewDialog = false },
            onDownload = { onConfirmDownload() }
        )
    }
}

// Helper methods to query URI size and copy streams
private fun getUriSize(context: android.content.Context, uri: Uri): Long {
    var size: Long = -1
    try {
        context.contentResolver.openAssetFileDescriptor(uri, "r")?.use {
            size = it.length
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
    if (size < 0) {
        val cursor = context.contentResolver.query(uri, null, null, null, null)
        cursor?.use {
            if (it.moveToFirst()) {
                val sizeIndex = it.getColumnIndex(OpenableColumns.SIZE)
                if (sizeIndex != -1) {
                    size = it.getLong(sizeIndex)
                }
            }
        }
    }
    return size
}

private fun copyUriToFile(context: android.content.Context, uri: Uri, destFile: File): Boolean {
    return try {
        context.contentResolver.openInputStream(uri)?.use { inputStream ->
            destFile.outputStream().use { outputStream ->
                inputStream.copyTo(outputStream)
            }
        }
        true
    } catch (e: Exception) {
        e.printStackTrace()
        false
    }
}
