package com.example.ui

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.background
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
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun FormsScreen(
    viewModel: BankingViewModel,
    modifier: Modifier = Modifier
) {
    val forms by viewModel.allForms.collectAsStateWithLifecycle()
    val context = LocalContext.current

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

    // PDF Preview States
    var showPreviewDialog by remember { mutableStateOf(false) }
    var previewTitle by remember { mutableStateOf("") }
    var previewTextContent by remember { mutableStateOf<String?>(null) }
    var onConfirmDownload by remember { mutableStateOf<() -> Unit>({}) }

    // Upload animation states
    var uploadingFormName by remember { mutableStateOf<String?>(null) }
    var uploadProgress by remember { mutableStateOf(0f) }
    var showSuccessAnimation by remember { mutableStateOf(false) }

    LaunchedEffect(uploadingFormName) {
        if (uploadingFormName != null) {
            uploadProgress = 0f
            showSuccessAnimation = false
            while (uploadProgress < 1f) {
                kotlinx.coroutines.delay(60)
                uploadProgress += 0.1f
            }
            showSuccessAnimation = true
            kotlinx.coroutines.delay(1000)
            showSuccessAnimation = false
            uploadingFormName = null
        }
    }

    fun triggerBlankPreview(formName: String) {
        val formattedDate = java.text.SimpleDateFormat("d-M-yyyy", java.util.Locale.getDefault()).format(java.util.Date())
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

    fun triggerArchivedPreview(formType: String, dateStr: String, id: Int) {
        val formattedDate = try {
            val parts = dateStr.split("-")
            if (parts.size == 3) {
                if (parts[0].length == 4) {
                    val d = parts[2].toInt().toString()
                    val m = parts[1].toInt().toString()
                    val y = parts[0]
                    "$d-$m-$y"
                } else {
                    val d = parts[0].toInt().toString()
                    val m = parts[1].toInt().toString()
                    val y = parts[2]
                    "$d-$m-$y"
                }
            } else {
                dateStr.replace(" ", "-").replace("/", "-")
            }
        } catch (e: Exception) {
            dateStr.replace(" ", "-").replace("/", "-")
        }
        val pdfFilename = "$formType Archived-$formattedDate.pdf"
        val fullText = """
            SECURE CLOUD-ARCHIVED DOCUMENT SLIP
            ==========================================
            Document Name: $formType
            Archive reference ID: FORM-VAULT-$id
            Registered Upload Date: $dateStr
            In-File Attributes: [No Client Pre-Fill Injected]
            
            SECURITY STATS:
            - Database Status: SECURED & PERSISTED
            - Transmittal State: Offline-Verified Ready
            - Integrity Seal: Certified Digital Storage
            
            --------------------------------------------------
            AUTHORIZED AUDIT LOG               SYSTEM SEAL
        """.trimIndent()

        previewTitle = "ARCHIVED: $formType"
        previewTextContent = fullText
        onConfirmDownload = {
            PdfHelper.generateAndSharePdf(context, pdfFilename, formType.uppercase(), fullText)
            Toast.makeText(context, "Downloaded archived $formType PDF", Toast.LENGTH_SHORT).show()
        }
        showPreviewDialog = true
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
                                    Text("Unfilled Desk Template", fontSize = 11.sp, color = Color.LightGray)
                                }
                            }

                            Row(
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Download Icon (Triggers Preview Dialog first)
                                IconButton(
                                    onClick = { triggerBlankPreview(formName) },
                                    colors = IconButtonDefaults.iconButtonColors(contentColor = GoldPrimary)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Download,
                                        contentDescription = "Preview & Download Blank Form"
                                    )
                                }

                                // Upload Icon (Triggers Instant Simulated Upload with no fields to fill)
                                IconButton(
                                    onClick = {
                                        viewModel.addForm(
                                            formType = formName,
                                            customerName = "N/A",
                                            accountNumber = "N/A",
                                            remarks = "Uploaded secure desk copy directly from console",
                                            signature = "NOT_SIGNED",
                                            fieldsJson = "{}"
                                        )
                                        uploadingFormName = formName
                                    },
                                    colors = IconButtonDefaults.iconButtonColors(contentColor = GreenAccent)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CloudUpload,
                                        contentDescription = "Upload Copy"
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
                                    Text("Archived: ${form.dateStr}", fontSize = 11.sp, color = GoldLight)
                                    Text("Database Status: SECURED", fontSize = 10.sp, color = GreenAccent, fontWeight = FontWeight.Bold)
                                }

                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Preview/Download Button
                                    IconButton(
                                        onClick = { triggerArchivedPreview(form.formType, form.dateStr, form.id) },
                                        colors = IconButtonDefaults.iconButtonColors(contentColor = GoldPrimary)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Download,
                                            contentDescription = "Preview and Download Archived Form"
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
}
