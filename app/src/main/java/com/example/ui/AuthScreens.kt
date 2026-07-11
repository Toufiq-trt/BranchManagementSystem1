package com.example.ui

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import com.example.data.BranchUser
import com.example.data.PasswordHistoryEntry
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    viewModel: BankingViewModel,
    onNavigate: (String) -> Unit
) {
    var ein by remember { mutableStateOf("") }
    var pin by remember { mutableStateOf("") }
    var errorMsg by remember { mutableStateOf("") }
    val context = LocalContext.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBg)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = null,
                    tint = GoldPrimary,
                    modifier = Modifier.size(56.dp)
                )

                Text(
                    text = "Branch Staff Login",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                Text(
                    text = "Enter your EIN and PIN to access secure branch operations.",
                    fontSize = 12.sp,
                    color = Color.White.copy(alpha = 0.6f),
                    textAlign = TextAlign.Center
                )

                OutlinedTextField(
                    value = ein,
                    onValueChange = { ein = it },
                    label = { Text("Employee ID Number (EIN)") },
                    leadingIcon = { Icon(Icons.Default.Badge, contentDescription = null, tint = GoldPrimary) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = GoldPrimary,
                        unfocusedBorderColor = Color.Gray,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )

                OutlinedTextField(
                    value = pin,
                    onValueChange = { pin = it },
                    label = { Text("Passcode PIN") },
                    leadingIcon = { Icon(Icons.Default.Key, contentDescription = null, tint = GoldPrimary) },
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = GoldPrimary,
                        unfocusedBorderColor = Color.Gray,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword)
                )

                if (errorMsg.isNotBlank()) {
                    Text(
                        text = errorMsg,
                        color = RedAccent,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center
                    )
                }

                Button(
                    onClick = {
                        if (ein.isBlank() || pin.isBlank()) {
                            errorMsg = "Please fill in all fields!"
                        } else {
                            viewModel.login(ein, pin) { success, msg ->
                                if (success) {
                                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                    // Check if standard user needs permission prompts
                                    val user = viewModel.currentUser
                                    if (user != null && !user.isToufiq) {
                                        // If any permission is requested by Toufiq, navigate to permission_prompt
                                        if (user.cameraAuthorized || user.storageAuthorized || user.locationAuthorized) {
                                            onNavigate("permission_prompt")
                                        } else {
                                            onNavigate("dashboard")
                                        }
                                    } else {
                                        onNavigate("dashboard")
                                    }
                                } else {
                                    errorMsg = msg
                                }
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary, contentColor = SlateDark),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Log In", fontWeight = FontWeight.Bold)
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "New Register",
                        color = GoldPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .clickable { onNavigate("register") }
                            .padding(4.dp)
                    )

                    Text(
                        text = "Officer TOUFIQ",
                        color = GoldPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .clickable { onNavigate("toufiq_login") }
                            .padding(4.dp)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterScreen(
    viewModel: BankingViewModel,
    onNavigate: (String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var ein by remember { mutableStateOf("") }
    var pin by remember { mutableStateOf("") }
    var errorMsg by remember { mutableStateOf("") }
    var successMsg by remember { mutableStateOf("") }
    val context = LocalContext.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBg)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.PersonAdd,
                    contentDescription = null,
                    tint = GoldPrimary,
                    modifier = Modifier.size(56.dp)
                )

                Text(
                    text = "Staff Registration",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                Text(
                    text = "Fill up Name, EIN, and PIN to submit registration request to Officer Toufiq.",
                    fontSize = 11.sp,
                    color = Color.White.copy(alpha = 0.6f),
                    textAlign = TextAlign.Center
                )

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Full Name") },
                    leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = GoldPrimary) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = GoldPrimary,
                        unfocusedBorderColor = Color.Gray,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    singleLine = true
                )

                OutlinedTextField(
                    value = ein,
                    onValueChange = { ein = it },
                    label = { Text("Employee ID (EIN)") },
                    leadingIcon = { Icon(Icons.Default.Badge, contentDescription = null, tint = GoldPrimary) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = GoldPrimary,
                        unfocusedBorderColor = Color.Gray,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )

                OutlinedTextField(
                    value = pin,
                    onValueChange = { pin = it },
                    label = { Text("Set Passcode PIN") },
                    leadingIcon = { Icon(Icons.Default.Key, contentDescription = null, tint = GoldPrimary) },
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = GoldPrimary,
                        unfocusedBorderColor = Color.Gray,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword)
                )

                if (errorMsg.isNotBlank()) {
                    Text(
                        text = errorMsg,
                        color = RedAccent,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center
                    )
                }

                if (successMsg.isNotBlank()) {
                    Text(
                        text = successMsg,
                        color = GreenAccent,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center
                    )
                }

                Button(
                    onClick = {
                        if (name.isBlank() || ein.isBlank() || pin.isBlank()) {
                            errorMsg = "All 3 fields are strictly required!"
                            successMsg = ""
                        } else {
                            viewModel.register(ein, name, pin) { success, msg ->
                                if (success) {
                                    successMsg = msg
                                    errorMsg = ""
                                    name = ""
                                    ein = ""
                                    pin = ""
                                    Toast.makeText(context, "Registration Pending!", Toast.LENGTH_LONG).show()
                                } else {
                                    errorMsg = msg
                                    successMsg = ""
                                }
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary, contentColor = SlateDark),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Register & Submit", fontWeight = FontWeight.Bold)
                }

                Text(
                    text = "Already registered? Log In",
                    color = GoldPrimary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .clickable { onNavigate("login") }
                        .padding(4.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ToufiqLoginScreen(
    viewModel: BankingViewModel,
    onNavigate: (String) -> Unit
) {
    var ein by remember { mutableStateOf("20104") }
    var pin by remember { mutableStateOf("") }
    var errorMsg by remember { mutableStateOf("") }
    val context = LocalContext.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBg)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.AdminPanelSettings,
                    contentDescription = null,
                    tint = GoldPrimary,
                    modifier = Modifier.size(56.dp)
                )

                Text(
                    text = "Toufiq Portal Authorization",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                Text(
                    text = "Direct administrative gateway for Officer Toufiq. No registration needed.",
                    fontSize = 11.sp,
                    color = Color.White.copy(alpha = 0.6f),
                    textAlign = TextAlign.Center
                )

                OutlinedTextField(
                    value = ein,
                    onValueChange = { ein = it },
                    label = { Text("Toufiq ID (EIN)") },
                    leadingIcon = { Icon(Icons.Default.Badge, contentDescription = null, tint = GoldPrimary) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = GoldPrimary,
                        unfocusedBorderColor = Color.Gray,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )

                OutlinedTextField(
                    value = pin,
                    onValueChange = { pin = it },
                    label = { Text("Toufiq PIN") },
                    leadingIcon = { Icon(Icons.Default.Key, contentDescription = null, tint = GoldPrimary) },
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = GoldPrimary,
                        unfocusedBorderColor = Color.Gray,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword)
                )

                if (errorMsg.isNotBlank()) {
                    Text(
                        text = errorMsg,
                        color = RedAccent,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center
                    )
                }

                Button(
                    onClick = {
                        if (ein != "20104") {
                            errorMsg = "Toufiq EIN is strictly locked to 20104!"
                        } else {
                            viewModel.login(ein, pin) { success, msg ->
                                if (success) {
                                    Toast.makeText(context, "Welcome back, Officer Toufiq!", Toast.LENGTH_SHORT).show()
                                    onNavigate("toufiq_panel")
                                } else {
                                    errorMsg = msg
                                }
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary, contentColor = SlateDark),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Authorize & Enter", fontWeight = FontWeight.Bold)
                }

                Text(
                    text = "Back to Main Login",
                    color = GoldPrimary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .clickable { onNavigate("login") }
                        .padding(4.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ToufiqPanelScreen(
    viewModel: BankingViewModel,
    onNavigate: (String) -> Unit
) {
    val users by viewModel.allUsers.collectAsStateWithLifecycle()
    val passwordLogs by viewModel.allPasswordLogs.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var selectedTab by remember(viewModel.toufiqPanelSelectedTab) { mutableStateOf(viewModel.toufiqPanelSelectedTab) } // 0 = Auth Requests, 1 = Registration List, 2 = Security Settings

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Top Header
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(SlateDark)
                .padding(16.dp)
        ) {
            Column {
                Text(
                    text = "TOUFIQ ADMINISTRATIVE GATEWAY",
                    color = GoldPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Text(
                    text = "Manage branch registration logs and security authorizations",
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 11.sp
                )
            }
        }

        // Sub Tabs Row
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = SlateDark,
            contentColor = GoldPrimary,
            indicator = { tabPositions ->
                TabRowDefaults.SecondaryIndicator(
                    modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                    color = GoldPrimary
                )
            }
        ) {
            val pendingCount = users.filter { !it.isAuthorized && !it.isToufiq }.size
            Tab(
                selected = selectedTab == 0,
                onClick = {
                    selectedTab = 0
                    viewModel.toufiqPanelSelectedTab = 0
                },
                text = { Text("Requests ($pendingCount)", fontSize = 11.sp) }
            )
            Tab(
                selected = selectedTab == 1,
                onClick = {
                    selectedTab = 1
                    viewModel.toufiqPanelSelectedTab = 1
                },
                text = { Text("Registration List", fontSize = 11.sp) }
            )
            Tab(
                selected = selectedTab == 2,
                onClick = {
                    selectedTab = 2
                    viewModel.toufiqPanelSelectedTab = 2
                },
                text = { Text("Security", fontSize = 11.sp) }
            )
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (selectedTab == 0) {
                // Authorization requests
                val pendingUsers = users.filter { !it.isAuthorized && !it.isToufiq }
                
                if (pendingUsers.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(48.dp), tint = GreenAccent)
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("No pending registration requests!", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                            }
                        }
                    }
                } else {
                    item {
                        Text(
                            text = "REGISTRATION AUTHORIZATION PENDING",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = GoldPrimary
                        )
                    }

                    items(pendingUsers, key = { it.ein }) { user ->
                        var reqStorage by remember { mutableStateOf(false) }
                        var reqCamera by remember { mutableStateOf(false) }
                        var reqLocation by remember { mutableStateOf(false) }

                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(user.name, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                        Text("EIN: ${user.ein}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                                    }
                                    Box(
                                        modifier = Modifier
                                            .background(GoldPrimary, RoundedCornerShape(4.dp))
                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        // Toufiq can see their PIN!
                                        Text("PIN: ${user.pin}", fontSize = 11.sp, color = SlateDark, fontWeight = FontWeight.Bold)
                                    }
                                }

                                HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))

                                Text(
                                    text = "SELECT REQUIRED DEVICE PERMISSIONS",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = GoldPrimary
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Storage, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Storage", fontSize = 12.sp)
                                    }
                                    Switch(
                                        checked = reqStorage,
                                        onCheckedChange = { reqStorage = it },
                                        colors = SwitchDefaults.colors(checkedThumbColor = GoldPrimary)
                                    )
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.PhotoCamera, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Camera", fontSize = 12.sp)
                                    }
                                    Switch(
                                        checked = reqCamera,
                                        onCheckedChange = { reqCamera = it },
                                        colors = SwitchDefaults.colors(checkedThumbColor = GoldPrimary)
                                    )
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.LocationOn, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Location GPS", fontSize = 12.sp)
                                    }
                                    Switch(
                                        checked = reqLocation,
                                        onCheckedChange = { reqLocation = it },
                                        colors = SwitchDefaults.colors(checkedThumbColor = GoldPrimary)
                                    )
                                }

                                Button(
                                    onClick = {
                                        viewModel.authorizeUser(user, reqCamera, reqStorage, reqLocation)
                                        Toast.makeText(context, "${user.name} successfully authorized!", Toast.LENGTH_SHORT).show()
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary, contentColor = SlateDark),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Icon(Icons.Default.HowToReg, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Authorize Registration", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }

            } else if (selectedTab == 1) {
                // Registration List & Password Logs Section
                item {
                    Text(
                        text = "👥 REGISTERED USERS MANAGEMENT",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = GoldPrimary
                    )
                }

                val registeredUsers = users.filter { it.isAuthorized && !it.isToufiq }
                if (registeredUsers.isEmpty()) {
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Box(modifier = Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                                Text("No registered authorized staff users yet.", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                            }
                        }
                    }
                } else {
                    items(registeredUsers, key = { it.ein }) { staff ->
                        val userLogs = passwordLogs.filter { it.ein == staff.ein }
                        
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(staff.name, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color.White)
                                        Text("EIN ID: ${staff.ein}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                                        Text("Current PIN: ${staff.pin}", fontSize = 12.sp, color = GreenAccent, fontWeight = FontWeight.SemiBold)
                                    }
                                    
                                    Button(
                                        onClick = {
                                            viewModel.deregisterUser(staff)
                                            Toast.makeText(context, "${staff.name} de-registered successfully!", Toast.LENGTH_SHORT).show()
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = RedAccent, contentColor = Color.White),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                        shape = RoundedCornerShape(6.dp)
                                    ) {
                                        Icon(Icons.Default.PersonRemove, contentDescription = null, modifier = Modifier.size(12.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("De-register", fontSize = 10.sp)
                                    }
                                }
                                
                                HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                                
                                Text("TOGGLE ACTIVE DEVICE PERMISSIONS", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = GoldLight)
                                
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Storage, contentDescription = null, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Storage", fontSize = 12.sp)
                                    }
                                    Switch(
                                        checked = staff.storageAuthorized,
                                        onCheckedChange = { isChecked ->
                                            viewModel.updateUserPermissions(staff, staff.cameraAuthorized, isChecked, staff.locationAuthorized)
                                        },
                                        colors = SwitchDefaults.colors(checkedThumbColor = GoldPrimary)
                                    )
                                }
                                
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.PhotoCamera, contentDescription = null, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Camera", fontSize = 12.sp)
                                    }
                                    Switch(
                                        checked = staff.cameraAuthorized,
                                        onCheckedChange = { isChecked ->
                                            viewModel.updateUserPermissions(staff, isChecked, staff.storageAuthorized, staff.locationAuthorized)
                                        },
                                        colors = SwitchDefaults.colors(checkedThumbColor = GoldPrimary)
                                    )
                                }
                                
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.LocationOn, contentDescription = null, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Location GPS", fontSize = 12.sp)
                                    }
                                    Switch(
                                        checked = staff.locationAuthorized,
                                        onCheckedChange = { isChecked ->
                                            viewModel.updateUserPermissions(staff, staff.cameraAuthorized, staff.storageAuthorized, isChecked)
                                        },
                                        colors = SwitchDefaults.colors(checkedThumbColor = GoldPrimary)
                                    )
                                }

                                if (userLogs.isNotEmpty()) {
                                    HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
                                    Text("PIN Password History logs:", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                                    userLogs.forEach { log ->
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text("Old: ${log.oldPin.ifEmpty { "Register" }} ➔ New: ${log.newPin}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
                                            Text(SimpleDateFormat("dd MMM, HH:mm", Locale.getDefault()).format(Date(log.changeTimestamp)), fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

            } else {
                // Security Tab - Update Toufiq PIN
                item {
                    Text(
                        text = "UPDATE TOUFIQ PASSCODE PIN",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = GoldPrimary
                    )
                }

                item {
                    var oldPinInput by remember { mutableStateOf("") }
                    var newPinInput by remember { mutableStateOf("") }
                    var confirmPinInput by remember { mutableStateOf("") }
                    var changeError by remember { mutableStateOf("") }

                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            OutlinedTextField(
                                value = oldPinInput,
                                onValueChange = { oldPinInput = it },
                                label = { Text("Current PIN") },
                                visualTransformation = PasswordVisualTransformation(),
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword)
                            )

                            OutlinedTextField(
                                value = newPinInput,
                                onValueChange = { newPinInput = it },
                                label = { Text("New PIN") },
                                visualTransformation = PasswordVisualTransformation(),
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword)
                            )

                            OutlinedTextField(
                                value = confirmPinInput,
                                onValueChange = { confirmPinInput = it },
                                label = { Text("Confirm New PIN") },
                                visualTransformation = PasswordVisualTransformation(),
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword)
                            )

                            if (changeError.isNotBlank()) {
                                Text(changeError, color = RedAccent, fontSize = 12.sp)
                            }

                            Button(
                                onClick = {
                                    val currentToufiq = users.find { it.ein == "20104" }
                                    if (currentToufiq == null) {
                                        changeError = "System error: Admin user not seeded!"
                                    } else if (currentToufiq.pin != oldPinInput) {
                                        changeError = "Incorrect current PIN!"
                                    } else if (newPinInput.length < 4) {
                                        changeError = "New PIN must be at least 4 digits!"
                                    } else if (newPinInput != confirmPinInput) {
                                        changeError = "New PINs do not match!"
                                    } else {
                                        viewModel.changePassword("20104", oldPinInput, newPinInput) { success, msg ->
                                            if (success) {
                                                Toast.makeText(context, "Toufiq passcode updated successfully!", Toast.LENGTH_SHORT).show()
                                                oldPinInput = ""
                                                newPinInput = ""
                                                confirmPinInput = ""
                                                changeError = ""
                                            } else {
                                                changeError = msg
                                            }
                                        }
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary, contentColor = SlateDark),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("Update Passcode", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChangePasswordScreen(
    viewModel: BankingViewModel,
    onNavigate: (String) -> Unit
) {
    val currentUser = viewModel.currentUser
    var oldPin by remember { mutableStateOf("") }
    var newPin by remember { mutableStateOf("") }
    var confirmPin by remember { mutableStateOf("") }
    var errorMsg by remember { mutableStateOf("") }
    val context = LocalContext.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBg)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.LockReset,
                    contentDescription = null,
                    tint = GoldPrimary,
                    modifier = Modifier.size(56.dp)
                )

                Text(
                    text = "Update Passcode PIN",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                Text(
                    text = "This will update your local security code. The old PIN is instantly logged in Toufiq's Passcode Logbook.",
                    fontSize = 11.sp,
                    color = Color.White.copy(alpha = 0.6f),
                    textAlign = TextAlign.Center
                )

                OutlinedTextField(
                    value = oldPin,
                    onValueChange = { oldPin = it },
                    label = { Text("Old PIN") },
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = GoldPrimary, unfocusedBorderColor = Color.Gray, focusedTextColor = Color.White, unfocusedTextColor = Color.White),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword)
                )

                OutlinedTextField(
                    value = newPin,
                    onValueChange = { newPin = it },
                    label = { Text("New PIN") },
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = GoldPrimary, unfocusedBorderColor = Color.Gray, focusedTextColor = Color.White, unfocusedTextColor = Color.White),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword)
                )

                OutlinedTextField(
                    value = confirmPin,
                    onValueChange = { confirmPin = it },
                    label = { Text("Confirm New PIN") },
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = GoldPrimary, unfocusedBorderColor = Color.Gray, focusedTextColor = Color.White, unfocusedTextColor = Color.White),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword)
                )

                if (errorMsg.isNotBlank()) {
                    Text(
                        text = errorMsg,
                        color = RedAccent,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center
                    )
                }

                Button(
                    onClick = {
                        if (currentUser == null) {
                            errorMsg = "No logged in user found."
                        } else if (currentUser.pin != oldPin) {
                            errorMsg = "Incorrect old PIN!"
                        } else if (newPin.length < 4) {
                            errorMsg = "New PIN must be at least 4 digits!"
                        } else if (newPin != confirmPin) {
                            errorMsg = "New PINs do not match!"
                        } else {
                            viewModel.changePassword(currentUser.ein, oldPin, newPin) { success, msg ->
                                if (success) {
                                    Toast.makeText(context, "PIN successfully updated!", Toast.LENGTH_SHORT).show()
                                    onNavigate("dashboard")
                                } else {
                                    errorMsg = msg
                                }
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary, contentColor = SlateDark),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Change Password", fontWeight = FontWeight.Bold)
                }

                TextButton(onClick = { onNavigate("dashboard") }) {
                    Text("Cancel", color = GoldPrimary)
                }
            }
        }
    }
}

@Composable
fun PermissionPromptScreen(
    viewModel: BankingViewModel,
    onNavigate: (String) -> Unit
) {
    val user = viewModel.currentUser
    val context = LocalContext.current

    if (user == null) {
        LaunchedEffect(Unit) { onNavigate("dashboard") }
        return
    }

    // Determine what Toufiq requested during authorization.
    // If permission requested == true, we must let user grant it.
    val needsStorage = user.storageAuthorized
    val needsCamera = user.cameraAuthorized
    val needsLocation = user.locationAuthorized

    val storageGranted = viewModel.simulatedStorageGranted
    val cameraGranted = viewModel.simulatedCameraGranted
    val locationGranted = viewModel.simulatedLocationGranted

    // Real system permission request launchers
    val storageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        viewModel.simulatedStorageGranted = true
        Toast.makeText(context, "Storage permission simulation activated!", Toast.LENGTH_SHORT).show()
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        viewModel.simulatedCameraGranted = true
        Toast.makeText(context, "Camera permission simulation activated!", Toast.LENGTH_SHORT).show()
    }

    val locationLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        viewModel.simulatedLocationGranted = true
        Toast.makeText(context, "GPS Location permission simulation activated!", Toast.LENGTH_SHORT).show()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBg)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Security,
                    contentDescription = null,
                    tint = GoldPrimary,
                    modifier = Modifier.size(60.dp)
                )

                Text(
                    text = "Security Handshake Required",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                Text(
                    text = "Officer Toufiq has designated specific device privileges required for your Employee Access Level.",
                    fontSize = 11.sp,
                    color = Color.White.copy(alpha = 0.6f),
                    textAlign = TextAlign.Center
                )

                HorizontalDivider(color = Color.White.copy(alpha = 0.1f))

                // Storage Privilege row
                if (needsStorage) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(if (storageGranted) SlateSecondary.copy(alpha = 0.2f) else SlateDark, RoundedCornerShape(8.dp))
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (storageGranted) Icons.Default.CheckCircle else Icons.Default.Storage,
                                contentDescription = null,
                                tint = if (storageGranted) GreenAccent else GoldPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text("Storage Drive Access", fontSize = 13.sp, color = Color.White, fontWeight = FontWeight.Bold)
                                Text("Required for PDF downloads", fontSize = 10.sp, color = Color.White.copy(alpha = 0.6f))
                            }
                        }

                        if (!storageGranted) {
                            Button(
                                onClick = {
                                    storageLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary, contentColor = SlateDark),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text("Grant", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        } else {
                            Text("GRANTED", color = GreenAccent, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // Camera Privilege row
                if (needsCamera) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(if (cameraGranted) SlateSecondary.copy(alpha = 0.2f) else SlateDark, RoundedCornerShape(8.dp))
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (cameraGranted) Icons.Default.CheckCircle else Icons.Default.PhotoCamera,
                                contentDescription = null,
                                tint = if (cameraGranted) GreenAccent else GoldPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text("Camera Scanning Access", fontSize = 13.sp, color = Color.White, fontWeight = FontWeight.Bold)
                                Text("Required for Photo OCR", fontSize = 10.sp, color = Color.White.copy(alpha = 0.6f))
                            }
                        }

                        if (!cameraGranted) {
                            Button(
                                onClick = {
                                    cameraLauncher.launch(Manifest.permission.CAMERA)
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary, contentColor = SlateDark),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text("Grant", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        } else {
                            Text("GRANTED", color = GreenAccent, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // Location Privilege row
                if (needsLocation) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(if (locationGranted) SlateSecondary.copy(alpha = 0.2f) else SlateDark, RoundedCornerShape(8.dp))
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (locationGranted) Icons.Default.CheckCircle else Icons.Default.LocationOn,
                                contentDescription = null,
                                tint = if (locationGranted) GreenAccent else GoldPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text("GPS Location Services", fontSize = 13.sp, color = Color.White, fontWeight = FontWeight.Bold)
                                Text("Required for field hunting sync", fontSize = 10.sp, color = Color.White.copy(alpha = 0.6f))
                            }
                        }

                        if (!locationGranted) {
                            Button(
                                onClick = {
                                    locationLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary, contentColor = SlateDark),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text("Grant", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        } else {
                            Text("GRANTED", color = GreenAccent, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                val allGranted = (!needsStorage || storageGranted) &&
                        (!needsCamera || cameraGranted) &&
                        (!needsLocation || locationGranted)

                Button(
                    onClick = {
                        if (allGranted) {
                            onNavigate("dashboard")
                        } else {
                            Toast.makeText(context, "Please grant all requested privileges to unlock!", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (allGranted) GreenAccent else SlateSecondary,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = if (allGranted) "Unlock App registers" else "Permissions Pending",
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun SecurityLockView(
    onNavigate: (String) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBg)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(16.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = null,
                tint = RedAccent,
                modifier = Modifier.size(72.dp)
            )

            Text(
                text = "🔒 Access Restricted",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            Text(
                text = "This is a secure branch management module. Please log in or register to view or edit registers.",
                fontSize = 12.sp,
                color = Color.White.copy(alpha = 0.6f),
                textAlign = TextAlign.Center,
                lineHeight = 18.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = { onNavigate("login") },
                colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary, contentColor = SlateDark),
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(Icons.Default.Login, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Log In Now", fontWeight = FontWeight.Bold)
            }
        }
    }
}
