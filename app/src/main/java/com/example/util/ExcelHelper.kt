package com.example.util

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.example.data.BankingItem
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

object ExcelHelper {

    fun generateAndShareExcel(
        context: Context,
        fileName: String,
        items: List<BankingItem>
    ) {
        try {
            val file = File(context.cacheDir, fileName)
            val fos = FileOutputStream(file)
            
            // Add UTF-8 BOM
            fos.write(0xEF)
            fos.write(0xBB)
            fos.write(0xBF)
            
            val sdf = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())
            
            // Header: Sl. Ac Number. Name. Phone Number. Address. Receive Date
            val header = "Sl.,Ac Number,Name,Phone Number,Address,Receive Date\n"
            fos.write(header.toByteArray(Charsets.UTF_8))
            
            items.forEachIndexed { index, item ->
                val dateStr = sdf.format(Date(item.receivedDate))
                val cleanName = item.customerName.replace(",", " ").uppercase().trim()
                val cleanAc = item.accountNumber.replace(",", " ").trim()
                val cleanPhone = item.phoneNumber.replace(",", " ").trim()
                val cleanAddress = item.address.replace(",", " ").replace("\n", " ").uppercase().trim()
                
                val row = "${index + 1},$cleanAc,$cleanName,$cleanPhone,$cleanAddress,$dateStr\n"
                fos.write(row.toByteArray(Charsets.UTF_8))
            }
            
            fos.close()

            // Share file
            val authority = "${context.packageName}.fileprovider"
            val uri = FileProvider.getUriForFile(context, authority, file)

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/csv"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(shareIntent, "Download Excel via"))
        } catch (e: Exception) {
            e.printStackTrace()
            android.widget.Toast.makeText(context, "Failed to export Excel: ${e.localizedMessage}", android.widget.Toast.LENGTH_LONG).show()
        }
    }

    fun generateGenericExcel(
        context: Context,
        fileName: String,
        headers: List<String>,
        rows: List<List<String>>
    ) {
        try {
            val file = File(context.cacheDir, fileName)
            val fos = FileOutputStream(file)
            
            // Add UTF-8 BOM
            fos.write(0xEF)
            fos.write(0xBB)
            fos.write(0xBF)
            
            // Header row
            val headerRow = "Sl.," + headers.joinToString(",") + "\n"
            fos.write(headerRow.toByteArray(Charsets.UTF_8))
            
            rows.forEachIndexed { index, row ->
                val cleanRow = row.map { cell ->
                    cell.replace(",", " ").replace("\n", " ").uppercase().trim()
                }
                val rowLine = "${index + 1}," + cleanRow.joinToString(",") + "\n"
                fos.write(rowLine.toByteArray(Charsets.UTF_8))
            }
            
            fos.close()

            // Share file
            val authority = "${context.packageName}.fileprovider"
            val uri = FileProvider.getUriForFile(context, authority, file)

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/csv"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(shareIntent, "Download Excel via"))
        } catch (e: Exception) {
            e.printStackTrace()
            android.widget.Toast.makeText(context, "Failed to export Excel: ${e.localizedMessage}", android.widget.Toast.LENGTH_LONG).show()
        }
    }
}
