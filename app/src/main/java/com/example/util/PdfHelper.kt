package com.example.util

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import androidx.core.content.FileProvider
import com.example.data.BankingItem
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
     * Generates a polished financial chart PDF with responsive column widths and landscape support.
     */
    fun generateCustomChartPdf(
        context: Context,
        fileName: String,
        chartTitle: String,
        chartSubtitle: String,
        headers: List<String>,
        rows: List<List<String>>
    ) {
        try {
            val isLandscape = headers.size > 5
            val pageWidth = if (isLandscape) 842 else 595
            val pageHeight = if (isLandscape) 595 else 842
            val startX = 30f
            val totalWidth = (pageWidth - 60).toFloat()
            val startY = 85f

            val numCols = headers.size.coerceAtLeast(1)
            val colWidth = totalWidth / numCols.toFloat()

            val pdfDocument = PdfDocument()

            // Paints
            val systemTitlePaint = Paint().apply {
                color = Color.BLACK
                textSize = 14f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }

            val titlePaint = Paint().apply {
                color = Color.parseColor("#B8860B") // Dark Gold
                textSize = 12f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }

            val subtitlePaint = Paint().apply {
                color = Color.DKGRAY
                textSize = 9f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            }

            val headerBgPaint = Paint().apply {
                color = Color.parseColor("#0F172A") // Slate Dark
                style = Paint.Style.FILL
            }

            val headerTextPaint = Paint().apply {
                color = Color.WHITE
                textSize = 8.5f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }

            val rowBgAltPaint = Paint().apply {
                color = Color.parseColor("#F8FAFC")
                style = Paint.Style.FILL
            }

            val cellTextPaint = Paint().apply {
                color = Color.BLACK
                textSize = 8f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            }

            val borderPaint = Paint().apply {
                color = Color.parseColor("#CBD5E1")
                style = Paint.Style.STROKE
                strokeWidth = 0.8f
            }

            val footerPaint = Paint().apply {
                color = Color.GRAY
                textSize = 8f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            }

            var currentPageNumber = 1
            var pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, currentPageNumber).create()
            var page = pdfDocument.startPage(pageInfo)
            var canvas = page.canvas

            fun drawFooter(currentCanvas: Canvas) {
                val footerY = (pageHeight - 20).toFloat()
                val dateStr = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).format(Date())
                val timeStr = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
                val footerLeft = "Date: $dateStr  Time: $timeStr"
                currentCanvas.drawText(footerLeft, startX, footerY, footerPaint)

                val footerMid = "DESIGNED BY TOUFIQ"
                val footerMidWidth = footerPaint.measureText(footerMid)
                currentCanvas.drawText(footerMid, (pageWidth - footerMidWidth) / 2f, footerY, footerPaint)

                val pageStr = "Page $currentPageNumber"
                val pageStrWidth = footerPaint.measureText(pageStr)
                currentCanvas.drawText(pageStr, pageWidth - startX - pageStrWidth, footerY, footerPaint)
            }

            fun drawHeader(currentCanvas: Canvas) {
                val sysTitle = "TOUFIQS SMART BANKING MANAGEMENT"
                val sysWidth = systemTitlePaint.measureText(sysTitle)
                currentCanvas.drawText(sysTitle, (pageWidth - sysWidth) / 2f, 35f, systemTitlePaint)

                val tWidth = titlePaint.measureText(chartTitle)
                currentCanvas.drawText(chartTitle, (pageWidth - tWidth) / 2f, 52f, titlePaint)

                val stWidth = subtitlePaint.measureText(chartSubtitle)
                currentCanvas.drawText(chartSubtitle, (pageWidth - stWidth) / 2f, 66f, subtitlePaint)
            }

            drawHeader(canvas)

            var y = startY
            val headerRowHeight = 24f
            val dataRowHeight = 18f
            val pageBottomLimit = (pageHeight - 40).toFloat()

            fun drawTableRow(currentCanvas: Canvas, currentY: Float, cellValues: List<String>, isHeader: Boolean, isAlt: Boolean) {
                val rowHeight = if (isHeader) headerRowHeight else dataRowHeight
                var x = startX

                if (isHeader) {
                    currentCanvas.drawRect(startX, currentY, startX + totalWidth, currentY + rowHeight, headerBgPaint)
                } else if (isAlt) {
                    currentCanvas.drawRect(startX, currentY, startX + totalWidth, currentY + rowHeight, rowBgAltPaint)
                }

                for (i in cellValues.indices) {
                    val text = cellValues.getOrElse(i) { "" }
                    currentCanvas.drawRect(x, currentY, x + colWidth, currentY + rowHeight, borderPaint)

                    val p = if (isHeader) headerTextPaint else cellTextPaint
                    val truncated = getTruncatedText(text, p, colWidth - 4f)
                    val textY = currentY + (rowHeight / 2f) - ((p.descent() + p.ascent()) / 2f)
                    val textWidth = p.measureText(truncated)
                    val textX = x + (colWidth - textWidth) / 2f
                    currentCanvas.drawText(truncated, textX, textY, p)

                    x += colWidth
                }
            }

            // Draw header
            drawTableRow(canvas, y, headers, isHeader = true, isAlt = false)
            y += headerRowHeight

            // Draw rows
            for (rowIndex in rows.indices) {
                if (y + dataRowHeight > pageBottomLimit) {
                    drawFooter(canvas)
                    pdfDocument.finishPage(page)

                    currentPageNumber++
                    pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, currentPageNumber).create()
                    page = pdfDocument.startPage(pageInfo)
                    canvas = page.canvas

                    drawHeader(canvas)
                    y = startY
                    drawTableRow(canvas, y, headers, isHeader = true, isAlt = false)
                    y += headerRowHeight
                }

                val rowData = rows[rowIndex]
                drawTableRow(canvas, y, rowData, isHeader = false, isAlt = rowIndex % 2 == 1)
                y += dataRowHeight
            }

            drawFooter(canvas)
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
            context.startActivity(Intent.createChooser(shareIntent, "Share Chart PDF via"))
        } catch (e: Exception) {
            e.printStackTrace()
            android.widget.Toast.makeText(context, "Failed to generate Chart PDF: ${e.localizedMessage}", android.widget.Toast.LENGTH_LONG).show()
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

    /**
     * Generates a formal 30-Day Overdue Notice Letter PDF for a customer.
     */
    fun generateCustomerNoticeLetterPdf(
        context: Context,
        fileName: String,
        item: BankingItem
    ) {
        try {
            val pdfDocument = PdfDocument()
            val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
            val page = pdfDocument.startPage(pageInfo)
            val canvas = page.canvas

            val sdf = SimpleDateFormat("dd MMMM yyyy", Locale.getDefault())
            val dateStr = sdf.format(Date())
            val rxDateStr = if (item.receivedDate > 0) sdf.format(Date(item.receivedDate)) else "N/A"
            val daysStaying = ((System.currentTimeMillis() - item.receivedDate) / (1000L * 3600 * 24)).toInt().coerceAtLeast(0)

            // Header Paints
            val headerPaint = Paint().apply {
                color = Color.rgb(15, 23, 42) // SlateDark
                textSize = 16f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }
            val subHeaderPaint = Paint().apply {
                color = Color.rgb(180, 83, 9) // Gold Primary
                textSize = 11f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }
            val labelPaint = Paint().apply {
                color = Color.BLACK
                textSize = 10f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }
            val valuePaint = Paint().apply {
                color = Color.DKGRAY
                textSize = 10f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            }
            val bodyPaint = Paint().apply {
                color = Color.BLACK
                textSize = 10.5f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            }
            val boxBgPaint = Paint().apply {
                color = Color.rgb(241, 245, 249)
                style = Paint.Style.FILL
            }
            val boxBorderPaint = Paint().apply {
                color = Color.rgb(203, 213, 225)
                style = Paint.Style.STROKE
                strokeWidth = 1f
            }
            val linePaint = Paint().apply {
                color = Color.rgb(203, 213, 225)
                strokeWidth = 1.5f
            }

            var y = 50f
            val startX = 40f
            val endX = 555f

            // Bank Title Banner
            canvas.drawText("PUBALI BANK PLC", startX, y, headerPaint)
            y += 18f
            canvas.drawText("CHIRIRBANDAR BRANCH | BRANCH MANAGEMENT SYSTEM", startX, y, subHeaderPaint)
            y += 14f
            canvas.drawText("Ref: PBL/BR/30D-NOTICE/${SimpleDateFormat("yyyy", Locale.getDefault()).format(Date())}/${item.id}", startX, y, valuePaint)
            canvas.drawText("Date: $dateStr", endX - 120f, y, labelPaint)
            y += 15f

            // Divider
            canvas.drawLine(startX, y, endX, y, linePaint)
            y += 25f

            // Notice Title
            val titlePaint = Paint().apply {
                color = Color.rgb(180, 83, 9)
                textSize = 12.5f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }
            canvas.drawText("OFFICIAL NOTICE: UNCOLLECTED ${item.type.replace("_", " ")} (> 30 DAYS)", startX, y, titlePaint)
            y += 20f

            // Customer Details Card Box
            val boxTop = y
            val boxHeight = 110f
            canvas.drawRect(startX, boxTop, endX, boxTop + boxHeight, boxBgPaint)
            canvas.drawRect(startX, boxTop, endX, boxTop + boxHeight, boxBorderPaint)

            var boxY = boxTop + 20f
            val col2X = 300f

            canvas.drawText("ACCOUNT NAME:", startX + 15f, boxY, labelPaint)
            canvas.drawText(item.customerName, startX + 120f, boxY, valuePaint)

            canvas.drawText("ITEM TYPE:", col2X, boxY, labelPaint)
            canvas.drawText(item.type.replace("_", " "), col2X + 80f, boxY, valuePaint)

            boxY += 22f
            canvas.drawText("ACCOUNT NO:", startX + 15f, boxY, labelPaint)
            canvas.drawText(item.accountNumber, startX + 120f, boxY, valuePaint)

            canvas.drawText("DAYS IN VAULT:", col2X, boxY, labelPaint)
            canvas.drawText("$daysStaying Days", col2X + 80f, boxY, valuePaint)

            boxY += 22f
            canvas.drawText("PHONE NO:", startX + 15f, boxY, labelPaint)
            canvas.drawText(item.phoneNumber, startX + 120f, boxY, valuePaint)

            canvas.drawText("RECEIVE DATE:", col2X, boxY, labelPaint)
            canvas.drawText(rxDateStr, col2X + 80f, boxY, valuePaint)

            boxY += 22f
            canvas.drawText("ADDRESS:", startX + 15f, boxY, labelPaint)
            val cleanAddr = if (item.address.length > 35) item.address.take(35) + "..." else item.address
            canvas.drawText(if (cleanAddr.isBlank()) "CHIRIRBANDAR" else cleanAddr, startX + 120f, boxY, valuePaint)

            y = boxTop + boxHeight + 30f

            // Notice Body Paragraphs
            val salutation = "Dear Valued Customer (${item.customerName}),"
            canvas.drawText(salutation, startX, y, labelPaint)
            y += 20f

            val itemLabel = item.type.replace("_", " ")
            val p1 = "This is an official communication from Pubali Bank PLC regarding your requested $itemLabel for Account Number ${item.accountNumber}. The item was safely received at our branch on $rxDateStr and has been stored in our secure vault for over $daysStaying days."

            fun drawWrappedText(text: String) {
                val maxWidth = endX - startX
                var line = ""
                for (word in text.split(" ")) {
                    val testLine = if (line.isEmpty()) word else "$line $word"
                    if (bodyPaint.measureText(testLine) > maxWidth) {
                        canvas.drawText(line, startX, y, bodyPaint)
                        y += 16f
                        line = word
                    } else {
                        line = testLine
                    }
                }
                if (line.isNotEmpty()) {
                    canvas.drawText(line, startX, y, bodyPaint)
                    y += 16f
                }
            }

            drawWrappedText(p1)
            y += 10f

            val p2 = "In accordance with standard banking security protocols, items remaining uncollected after 90 days from the date of receipt are scheduled for mandatory destruction or return to the central card/cheque issuing facility."
            drawWrappedText(p2)
            y += 10f

            val p3 = "To avoid cancellation or inconvenience, you are kindly requested to visit our Chirirbandar branch during banking hours with your original National ID (NID) or Passport to collect your $itemLabel."
            drawWrappedText(p3)
            y += 10f

            val p4 = "If you have already collected your item or received previous confirmation, please disregard this notice."
            drawWrappedText(p4)
            y += 45f

            // Signatures block
            val sigLineY = y
            canvas.drawLine(startX, sigLineY, startX + 150f, sigLineY, linePaint)
            canvas.drawLine(endX - 160f, sigLineY, endX, sigLineY, linePaint)
            y += 15f
            canvas.drawText("Prepared By (Branch Officer)", startX, y, valuePaint)
            canvas.drawText("Branch Manager / Authorized Signatory", endX - 180f, y, valuePaint)

            y += 14f
            canvas.drawText("Pubali Bank PLC", startX, y, labelPaint)
            canvas.drawText("Pubali Bank PLC, Chirirbandar", endX - 180f, y, labelPaint)

            // Footer
            y = 800f
            canvas.drawLine(startX, y, endX, y, linePaint)
            y += 15f
            val footerPaint = Paint().apply {
                color = Color.GRAY
                textSize = 8.5f
            }
            canvas.drawText("Generated by Branch Management System | Officer Toufiq | Notice Ref: #${item.id}", startX, y, footerPaint)

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
            context.startActivity(Intent.createChooser(shareIntent, "Download / Share Notice Letter PDF"))
        } catch (e: Exception) {
            e.printStackTrace()
            android.widget.Toast.makeText(context, "Error generating Notice Letter PDF: ${e.localizedMessage}", android.widget.Toast.LENGTH_LONG).show()
        }
    }
}
