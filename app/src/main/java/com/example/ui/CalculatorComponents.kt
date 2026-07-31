package com.example.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.GoldPrimary
import com.example.ui.theme.SlateDark
import java.net.URLEncoder

/**
 * Utility to launch WhatsApp with pre-filled Bengali text to Toufiq's number.
 */
fun launchWhatsAppChat(
    context: Context,
    phone: String = "01517836078",
    initialMessage: String = "Assalamualaikum Toufiq Vai.. Ami ekta DPS/FD Korte Chai."
) {
    try {
        val cleanPhone = if (phone.startsWith("0")) "88$phone" else phone
        val encodedMsg = URLEncoder.encode(initialMessage, "UTF-8")
        val url = "https://api.whatsapp.com/send?phone=$cleanPhone&text=$encodedMsg"
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        context.startActivity(intent)
    } catch (e: Exception) {
        android.widget.Toast.makeText(context, "Could not launch WhatsApp: ${e.localizedMessage}", android.widget.Toast.LENGTH_SHORT).show()
    }
}

/**
 * Diagonal Watermark overlay component for financial chart screens & dialogs.
 */
@Composable
fun WatermarkOverlay(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.rotate(-22f)
        ) {
            Text(
                text = "For FD and DPS",
                fontSize = 26.sp,
                fontWeight = FontWeight.ExtraBold,
                color = GoldPrimary.copy(alpha = 0.25f),
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "WhatsApp on : 01517836078",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White.copy(alpha = 0.25f),
                textAlign = TextAlign.Center
            )
        }
    }
}

/**
 * Official Contact signature footer displaying Officer details and clickable WhatsApp launcher.
 */
@Composable
fun OfficerContactFooter(
    modifier: Modifier = Modifier,
    initialMessage: String = "Assalamualaikum Toufiq Vai.. Ami ekta DPS/FD Korte Chai."
) {
    val context = LocalContext.current
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 6.dp),
        horizontalAlignment = Alignment.End
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = SlateDark),
            shape = RoundedCornerShape(8.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier.padding(10.dp),
                horizontalAlignment = Alignment.End
            ) {
                Text(
                    text = "Md Toufiqur Rahman (Toufiq)",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = GoldPrimary
                )
                Text(
                    text = "Trainee Assistant Officer",
                    fontSize = 11.sp,
                    color = Color.LightGray
                )
                Text(
                    text = "Shimanto Bank PLC.",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )
                Text(
                    text = "Chirirbandar Branch, Dinajpur.",
                    fontSize = 10.sp,
                    color = Color.LightGray
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .background(Color(0xFF25D366).copy(alpha = 0.2f), RoundedCornerShape(6.dp))
                        .clickable {
                            launchWhatsAppChat(context, "01517836078", initialMessage)
                        }
                        .padding(horizontal = 10.dp, vertical = 5.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Chat,
                        contentDescription = "WhatsApp",
                        tint = Color(0xFF25D366),
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "WhatsApp: 01517836078",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF25D366)
                    )
                }
            }
        }
    }
}
