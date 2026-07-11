package com.example.util

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

object PdfHelper {

    // Helper to truncate text with ellipsis to fit a cell width
    private fun getTruncatedText(text: String, paint: Paint, maxWidth: Float): String {
        if (paint.measureText(text) <= maxWidth) return text
        var result = text
        while (result.isNotEmpty() && paint.measureText("$result...") > maxWidth) {
            result = result.dropLast(1)
        }
        return if (result.isEmpty()) "" else "$result..."
    }

    /**
     * Generates a beautifully formatted table PDF matching the user's design requirements.
     * Table columns: TYPE, AC NUMBER, NAME, ADDRESS, PHONE NUMBER (and optionally 1 MONTH COMPLETE)
     */
    fun generateTablePdf(
        context: Context,
        fileName: String,
        title: String,
        headers: List<String>,
        rows: List<List<String>>
    ) {
        try {
            val pdfDocument = PdfDocument()
            val totalWidth = 535f // 595 - 2 * 30 (margins of 30pt)
            val startX = 30f
            val startY = 80f
            
            // Calculate column widths dynamically based on headers count
            val columnWidths = if (headers.size == 6) {
                // 6 columns: TYPE, AC NUMBER, NAME, PHONE NUMBER, ADDRESS, 1 MONTH COMPLETE
                listOf(65f, 80f, 100f, 85f, 110f, 95f)
            } else {
                // 5 columns: TYPE, AC NUMBER, NAME, PHONE NUMBER, ADDRESS
                listOf(70f, 90f, 125f, 100f, 150f)
            }

            // Paints setup
            val titlePaint = Paint().apply {
                color = Color.BLACK
                textSize = 14f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }

            val subtitlePaint = Paint().apply {
                color = Color.BLACK
                textSize = 11f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }

            val headerBgPaint = Paint().apply {
                color = Color.BLACK
                style = Paint.Style.FILL
            }

            val headerTextPaint = Paint().apply {
                color = Color.WHITE
                textSize = 9f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }

            val cellTextPaint = Paint().apply {
                color = Color.BLACK
                textSize = 9f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            }

            val borderPaint = Paint().apply {
                color = Color.BLACK
                style = Paint.Style.STROKE
                strokeWidth = 1f
            }

            var currentPageNumber = 1
            var pageInfo = PdfDocument.PageInfo.Builder(595, 842, currentPageNumber).create()
            var page = pdfDocument.startPage(pageInfo)
            var canvas = page.canvas

            // Footer function to draw on every page
            fun drawFooter(currentCanvas: Canvas) {
                val footerY = 820f
                val footerPaint = Paint().apply {
                    color = Color.GRAY
                    textSize = 8f
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                }
                
                // Left side: Date & Time in DD-MM-YYYY format
                val dateStr = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).format(Date())
                val timeStr = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
                val footerLeft = "Date: $dateStr  Time: $timeStr"
                currentCanvas.drawText(footerLeft, startX, footerY, footerPaint)
                
                // Middle: DESIGNED BY TOUFIQ
                val footerMid = "DESIGNED BY TOUFIQ"
                val footerMidWidth = footerPaint.measureText(footerMid)
                currentCanvas.drawText(footerMid, (595f - footerMidWidth) / 2f, footerY, footerPaint)
            }

            // 1. Draw Page Header (Total Customers & Title)
            val systemTitleText = "TOUFIQS SMART BANKING MANAGEMENT"
            val titleWidth = titlePaint.measureText(systemTitleText)
            canvas.drawText(systemTitleText, (595f - titleWidth) / 2f, 40f, titlePaint)

            // Total items on the left side of the header after system title section (at y = 60)
            val totalItemsText = "Total Items: ${rows.size}"
            canvas.drawText(totalItemsText, startX, 60f, subtitlePaint)
            
            // Subtitle on the right side of the header
            val formattedTitle = title.uppercase(Locale.getDefault())
            val subtitleWidth = subtitlePaint.measureText(formattedTitle)
            canvas.drawText(formattedTitle, 595f - startX - subtitleWidth, 60f, subtitlePaint)

            var y = startY
            val headerRowHeight = 26f
            val dataRowHeight = 22f
            val pageBottomLimit = 800f

            // Function to draw a single table row (header or cell data)
            fun drawTableRow(
                currentCanvas: Canvas,
                currentY: Float,
                cellValues: List<String>,
                isHeader: Boolean
            ) {
                val rowHeight = if (isHeader) headerRowHeight else dataRowHeight
                var x = startX

                // Draw background for header
                if (isHeader) {
                    currentCanvas.drawRect(startX, currentY, startX + totalWidth, currentY + rowHeight, headerBgPaint)
                }

                // Draw cells and inner borders
                for (i in cellValues.indices) {
                    val colWidth = columnWidths.getOrElse(i) { 80f }
                    val text = cellValues.getOrElse(i) { "" }
                    
                    // Cell border rect
                    currentCanvas.drawRect(x, currentY, x + colWidth, currentY + rowHeight, borderPaint)

                    // Text position (centered vertically, centered horizontally)
                    val currentTextPaint = if (isHeader) headerTextPaint else cellTextPaint
                    val truncated = getTruncatedText(text, currentTextPaint, colWidth - 8f)
                    
                    // Vertical and horizontal centering offsets
                    val textY = currentY + (rowHeight / 2f) - ((currentTextPaint.descent() + currentTextPaint.ascent()) / 2f)
                    val textWidth = currentTextPaint.measureText(truncated)
                    val textX = x + (colWidth - textWidth) / 2f
                    currentCanvas.drawText(truncated, textX, textY, currentTextPaint)

                    x += colWidth
                }
            }

            // Draw initial headers
            drawTableRow(canvas, y, headers, true)
            y += headerRowHeight

            // Draw rows
            for (rowIndex in rows.indices) {
                // Pagination check
                if (y + dataRowHeight > pageBottomLimit) {
                    drawFooter(canvas) // Draw footer on the page being finished
                    pdfDocument.finishPage(page)
                    currentPageNumber++
                    pageInfo = PdfDocument.PageInfo.Builder(595, 842, currentPageNumber).create()
                    page = pdfDocument.startPage(pageInfo)
                    canvas = page.canvas

                    // On next pages, we can just draw the table headers at top
                    y = 40f
                    drawTableRow(canvas, y, headers, true)
                    y += headerRowHeight
                }

                val rowData = rows[rowIndex]
                drawTableRow(canvas, y, rowData, false)
                y += dataRowHeight
            }

            drawFooter(canvas) // Draw footer on the final page
            pdfDocument.finishPage(page)

            // Write to a temporary file in cache directory
            val file = File(context.cacheDir, fileName)
            val outputStream = FileOutputStream(file)
            pdfDocument.writeTo(outputStream)
            pdfDocument.close()
            outputStream.close()

            // Share via FileProvider
            val authority = "${context.packageName}.fileprovider"
            val uri = FileProvider.getUriForFile(context, authority, file)

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(shareIntent, "Share PDF via"))
        } catch (e: Exception) {
            e.printStackTrace()
            android.widget.Toast.makeText(context, "Failed to generate PDF: ${e.localizedMessage}", android.widget.Toast.LENGTH_LONG).show()
        }
    }

    /**
     * Legacy PDF share helper for unstructured balance reports.
     */
    fun generateAndSharePdf(context: Context, fileName: String, title: String, content: String) {
        try {
            val pdfDocument = PdfDocument()
            val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
            val page = pdfDocument.startPage(pageInfo)
            val canvas = page.canvas

            val titlePaint = Paint().apply {
                color = Color.BLACK
                textSize = 16f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }

            val bodyPaint = Paint().apply {
                color = Color.BLACK
                textSize = 10f
                typeface = Typeface.create(Typeface.MONOSPACE, Typeface.NORMAL)
            }

            var y = 50f
            canvas.drawText(title, 50f, y, titlePaint)
            y += 30f

            val lines = content.split("\n")
            for (line in lines) {
                var currentLine = line
                while (currentLine.length > 60) {
                    val sub = currentLine.substring(0, 60)
                    canvas.drawText(sub, 50f, y, bodyPaint)
                    y += 15f
                    if (y > 800) break
                    currentLine = currentLine.substring(60)
                }
                canvas.drawText(currentLine, 50f, y, bodyPaint)
                y += 15f
                if (y > 800) break
            }

            pdfDocument.finishPage(page)

            val file = File(context.cacheDir, fileName)
            val outputStream = FileOutputStream(file)
            pdfDocument.writeTo(outputStream)
            pdfDocument.close()
            outputStream.close()

            val authority = "${context.packageName}.fileprovider"
            val uri = FileProvider.getUriForFile(context, authority, file)

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(shareIntent, "Share PDF via"))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
