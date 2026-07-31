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
import androidx.compose.material.icons.filled.Percent
import androidx.compose.material.icons.filled.PictureAsPdf
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
fun LoanCalculatorScreen(
    viewModel: BankingViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var amountText by remember { mutableStateOf("") }
    var interestText by remember { mutableStateOf("9.0") }
    var tenureText by remember { mutableStateOf("5") } // Default 5 years
    var tenureInYears by remember { mutableStateOf(true) } // true = Years, false = Months

    var showChartDialog by remember { mutableStateOf(false) }

    val amount = amountText.toDoubleOrNull() ?: 0.0
    val interestRate = interestText.toDoubleOrNull() ?: 0.0
    val tenureVal = tenureText.toIntOrNull() ?: 0

    val n = if (tenureInYears) tenureVal * 12 else tenureVal
    val r = (interestRate / 12.0) / 100.0

    // EMI Calculation
    val emi = if (amount > 0.0 && r > 0.0 && n > 0) {
        val base = (1.0 + r).pow(n.toDouble())
        (amount * r * base) / (base - 1.0)
    } else if (amount > 0.0 && n > 0 && r == 0.0) {
        amount / n
    } else {
        0.0
    }

    val totalPayable = if (n > 0) emi * n else 0.0
    val extraPaid = if (totalPayable > amount) totalPayable - amount else 0.0

    val formatter = DecimalFormat("#,##,##0.00")

    // Chart Data Pre-computation (Tk 1 Lac to Tk 10 Lac)
    fun calcEmi(principal: Double, ratePa: Double, months: Int): Double {
        val rateMonthly = (ratePa / 12.0) / 100.0
        if (principal <= 0 || months <= 0) return 0.0
        if (rateMonthly <= 0) return principal / months
        val base = (1.0 + rateMonthly).pow(months.toDouble())
        return (principal * rateMonthly * base) / (base - 1.0)
    }

    val loanAmounts = (100000..1000000 step 100000).toList()
    val headers = listOf("Loan Amount", "1Y EMI (9%)", "1Y EMI (10%)", "3Y EMI (9%)", "3Y EMI (10%)", "5Y EMI (9%)", "5Y EMI (10%)", "5Y Total (9%)")

    val chartRows = loanAmounts.map { principal ->
        val p = principal.toDouble()
        val emi1y9 = calcEmi(p, 9.0, 12)
        val emi1y10 = calcEmi(p, 10.0, 12)

        val emi3y9 = calcEmi(p, 9.0, 36)
        val emi3y10 = calcEmi(p, 10.0, 36)

        val emi5y9 = calcEmi(p, 9.0, 60)
        val emi5y10 = calcEmi(p, 10.0, 60)
        val tot5y9 = emi5y9 * 60.0

        listOf(
            "Tk ${formatter.format(p)}",
            formatter.format(emi1y9),
            formatter.format(emi1y10),
            formatter.format(emi3y9),
            formatter.format(emi3y10),
            formatter.format(emi5y9),
            formatter.format(emi5y10),
            formatter.format(tot5y9)
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "LOAN CALCULATOR",
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
                            contentDescription = "Loan Chart PDF",
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
            // Inputs Card
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        "Loan Eligibility & Pricing",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = GoldPrimary
                    )

                    // Principal Loan Amount
                    OutlinedTextField(
                        value = amountText,
                        onValueChange = { amountText = it },
                        label = { Text("Requested Loan Amount") },
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
                        label = { Text("Annual Interest Rate") },
                        suffix = { Icon(Icons.Default.Percent, contentDescription = null, modifier = Modifier.size(16.dp), tint = GoldPrimary) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = GoldPrimary,
                            unfocusedBorderColor = SlateSecondary,
                            focusedLabelColor = GoldPrimary
                        )
                    )

                    // Loan Tenure
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = tenureText,
                            onValueChange = { tenureText = it },
                            label = { Text("Tenure") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1.2f),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = GoldPrimary,
                                unfocusedBorderColor = SlateSecondary,
                                focusedLabelColor = GoldPrimary
                            )
                        )

                        // Tenure units selector
                        Row(
                            modifier = Modifier
                                .weight(1f)
                                .height(56.dp)
                                .background(SlateDark, RoundedCornerShape(8.dp))
                                .padding(2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Button(
                                onClick = { tenureInYears = true },
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight(),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (tenureInYears) GoldPrimary else Color.Transparent,
                                    contentColor = if (tenureInYears) SlateDark else Color.White
                                ),
                                shape = RoundedCornerShape(6.dp),
                                contentPadding = PaddingValues(0.dp)
                            ) {
                                Text("Yrs", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }

                            Button(
                                onClick = { tenureInYears = false },
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight(),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (!tenureInYears) GoldPrimary else Color.Transparent,
                                    contentColor = if (!tenureInYears) SlateDark else Color.White
                                ),
                                shape = RoundedCornerShape(6.dp),
                                contentPadding = PaddingValues(0.dp)
                            ) {
                                Text("Mths", fontSize = 11.sp, fontWeight = FontWeight.Bold)
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
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "ESTIMATED REPAYMENT SCHEDULE",
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

                    OutputRow("Principal Requested", "৳ ${formatter.format(amount)}")
                    OutputRow("Annual Interest Rate", "$interestRate% p.a.")
                    OutputRow("Total Number of Installments", "$n Months")
                    
                    HorizontalDivider(color = SlateSecondary.copy(alpha = 0.4f))

                    OutputRow("Monthly EMI Installment", "৳ ${formatter.format(emi)}", color = GoldLight, isBold = true, fontSize = 15.sp)
                    OutputRow("Extra Interest Cost", "৳ ${formatter.format(extraPaid)}", color = RedAccent)
                    
                    // Fixed Total Payable Row: Fits up to 8+ digits cleanly on 1 line
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Total Payable (Princ+Int)",
                            fontSize = 12.sp,
                            color = Color.LightGray,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "৳ ${formatter.format(totalPayable)}",
                            fontSize = 14.sp,
                            color = GreenAccent,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                    }
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
                        Text("LOAN REPAYMENT CHART", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = GoldPrimary)
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
                            "Loan EMI & Repayment Chart (Tk 1 Lac - Tk 10 Lac)",
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
                                fileName = "Loan_Repayment_Chart.pdf",
                                chartTitle = "LOAN EMI & REPAYMENT SCHEDULE CHART",
                                chartSubtitle = "Comparison across 1Y, 3Y, 5Y Tenures (9% & 10% Rates)",
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
