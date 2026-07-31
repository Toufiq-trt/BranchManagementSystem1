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
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import com.example.util.PdfHelper
import java.text.DecimalFormat
import kotlin.math.pow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DpsCalculatorScreen(
    viewModel: BankingViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var monthlyDepositText by remember { mutableStateOf("") }
    var interestText by remember { mutableStateOf("7.5") }
    var tenureText by remember { mutableStateOf("5") } // Default 5 years
    var taxReturnYes by remember { mutableStateOf(true) } // true = Yes (10% tax), false = No (15% tax)

    var showChartDialog by remember { mutableStateOf(false) }

    val monthlyDeposit = monthlyDepositText.toDoubleOrNull() ?: 0.0
    val interestRate = interestText.toDoubleOrNull() ?: 0.0
    val tenureYears = tenureText.toIntOrNull() ?: 0

    val n = tenureYears * 12
    val i = (interestRate / 12.0) / 100.0

    // DPS Compound Formula: M = P * [ (1 + i)^n - 1 ] / i * (1 + i)
    val maturityValue = if (monthlyDeposit > 0.0 && n > 0) {
        if (i > 0.0) {
            monthlyDeposit * ((1.0 + i).pow(n.toDouble()) - 1.0) / i * (1.0 + i)
        } else {
            monthlyDeposit * n
        }
    } else {
        0.0
    }

    val totalSavings = monthlyDeposit * n
    val profit = if (maturityValue > totalSavings) maturityValue - totalSavings else 0.0

    val taxRate = if (taxReturnYes) 0.10 else 0.15
    val taxAmount = profit * taxRate
    val pocketAmount = maturityValue - taxAmount

    val formatter = DecimalFormat("#,##,##0.00")

    // Chart Data Pre-computation
    fun calcDpsMat(p: Double, rate: Double, years: Int): Double {
        val count = years * 12
        val rateMonthly = (rate / 12.0) / 100.0
        return if (rateMonthly > 0) p * ((1.0 + rateMonthly).pow(count.toDouble()) - 1.0) / rateMonthly * (1.0 + rateMonthly) else p * count
    }

    val depositAmounts = (500..10000 step 500).toList()
    val headers = listOf("Deposit (Tk)", "3Y (10%)", "3Y (10.5%)", "5Y (10%)", "5Y (10.5%)", "7Y (10%)", "7Y (10.5%)", "10Y (10%)", "10Y (10.5%)")

    val chartRows = depositAmounts.map { dep ->
        val p = dep.toDouble()
        val v3g = calcDpsMat(p, 10.0, 3)
        val v3w = calcDpsMat(p, 10.5, 3)
        val v5g = calcDpsMat(p, 10.0, 5)
        val v5w = calcDpsMat(p, 10.5, 5)
        val v7g = calcDpsMat(p, 10.0, 7)
        val v7w = calcDpsMat(p, 10.5, 7)
        val v10g = calcDpsMat(p, 10.0, 10)
        val v10w = calcDpsMat(p, 10.5, 10)

        listOf(
            "Tk ${formatter.format(p)}",
            formatter.format(v3g),
            formatter.format(v3w),
            formatter.format(v5g),
            formatter.format(v5w),
            formatter.format(v7g),
            formatter.format(v7w),
            formatter.format(v10g),
            formatter.format(v10w)
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "DPS CALCULATOR",
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
                            contentDescription = "DPS Chart PDF",
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
                .padding(top = innerPadding.calculateTopPadding(), start = 16.dp, end = 16.dp, bottom = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Introductory signboard removed as requested
            
            // Inputs Card
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        "Deposit Parameters",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = GoldPrimary
                    )

                    // Monthly Deposit
                    OutlinedTextField(
                        value = monthlyDepositText,
                        onValueChange = { monthlyDepositText = it },
                        label = { Text("Monthly Deposit Installment") },
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

                    // Annual Interest Rate
                    OutlinedTextField(
                        value = interestText,
                        onValueChange = { interestText = it },
                        label = { Text("DPS Annual Interest Rate") },
                        suffix = { Text("%") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = GoldPrimary,
                            unfocusedBorderColor = SlateSecondary,
                            focusedLabelColor = GoldPrimary
                        )
                    )

                    // Tenure (Years) and Tax Return side-by-side
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = tenureText,
                            onValueChange = { tenureText = it },
                            label = { Text("Tenure") },
                            suffix = { Text("Yrs") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = GoldPrimary,
                                unfocusedBorderColor = SlateSecondary,
                                focusedLabelColor = GoldPrimary
                            )
                        )

                        Column(
                            modifier = Modifier.weight(1.5f),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                "Tax Return Submitted?",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Button(
                                    onClick = { taxReturnYes = true },
                                    modifier = Modifier.weight(1f),
                                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (taxReturnYes) GoldPrimary else SlateDark,
                                        contentColor = if (taxReturnYes) SlateDark else Color.White
                                    ),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text("YES (10%)", fontWeight = FontWeight.Bold, fontSize = 10.sp)
                                }

                                Button(
                                    onClick = { taxReturnYes = false },
                                    modifier = Modifier.weight(1f),
                                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (!taxReturnYes) GoldPrimary else SlateDark,
                                        contentColor = if (!taxReturnYes) SlateDark else Color.White
                                    ),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text("NO (15%)", fontWeight = FontWeight.Bold, fontSize = 10.sp)
                                }
                            }
                        }
                    }
                }
            }

            // Calculations Output Card
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
                            "ESTIMATED CUMULATIVE SAVINGS",
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

                    OutputRow("Total Savings:", "৳ ${formatter.format(totalSavings)}")
                    OutputRow("Compund Interest Earned :", "৳ ${formatter.format(profit)}", color = GreenAccent)
                    OutputRow("Maturity Value:", "৳ ${formatter.format(maturityValue)}")
                    OutputRow("AIT /Tax:", "৳ ${formatter.format(taxAmount)}", color = RedAccent)
                    
                    HorizontalDivider(color = SlateSecondary.copy(alpha = 0.4f))

                    OutputRow("Pocket Amount:", "৳ ${formatter.format(pocketAmount)}", color = GoldLight, isBold = true, fontSize = 16.sp)
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
                        Text("DPS MATURITY CHART", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = GoldPrimary)
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
                            "Monthly Deposit Range: Tk 500 - Tk 10,000 (General 10% vs Women 10.5% in Light Pink)",
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
                            WatermarkOverlay()
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .horizontalScroll(rememberScrollState())
                                    .verticalScroll(rememberScrollState())
                            ) {
                                // Year Category Banner Row
                                Row(
                                    modifier = Modifier
                                        .background(Color(0xFF1E293B))
                                        .padding(vertical = 4.dp, horizontal = 2.dp)
                                ) {
                                    Text("DEPOSIT", fontSize = 9.5.sp, fontWeight = FontWeight.Bold, color = Color.LightGray, modifier = Modifier.width(90.dp), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                                    Spacer(modifier = Modifier.width(2.dp))
                                    Text("--- 3 YEARS ---", fontSize = 9.5.sp, fontWeight = FontWeight.Bold, color = GoldPrimary, modifier = Modifier.width(190.dp), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                                    Spacer(modifier = Modifier.width(2.dp))
                                    Text("--- 5 YEARS ---", fontSize = 9.5.sp, fontWeight = FontWeight.Bold, color = GreenAccent, modifier = Modifier.width(190.dp), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                                    Spacer(modifier = Modifier.width(2.dp))
                                    Text("--- 7 YEARS ---", fontSize = 9.5.sp, fontWeight = FontWeight.Bold, color = GoldLight, modifier = Modifier.width(190.dp), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                                    Spacer(modifier = Modifier.width(2.dp))
                                    Text("--- 10 YEARS ---", fontSize = 9.5.sp, fontWeight = FontWeight.Bold, color = Color(0xFF38BDF8), modifier = Modifier.width(190.dp), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                                }
                                HorizontalDivider(color = GoldPrimary.copy(alpha = 0.5f), thickness = 1.dp)

                                // Header Row
                                Row(
                                    modifier = Modifier
                                        .background(SlateDark)
                                        .padding(vertical = 6.dp, horizontal = 4.dp)
                                ) {
                                    headers.forEachIndexed { hIdx, h ->
                                        val isWomenHeader = hIdx in listOf(2, 4, 6, 8)
                                        Text(
                                            text = h,
                                            fontSize = 9.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isWomenHeader) Color(0xFFFFB6C1) else GoldLight,
                                            modifier = Modifier.width(90.dp),
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
                                            val isWomenCol = cIdx in listOf(2, 4, 6, 8)
                                            Box(
                                                modifier = Modifier
                                                    .width(90.dp)
                                                    .background(
                                                        if (isWomenCol) Color(0xFFFFD1DC).copy(alpha = 0.25f)
                                                        else Color.Transparent,
                                                        RoundedCornerShape(2.dp)
                                                    )
                                                    .padding(vertical = 2.dp)
                                            ) {
                                                Text(
                                                    text = cell,
                                                    fontSize = 10.5.sp,
                                                    color = if (cIdx == 0) GoldPrimary else if (isWomenCol) Color(0xFFFFB6C1) else Color.White,
                                                    fontWeight = if (cIdx == 0 || isWomenCol) FontWeight.Bold else FontWeight.Normal,
                                                    modifier = Modifier.fillMaxWidth(),
                                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // Bottom Right Officer Contact Block
                        OfficerContactFooter(initialMessage = "Assalamualaikum Toufiq Vai.. Ami ekta DPS/FD Korte Chai.")
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            showChartDialog = false
                            PdfHelper.generateCustomChartPdf(
                                context = context,
                                fileName = "DPS_Maturity_Chart.pdf",
                                chartTitle = "DPS SAVINGS MATURITY CHART",
                                chartSubtitle = "General Rate 10.00% p.a. vs Women Rate 10.50% p.a. (Tk 500 - Tk 10,000)",
                                headers = headers,
                                rows = chartRows,
                                highlightColumns = listOf(2, 4, 6, 8),
                                thickBorderColumns = listOf(0, 2, 4, 6, 8)
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
