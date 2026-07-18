package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.platform.LocalContext
import android.widget.Toast
import androidx.activity.compose.BackHandler
import com.example.ui.*
import com.example.ui.theme.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val viewModel: BankingViewModel = viewModel()
            
            MyApplicationTheme(darkTheme = viewModel.isDarkMode) {
                MainContainer(viewModel)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainContainer(viewModel: BankingViewModel) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    
    // AI Chat Sheet toggle
    var showAiChatSheet by remember { mutableStateOf(false) }

    var backPressedTime by remember { mutableLongStateOf(0L) }
    BackHandler(enabled = true) {
        if (viewModel.searchQuery.isNotBlank()) {
            viewModel.searchQuery = ""
        } else if (viewModel.currentScreen != "dashboard") {
            viewModel.currentScreen = "dashboard"
        } else {
            val now = System.currentTimeMillis()
            if (now - backPressedTime < 2000L) {
                (context as? android.app.Activity)?.finish()
            } else {
                Toast.makeText(context, "Press back again to exit", Toast.LENGTH_SHORT).show()
                backPressedTime = now
            }
        }
    }

    // Navigation lists
    val menuItems = listOf(
        NavigationItem("dashboard", "Dashboard", Icons.Default.Dashboard),
        NavigationItem("reports", "All Reports", Icons.Default.Assessment),
        NavigationItem("atm_calc", "ATM Replenishment", Icons.Default.LocalAtm),
        NavigationItem("hunting", "Customer Findings", Icons.Default.Groups),
        NavigationItem("todo_log", "Todo List Log", Icons.Default.PlaylistAddCheck),
        NavigationItem("recycle_bin", "Recycle Bin", Icons.Default.Delete),
        NavigationItem("settings", "System Settings", Icons.Default.Settings)
    )

    // Security screen check - Bypass/Remove PIN requirement for direct login
    if (false) {
        PasscodeLockScreen(viewModel)
    } else {
        ModalNavigationDrawer(
            drawerState = drawerState,
            drawerContent = {
                ModalDrawerSheet(
                    drawerContainerColor = MaterialTheme.colorScheme.surface,
                    drawerShape = RoundedCornerShape(topEnd = 16.dp, bottomEnd = 16.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(110.dp)
                            .background(
                                brush = Brush.horizontalGradient(
                                    colors = listOf(SlateDark, SlateSecondary)
                                )
                            )
                            .padding(16.dp),
                        contentAlignment = Alignment.BottomStart
                    ) {
                        Column {
                            Text(
                                text = "TOUFIQ'S SMART",
                                color = Color.White.copy(alpha = 0.8f),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp
                            )
                            Text(
                                text = "BANKING TRACKER",
                                color = GoldPrimary,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    ) {
                        items(menuItems) { item ->
                            val isSelected = viewModel.currentScreen == item.id
                            NavigationDrawerItem(
                                label = { Text(item.title, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                                selected = isSelected,
                                onClick = {
                                    viewModel.currentScreen = item.id
                                    scope.launch { drawerState.close() }
                                },
                                icon = { Icon(item.icon, contentDescription = null, tint = if (isSelected) GoldPrimary else MaterialTheme.colorScheme.onSurface) },
                                badge = {
                                    if (item.id == "settings" && viewModel.isUpdateAvailable) {
                                        Box(
                                            modifier = Modifier
                                                .background(GoldPrimary, RoundedCornerShape(4.dp))
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                text = "UPDATE",
                                                color = SlateDark,
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                },
                                colors = NavigationDrawerItemDefaults.colors(
                                    selectedContainerColor = GoldPrimary.copy(alpha = 0.15f),
                                    selectedTextColor = GoldPrimary
                                ),
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp)
                            )
                        }
                    }

                    if (viewModel.isLoggedIn && viewModel.isPasscodeEnabled && viewModel.passcodeLock.isNotEmpty()) {
                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
                        )
                        NavigationDrawerItem(
                            label = { Text("Lock Terminal", color = GoldPrimary, fontWeight = FontWeight.Bold) },
                            selected = false,
                            onClick = {
                                scope.launch { drawerState.close() }
                                viewModel.isLoggedIn = false
                                Toast.makeText(context, "Terminal Locked!", Toast.LENGTH_SHORT).show()
                            },
                            icon = { Icon(Icons.Default.Lock, contentDescription = "Lock Terminal", tint = GoldPrimary) },
                            colors = NavigationDrawerItemDefaults.colors(
                                unselectedTextColor = GoldPrimary
                            ),
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                        )
                    }
                }
            }
        ) {
            val context = LocalContext.current
            Scaffold(
                topBar = {
                    Column(modifier = Modifier.background(MaterialTheme.colorScheme.surface)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .statusBarsPadding()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(onClick = { scope.launch { drawerState.open() } }) {
                                    Icon(Icons.Default.Menu, contentDescription = "Menu")
                                }
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Branch Management System",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    color = GoldPrimary
                                )
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                // Dynamic Date display
                                val sdf = SimpleDateFormat("EEE, dd MMM", Locale.getDefault())
                                Text(
                                    text = sdf.format(Date()),
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }

                        // UNIVERSAL INSTANT SEARCH BAR
                        if (viewModel.currentScreen == "dashboard") {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedTextField(
                                    value = viewModel.searchQuery,
                                    onValueChange = { viewModel.searchQuery = it },
                                    placeholder = { Text("Search instantly across cards, PINs, cheque books, DPS...", fontSize = 12.sp) },
                                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search", tint = GoldPrimary) },
                                    trailingIcon = {
                                        if (viewModel.searchQuery.isNotBlank()) {
                                            IconButton(onClick = { viewModel.searchQuery = "" }) {
                                                Icon(Icons.Default.Close, contentDescription = "Clear")
                                            }
                                        }
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(52.dp),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = GoldPrimary,
                                        unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
                                    ),
                                    singleLine = true
                                )
                            }
                        }
                    }
                },
            ) { innerPadding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    // Check if universal search is active
                    if (viewModel.searchQuery.isNotBlank()) {
                        UniversalSearchResultsView(viewModel)
                    } else {
                        // Regular Screen Router
                        when (viewModel.currentScreen) {
                            "dashboard" -> DashboardScreen(viewModel, onNavigate = { viewModel.currentScreen = it })
                            "todo_list" -> TaskAndHuntingScreen(viewModel, "TODO")
                            "debit_card" -> BankingItemScreen(viewModel, "DEBIT_CARD")
                            "pin" -> BankingItemScreen(viewModel, "PIN")
                            "cheque_book" -> BankingItemScreen(viewModel, "CHEQUE_BOOK")
                            "dps" -> BankingItemScreen(viewModel, "DPS")
                            "prize_bond" -> VaultStockScreen(viewModel, "PRIZE_BOND")
                            "pay_order" -> VaultStockScreen(viewModel, "PAY_ORDER")
                            "atm_calc" -> AtmCalculatorScreen(viewModel)
                            "hunting" -> TaskAndHuntingScreen(viewModel, "HUNTING")
                            "forms" -> FormsScreen(viewModel)
                            "reports" -> ReportsScreen(viewModel)
                            "recycle_bin" -> RecycleBinScreen(viewModel)
                            "settings" -> SettingsScreen(viewModel)
                            "todo_log" -> TodoLogScreen(viewModel)
                            "fd_calc" -> FdCalculatorScreen(viewModel)
                            "loan_calc" -> LoanCalculatorScreen(viewModel)
                            "dps_calc" -> DpsCalculatorScreen(viewModel)
                            else -> DashboardScreen(viewModel, onNavigate = { viewModel.currentScreen = it })
                        }
                    }

                    // Bottom Sheet AI Conversational assistant drawer
                    if (showAiChatSheet) {
                        AiChatModalSheet(
                            viewModel = viewModel,
                            onDismiss = { showAiChatSheet = false }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun UniversalSearchResultsView(viewModel: BankingViewModel) {
    val results = viewModel.getUniversalSearchResults(viewModel.searchQuery)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Instant Search Results (${results.size})",
            fontWeight = FontWeight.Bold,
            color = GoldPrimary,
            fontSize = 14.sp
        )

        if (results.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No records matched your search parameters.", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(results) { res ->
                    val item = res.originalItem
                    val context = LocalContext.current
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                // Close search and jump to the corresponding module
                                viewModel.searchQuery = ""
                                when (res.moduleType) {
                                    "DEBIT_CARD" -> viewModel.currentScreen = "debit_card"
                                    "PIN" -> viewModel.currentScreen = "pin"
                                    "CHEQUE_BOOK" -> viewModel.currentScreen = "cheque_book"
                                    "DPS" -> viewModel.currentScreen = "dps"
                                    "CUSTOMER HUNTING" -> viewModel.currentScreen = "hunting"
                                    "DIGITAL FORM" -> viewModel.currentScreen = "forms"
                                }
                            },
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            // Header with Name & Module Type
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(res.title, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color.White)
                                Box(
                                    modifier = Modifier
                                        .background(GoldPrimary.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(res.moduleType, fontSize = 9.sp, color = GoldPrimary, fontWeight = FontWeight.Bold)
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(6.dp))

                            if (item != null) {
                                // Staying Days calculation
                                val now = System.currentTimeMillis()
                                val stayingDays = ((now - item.receivedDate) / (1000L * 3600 * 24)).coerceAtLeast(0)
                                val destroyDateStr = SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(item.destroyAfter))

                                // Display Specific Fields
                                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                    Text("Account No: ${item.accountNumber}", fontSize = 12.sp, color = Color.LightGray)
                                    Text("Phone Number: ${item.phoneNumber}", fontSize = 12.sp, color = Color.LightGray)
                                    Text("Address: ${item.address}", fontSize = 12.sp, color = Color.LightGray)
                                    Text("Received Date: ${res.receivedDate}", fontSize = 12.sp, color = Color.LightGray)
                                    Text("Staying Days: $stayingDays days in vault", fontSize = 12.sp, color = GoldLight, fontWeight = FontWeight.Bold)
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                // Deliver/Destroy Option row or Status
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Status info
                                    when {
                                        item.isDestroyed -> {
                                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                                Icon(Icons.Default.DeleteForever, contentDescription = null, tint = RedAccent, modifier = Modifier.size(16.dp))
                                                Text("DESTROYED on $destroyDateStr", fontSize = 12.sp, color = RedAccent, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                        item.isDelivered -> {
                                            val deliveryDateStr = if (item.deliveryDate > 0) {
                                                SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(item.deliveryDate))
                                            } else {
                                                "N/A"
                                            }
                                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = GreenAccent, modifier = Modifier.size(16.dp))
                                                Text("Delivered on $deliveryDateStr", fontSize = 12.sp, color = GreenAccent, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                        else -> {
                                            Text("Status: Active/Pending", fontSize = 12.sp, color = GreenAccent, fontWeight = FontWeight.Bold)
                                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                // Destroy action
                                                Button(
                                                    onClick = {
                                                        viewModel.updateBankingItem(item.copy(isDestroyed = true))
                                                        Toast.makeText(context, "Successfully Destroyed/Expired!", Toast.LENGTH_SHORT).show()
                                                    },
                                                    colors = ButtonDefaults.buttonColors(containerColor = RedAccent, contentColor = Color.White),
                                                    shape = RoundedCornerShape(6.dp),
                                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                                                    modifier = Modifier.height(28.dp)
                                                ) {
                                                    Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(12.dp))
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text("Destroy", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                                }

                                                // Deliver action
                                                Button(
                                                    onClick = {
                                                        viewModel.markAsDelivered(item)
                                                        Toast.makeText(context, "Successfully Delivered!", Toast.LENGTH_SHORT).show()
                                                    },
                                                    colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary, contentColor = SlateDark),
                                                    shape = RoundedCornerShape(6.dp),
                                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                                                    modifier = Modifier.height(28.dp)
                                                ) {
                                                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(12.dp))
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text("Deliver", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                                }
                                            }
                                        }
                                    }
                                }
                            } else {
                                // Customer hunting lead / Saved form search details
                                Text(res.subtitle, fontSize = 12.sp, color = Color.LightGray)
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Date: ${res.receivedDate}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                                    Text("Status: ${res.status}", fontSize = 11.sp, color = GreenAccent, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PasscodeLockScreen(viewModel: BankingViewModel) {
    var inputCode by remember { mutableStateOf("") }
    var errorMsg by remember { mutableStateOf("") }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(SlateDark),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(24.dp)
        ) {
            Icon(Icons.Default.Lock, contentDescription = null, tint = GoldPrimary, modifier = Modifier.size(60.dp))
            Text("Toufiq's Smart Banking Security", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Text("Enter your 4-digit passcode to open the branch vault.", color = Color.White.copy(alpha = 0.6f), fontSize = 12.sp)

            OutlinedTextField(
                value = inputCode,
                onValueChange = {
                    inputCode = it
                    if (it == viewModel.passcodeLock) {
                        viewModel.isLoggedIn = true
                        errorMsg = ""
                    } else if (it.length >= 4) {
                        errorMsg = "Incorrect passcode! Try again."
                        inputCode = ""
                    }
                },
                placeholder = { Text("xxxx") },
                singleLine = true,
                modifier = Modifier.width(150.dp),
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = GoldPrimary, unfocusedBorderColor = Color.Gray, focusedTextColor = Color.White)
            )

            if (errorMsg.isNotBlank()) {
                Text(errorMsg, color = RedAccent, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiChatModalSheet(
    viewModel: BankingViewModel,
    onDismiss: () -> Unit
) {
    val messages by viewModel.aiMessages.collectAsStateWithLifecycle()
    var currentInput by remember { mutableStateOf("") }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.75f)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = GoldPrimary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("TOUFIQ'S INTELLIGENT BOT", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = GoldPrimary)
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close")
                }
            }

            // Message History list
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(messages) { msg ->
                    val isUser = msg.second
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
                    ) {
                        Box(
                            modifier = Modifier
                                .background(
                                    if (isUser) GoldPrimary else MaterialTheme.colorScheme.surfaceVariant,
                                    RoundedCornerShape(
                                        topStart = 12.dp,
                                        topEnd = 12.dp,
                                        bottomStart = if (isUser) 12.dp else 0.dp,
                                        bottomEnd = if (isUser) 0.dp else 12.dp
                                    )
                                )
                                .padding(12.dp)
                                .widthIn(max = 280.dp)
                        ) {
                            Text(
                                text = msg.first,
                                color = if (isUser) SlateDark else MaterialTheme.colorScheme.onSurface,
                                fontSize = 13.sp,
                                lineHeight = 18.sp
                            )
                        }
                    }
                }

                if (viewModel.isAiGenerating) {
                    item {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start) {
                            Box(
                                modifier = Modifier
                                    .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp))
                                    .padding(12.dp)
                            ) {
                                CircularProgressIndicator(color = GoldPrimary, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }

            // Input Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = currentInput,
                    onValueChange = { currentInput = it },
                    placeholder = { Text("Ask Toufiq's AI Assistant...") },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )
                IconButton(
                    onClick = {
                        if (currentInput.isNotBlank()) {
                            viewModel.sendAiChatMessage(currentInput)
                            currentInput = ""
                        }
                    },
                    colors = IconButtonDefaults.iconButtonColors(containerColor = GoldPrimary, contentColor = SlateDark)
                ) {
                    Icon(Icons.Default.Send, contentDescription = "Send")
                }
            }
        }
    }
}

data class NavigationItem(val id: String, val title: String, val icon: androidx.compose.ui.graphics.vector.ImageVector)
