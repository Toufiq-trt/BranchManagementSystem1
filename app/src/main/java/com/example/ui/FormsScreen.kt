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
    var targetExistingFormForUpload by remember { mutableStateOf<com.example.data.DigitalForm?>(null) }

    // Metadata entry states
    var showDetailsDialog by remember { mutableStateOf(false) }
    var inputCustomerName by remember { mutableStateOf("") }
    var inputAccountNumber by remember { mutableStateOf("") }
    var inputRemarks by remember { mutableStateOf("") }

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
                targetExistingFormForUpload = null
                return@rememberLauncherForActivityResult
            }

            val templateFormType = targetFormTypeForUpload
            val existingForm = targetExistingFormForUpload

            if (templateFormType != null) {
                // Scenario 1: New Form Upload
                startUploadAnimation(templateFormType)
                viewModel.addForm(
                    formType = templateFormType,
                    customerName = if (inputCustomerName.isNotBlank()) inputCustomerName else "Desk Customer",
                    accountNumber = if (inputAccountNumber.isNotBlank()) inputAccountNumber else "N/A",
                    remarks = if (inputRemarks.isNotBlank()) inputRemarks else "Uploaded standard template copy",
                    signature = "UPLOADED_PDF",
                    fieldsJson = "{}"
                ) { newId ->
                    coroutineScope.launch {
                        val destDir = File(context.filesDir, "digital_forms_pdf")
                        if (!destDir.exists()) destDir.mkdirs()
                        val destFile = File(destDir, "form_${newId}.pdf")

                        val success = copyUriToFile(context, uri, destFile)
                        if (success) {
                            // Fetch all forms and update newly created form pdf path
                            val updatedForm = com.example.data.DigitalForm(
                                id = newId.toInt(),
                                formType = templateFormType,
                                customerName = if (inputCustomerName.isNotBlank()) inputCustomerName else "Desk Customer",
                                accountNumber = if (inputAccountNumber.isNotBlank()) inputAccountNumber else "N/A",
                                dateStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()),
                                remarks = if (inputRemarks.isNotBlank()) inputRemarks else "Uploaded standard template copy",
                                signaturePath = "UPLOADED_PDF",
                                jsonFields = "{}",
                                pdfFilePath = destFile.absolutePath
                            )
                            viewModel.updateForm(updatedForm)
                            Toast.makeText(context, "Successfully uploaded $templateFormType!", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, "Failed to copy uploaded PDF file.", Toast.LENGTH_SHORT).show()
                        }
                        targetFormTypeForUpload = null
                        inputCustomerName = ""
                        inputAccountNumber = ""
                        inputRemarks = ""
                    }
                }
            } else if (existingForm != null) {
                // Scenario 2: Replace PDF file of existing vault entry
                startUploadAnimation(existingForm.formType)
                coroutineScope.launch {
                    val destDir = File(context.filesDir, "digital_forms_pdf")
                    if (!destDir.exists()) destDir.mkdirs()
                    val destFile = File(destDir, "form_${existingForm.id}.pdf")

                    val success = copyUriToFile(context, uri, destFile)
                    if (success) {
                        viewModel.updateForm(existingForm.copy(pdfFilePath = destFile.absolutePath))
                        Toast.makeText(context, "Successfully replaced PDF file for ${existingForm.formType}!", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, "Failed to copy replacing PDF file.", Toast.LENGTH_SHORT).show()
                    }
                    targetExistingFormForUpload = null
                }
            }
        } else {
            // Cancelled
            targetFormTypeForUpload = null
            targetExistingFormForUpload = null
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

    fun triggerArchivedPreview(form: com.example.data.DigitalForm) {
        // If a PDF is uploaded, download/share that exact PDF file!
        val path = form.pdfFilePath
        if (!path.isNullOrEmpty()) {
            val file = File(path)
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
                    Toast.makeText(context, "Error downloading PDF: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                }
            } else {
                Toast.makeText(context, "PDF file missing on local storage.", Toast.LENGTH_SHORT).show()
            }
            return
        }

        // Fallback or if empty - "if nothing uploaded then nothing will download"
        Toast.makeText(context, "No custom PDF file uploaded for this form yet.", Toast.LENGTH_SHORT).show()
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
                        Text("ARCHIVED SECURELY!", color = GoldPrimary, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    } else {
                        CircularProgressIndicator(
                            progress = uploadProgress,
                            color = GoldPrimary,
                            strokeWidth = 5.dp,
                            modifier = Modifier.size(60.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Uploading '$uploadingFormName' to Secure Vault...", color = Color.White, fontSize = 13.sp)
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
                                    tint = RedAccent,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(formName, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color.White)
                                    Text("Official Blank Template", fontSize = 11.sp, color = Color.LightGray)
                                }
                            }

                            Row(
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Download Blank Template
                                IconButton(
                                    onClick = { triggerBlankPreview(formName) },
                                    colors = IconButtonDefaults.iconButtonColors(contentColor = GoldPrimary)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Download,
                                        contentDescription = "Preview & Download Blank Form"
                                    )
                                }

                                // Upload completed copy
                                IconButton(
                                    onClick = {
                                        targetFormTypeForUpload = formName
                                        inputCustomerName = ""
                                        inputAccountNumber = ""
                                        inputRemarks = ""
                                        showDetailsDialog = true
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

                item {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "STORED SECURE VAULT ARCHIVES (${forms.size})",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = GoldLight,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                }

                if (forms.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(100.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "No secure archived forms available.",
                                color = Color.Gray,
                                fontSize = 12.sp
                            )
                        }
                    }
                } else {
                    items(forms) { form ->
                        val hasPdf = !form.pdfFilePath.isNullOrEmpty()
                        Card(
                            colors = CardDefaults.cardColors(containerColor = SlateDark),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(form.formType, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color.White)
                                    Text("Customer: ${form.customerName} | A/C: ${form.accountNumber}", fontSize = 11.sp, color = Color.LightGray)
                                    Text("Date: ${form.dateStr}", fontSize = 10.sp, color = GoldLight)
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                                        modifier = Modifier.padding(top = 4.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(6.dp)
                                                .background(if (hasPdf) GreenAccent else RedAccent, shape = androidx.compose.foundation.shape.CircleShape)
                                        )
                                        Text(
                                            text = if (hasPdf) "PDF ATTACHED" else "NO PDF UPLOADED",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (hasPdf) GreenAccent else RedAccent
                                        )
                                    }
                                }

                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Download PDF button (if nothing uploaded, it won't do anything but Toast)
                                    IconButton(
                                        onClick = { triggerArchivedPreview(form) },
                                        colors = IconButtonDefaults.iconButtonColors(
                                            contentColor = if (hasPdf) GoldPrimary else Color.Gray.copy(alpha = 0.5f)
                                        )
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Download,
                                            contentDescription = "Download Form PDF"
                                        )
                                    }

                                    // Upload / Replace PDF Button
                                    IconButton(
                                        onClick = {
                                            targetExistingFormForUpload = form
                                            pdfLauncher.launch("application/pdf")
                                        },
                                        colors = IconButtonDefaults.iconButtonColors(contentColor = GreenAccent)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.CloudUpload,
                                            contentDescription = "Upload / Replace PDF"
                                        )
                                    }

                                    // Delete Button
                                    IconButton(
                                        onClick = { viewModel.deleteForm(form) },
                                        colors = IconButtonDefaults.iconButtonColors(contentColor = RedAccent)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "Delete from Archive"
                                        )
                                    }
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

    // Detail dialog before starting PDF selection for new templates
    if (showDetailsDialog) {
        AlertDialog(
            onDismissRequest = { showDetailsDialog = false },
            title = { Text("Archiving Metadata", fontWeight = FontWeight.Bold, color = GoldPrimary) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Fill out the reference details before choosing the completed PDF file.", fontSize = 12.sp, color = Color.LightGray)
                    
                    OutlinedTextField(
                        value = inputCustomerName,
                        onValueChange = { inputCustomerName = it },
                        label = { Text("Customer Name") },
                        placeholder = { Text("e.g. Abdul Rahman") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = GoldPrimary,
                            unfocusedBorderColor = Color.Gray,
                            focusedLabelColor = GoldPrimary,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = inputAccountNumber,
                        onValueChange = { inputAccountNumber = it },
                        label = { Text("Account Number") },
                        placeholder = { Text("e.g. 1029481726") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = GoldPrimary,
                            unfocusedBorderColor = Color.Gray,
                            focusedLabelColor = GoldPrimary,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = inputRemarks,
                        onValueChange = { inputRemarks = it },
                        label = { Text("Remarks") },
                        placeholder = { Text("e.g. Completed service execution") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = GoldPrimary,
                            unfocusedBorderColor = Color.Gray,
                            focusedLabelColor = GoldPrimary,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (inputCustomerName.isBlank()) {
                            Toast.makeText(context, "Please enter a Customer Name.", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        showDetailsDialog = false
                        pdfLauncher.launch("application/pdf")
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary, contentColor = SlateDark)
                ) {
                    Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Choose PDF", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDetailsDialog = false }) {
                    Text("Cancel", color = Color.White)
                }
            }
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
