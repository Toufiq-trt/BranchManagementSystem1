package com.example.util

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import androidx.core.content.FileProvider
import com.example.data.BankingItem
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
                val dateStr = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).format(Date())
                val timeStr = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
                val footerLeft = "Date: $dateStr  Time: $timeStr"
                currentCanvas.drawText(footerLeft, startX, footerY, footerPaint)

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
                val dateStr = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).format(Date())
                val timeStr = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
                val footerLeft = "Date: $dateStr  Time: $timeStr"
                currentCanvas.drawText(footerLeft, startX, footerY, footerPaint)

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
            canvas.drawText("Shimanto Bank PLC.", startX, y, headerPaint)
            y += 18f
            canvas.drawText("CHIRIRBANDAR BRANCH, DINAJPUR | BRANCH MANAGEMENT SYSTEM", startX, y, subHeaderPaint)
            y += 14f
            canvas.drawText("Ref: SBL/BR/30D-NOTICE/${SimpleDateFormat("yyyy", Locale.getDefault()).format(Date())}/${item.id}", startX, y, valuePaint)
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
            canvas.drawText(if (cleanAddr.isBlank()) "CHIRIRBANDAR, DINAJPUR" else cleanAddr, startX + 120f, boxY, valuePaint)

            y = boxTop + boxHeight + 30f

            // Notice Body Paragraphs
            val salutation = "Dear Valued Customer (${item.customerName}),"
            canvas.drawText(salutation, startX, y, labelPaint)
            y += 20f

            val itemLabel = item.type.replace("_", " ")
            val p1 = "This is an official communication from Shimanto Bank PLC. regarding your requested $itemLabel for Account Number ${item.accountNumber}. The item was safely received at our branch on $rxDateStr and has been stored in our secure vault for over $daysStaying days."

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

            val p3 = "To avoid cancellation or inconvenience, you are kindly requested to visit our Chirirbandar Branch, Dinajpur during banking hours with your original National ID (NID) or Passport to collect your $itemLabel."
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
            canvas.drawText("Shimanto Bank PLC.", startX, y, labelPaint)
            canvas.drawText("Shimanto Bank PLC., Chirirbandar Branch, Dinajpur", endX - 180f, y, labelPaint)

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
