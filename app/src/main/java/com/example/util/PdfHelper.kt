package com.example.util

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import androidx.core.content.FileProvider
import com.example.data.BankingItem
import com.example.data.AtmLoadingLog
import java.text.NumberFormat
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

object PdfHelper {

    private var cachedFbQrBitmap: Bitmap? = null

    /**
     * Generates a QR Code Bitmap encoding the Facebook profile URL
     * with the official Facebook logo inside the center of the QR code.
     */
    private fun getFacebookQrBitmap(): Bitmap? {
        if (cachedFbQrBitmap != null) return cachedFbQrBitmap
        return try {
            val link = "https://www.facebook.com/toufiqurahmantareq/"
            val size = 200 // 200x200 px image
            val hints = EnumMap<EncodeHintType, Any>(EncodeHintType::class.java)
            hints[EncodeHintType.CHARACTER_SET] = "UTF-8"
            hints[EncodeHintType.ERROR_CORRECTION] = ErrorCorrectionLevel.H
            hints[EncodeHintType.MARGIN] = 1

            val writer = QRCodeWriter()
            val bitMatrix = writer.encode(link, BarcodeFormat.QR_CODE, size, size, hints)
            val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
            for (x in 0 until size) {
                for (y in 0 until size) {
                    bmp.setPixel(x, y, if (bitMatrix.get(x, y)) Color.BLACK else Color.WHITE)
                }
            }

            val canvas = Canvas(bmp)
            val logoSize = size / 4f // 50px
            val center = size / 2f

            // Draw white background circle for logo cutout
            val bgPaint = Paint().apply {
                color = Color.WHITE
                style = Paint.Style.FILL
                isAntiAlias = true
            }
            canvas.drawCircle(center, center, logoSize / 2f + 4f, bgPaint)

            // Draw Facebook official blue circle
            val fbPaint = Paint().apply {
                color = Color.parseColor("#1877F2")
                style = Paint.Style.FILL
                isAntiAlias = true
            }
            canvas.drawCircle(center, center, logoSize / 2f, fbPaint)

            // Draw white 'f' logo inside circle
            val fPaint = Paint().apply {
                color = Color.WHITE
                textSize = logoSize * 0.82f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textAlign = Paint.Align.CENTER
                isAntiAlias = true
            }
            val fm = fPaint.fontMetrics
            val textY = center - (fm.ascent + fm.descent) / 2f
            canvas.drawText("f", center + 1f, textY, fPaint)

            cachedFbQrBitmap = bmp
            bmp
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Draws the headline "Follow for Financial Advice" and Facebook QR code with FB symbol in center
     * at the bottom middle of the document canvas directly above "DESIGNED BY TOUFIQ".
     */
    private fun drawFacebookQrSection(
        canvas: Canvas,
        pageWidth: Int,
        designedByY: Float
    ) {
        val qrBitmap = getFacebookQrBitmap() ?: return
        val centerX = pageWidth / 2f

        val headline = "Follow for Financial Advice"
        val headlinePaint = Paint().apply {
            color = Color.parseColor("#1877F2") // Official Facebook Blue
            textSize = 7.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
        }

        val qrSize = 36f // 36pt x 36pt
        val qrBottom = designedByY - 10f
        val qrTop = qrBottom - qrSize
        val headlineY = qrTop - 4f

        canvas.drawText(headline, centerX, headlineY, headlinePaint)

        val qrLeft = centerX - qrSize / 2f
        val destRect = RectF(qrLeft, qrTop, qrLeft + qrSize, qrBottom)
        canvas.drawBitmap(qrBitmap, null, destRect, null)
    }

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
            } else if (headers.contains("ACCOUNT NAME") || headers.contains("PARTICULAR")) {
                // 5 columns: ACCOUNT NAME, ACCOUNT NUMBER, PHONE NO., RECEIVED DATE, PARTICULAR
                listOf(125f, 95f, 90f, 85f, 140f)
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
                
                // Left side: Clean Date & Time
                val dateStr = SimpleDateFormat("dd-MM-yyyy HH:mm", Locale.getDefault()).format(Date())
                currentCanvas.drawText("Generated: $dateStr", startX, footerY, footerPaint)
                
                // Middle: DESIGNED BY TOUFIQ
                val footerMid = "DESIGNED BY TOUFIQ"
                val footerMidWidth = footerPaint.measureText(footerMid)
                currentCanvas.drawText(footerMid, (595f - footerMidWidth) / 2f, footerY, footerPaint)

                drawFacebookQrSection(currentCanvas, 595, footerY)
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
            val pageBottomLimit = 760f

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
     * Generates a polished financial chart PDF with responsive column widths, landscape support,
     * diagonal watermark, highlighted columns, segment category borders, and official contact footer.
     */
    fun generateCustomChartPdf(
        context: Context,
        fileName: String,
        chartTitle: String,
        chartSubtitle: String,
        headers: List<String>,
        rows: List<List<String>>,
        highlightColumns: List<Int> = emptyList(),
        thickBorderColumns: List<Int> = emptyList()
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

            val pinkHighlightPaint = Paint().apply {
                color = Color.parseColor("#FFF0F5") // Light Pink for Women column
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

            val thickBorderPaint = Paint().apply {
                color = Color.parseColor("#0F172A") // Dark Slate thick border
                style = Paint.Style.STROKE
                strokeWidth = 2.2f
            }

            val footerPaint = Paint().apply {
                color = Color.GRAY
                textSize = 8f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            }

            val officerBoldPaint = Paint().apply {
                color = Color.BLACK
                textSize = 8f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textAlign = Paint.Align.RIGHT
            }

            val officerNormalPaint = Paint().apply {
                color = Color.DKGRAY
                textSize = 7.5f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                textAlign = Paint.Align.RIGHT
            }

            val officerGoldPaint = Paint().apply {
                color = Color.parseColor("#B8860B")
                textSize = 8f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textAlign = Paint.Align.RIGHT
            }

            val whatsappLinkPaint = Paint().apply {
                color = Color.parseColor("#15803D") // WhatsApp Green
                textSize = 8f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textAlign = Paint.Align.RIGHT
            }

            var currentPageNumber = 1
            var pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, currentPageNumber).create()
            var page = pdfDocument.startPage(pageInfo)
            var canvas = page.canvas

            fun drawWatermark(currentCanvas: Canvas) {
                currentCanvas.save()
                val wmPaint = Paint().apply {
                    color = Color.argb(45, 184, 134, 11) // Crisp semi-transparent gold
                    textSize = 32f
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                    textAlign = Paint.Align.CENTER
                }
                currentCanvas.rotate(-25f, pageWidth / 2f, pageHeight / 2f)
                currentCanvas.drawText("For FD and DPS", pageWidth / 2f, pageHeight / 2f - 20f, wmPaint)
                currentCanvas.drawText("WhatsApp on : 01517836078", pageWidth / 2f, pageHeight / 2f + 20f, wmPaint)
                currentCanvas.restore()
            }

            fun drawOfficerBlock(currentCanvas: Canvas) {
                val rightX = (pageWidth - 30).toFloat()
                var blockY = (pageHeight - 75).toFloat()

                currentCanvas.drawText("Md Toufiqur Rahman (Toufiq)", rightX, blockY, officerGoldPaint)
                blockY += 10f
                currentCanvas.drawText("Trainee Assistant Officer", rightX, blockY, officerNormalPaint)
                blockY += 10f
                currentCanvas.drawText("Shimanto Bank PLC.", rightX, blockY, officerBoldPaint)
                blockY += 10f
                currentCanvas.drawText("Chirirbandar Branch, Dinajpur.", rightX, blockY, officerNormalPaint)
                blockY += 10f
                currentCanvas.drawText("WhatsApp: 01517836078", rightX, blockY, officerBoldPaint)
            }

            fun drawFooter(currentCanvas: Canvas) {
                drawWatermark(currentCanvas) // Render watermark ON TOP of row backgrounds so it is 100% visible
                val footerY = (pageHeight - 15).toFloat()
                
                val dateStr = SimpleDateFormat("dd-MM-yyyy HH:mm", Locale.getDefault()).format(Date())
                currentCanvas.drawText("Generated: $dateStr", startX, footerY, footerPaint)

                val footerMid = "DESIGNED BY TOUFIQ"
                val footerMidWidth = footerPaint.measureText(footerMid)
                currentCanvas.drawText(footerMid, (pageWidth - footerMidWidth) / 2f, footerY, footerPaint)

                drawFacebookQrSection(currentCanvas, pageWidth, footerY)

                val pageStr = "Page $currentPageNumber"
                val pageStrWidth = footerPaint.measureText(pageStr)
                currentCanvas.drawText(pageStr, pageWidth - startX - pageStrWidth, footerY, footerPaint)

                drawOfficerBlock(currentCanvas)
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
            val pageBottomLimit = (pageHeight - 85).toFloat() // Leave room for officer contact block at bottom right

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

                    // Fill cell background if highlighted column (e.g., Women DPS columns)
                    if (!isHeader && i in highlightColumns) {
                        currentCanvas.drawRect(x, currentY, x + colWidth, currentY + rowHeight, pinkHighlightPaint)
                    }

                    // Standard cell border
                    currentCanvas.drawRect(x, currentY, x + colWidth, currentY + rowHeight, borderPaint)

                    // Draw thick vertical segment border if specified
                    if (i in thickBorderColumns) {
                        currentCanvas.drawLine(x + colWidth, currentY, x + colWidth, currentY + rowHeight, thickBorderPaint)
                    }

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
     * Specialized combined PDF generator for DPS (includes both General/Women DPS table and Millionaire Scheme table on the same chart document).
     */
    fun generateDpsCombinedPdf(
        context: Context,
        fileName: String,
        chartTitle: String,
        chartSubtitle: String,
        headers1: List<String>,
        rows1: List<List<String>>,
        headers2: List<String>,
        rows2: List<List<String>>
    ) {
        try {
            val pdfDocument = PdfDocument()
            val pageWidth = 842 // A4 Landscape width
            val pageHeight = 595 // A4 Landscape height
            val startX = 20f
            val startY = 75f
            val totalWidth = (pageWidth - 40).toFloat()

            // Paints
            val systemTitlePaint = Paint().apply {
                color = Color.parseColor("#B8860B")
                textSize = 10f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }
            val titlePaint = Paint().apply {
                color = Color.parseColor("#0F172A")
                textSize = 13f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }
            val subtitlePaint = Paint().apply {
                color = Color.parseColor("#475569")
                textSize = 8.5f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            }
            val sectionBannerPaint = Paint().apply {
                color = Color.parseColor("#1E293B")
                style = Paint.Style.FILL
            }
            val sectionTextPaint = Paint().apply {
                color = Color.parseColor("#FFD700")
                textSize = 9f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }
            val headerBgPaint = Paint().apply {
                color = Color.parseColor("#0F172A")
                style = Paint.Style.FILL
            }
            val headerTextPaint = Paint().apply {
                color = Color.parseColor("#FFD700")
                textSize = 7.5f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }
            val cellTextPaint = Paint().apply {
                color = Color.BLACK
                textSize = 7.5f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            }
            val borderPaint = Paint().apply {
                color = Color.parseColor("#CBD5E1")
                style = Paint.Style.STROKE
                strokeWidth = 0.5f
            }
            val pinkHighlightPaint = Paint().apply {
                color = Color.parseColor("#FFE4E1") // Light pink highlight for women columns
                style = Paint.Style.FILL
            }
            val goldHighlightPaint = Paint().apply {
                color = Color.parseColor("#FEF3C7") // Light gold highlight for maturity
                style = Paint.Style.FILL
            }
            val rowBgAltPaint = Paint().apply {
                color = Color.parseColor("#F8FAFC")
                style = Paint.Style.FILL
            }
            val footerPaint = Paint().apply {
                color = Color.parseColor("#64748B")
                textSize = 7.5f
            }
            val officerGoldPaint = Paint().apply {
                color = Color.parseColor("#B8860B")
                textSize = 8f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textAlign = Paint.Align.RIGHT
            }
            val officerBoldPaint = Paint().apply {
                color = Color.parseColor("#0F172A")
                textSize = 8f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textAlign = Paint.Align.RIGHT
            }
            val officerNormalPaint = Paint().apply {
                color = Color.parseColor("#475569")
                textSize = 7.5f
                textAlign = Paint.Align.RIGHT
            }

            var currentPageNumber = 1
            var pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, currentPageNumber).create()
            var page = pdfDocument.startPage(pageInfo)
            var canvas = page.canvas

            fun drawWatermark(currentCanvas: Canvas) {
                currentCanvas.save()
                val wmPaint = Paint().apply {
                    color = Color.argb(45, 184, 134, 11)
                    textSize = 32f
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                    textAlign = Paint.Align.CENTER
                }
                currentCanvas.rotate(-25f, pageWidth / 2f, pageHeight / 2f)
                currentCanvas.drawText("For FD and DPS", pageWidth / 2f, pageHeight / 2f - 20f, wmPaint)
                currentCanvas.drawText("WhatsApp on : 01517836078", pageWidth / 2f, pageHeight / 2f + 20f, wmPaint)
                currentCanvas.restore()
            }

            fun drawOfficerBlock(currentCanvas: Canvas) {
                val rightX = (pageWidth - 30).toFloat()
                var blockY = (pageHeight - 75).toFloat()

                currentCanvas.drawText("Md Toufiqur Rahman (Toufiq)", rightX, blockY, officerGoldPaint)
                blockY += 10f
                currentCanvas.drawText("Trainee Assistant Officer", rightX, blockY, officerNormalPaint)
                blockY += 10f
                currentCanvas.drawText("Shimanto Bank PLC.", rightX, blockY, officerBoldPaint)
                blockY += 10f
                currentCanvas.drawText("Chirirbandar Branch, Dinajpur.", rightX, blockY, officerNormalPaint)
                blockY += 10f
                currentCanvas.drawText("WhatsApp: 01517836078", rightX, blockY, officerBoldPaint)
            }

            fun drawFooter(currentCanvas: Canvas) {
                drawWatermark(currentCanvas)
                val footerY = (pageHeight - 15).toFloat()
                
                val dateStr = SimpleDateFormat("dd-MM-yyyy HH:mm", Locale.getDefault()).format(Date())
                currentCanvas.drawText("Generated: $dateStr", startX, footerY, footerPaint)

                val footerMid = "DESIGNED BY TOUFIQ"
                val footerMidWidth = footerPaint.measureText(footerMid)
                currentCanvas.drawText(footerMid, (pageWidth - footerMidWidth) / 2f, footerY, footerPaint)

                drawFacebookQrSection(currentCanvas, pageWidth, footerY)

                val pageStr = "Page $currentPageNumber"
                val pageStrWidth = footerPaint.measureText(pageStr)
                currentCanvas.drawText(pageStr, pageWidth - startX - pageStrWidth, footerY, footerPaint)

                drawOfficerBlock(currentCanvas)
            }

            fun drawHeader(currentCanvas: Canvas) {
                val sysTitle = "TOUFIQS SMART BANKING MANAGEMENT"
                val sysWidth = systemTitlePaint.measureText(sysTitle)
                currentCanvas.drawText(sysTitle, (pageWidth - sysWidth) / 2f, 32f, systemTitlePaint)

                val tWidth = titlePaint.measureText(chartTitle)
                currentCanvas.drawText(chartTitle, (pageWidth - tWidth) / 2f, 48f, titlePaint)

                val stWidth = subtitlePaint.measureText(chartSubtitle)
                currentCanvas.drawText(chartSubtitle, (pageWidth - stWidth) / 2f, 62f, subtitlePaint)
            }

            drawHeader(canvas)

            var y = startY
            val headerRowHeight = 20f
            val dataRowHeight = 15f
            val pageBottomLimit = (pageHeight - 85).toFloat()

            // Draw Section 1 Header Banner
            canvas.drawRect(startX, y, startX + totalWidth, y + 18f, sectionBannerPaint)
            val sec1Text = "--- 1. GENERAL (10.00% p.a.) & WOMEN (10.50% p.a.) DPS SAVINGS MATURITY TABLE ---"
            val sec1Width = sectionTextPaint.measureText(sec1Text)
            canvas.drawText(sec1Text, startX + (totalWidth - sec1Width) / 2f, y + 13f, sectionTextPaint)
            y += 20f

            // Table 1 (9 columns)
            val colWidth1 = totalWidth / headers1.size.toFloat()
            canvas.drawRect(startX, y, startX + totalWidth, y + headerRowHeight, headerBgPaint)
            var x1 = startX
            for (i in headers1.indices) {
                canvas.drawRect(x1, y, x1 + colWidth1, y + headerRowHeight, borderPaint)
                val text = headers1[i]
                val truncated = getTruncatedText(text, headerTextPaint, colWidth1 - 4f)
                val textY = y + (headerRowHeight / 2f) - ((headerTextPaint.descent() + headerTextPaint.ascent()) / 2f)
                val textX = x1 + (colWidth1 - headerTextPaint.measureText(truncated)) / 2f
                canvas.drawText(truncated, textX, textY, headerTextPaint)
                x1 += colWidth1
            }
            y += headerRowHeight

            // Table 1 Rows
            for (rowIndex in rows1.indices) {
                if (y + dataRowHeight > pageBottomLimit) {
                    drawFooter(canvas)
                    pdfDocument.finishPage(page)
                    currentPageNumber++
                    pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, currentPageNumber).create()
                    page = pdfDocument.startPage(pageInfo)
                    canvas = page.canvas
                    drawHeader(canvas)
                    y = startY
                }
                val isAlt = rowIndex % 2 == 1
                if (isAlt) {
                    canvas.drawRect(startX, y, startX + totalWidth, y + dataRowHeight, rowBgAltPaint)
                }
                val rowData = rows1[rowIndex]
                var rowX = startX
                for (cIdx in rowData.indices) {
                    if (cIdx in listOf(2, 4, 6, 8)) {
                        canvas.drawRect(rowX, y, rowX + colWidth1, y + dataRowHeight, pinkHighlightPaint)
                    }
                    canvas.drawRect(rowX, y, rowX + colWidth1, y + dataRowHeight, borderPaint)
                    val text = rowData[cIdx]
                    val truncated = getTruncatedText(text, cellTextPaint, colWidth1 - 4f)
                    val textY = y + (dataRowHeight / 2f) - ((cellTextPaint.descent() + cellTextPaint.ascent()) / 2f)
                    val textX = rowX + (colWidth1 - cellTextPaint.measureText(truncated)) / 2f
                    canvas.drawText(truncated, textX, textY, cellTextPaint)
                    rowX += colWidth1
                }
                y += dataRowHeight
            }

            y += 12f // Spacer

            // Check space for Section 2
            if (y + 100f > pageBottomLimit) {
                drawFooter(canvas)
                pdfDocument.finishPage(page)
                currentPageNumber++
                pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, currentPageNumber).create()
                page = pdfDocument.startPage(pageInfo)
                canvas = page.canvas
                drawHeader(canvas)
                y = startY
            }

            // Draw Section 2 Header Banner
            canvas.drawRect(startX, y, startX + totalWidth, y + 20f, sectionBannerPaint)
            val sec2Text = "★ 2. SPECIAL MILLIONAIRE SCHEME (GUARANTEED Tk 10,00,000 / 10 LAC ON MATURITY) ★"
            val sec2Width = sectionTextPaint.measureText(sec2Text)
            canvas.drawText(sec2Text, startX + (totalWidth - sec2Width) / 2f, y + 14f, sectionTextPaint)
            y += 22f

            // Table 2 (4 columns)
            val colWidth2 = totalWidth / headers2.size.toFloat()
            canvas.drawRect(startX, y, startX + totalWidth, y + headerRowHeight, headerBgPaint)
            var x2 = startX
            for (i in headers2.indices) {
                canvas.drawRect(x2, y, x2 + colWidth2, y + headerRowHeight, borderPaint)
                val text = headers2[i]
                val truncated = getTruncatedText(text, headerTextPaint, colWidth2 - 4f)
                val textY = y + (headerRowHeight / 2f) - ((headerTextPaint.descent() + headerTextPaint.ascent()) / 2f)
                val textX = x2 + (colWidth2 - headerTextPaint.measureText(truncated)) / 2f
                canvas.drawText(truncated, textX, textY, headerTextPaint)
                x2 += colWidth2
            }
            y += headerRowHeight

            // Table 2 Rows
            for (rowIndex in rows2.indices) {
                if (y + dataRowHeight > pageBottomLimit) {
                    drawFooter(canvas)
                    pdfDocument.finishPage(page)
                    currentPageNumber++
                    pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, currentPageNumber).create()
                    page = pdfDocument.startPage(pageInfo)
                    canvas = page.canvas
                    drawHeader(canvas)
                    y = startY
                }
                val isAlt = rowIndex % 2 == 1
                if (isAlt) {
                    canvas.drawRect(startX, y, startX + totalWidth, y + dataRowHeight, rowBgAltPaint)
                }
                val rowData = rows2[rowIndex]
                var rowX = startX
                for (cIdx in rowData.indices) {
                    if (cIdx == 3) {
                        canvas.drawRect(rowX, y, rowX + colWidth2, y + dataRowHeight, goldHighlightPaint)
                    }
                    canvas.drawRect(rowX, y, rowX + colWidth2, y + dataRowHeight, borderPaint)
                    val text = rowData[cIdx]
                    val truncated = getTruncatedText(text, cellTextPaint, colWidth2 - 4f)
                    val textY = y + (dataRowHeight / 2f) - ((cellTextPaint.descent() + cellTextPaint.ascent()) / 2f)
                    val textX = rowX + (colWidth2 - cellTextPaint.measureText(truncated)) / 2f
                    canvas.drawText(truncated, textX, textY, cellTextPaint)
                    rowX += colWidth2
                }
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
            context.startActivity(Intent.createChooser(shareIntent, "Share DPS Combined Chart PDF via"))
        } catch (e: Exception) {
            e.printStackTrace()
            android.widget.Toast.makeText(context, "Failed to generate DPS Combined Chart PDF: ${e.localizedMessage}", android.widget.Toast.LENGTH_LONG).show()
        }
    }

    private enum class IconType {
        USER, CALENDAR, CLOCK, DOWNLOAD, UPLOAD, STACK_CASH, MONEY_BAG, PENCIL
    }

    private fun drawSquareIcon(
        canvas: Canvas,
        x: Float,
        y: Float,
        size: Float,
        bgColor: Int,
        iconType: IconType
    ) {
        // Draw rounded rectangle background
        val bgRect = RectF(x, y, x + size, y + size)
        val bgPaint = Paint().apply { color = bgColor; style = Paint.Style.FILL; isAntiAlias = true }
        canvas.drawRoundRect(bgRect, 4f, 4f, bgPaint)

        val iconPaint = Paint().apply {
            color = Color.WHITE
            style = Paint.Style.STROKE
            strokeWidth = 1.6f
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
            isAntiAlias = true
        }
        val fillPaint = Paint().apply {
            color = Color.WHITE
            style = Paint.Style.FILL
            isAntiAlias = true
        }

        val cx = x + size / 2f
        val cy = y + size / 2f

        when (iconType) {
            IconType.USER -> {
                canvas.drawCircle(cx, cy - 3f, 3.2f, iconPaint)
                val bodyPath = Path().apply {
                    arcTo(RectF(cx - 5.5f, cy + 0.5f, cx + 5.5f, cy + 8f), 180f, 180f, false)
                }
                canvas.drawPath(bodyPath, iconPaint)
            }
            IconType.CALENDAR -> {
                val calRect = RectF(cx - 5.5f, cy - 3.5f, cx + 5.5f, cy + 5.5f)
                canvas.drawRoundRect(calRect, 1.5f, 1.5f, iconPaint)
                canvas.drawLine(cx - 5.5f, cy - 1f, cx + 5.5f, cy - 1f, iconPaint)
                canvas.drawCircle(cx - 2.5f, cy + 2f, 0.8f, fillPaint)
                canvas.drawCircle(cx + 2.5f, cy + 2f, 0.8f, fillPaint)
                canvas.drawLine(cx - 2.5f, cy - 5.5f, cx - 2.5f, cy - 3.5f, iconPaint)
                canvas.drawLine(cx + 2.5f, cy - 5.5f, cx + 2.5f, cy - 3.5f, iconPaint)
            }
            IconType.CLOCK -> {
                canvas.drawCircle(cx, cy, 5.5f, iconPaint)
                canvas.drawLine(cx, cy, cx, cy - 3.2f, iconPaint)
                canvas.drawLine(cx, cy, cx + 2.2f, cy, iconPaint)
            }
            IconType.DOWNLOAD -> {
                canvas.drawLine(cx - 5.5f, cy + 1.5f, cx - 5.5f, cy + 4.5f, iconPaint)
                canvas.drawLine(cx - 5.5f, cy + 4.5f, cx + 5.5f, cy + 4.5f, iconPaint)
                canvas.drawLine(cx + 5.5f, cy + 4.5f, cx + 5.5f, cy + 1.5f, iconPaint)
                canvas.drawLine(cx, cy - 4.5f, cx, cy + 1.5f, iconPaint)
                val arrowHead = Path().apply {
                    moveTo(cx - 2.8f, cy - 1f)
                    lineTo(cx, cy + 1.5f)
                    lineTo(cx + 2.8f, cy - 1f)
                }
                canvas.drawPath(arrowHead, iconPaint)
            }
            IconType.UPLOAD -> {
                canvas.drawLine(cx - 5.5f, cy + 1.5f, cx - 5.5f, cy + 4.5f, iconPaint)
                canvas.drawLine(cx - 5.5f, cy + 4.5f, cx + 5.5f, cy + 4.5f, iconPaint)
                canvas.drawLine(cx + 5.5f, cy + 4.5f, cx + 5.5f, cy + 1.5f, iconPaint)
                canvas.drawLine(cx, cy + 2f, cx, cy - 4.5f, iconPaint)
                val arrowHead = Path().apply {
                    moveTo(cx - 2.8f, cy - 2f)
                    lineTo(cx, cy - 4.5f)
                    lineTo(cx + 2.8f, cy - 2f)
                }
                canvas.drawPath(arrowHead, iconPaint)
            }
            IconType.STACK_CASH -> {
                val bill1 = RectF(cx - 5.5f, cy - 4.5f, cx + 3.5f, cy + 0.5f)
                val bill2 = RectF(cx - 3.5f, cy - 1.5f, cx + 5.5f, cy + 3.5f)
                canvas.drawRoundRect(bill1, 1f, 1f, iconPaint)
                canvas.drawRoundRect(bill2, 1f, 1f, iconPaint)
                canvas.drawCircle(cx + 1f, cy + 1f, 1f, fillPaint)
            }
            IconType.MONEY_BAG -> {
                val bagPath = Path().apply {
                    moveTo(cx - 2f, cy - 4.5f)
                    lineTo(cx + 2f, cy - 4.5f)
                    lineTo(cx + 2.5f, cy - 2.5f)
                    arcTo(RectF(cx - 5.5f, cy - 2.5f, cx + 5.5f, cy + 5.5f), -60f, 300f, false)
                    close()
                }
                canvas.drawPath(bagPath, iconPaint)
                val takaTextPaint = Paint().apply {
                    color = Color.WHITE
                    textSize = 6f
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                    textAlign = Paint.Align.CENTER
                    isAntiAlias = true
                }
                canvas.drawText("৳", cx, cy + 3.2f, takaTextPaint)
            }
            IconType.PENCIL -> {
                val pencilPath = Path().apply {
                    moveTo(cx - 4.5f, cy + 3.5f)
                    lineTo(cx - 4.5f, cy + 1f)
                    lineTo(cx + 1.5f, cy - 5f)
                    lineTo(cx + 4f, cy - 2.5f)
                    lineTo(cx - 2f, cy + 3.5f)
                    close()
                }
                canvas.drawPath(pencilPath, iconPaint)
            }
        }
    }

    /**
     * Generates a beautifully structured ATM Replenishment PDF report matching reference standard.
     */
    fun generateAtmReplenishmentPdf(
        context: Context,
        fileName: String,
        log: AtmLoadingLog
    ) {
        val document = PdfDocument()
        val pageWidth = 595
        val pageHeight = 842
        val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create()
        val page = document.startPage(pageInfo)
        val canvas = page.canvas

        // Pure white background
        val bgPaint = Paint().apply { color = Color.WHITE }
        canvas.drawRect(0f, 0f, pageWidth.toFloat(), pageHeight.toFloat(), bgPaint)

        // Colors matching exact brand identity
        val darkNavy = Color.parseColor("#0B2341")
        val accentBlue = Color.parseColor("#1D3557")
        val borderGray = Color.parseColor("#CBD5E1")
        val gridBorderColor = Color.parseColor("#E2E8F0")

        val marginX = 35f
        val tableW = 525f

        // 1. Top Right Corner Diagonal Stripes
        val stripeNavy = Paint().apply { color = darkNavy; strokeWidth = 3.5f; style = Paint.Style.STROKE; isAntiAlias = true }
        val stripeAccent = Paint().apply { color = accentBlue; strokeWidth = 3.5f; style = Paint.Style.STROKE; isAntiAlias = true }
        
        canvas.drawLine(525f, 15f, 580f, 15f, stripeNavy)
        canvas.drawLine(535f, 21f, 580f, 21f, stripeAccent)
        canvas.drawLine(545f, 27f, 580f, 27f, stripeNavy)

        // 2. Main Title: ATM LOAD REPORT
        val mainTitlePaint = Paint().apply {
            color = darkNavy
            textSize = 24f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
        }
        canvas.drawText("ATM LOAD REPORT", pageWidth / 2f, 52f, mainTitlePaint)

        // Diamond rule under main title
        val lineY = 62f
        val rulePaint = Paint().apply { color = darkNavy; strokeWidth = 1f; isAntiAlias = true }
        canvas.drawLine(pageWidth / 2f - 90f, lineY, pageWidth / 2f - 8f, lineY, rulePaint)
        canvas.drawLine(pageWidth / 2f + 8f, lineY, pageWidth / 2f + 90f, lineY, rulePaint)

        val diamondPath = Path().apply {
            moveTo(pageWidth / 2f, lineY - 3.5f)
            lineTo(pageWidth / 2f + 3.5f, lineY)
            lineTo(pageWidth / 2f, lineY + 3.5f)
            lineTo(pageWidth / 2f - 3.5f, lineY)
            close()
        }
        val diamondPaint = Paint().apply { color = darkNavy; style = Paint.Style.FILL; isAntiAlias = true }
        canvas.drawPath(diamondPath, diamondPaint)

        // Subtitle
        val subTitlePaint = Paint().apply {
            color = darkNavy
            textSize = 9.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
            letterSpacing = 0.08f
            isAntiAlias = true
        }
        canvas.drawText("TOUFIQ'S SMART BANKING SOLUTION", pageWidth / 2f, 78f, subTitlePaint)

        var currentY = 94f

        // 3. Pill Badge
        val pillHeight = 26f
        val pillRect = RectF(65f, currentY, pageWidth - 65f, currentY + pillHeight)
        val pillBgPaint = Paint().apply { color = darkNavy; style = Paint.Style.FILL; isAntiAlias = true }
        canvas.drawRoundRect(pillRect, 6f, 6f, pillBgPaint)

        val pillTextPaint = Paint().apply {
            color = Color.WHITE
            textSize = 10.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
            letterSpacing = 0.04f
            isAntiAlias = true
        }
        val badgeText = "ATM REPLENISHMENT SLIP OF ${log.atmName.uppercase()}"
        canvas.drawText(badgeText, pageWidth / 2f, currentY + 17f, pillTextPaint)

        currentY += 38f

        // 4. Load Information Card
        val infoCardHeight = 84f
        val infoCardRect = RectF(marginX, currentY, marginX + tableW, currentY + infoCardHeight)
        val cardBgPaint = Paint().apply { color = Color.WHITE; style = Paint.Style.FILL }
        val cardBorderPaint = Paint().apply { color = darkNavy; style = Paint.Style.STROKE; strokeWidth = 1f; isAntiAlias = true }

        canvas.drawRoundRect(infoCardRect, 8f, 8f, cardBgPaint)
        canvas.drawRoundRect(infoCardRect, 8f, 8f, cardBorderPaint)

        val iconBoxSize = 22f
        val r1Y = currentY + 10f
        val r2Y = currentY + 35f
        val r3Y = currentY + 60f

        val lblPaint = Paint().apply { color = darkNavy; textSize = 10f; typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL); isAntiAlias = true }
        val valPaint = Paint().apply { color = darkNavy; textSize = 10.5f; typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD); isAntiAlias = true }

        // Row 1: Load By
        drawSquareIcon(canvas, marginX + 14f, r1Y, iconBoxSize, darkNavy, IconType.USER)
        canvas.drawText("Load By", marginX + 48f, r1Y + 15f, lblPaint)
        canvas.drawText(":", marginX + 110f, r1Y + 15f, lblPaint)
        canvas.drawText(log.operatorName.uppercase(), marginX + 122f, r1Y + 15f, valPaint)

        // Row 2: Date
        drawSquareIcon(canvas, marginX + 14f, r2Y, iconBoxSize, darkNavy, IconType.CALENDAR)
        canvas.drawText("Date", marginX + 48f, r2Y + 15f, lblPaint)
        canvas.drawText(":", marginX + 110f, r2Y + 15f, lblPaint)
        canvas.drawText(log.dateStr, marginX + 122f, r2Y + 15f, valPaint)

        // Row 3: Time
        drawSquareIcon(canvas, marginX + 14f, r3Y, iconBoxSize, darkNavy, IconType.CLOCK)
        canvas.drawText("Time", marginX + 48f, r3Y + 15f, lblPaint)
        canvas.drawText(":", marginX + 110f, r3Y + 15f, lblPaint)
        val timeDisplay = if (log.timeStr.isNotBlank()) log.timeStr else SimpleDateFormat("hh:mm:ss a", Locale.US).format(Date())
        canvas.drawText(timeDisplay, marginX + 122f, r3Y + 15f, valPaint)

        currentY += infoCardHeight + 22f

        val formatter = NumberFormat.getNumberInstance(Locale.US)

        // 5. Section 1: CASSETTE WISE RECEIVED (REMAINING)
        val secHeaderHeight = 22f
        drawSquareIcon(canvas, marginX, currentY, secHeaderHeight, darkNavy, IconType.DOWNLOAD)

        val secTitlePaint = Paint().apply {
            color = darkNavy
            textSize = 11f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            letterSpacing = 0.02f
            isAntiAlias = true
        }
        val sec1Title = "CASSETTE WISE RECEIVED (REMAINING)"
        canvas.drawText(sec1Title, marginX + 30f, currentY + 15f, secTitlePaint)

        val titleW1 = secTitlePaint.measureText(sec1Title)
        canvas.drawLine(marginX + 38f + titleW1, currentY + 11f, marginX + tableW, currentY + 11f, rulePaint)

        currentY += secHeaderHeight + 10f

        // Table 1 Header
        val thHeight = 22f
        val tdHeight = 22f
        val tableHeaderRect1 = RectF(marginX, currentY, marginX + tableW, currentY + thHeight)
        val thBgPaint = Paint().apply { color = darkNavy; style = Paint.Style.FILL; isAntiAlias = true }
        canvas.drawRoundRect(tableHeaderRect1, 3f, 3f, thBgPaint)

        val thLeftPaint = Paint().apply { color = Color.WHITE; textSize = 9.5f; typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD); isAntiAlias = true }
        val thCenterPaint = Paint().apply { color = Color.WHITE; textSize = 9.5f; typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD); textAlign = Paint.Align.CENTER; isAntiAlias = true }

        canvas.drawText("CASSETTE", marginX + 30f, currentY + 15f, thLeftPaint)
        canvas.drawText("DENOMINATION", marginX + 260f, currentY + 15f, thCenterPaint)
        canvas.drawText("NOTES RECEIVED", marginX + 440f, currentY + 15f, thCenterPaint)

        currentY += thHeight

        val recData = listOf(
            Triple("Cassette 1", "1000 Taka", "${log.c1Remaining} notes"),
            Triple("Cassette 2", "1000 Taka", "${log.c2Remaining} notes"),
            Triple("Cassette 3", "500 Taka", "${log.c3Remaining} notes"),
            Triple("Cassette 4", "500 Taka", "${log.c4Remaining} notes")
        )

        val tdLeft = Paint().apply { color = darkNavy; textSize = 9.5f; typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL); isAntiAlias = true }
        val tdCenter = Paint().apply { color = darkNavy; textSize = 9.5f; typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL); textAlign = Paint.Align.CENTER; isAntiAlias = true }
        val gridStrokePaint = Paint().apply { color = gridBorderColor; style = Paint.Style.STROKE; strokeWidth = 1f; isAntiAlias = true }

        recData.forEachIndexed { idx, (cas, den, notes) ->
            val rRect = RectF(marginX, currentY, marginX + tableW, currentY + tdHeight)
            val rBg = if (idx % 2 == 0) Color.WHITE else Color.parseColor("#F8FAFC")
            canvas.drawRect(rRect, Paint().apply { color = rBg })
            canvas.drawRect(rRect, gridStrokePaint)

            canvas.drawText(cas, marginX + 30f, currentY + 15f, tdLeft)
            canvas.drawText(den, marginX + 260f, currentY + 15f, tdCenter)
            canvas.drawText(notes, marginX + 440f, currentY + 15f, tdCenter)

            currentY += tdHeight
        }

        currentY += 10f

        // Section 1 Summary Cards (3 Cards)
        val cardWidth = (tableW - 16f) / 3f
        val cardHeight = 44f

        val tot1000Rec = log.c1Remaining + log.c2Remaining
        val tot500Rec = log.c3Remaining + log.c4Remaining
        val totRecAmount = (tot1000Rec * 1000L) + (tot500Rec * 500L)

        val summary1 = listOf(
            Triple(IconType.STACK_CASH, "Total Received 1000 Notes", "${formatter.format(tot1000Rec)} Pcs"),
            Triple(IconType.STACK_CASH, "Total Received 500 Notes", "${formatter.format(tot500Rec)} Pcs"),
            Triple(IconType.MONEY_BAG, "Total Received Amount", "BDT ${formatter.format(totRecAmount)}")
        )

        summary1.forEachIndexed { i, (icon, label, value) ->
            val cX = marginX + i * (cardWidth + 8f)
            val cRect = RectF(cX, currentY, cX + cardWidth, currentY + cardHeight)
            canvas.drawRoundRect(cRect, 6f, 6f, cardBgPaint)
            canvas.drawRoundRect(cRect, 6f, 6f, cardBorderPaint)

            drawSquareIcon(canvas, cX + 8f, currentY + 10f, 24f, darkNavy, icon)

            val cLblPaint = Paint().apply { color = darkNavy; textSize = 7.5f; typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL); isAntiAlias = true }
            val cValPaint = Paint().apply { color = darkNavy; textSize = 11f; typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD); isAntiAlias = true }

            canvas.drawText(label, cX + 38f, currentY + 18f, cLblPaint)
            canvas.drawText(value, cX + 38f, currentY + 34f, cValPaint)
        }

        currentY += cardHeight + 22f

        // 6. Section 2: CASSETTE WISE LOADED (REPLENISHMENT)
        drawSquareIcon(canvas, marginX, currentY, secHeaderHeight, darkNavy, IconType.UPLOAD)

        val sec2Title = "CASSETTE WISE LOADED (REPLENISHMENT)"
        canvas.drawText(sec2Title, marginX + 30f, currentY + 15f, secTitlePaint)

        val titleW2 = secTitlePaint.measureText(sec2Title)
        canvas.drawLine(marginX + 38f + titleW2, currentY + 11f, marginX + tableW, currentY + 11f, rulePaint)

        currentY += secHeaderHeight + 10f

        // Table 2 Header
        val tableHeaderRect2 = RectF(marginX, currentY, marginX + tableW, currentY + thHeight)
        canvas.drawRoundRect(tableHeaderRect2, 3f, 3f, thBgPaint)

        canvas.drawText("CASSETTE", marginX + 30f, currentY + 15f, thLeftPaint)
        canvas.drawText("DENOMINATION", marginX + 260f, currentY + 15f, thCenterPaint)
        canvas.drawText("NOTES LOADED", marginX + 440f, currentY + 15f, thCenterPaint)

        currentY += thHeight

        val loadData = listOf(
            Triple("Cassette 1", "1000 Taka", "${log.c1Loading} notes"),
            Triple("Cassette 2", "1000 Taka", "${log.c2Loading} notes"),
            Triple("Cassette 3", "500 Taka", "${log.c3Loading} notes"),
            Triple("Cassette 4", "500 Taka", "${log.c4Loading} notes")
        )

        loadData.forEachIndexed { idx, (cas, den, notes) ->
            val rRect = RectF(marginX, currentY, marginX + tableW, currentY + tdHeight)
            val rBg = if (idx % 2 == 0) Color.WHITE else Color.parseColor("#F8FAFC")
            canvas.drawRect(rRect, Paint().apply { color = rBg })
            canvas.drawRect(rRect, gridStrokePaint)

            canvas.drawText(cas, marginX + 30f, currentY + 15f, tdLeft)
            canvas.drawText(den, marginX + 260f, currentY + 15f, tdCenter)
            canvas.drawText(notes, marginX + 440f, currentY + 15f, tdCenter)

            currentY += tdHeight
        }

        currentY += 10f

        // Section 2 Summary Cards (3 Cards)
        val tot1000Load = log.c1Loading + log.c2Loading
        val tot500Load = log.c3Loading + log.c4Loading
        val totLoadAmount = log.loadingAmount

        val summary2 = listOf(
            Triple(IconType.STACK_CASH, "Total Loaded 1000 Notes", "${formatter.format(tot1000Load)} Pcs"),
            Triple(IconType.STACK_CASH, "Total Loaded 500 Notes", "${formatter.format(tot500Load)} Pcs"),
            Triple(IconType.MONEY_BAG, "TOTAL LOAD AMOUNT", "BDT ${formatter.format(totLoadAmount)}")
        )

        summary2.forEachIndexed { i, (icon, label, value) ->
            val cX = marginX + i * (cardWidth + 8f)
            val cRect = RectF(cX, currentY, cX + cardWidth, currentY + cardHeight)
            canvas.drawRoundRect(cRect, 6f, 6f, cardBgPaint)
            canvas.drawRoundRect(cRect, 6f, 6f, cardBorderPaint)

            drawSquareIcon(canvas, cX + 8f, currentY + 10f, 24f, darkNavy, icon)

            val cLblPaint = Paint().apply { color = darkNavy; textSize = 7.5f; typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL); isAntiAlias = true }
            val cValPaint = Paint().apply { color = darkNavy; textSize = 11f; typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD); isAntiAlias = true }

            canvas.drawText(label, cX + 38f, currentY + 18f, cLblPaint)
            canvas.drawText(value, cX + 38f, currentY + 34f, cValPaint)
        }

        currentY += cardHeight + 16f

        // 7. Remarks Section
        val remBoxHeight = 32f
        val remRect = RectF(marginX, currentY, marginX + tableW, currentY + remBoxHeight)
        canvas.drawRoundRect(remRect, 6f, 6f, cardBgPaint)
        canvas.drawRoundRect(remRect, 6f, 6f, cardBorderPaint)

        drawSquareIcon(canvas, marginX + 8f, currentY + 5f, 22f, darkNavy, IconType.PENCIL)

        val remTextPaint = Paint().apply { color = darkNavy; textSize = 10f; typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD); isAntiAlias = true }
        val remarkStr = if (log.remarks.isNotBlank()) log.remarks.uppercase() else "N/A"
        canvas.drawText("Remarks: $remarkStr", marginX + 38f, currentY + 20f, remTextPaint)

        // 8. Loader Sign Section (Bottom Right)
        val sigY = 742f

        // Dotted line for Loader Signature
        val dottedPaint = Paint().apply {
            color = darkNavy
            strokeWidth = 1f
            style = Paint.Style.STROKE
            pathEffect = DashPathEffect(floatArrayOf(2f, 2f), 0f)
            isAntiAlias = true
        }
        canvas.drawLine(pageWidth - 200f, sigY, pageWidth - 40f, sigY, dottedPaint)

        val sigTextPaint = Paint().apply {
            color = darkNavy
            textSize = 10f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
        }
        canvas.drawText("Loader Sign", pageWidth - 120f, sigY + 16f, sigTextPaint)

        // 9. Footer Bar
        val footerY = 812f
        
        // Footer line with center text and diamonds
        val ftLineY = footerY - 12f
        canvas.drawLine(35f, ftLineY, pageWidth - 35f, ftLineY, rulePaint)

        // Small diamonds on the footer line
        val ftDiamondLeft = Path().apply {
            moveTo(35f, ftLineY - 2.5f)
            lineTo(37.5f, ftLineY)
            lineTo(35f, ftLineY + 2.5f)
            lineTo(32.5f, ftLineY)
            close()
        }
        val ftDiamondRight = Path().apply {
            moveTo(pageWidth - 35f, ftLineY - 2.5f)
            lineTo(pageWidth - 32.5f, ftLineY)
            lineTo(pageWidth - 35f, ftLineY + 2.5f)
            lineTo(pageWidth - 37.5f, ftLineY)
            close()
        }
        canvas.drawPath(ftDiamondLeft, diamondPaint)
        canvas.drawPath(ftDiamondRight, diamondPaint)

        val footerTextPaint = Paint().apply {
            color = darkNavy
            textSize = 8.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
            letterSpacing = 0.05f
            isAntiAlias = true
        }
        canvas.drawText("THIS REPORT IS GENERATED BY TOUFIQ'S SMART BANKING SOLUTION APP", pageWidth / 2f, footerY, footerTextPaint)

        // Bottom right corner stripes
        canvas.drawLine(525f, 826f, 580f, 826f, stripeNavy)
        canvas.drawLine(535f, 831f, 580f, 831f, stripeAccent)
        canvas.drawLine(545f, 836f, 580f, 836f, stripeNavy)

        document.finishPage(page)

        // Save and Share
        try {
            val dir = File(context.cacheDir, "pdf_reports")
            if (!dir.exists()) dir.mkdirs()
            val file = File(dir, fileName)
            FileOutputStream(file).use { out ->
                document.writeTo(out)
            }
            document.close()

            val contentUri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, contentUri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(shareIntent, "Share ATM Load Report"))
        } catch (e: Exception) {
            e.printStackTrace()
            document.close()
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
        generateNoticeLetterBatchPdf(context, fileName, listOf(item))
    }

    fun generateNoticeLetterBatchPdf(
        context: Context,
        fileName: String,
        items: List<BankingItem>
    ) {
        if (items.isEmpty()) return
        try {
            val pdfDocument = PdfDocument()

            val fontTimesNormal = Typeface.create("serif", Typeface.NORMAL)
            val fontTimesBold = Typeface.create("serif", Typeface.BOLD)
            val fontTimesBoldItalic = Typeface.create("serif", Typeface.BOLD_ITALIC)

            val currentMonthUpper = SimpleDateFormat("MMMM", Locale.ENGLISH).format(Date()).uppercase()
            val currentDateStr = SimpleDateFormat("dd/MM/yyyy", Locale.ENGLISH).format(Date())

            // A4 Dimensions & 1 inch (72 points) margins
            val pageWidth = 595
            val pageHeight = 842
            val startX = 72f
            val endX = 523f // 595 - 72
            val tableWidth = endX - startX // 451f

            val labelPaint = Paint().apply {
                color = Color.BLACK
                textSize = 11f
                typeface = fontTimesBold
                isAntiAlias = true
            }
            val valuePaint = Paint().apply {
                color = Color.BLACK
                textSize = 11f
                typeface = fontTimesNormal
                isAntiAlias = true
            }
            val bodyPaint = Paint().apply {
                color = Color.BLACK
                textSize = 11f
                typeface = fontTimesNormal
                isAntiAlias = true
            }
            val boldItalicBodyPaint = Paint().apply {
                color = Color.BLACK
                textSize = 11f
                typeface = fontTimesBoldItalic
                isAntiAlias = true
            }
            val linePaint = Paint().apply {
                color = Color.BLACK
                strokeWidth = 1f
            }
            val borderPaint = Paint().apply {
                color = Color.BLACK
                style = Paint.Style.STROKE
                strokeWidth = 1f
            }

            for (pageIndex in items.indices) {
                val item = items[pageIndex]
                val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageIndex + 1).create()
                val page = pdfDocument.startPage(pageInfo)
                val canvas = page.canvas

                val itemTypeTag = when (item.type) {
                    "DEBIT_CARD" -> "Debit Card and PIN"
                    "PIN" -> "Debit Card and PIN"
                    "CHEQUE_BOOK" -> "Cheque Book"
                    "DPS" -> "DPS Document"
                    else -> item.type.replace("_", " ")
                }

                val itemTypeTagBody = when (item.type) {
                    "DEBIT_CARD" -> "Debit Card and PIN"
                    "PIN" -> "Debit Card and PIN"
                    "CHEQUE_BOOK" -> "cheque book"
                    "DPS" -> "DPS document"
                    else -> item.type.lowercase()
                }

                // Logo Header Section
                val logoBitmap = try {
                    android.graphics.BitmapFactory.decodeResource(context.resources, com.example.R.drawable.shimanto_bank_header)
                        ?: android.graphics.BitmapFactory.decodeResource(context.resources, com.example.R.drawable.app_logo)
                } catch (e: Exception) {
                    null
                }

                var y = 35f
                if (logoBitmap != null) {
                    val logoWidth = 240f
                    val logoHeight = logoWidth * (logoBitmap.height.toFloat() / logoBitmap.width.toFloat())
                    val logoPaint = Paint().apply {
                        isAntiAlias = true
                        isFilterBitmap = true
                        isDither = true
                    }
                    canvas.drawBitmap(logoBitmap, null, RectF(startX, y, startX + logoWidth, y + logoHeight), logoPaint)
                    y += logoHeight + 20f
                } else {
                    y = 80f
                }

                // Reference & Date Lines
                val refStr = "Ref: SMBPLC/7003/$itemTypeTag/2026/$currentMonthUpper/"
                canvas.drawText(refStr, startX, y, valuePaint)
                y += 18f
                canvas.drawText("Date: $currentDateStr", startX, y, valuePaint)
                y += 20f

                // Customer Recipient 2-Column Table (Bold labels & bold values)
                val col1Width = 110f
                val col2Width = tableWidth - col1Width
                val tableRight = endX
                val rowHeight = 22f

                val tableRows = listOf(
                    "Name" to item.customerName,
                    "Father Name" to "",
                    "Address" to if (item.address.isBlank()) "CHIRIRBANDAR, DINAJPUR" else item.address,
                    "Reg no" to item.regNo,
                    "Mobile No" to item.phoneNumber
                )

                for ((label, value) in tableRows) {
                    canvas.drawRect(startX, y, tableRight, y + rowHeight, borderPaint)
                    canvas.drawLine(startX + col1Width, y, startX + col1Width, y + rowHeight, borderPaint)

                    canvas.drawText(label, startX + 8f, y + 15f, labelPaint)
                    canvas.drawText(value, startX + col1Width + 8f, y + 15f, labelPaint)

                    y += rowHeight
                }
                y += 24f

                // Subject Line (Bold)
                val subjectText = "Subject: Collection of $itemTypeTag."
                canvas.drawText(subjectText, startX, y, labelPaint)
                y += 36f // 2 line gap after Subject

                // Salutation (Bold)
                canvas.drawText("Dear Sir / Madam,", startX, y, labelPaint)
                y += 24f // 1 line gap after Salutation

                // Helper for Justified Text (Ctrl+J) rendering
                data class WordToken(val text: String, val paint: Paint)

                fun drawJustifiedParagraph(tokens: List<WordToken>, lineGap: Float = 18f): Float {
                    val wordList = mutableListOf<WordToken>()
                    for (token in tokens) {
                        for (w in token.text.split(" ")) {
                            if (w.isNotBlank()) {
                                wordList.add(WordToken(w, token.paint))
                            }
                        }
                    }
                    if (wordList.isEmpty()) return y

                    val lineWords = mutableListOf<WordToken>()
                    var lineLength = 0f

                    var i = 0
                    while (i < wordList.size) {
                        val wordToken = wordList[i]
                        val wWidth = wordToken.paint.measureText(wordToken.text)
                        val sWidth = wordToken.paint.measureText(" ")
                        val totalNeeded = if (lineWords.isEmpty()) wWidth else lineLength + sWidth + wWidth

                        if (totalNeeded <= tableWidth) {
                            lineWords.add(wordToken)
                            lineLength = totalNeeded
                            i++
                        } else {
                            // Render justified line
                            val totalWordWidths = lineWords.sumOf { it.paint.measureText(it.text).toDouble() }.toFloat()
                            val extraSpace = tableWidth - totalWordWidths
                            val gapCount = lineWords.size - 1
                            val gapWidth = if (gapCount > 0) extraSpace / gapCount else sWidth

                            var currX = startX
                            for (idxWord in lineWords.indices) {
                                val itemW = lineWords[idxWord]
                                canvas.drawText(itemW.text, currX, y, itemW.paint)
                                currX += itemW.paint.measureText(itemW.text) + gapWidth
                            }
                            y += lineGap
                            lineWords.clear()
                            lineLength = 0f
                        }
                    }

                    // Render final line left aligned
                    if (lineWords.isNotEmpty()) {
                        var currX = startX
                        for (itemW in lineWords) {
                            canvas.drawText(itemW.text, currX, y, itemW.paint)
                            currX += itemW.paint.measureText(itemW.text) + itemW.paint.measureText(" ")
                        }
                        y += lineGap
                    }
                    return y
                }

                // Paragraph 0 (Justified)
                val p0Tokens = listOf(
                    WordToken("Thank you for choosing Shimanto Bank PLC as your banking partner.", bodyPaint)
                )
                drawJustifiedParagraph(p0Tokens)
                y += 18f

                // Paragraph 1 (Acct Number Bold Italic, Justified)
                val p1Tokens = listOf(
                    WordToken("This is to inform you that your $itemTypeTagBody of account number", bodyPaint),
                    WordToken("“${item.accountNumber}”", boldItalicBodyPaint),
                    WordToken("have not been collected from our Chirirbandar Branch. You are hereby requested to collect the $itemTypeTagBody within 30 days from the issuing date of this letter.", bodyPaint)
                )
                drawJustifiedParagraph(p1Tokens)
                y += 18f

                // Paragraph 2 (Justified)
                val p2Tokens = listOf(
                    WordToken("Please note as per bank’s laid down process, we will destroy your $itemTypeTagBody if not collected within the stipulated time frame and a destruction charge will be deducted from your account as per our published schedule of charges.", bodyPaint)
                )
                drawJustifiedParagraph(p2Tokens)
                y += 18f

                // Paragraph 3 (Justified)
                val p3Tokens = listOf(
                    WordToken("Hence, kindly consider this as the final reminder from the bank to collect your $itemTypeTagBody within the mentioned time. We appreciate your cooperation in helping us to serve you better.", bodyPaint)
                )
                drawJustifiedParagraph(p3Tokens)
                y += 20f

                // Contact line
                val p4 = "Please feel free to contact us for any further assistance. 09610870230-34"
                canvas.drawText(p4, startX, y, labelPaint)
                y += 45f // 2 line gap before Yours Sincerely

                // Yours Sincerely & Authorised Signatories
                canvas.drawText("Yours Sincerely,", startX, y, labelPaint)
                y += 60f // 3 line gap before signatures

                val sig1X = startX
                val sig2X = endX - 140f
                canvas.drawLine(sig1X, y, sig1X + 130f, y, linePaint)
                canvas.drawLine(sig2X, y, sig2X + 130f, y, linePaint)
                y += 16f
                canvas.drawText("Authorised signatory", sig1X, y, labelPaint)
                canvas.drawText("Authorised signatory", sig2X, y, labelPaint)

                pdfDocument.finishPage(page)
            }

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
            context.startActivity(Intent.createChooser(shareIntent, "Download / Print Notice Letters PDF"))
        } catch (e: Exception) {
            e.printStackTrace()
            android.widget.Toast.makeText(context, "Error generating Notice Letter PDF: ${e.localizedMessage}", android.widget.Toast.LENGTH_LONG).show()
        }
    }

    /**
     * Builds grouped, sorted search result report rows and title/filename based on user specifications:
     * - Filename: "<Query> Cheque and Card list.pdf"
     * - Columns: ACCOUNT NAME | ACCOUNT NUMBER | PHONE NO. | RECEIVED DATE | PARTICULAR
     * - Sorting: First customers having BOTH Cheque and Card, then ONLY Cheque, then ONLY Card.
     * - PARTICULAR value: "Cheque and Card", "Cheque Book", or "Debit Card".
     */
    fun buildSearchResultReportRows(
        query: String,
        items: List<BankingItem>
    ): Triple<String, List<String>, List<List<String>>> {
        val cleanQuery = query.trim().ifEmpty { "Search" }
        val fileName = "$cleanQuery Cheque and Card list.pdf"
        val headers = listOf("ACCOUNT NAME", "ACCOUNT NUMBER", "PHONE NO.", "RECEIVED DATE", "PARTICULAR")
        val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

        val grouped = items.groupBy { 
            if (it.accountNumber.isNotBlank()) it.accountNumber.trim() else it.customerName.trim()
        }

        data class AccountReportData(
            val name: String,
            val acNo: String,
            val phone: String,
            val dateStr: String,
            val recDate: Long,
            val particular: String,
            val groupRank: Int
        )

        val reportDataList = grouped.map { (_, itemList) ->
            val hasCheque = itemList.any { it.type.equals("CHEQUE_BOOK", ignoreCase = true) }
            val hasCard = itemList.any { it.type.equals("DEBIT_CARD", ignoreCase = true) || it.type.equals("PIN", ignoreCase = true) }

            val first = itemList.first()
            val latestRecDate = itemList.maxOfOrNull { it.receivedDate } ?: first.receivedDate
            val dateStr = sdf.format(Date(latestRecDate))

            val pair: Pair<String, Int> = when {
                hasCheque && hasCard -> Pair("Cheque and Card", 1)
                hasCheque -> Pair("Cheque Book", 2)
                hasCard -> if (itemList.any { it.type.equals("DEBIT_CARD", ignoreCase = true) }) Pair("Debit Card", 3) else Pair("PIN Mailer", 3)
                else -> Pair("DPS", 4)
            }
            val particular = pair.first
            val groupRank = pair.second

            AccountReportData(
                name = first.customerName,
                acNo = first.accountNumber,
                phone = first.phoneNumber,
                dateStr = dateStr,
                recDate = latestRecDate,
                particular = particular,
                groupRank = groupRank
            )
        }

        val sortedData = reportDataList.sortedWith(compareBy<AccountReportData> { it.groupRank }.thenByDescending { it.recDate })

        val rows = sortedData.map {
            listOf(it.name, it.acNo, it.phone, it.dateStr, it.particular)
        }

        return Triple(fileName, headers, rows)
    }

    data class FinancialAdvisorInput(
        val name: String = "",
        val age: String = "",
        val employment: String = "Service",
        val salary: Double = 0.0,
        val otherIncome: Double = 0.0,
        val rentExpense: Double = 0.0,
        val foodExpense: Double = 0.0,
        val electricityExpense: Double = 0.0,
        val transportExpense: Double = 0.0,
        val otherExpense: Double = 0.0,
        val totalIncome: Double = 0.0,
        val totalExpense: Double = 0.0,
        val monthlySavings: Double = 0.0,
        val cashInHand: Double = 0.0,
        val hasLoan: Boolean = false,
        val loanAmount: Double = 0.0,
        val loanOutstanding: Double = 0.0,
        val loanEmi: Double = 0.0,
        val loanFinishDate: String = "",
        val hasRunningSavings: Boolean = false,
        val savingsCategory: String = "DPS",
        val savingsAmount: Double = 0.0,
        val savingsTenure: Double = 0.0,
        val savingsInterestRate: Double = 0.0,
        val fdrPayoutType: String = "Interest Reinvested (1 Year)",
        val savingsStartDate: String = "",
        val savingsFinishDate: String = "",
        // Legacy compatibility defaults
        val businessIncome: Double = 0.0,
        val monthlyExpenses: Double = 0.0,
        val currentDps: Double = 0.0,
        val currentFdr: Double = 0.0,
        val currentLoan: Double = 0.0,
        val loanInterestRate: Double = 0.0,
        val loanRemainingYears: Double = 0.0,
        val availableLoanCash: Double = 0.0,
        val creditCardLimit: Double = 0.0,
        val targetWealth: Double = 0.0,
        val targetYears: Int = 10,
        val riskProfile: String = "Moderate"
    )

    fun generateFinancialAdvisorPdf(
        context: Context,
        fileName: String,
        input: FinancialAdvisorInput,
        reportText: String
    ) {
        val pdfDocument = PdfDocument()
        val pageWidth = 595
        val pageHeight = 842
        val marginX = 35f
        val contentW = 525f

        var currentPageNumber = 1
        var pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, currentPageNumber).create()
        var page = pdfDocument.startPage(pageInfo)
        var canvas = page.canvas

        val bgPaint = Paint().apply { color = Color.WHITE }
        val darkNavy = Color.parseColor("#0B2341")
        val goldPrimary = Color.parseColor("#D4AF37")
        val slateDark = Color.parseColor("#1E293B")
        val cardBgColor = Color.parseColor("#F8FAFC")

        val titlePaint = Paint().apply {
            color = darkNavy
            textSize = 18f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
        }

        val subTitlePaint = Paint().apply {
            color = goldPrimary
            textSize = 10f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
            letterSpacing = 0.08f
            isAntiAlias = true
        }

        val headerLabelPaint = Paint().apply {
            color = darkNavy
            textSize = 10f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }

        val bodyTextPaint = Paint().apply {
            color = slateDark
            textSize = 9.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            isAntiAlias = true
        }

        val bodyBoldPaint = Paint().apply {
            color = darkNavy
            textSize = 9.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }

        val footerPaint = Paint().apply {
            color = Color.parseColor("#64748B")
            textSize = 8f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            isAntiAlias = true
        }

        val officerGoldPaint = Paint().apply {
            color = goldPrimary
            textSize = 9f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.RIGHT
            isAntiAlias = true
        }

        val officerBoldPaint = Paint().apply {
            color = darkNavy
            textSize = 8.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.RIGHT
            isAntiAlias = true
        }

        val officerNormalPaint = Paint().apply {
            color = slateDark
            textSize = 8f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            textAlign = Paint.Align.RIGHT
            isAntiAlias = true
        }

        val wmPaint = Paint().apply {
            color = Color.parseColor("#12000000")
            textSize = 30f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
        }

        fun drawWatermarkOnCanvas(c: Canvas) {
            c.save()
            c.rotate(-25f, pageWidth / 2f, pageHeight / 2f)
            c.drawText("For FD and DPS", pageWidth / 2f, pageHeight / 2f - 20f, wmPaint)
            c.drawText("WhatsApp on : 01517836078", pageWidth / 2f, pageHeight / 2f + 20f, wmPaint)
            c.restore()
        }

        fun drawFooterOnCanvas(c: Canvas, isLastPage: Boolean) {
            drawWatermarkOnCanvas(c)
            val footerY = (pageHeight - 15).toFloat()
            val dateStr = SimpleDateFormat("dd-MM-yyyy HH:mm", Locale.getDefault()).format(Date())
            c.drawText("Generated: $dateStr", marginX, footerY, footerPaint)

            val footerMid = "DESIGNED BY TOUFIQ"
            val footerMidWidth = footerPaint.measureText(footerMid)
            c.drawText(footerMid, (pageWidth - footerMidWidth) / 2f, footerY, footerPaint)

            getFacebookQrBitmap()?.let { qrBmp ->
                val qrSize = 36f
                val qrX = (pageWidth / 2f) + (footerMidWidth / 2f) + 10f
                val qrY = footerY - 26f
                c.drawBitmap(qrBmp, null, RectF(qrX, qrY, qrX + qrSize, qrY + qrSize), null)
            }

            val pageStr = "Page $currentPageNumber"
            val pageStrWidth = footerPaint.measureText(pageStr)
            c.drawText(pageStr, pageWidth - marginX - pageStrWidth, footerY, footerPaint)

            if (isLastPage) {
                val rightX = (pageWidth - 35).toFloat()
                var blockY = (pageHeight - 75).toFloat()

                c.drawText("Md Toufiqur Rahman (Toufiq)", rightX, blockY, officerGoldPaint)
                blockY += 10f
                c.drawText("Trainee Assistant Officer", rightX, blockY, officerNormalPaint)
                blockY += 10f
                c.drawText("Shimanto Bank PLC.", rightX, blockY, officerBoldPaint)
                blockY += 10f
                c.drawText("Chirirbandar Branch, Dinajpur.", rightX, blockY, officerNormalPaint)
                blockY += 10f
                c.drawText("WhatsApp: 01517836078", rightX, blockY, officerBoldPaint)
            }
        }

        fun startNewPage() {
            drawFooterOnCanvas(canvas, false)
            pdfDocument.finishPage(page)
            currentPageNumber++
            pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, currentPageNumber).create()
            page = pdfDocument.startPage(pageInfo)
            canvas = page.canvas
            canvas.drawRect(0f, 0f, pageWidth.toFloat(), pageHeight.toFloat(), bgPaint)
        }

        canvas.drawRect(0f, 0f, pageWidth.toFloat(), pageHeight.toFloat(), bgPaint)

        // Title Block
        canvas.drawText("TOUFIQ FINANCIAL ADVISOR", pageWidth / 2f, 42f, titlePaint)
        canvas.drawText("SHIMANTO BANK PLC. | WEALTH MANAGEMENT ADVISORY", pageWidth / 2f, 58f, subTitlePaint)

        var currentY = 74f

        // Customer Summary Header Card
        val cardHeight = 65f
        val cardRect = RectF(marginX, currentY, marginX + contentW, currentY + cardHeight)
        val cardBgPaint = Paint().apply { color = cardBgColor; style = Paint.Style.FILL }
        val cardBorderPaint = Paint().apply { color = darkNavy; style = Paint.Style.STROKE; strokeWidth = 1f; isAntiAlias = true }
        canvas.drawRoundRect(cardRect, 6f, 6f, cardBgPaint)
        canvas.drawRoundRect(cardRect, 6f, 6f, cardBorderPaint)

        val fmt = NumberFormat.getNumberInstance(Locale.US)
        val nameStr = if (input.name.isNotBlank()) input.name else "Valued Client"
        val incVal = if (input.totalIncome > 0) input.totalIncome else (input.salary + input.otherIncome)
        val expVal = if (input.totalExpense > 0) input.totalExpense else input.monthlyExpenses
        val savVal = if (input.monthlySavings > 0) input.monthlySavings else (incVal - expVal).coerceAtLeast(0.0)

        canvas.drawText("Client: $nameStr (Age: ${input.age}, ${input.employment})", marginX + 12f, currentY + 18f, headerLabelPaint)
        canvas.drawText("Income: BDT ${fmt.format(incVal)} | Expense: BDT ${fmt.format(expVal)} | Net Savings: BDT ${fmt.format(savVal)}", marginX + 12f, currentY + 34f, bodyTextPaint)
        canvas.drawText("Cash in Hand: BDT ${fmt.format(input.cashInHand)} | Loan: ${if (input.hasLoan) "BDT ${fmt.format(input.loanAmount)}" else "No"} | DPS/FDR: ${if (input.hasRunningSavings) "${input.savingsCategory} BDT ${fmt.format(input.savingsAmount)}" else "No"}", marginX + 12f, currentY + 50f, bodyBoldPaint)

        currentY += cardHeight + 15f

        val lines = reportText.split("\n")
        val lineSpacing = 14f

        for (line in lines) {
            if (currentY > pageHeight - 110f) {
                startNewPage()
                currentY = 45f
            }

            val trimmed = line.trim()
            if (trimmed.isEmpty()) {
                currentY += 6f
                continue
            }

            if (trimmed.startsWith("---") || trimmed.startsWith("===")) {
                val rulePaint = Paint().apply { color = goldPrimary; strokeWidth = 1f }
                canvas.drawLine(marginX, currentY, marginX + contentW, currentY, rulePaint)
                currentY += 10f
                continue
            }

            val isSectionHeader = trimmed.uppercase() == trimmed && trimmed.length < 50 && !trimmed.contains("BDT") && !trimmed.contains(":")
            val isStep = trimmed.startsWith("Step") || trimmed.startsWith("Strategy") || trimmed.contains("FINANCIAL SUMMARY") || trimmed.contains("RECOMMENDED STRATEGY") || trimmed.contains("DETAILED INVESTMENT ROADMAP") || trimmed.contains("SHOW TIMELINE") || trimmed.contains("SHOW FINAL RESULT")

            val paintToUse = when {
                isSectionHeader || isStep -> Paint().apply {
                    color = darkNavy
                    textSize = 10f
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                    isAntiAlias = true
                }
                else -> bodyTextPaint
            }

            var start = 0
            while (start < trimmed.length) {
                val count = paintToUse.breakText(trimmed, start, trimmed.length, true, contentW, null)
                val sub = trimmed.substring(start, start + count)
                canvas.drawText(sub, marginX, currentY, paintToUse)
                currentY += lineSpacing
                start += count

                if (currentY > pageHeight - 110f) {
                    startNewPage()
                    currentY = 45f
                }
            }
        }

        drawFooterOnCanvas(canvas, true)
        pdfDocument.finishPage(page)

        try {
            val dir = File(context.cacheDir, "pdf_reports")
            if (!dir.exists()) dir.mkdirs()
            val file = File(dir, fileName)
            FileOutputStream(file).use { out ->
                pdfDocument.writeTo(out)
            }
            pdfDocument.close()

            val contentUri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, contentUri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(shareIntent, "Share Financial Advisor Report"))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun getProcessedSignatureBitmap(context: Context): Bitmap? {
        try {
            val raw = BitmapFactory.decodeResource(context.resources, com.example.R.drawable.signature_toufiq) ?: return null
            val mutable = raw.copy(Bitmap.Config.ARGB_8888, true)
            val width = mutable.width
            val height = mutable.height
            val pixels = IntArray(width * height)
            mutable.getPixels(pixels, 0, width, 0, 0, width, height)
            for (i in pixels.indices) {
                val color = pixels[i]
                val r = (color shr 16) and 0xFF
                val g = (color shr 8) and 0xFF
                val b = color and 0xFF
                val brightness = (r * 0.299 + g * 0.587 + b * 0.114).toInt()
                if (brightness > 175) {
                    pixels[i] = 0x00FFFFFF // Transparent background
                } else {
                    // Dark sharp ink
                    pixels[i] = (0xFF shl 24) or (15 and 0xFF shl 16) or (25 and 0xFF shl 8) or (45 and 0xFF)
                }
            }
            mutable.setPixels(pixels, 0, width, 0, 0, width, height)
            return mutable
        } catch (e: Exception) {
            return null
        }
    }
}
