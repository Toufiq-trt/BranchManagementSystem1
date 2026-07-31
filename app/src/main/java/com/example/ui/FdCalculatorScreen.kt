package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import com.example.util.PdfHelper
import java.text.DecimalFormat

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FdCalculatorScreen(
    viewModel: BankingViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var amountText by remember { mutableStateOf("") }
    var tenure89d by remember { mutableStateOf(true) } // true = 89 Days Special FDR, false = 1 Year
    var taxReturnYes by remember { mutableStateOf(true) } // true = Yes, false = No

    var showChartDialog by remember { mutableStateOf(false) }

    val keyboardController = LocalSoftwareKeyboardController.current

    val amount = amountText.toDoubleOrNull() ?: 0.0

    // Profit calculation:
    // Interest Rate: 89 Days Special FDR = 8.00% p.a., 1 Year = 11.00% p.a. (Banking 360-day basis for 89 days)
    val rate = if (tenure89d) 0.08 else 0.11
    val grossProfit = if (tenure89d) {
        amount * rate * (89.0 / 360.0)
    } else {
        amount * rate * 1.0
    }

    // Tax calculation:
    // Tax rate: 10% if Tax Return is Yes, 15% if No
    val taxRate = if (taxReturnYes) 0.10 else 0.15
    val taxAmount = grossProfit * taxRate

    val netInterest = grossProfit - taxAmount
    val totalMaturity = amount + netInterest

    val formatter = DecimalFormat("#,##,##0.00")

    // Chart Data Pre-computation (Tk 1 Lac to Tk 10 Lac)
    val fdAmounts = (100000..1000000 step 100000).toList()
    val headers = listOf("FDR Amount", "89D Gross", "89D Net (10%)", "89D Net (15%)", "1Y Gross", "1Y Net (10%)", "1Y Net (15%)", "1Y Monthly (10%)")

    val chartRows = fdAmounts.map { principal ->
        val p = principal.toDouble()
        val g89 = p * 0.08 * (89.0 / 360.0)
        val n89_10 = g89 * 0.90
        val n89_15 = g89 * 0.85

        val g1y = p * 0.11
        val n1y_10 = g1y * 0.90
        val n1y_15 = g1y * 0.85
        val m1y_10 = n1y_10 / 12.0

        listOf(
            "Tk ${formatter.format(p)}",
            formatter.format(g89),
            formatter.format(n89_10),
            formatter.format(n89_15),
            formatter.format(g1y),
            formatter.format(n1y_10),
            formatter.format(n1y_15),
            formatter.format(m1y_10)
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "FD CALCULATOR",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = GoldPrimary,
                        letterSpacing = 1.sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { viewModel.currentScreen = "dashboard" }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = GoldPrimary
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { showChartDialog = true }) {
                        Icon(
                            imageVector = Icons.Default.BarChart,
                            contentDescription = "FD Chart PDF",
                            tint = GoldPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = SlateDark),
                windowInsets = WindowInsets(0.dp)
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(top = innerPadding.calculateTopPadding(), start = 16.dp, end = 16.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Introductory signboard card removed as requested

            // Input Fields Card
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        "Investment Parameters",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = GoldPrimary
                    )

                    // Principal Amount Input
                    OutlinedTextField(
                        value = amountText,
                        onValueChange = { amountText = it },
                        label = { Text("FD Principal Amount") },
                        prefix = { Text("৳ ") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = GoldPrimary,
                            unfocusedBorderColor = SlateSecondary,
                            focusedLabelColor = GoldPrimary
                        )
                    )

                    // Tenure Toggle
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            "Select Tenure",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = { tenure89d = true },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (tenure89d) GoldPrimary else SlateDark,
                                    contentColor = if (tenure89d) SlateDark else Color.White
                                ),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("89 Days Special FDR", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                            }

                            Button(
                                onClick = { tenure89d = false },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (!tenure89d) GoldPrimary else SlateDark,
                                    contentColor = if (!tenure89d) SlateDark else Color.White
                                ),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("1 Year", fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    // Tax Return Status Toggle
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            "Tax Return Submitted (TIN / PSR)?",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = { taxReturnYes = true },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (taxReturnYes) GoldPrimary else SlateDark,
                                    contentColor = if (taxReturnYes) SlateDark else Color.White
                                ),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("Yes (10% Tax)", fontWeight = FontWeight.Bold)
                            }

                            Button(
                                onClick = { taxReturnYes = false },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (!taxReturnYes) GoldPrimary else SlateDark,
                                    contentColor = if (!taxReturnYes) SlateDark else Color.White
                                ),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("No (15% Tax)", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // Calculation Output Card
            Card(
                colors = CardDefaults.cardColors(containerColor = SlateDark),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "ESTIMATED EARNINGS",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = GoldPrimary
                        )
                        Icon(
                            imageVector = Icons.Default.Calculate,
                            contentDescription = null,
                            tint = GoldPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    HorizontalDivider(color = SlateSecondary.copy(alpha = 0.4f))

                    // Outputs
                    OutputRow("Principal Amount", "৳ ${formatter.format(amount)}")
                    OutputRow("Interest Rate", if (tenure89d) "8.00% p.a. (89 Days)" else "11.00% p.a.")
                    OutputRow("Gross Interest", "৳ ${formatter.format(grossProfit)}", isBold = true)
                    OutputRow("AIT / Tax Deducted (${if (taxReturnYes) "10%" else "15%"})", "৳ ${formatter.format(taxAmount)}", color = RedAccent)
                    
                    if (tenure89d) {
                        OutputRow("after 89 days total profit after tax deduct:", "৳ ${formatter.format(netInterest)}", color = GreenAccent, isBold = true)
                    } else {
                        OutputRow("Monthly Gain after Tax Deduct:", "৳ ${formatter.format(netInterest / 12.0)}", color = GreenAccent, isBold = true)
                    }

                    HorizontalDivider(color = SlateSecondary.copy(alpha = 0.4f))

                    OutputRow("Net Interest Profit", "৳ ${formatter.format(netInterest)}", color = GreenAccent, isBold = true)
                    OutputRow("Total Maturity Value", "৳ ${formatter.format(totalMaturity)}", color = GoldLight, isBold = true, fontSize = 16.sp)
                }
            }
        }

        if (showChartDialog) {
            AlertDialog(
                onDismissRequest = { showChartDialog = false },
                title = {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("FD PROFIT CHART", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = GoldPrimary)
                        IconButton(onClick = { showChartDialog = false }) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.Gray)
                        }
                    }
                },
                text = {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            "Fixed Deposit Profit Breakdown (Tk 1 Lac - Tk 10 Lac)",
                            fontSize = 11.sp,
                            color = Color.Gray
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(320.dp)
                                .background(Color(0xFF0F172A), RoundedCornerShape(8.dp))
                                .padding(4.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .horizontalScroll(rememberScrollState())
                                    .verticalScroll(rememberScrollState())
                            ) {
                                // Header Row
                                Row(
                                    modifier = Modifier
                                        .background(SlateDark)
                                        .padding(vertical = 6.dp, horizontal = 4.dp)
                                ) {
                                    headers.forEach { h ->
                                        Text(
                                            text = h,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = GoldLight,
                                            modifier = Modifier.width(85.dp),
                                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                        )
                                    }
                                }
                                HorizontalDivider(color = Color.Gray.copy(alpha = 0.3f))
                                // Data Rows
                                chartRows.forEachIndexed { idx, row ->
                                    Row(
                                        modifier = Modifier
                                            .background(if (idx % 2 == 1) Color.White.copy(alpha = 0.05f) else Color.Transparent)
                                            .padding(vertical = 4.dp, horizontal = 4.dp)
                                    ) {
                                        row.forEachIndexed { cIdx, cell ->
                                            Text(
                                                text = cell,
                                                fontSize = 9.sp,
                                                color = if (cIdx == 0) GoldPrimary else Color.White,
                                                fontWeight = if (cIdx == 0) FontWeight.Bold else FontWeight.Normal,
                                                modifier = Modifier.width(85.dp),
                                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            showChartDialog = false
                            PdfHelper.generateCustomChartPdf(
                                context = context,
                                fileName = "FD_Profit_Chart.pdf",
                                chartTitle = "FIXED DEPOSIT (FDR) PROFIT CHART",
                                chartSubtitle = "89 Days Special FDR (8.00% p.a.) vs 1 Year FDR (11.00% p.a.)",
                                headers = headers,
                                rows = chartRows
                            )
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary, contentColor = SlateDark)
                    ) {
                        Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Download PDF Chart", fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showChartDialog = false }) {
                        Text("Close")
                    }
                }
            )
        }
    }
}

@Composable
fun OutputRow(
    label: String,
    value: String,
    color: Color = Color.White,
    isBold: Boolean = false,
    fontSize: androidx.compose.ui.unit.TextUnit = 13.sp
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontSize = fontSize,
            color = Color.LightGray,
            fontWeight = if (isBold) FontWeight.SemiBold else FontWeight.Normal,
            modifier = Modifier.weight(1f, fill = false),
            maxLines = 2
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = value,
            fontSize = fontSize,
            fontWeight = if (isBold) FontWeight.Bold else FontWeight.Normal,
            color = color,
            maxLines = 1,
            softWrap = false
        )
    }
}
