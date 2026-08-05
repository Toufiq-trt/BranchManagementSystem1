package com.example.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.BankingItem
import com.example.ui.theme.GreenAccent
import java.net.URLEncoder
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object WhatsAppHelper {
    val messagedKeys = mutableStateMapOf<String, Boolean>()

    fun markAsMessaged(phone: String, itemId: Int?) {
        val clean = phone.replace(Regex("[^0-9]"), "").trim()
        if (clean.isNotBlank()) messagedKeys[clean] = true
        if (itemId != null && itemId > 0) messagedKeys["item_$itemId"] = true
    }

    fun isMessaged(phone: String, itemId: Int?): Boolean {
        val clean = phone.replace(Regex("[^0-9]"), "").trim()
        if (clean.isNotBlank() && messagedKeys.containsKey(clean)) return true
        if (itemId != null && itemId > 0 && messagedKeys.containsKey("item_$itemId")) return true
        return false
    }

    fun openWhatsAppMessage(
        context: Context,
        phoneNumber: String,
        itemType: String,
        item: BankingItem? = null,
        customerName: String? = null,
        receivedDate: Long? = null,
        destroyDate: Long? = null
    ) {
        if (phoneNumber.isBlank()) {
            Toast.makeText(context, "Phone number is empty", Toast.LENGTH_SHORT).show()
            return
        }

        markAsMessaged(phoneNumber, item?.id)

        val name = item?.customerName ?: customerName ?: ""
        val recTime = item?.receivedDate ?: receivedDate ?: System.currentTimeMillis()
        val destTime = item?.destroyAfter ?: destroyDate ?: (recTime + 90L * 24 * 3600 * 1000)

        val banglaItemType = when (itemType.uppercase()) {
            "DEBIT_CARD" -> "ডেবিট কার্ড"
            "PIN" -> "পিন মেইলার"
            "CHEQUE_BOOK" -> "চেক বই"
            "DPS" -> "ডিপিএস"
            else -> itemType
        }

        val shortType = when (itemType.uppercase()) {
            "DEBIT_CARD", "PIN" -> "কার্ড"
            "CHEQUE_BOOK" -> "চেক বই"
            "DPS" -> "ডিপিএস"
            else -> "কার্ড বা চেক"
        }

        val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        val receivedDateStr = sdf.format(Date(recTime))
        val destroyDateStr = sdf.format(Date(destTime))

        val nameGreeting = if (name.isNotBlank()) " $name" else ""

        val message = """
আসসালামু আলাইকুম$nameGreeting,
আপনার $banglaItemType টি $receivedDateStr এ সীমান্ত ব্যাংক চিরিরবন্দর শাখায় এসেছে যা $destroyDateStr এ নষ্ট হয়ে যাবে।
অনুগ্রহ পুর্বক আপনার $shortType টি নষ্ট হওয়ার পুর্বেই সংগ্রহ করুন।
আপনি বর্তমানে কতো বিজিবি এবং কোথায় আছেন সেটা মেসেজে জানিয়ে সহযোগিতা করুন।
ধন্যবাদ।
এই নাম্বারে কল না করার জন্য অনুরোধ করা হলো
যেকোনো প্রয়োজনে শুধু মাত্র মেসেজ দিন
""".trimIndent()

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
    item: BankingItem? = null,
    customerName: String? = null,
    receivedDate: Long? = null,
    destroyDate: Long? = null,
    modifier: Modifier = Modifier,
    textColor: Color = Color.Unspecified
) {
    val context = LocalContext.current
    if (phoneNumber.isBlank()) {
        Text(text = "Phone: N/A", fontSize = 12.sp, color = Color.Gray)
        return
    }

    val isSent = WhatsAppHelper.isMessaged(phoneNumber, item?.id)

    Row(
        modifier = modifier.clickable {
            WhatsAppHelper.openWhatsAppMessage(
                context = context,
                phoneNumber = phoneNumber,
                itemType = itemType,
                item = item,
                customerName = customerName,
                receivedDate = receivedDate,
                destroyDate = destroyDate
            )
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
        if (isSent) {
            Surface(
                color = GreenAccent,
                shape = CircleShape,
                modifier = Modifier.size(16.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Messaged",
                        tint = Color.White,
                        modifier = Modifier.size(11.dp)
                    )
                }
            }
        }
    }
}
