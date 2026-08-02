package com.example.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.GreenAccent
import java.net.URLEncoder

object WhatsAppHelper {
    fun openWhatsAppMessage(context: Context, phoneNumber: String, itemType: String) {
        if (phoneNumber.isBlank()) {
            Toast.makeText(context, "Phone number is empty", Toast.LENGTH_SHORT).show()
            return
        }

        val banglaItemType = when (itemType.uppercase()) {
            "DEBIT_CARD" -> "ডেবিট কার্ড"
            "PIN" -> "পিন"
            "CHEQUE_BOOK" -> "চেক বই"
            "DPS" -> "ডিপিএস"
            else -> itemType
        }

        val message = "আসসালামু আলাইকুম,  আপনার $banglaItemType টি চিরির বন্দর শাখায় আছে। অনুগ্রহ পুর্বক আপনার $banglaItemType টি দ্রুত সংগ্রহ করুন। অন্যথায় আপনার $banglaItemType টি নষ্ট করা হতে পারে।\nএবং আপনি বর্তমানে কোথায় বা কতো বিজিবিতে আছেন মেসেজে তা জানিয়ে আমাদের সহায়তা করুন।\nধন্যবাদ।"

        var cleanPhone = phoneNumber.replace(Regex("[^0-9]"), "")
        if (cleanPhone.startsWith("01")) {
            cleanPhone = "88$cleanPhone"
        }

        try {
            val encodedMessage = URLEncoder.encode(message, "UTF-8")
            val url = "https://api.whatsapp.com/send?phone=$cleanPhone&text=$encodedMessage"
            val intent = Intent(Intent.ACTION_VIEW).apply {
                data = Uri.parse(url)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "Could not open WhatsApp: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
        }
    }
}

@Composable
fun WhatsAppClickablePhone(
    phoneNumber: String,
    itemType: String,
    modifier: Modifier = Modifier,
    textColor: Color = Color.Unspecified
) {
    val context = LocalContext.current
    if (phoneNumber.isBlank()) {
        Text(text = "Phone: N/A", fontSize = 12.sp, color = Color.Gray)
        return
    }

    Row(
        modifier = modifier.clickable {
            WhatsAppHelper.openWhatsAppMessage(context, phoneNumber, itemType)
        },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Surface(
            color = GreenAccent.copy(alpha = 0.2f),
            shape = RoundedCornerShape(4.dp)
        ) {
            Text(
                text = "WhatsApp 💬",
                color = GreenAccent,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
            )
        }
        Text(
            text = "Phone: $phoneNumber",
            fontSize = 12.sp,
            color = if (textColor != Color.Unspecified) textColor else GreenAccent,
            fontWeight = FontWeight.Medium
        )
    }
}
