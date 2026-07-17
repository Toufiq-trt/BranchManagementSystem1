package com.example.util

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.example.data.BankingItem
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

object DocxHelper {

    fun generateAndShareDocx(
        context: Context,
        fileName: String,
        items: List<BankingItem>
    ) {
        try {
            val file = File(context.cacheDir, fileName)
            val fos = FileOutputStream(file)
            val zos = ZipOutputStream(fos)

            // 1. [Content_Types].xml
            val contentTypesXml = """
                <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
                <Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
                    <Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
                    <Default Extension="xml" ContentType="application/xml"/>
                    <Override PartName="/word/document.xml" ContentType="application/vnd.openxmlformats-officedocument.wordprocessingml.document.main+xml"/>
                </Types>
            """.trimIndent()

            zos.putNextEntry(ZipEntry("[Content_Types].xml"))
            zos.write(contentTypesXml.toByteArray())
            zos.closeEntry()

            // 2. _rels/.rels
            val relsXml = """
                <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
                <Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
                    <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="word/document.xml"/>
                </Relationships>
            """.trimIndent()

            zos.putNextEntry(ZipEntry("_rels/.rels"))
            zos.write(relsXml.toByteArray())
            zos.closeEntry()

            // 3. word/document.xml
            val sdf = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
            val currentDate = sdf.format(Date())

            val docXmlBuilder = StringBuilder()
            docXmlBuilder.append("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>""")
            docXmlBuilder.append("""<w:document xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main">""")
            docXmlBuilder.append("<w:body>")

            items.forEachIndexed { index, item ->
                val recDate = sdf.format(Date(item.receivedDate))
                val typeLabel = when (item.type) {
                    "DEBIT_CARD" -> "DEBIT CARD"
                    "PIN" -> "PIN MAILER"
                    "CHEQUE_BOOK" -> "CHEQUE BOOK"
                    else -> "DPS SLIP"
                }

                docXmlBuilder.append("""
                    <w:p>
                        <w:r><w:rPr><w:b/><w:sz w:val="28"/></w:rPr><w:t>URGENT NOTIFICATION: UNCLAIMED SECURITY INSTRUMENT</w:t></w:r>
                    </w:p>
                    <w:p>
                        <w:r><w:rPr><w:b/></w:rPr><w:t>TOUFIQ BRANCH OPERATIONS REGISTER</w:t></w:r>
                    </w:p>
                    <w:p>
                        <w:r><w:t>Date: $currentDate</w:t></w:r>
                    </w:p>
                    <w:p>
                        <w:r><w:t>------------------------------------------------------------------</w:t></w:r>
                    </w:p>
                    <w:p>
                        <w:r><w:rPr><w:b/></w:rPr><w:t>To: ${item.customerName}</w:t></w:r>
                    </w:p>
                    <w:p>
                        <w:r><w:t>Account Number: ${item.accountNumber}</w:t></w:r>
                    </w:p>
                    <w:p>
                        <w:r><w:t>Phone Number: ${item.phoneNumber}</w:t></w:r>
                    </w:p>
                    <w:p>
                        <w:r><w:t>Address: ${item.address}</w:t></w:r>
                    </w:p>
                    <w:p>
                        <w:r><w:t>------------------------------------------------------------------</w:t></w:r>
                    </w:p>
                    <w:p>
                        <w:r><w:t>Dear ${item.customerName},</w:t></w:r>
                    </w:p>
                    <w:p>
                        <w:r><w:t>This is an official notice that your banking instrument ($typeLabel) has been received and logged at our Toufiq Branch on $recDate.</w:t></w:r>
                    </w:p>
                    <w:p>
                        <w:r><w:t>It has now remained unclaimed for more than 30 days. We request you to visit our branch during office hours to collect your instrument. Please bring a valid Photo Identification (National ID card, Passport, or driving license) for verification.</w:t></w:r>
                    </w:p>
                    <w:p>
                        <w:r><w:rPr><w:color w:val="FF0000"/><w:b/></w:rPr><w:t>IMPORTANT SECURITY NOTICE: As per banking regulatory mandates, unclaimed security items remaining in the vault for over 90 days are subject to permanent destruction.</w:t></w:r>
                    </w:p>
                    <w:p>
                        <w:r><w:t>If you have already collected your instrument, please ignore this notice.</w:t></w:r>
                    </w:p>
                    <w:p>
                        <w:r><w:t>Sincerely,</w:t></w:r>
                    </w:p>
                    <w:p>
                        <w:r><w:rPr><w:b/></w:rPr><w:t>Branch Manager, Toufiq Branch</w:t></w:r>
                    </w:p>
                """.trimIndent())

                // Insert page break between customers, except after the last customer
                if (index < items.size - 1) {
                    docXmlBuilder.append("""
                        <w:p>
                            <w:r>
                                <w:br w:type="page"/>
                            </w:r>
                        </w:p>
                    """.trimIndent())
                }
            }

            docXmlBuilder.append("</w:body>")
            docXmlBuilder.append("</w:document>")

            zos.putNextEntry(ZipEntry("word/document.xml"))
            zos.write(docXmlBuilder.toString().toByteArray())
            zos.closeEntry()

            zos.close()
            fos.close()

            // Share the file
            val authority = "${context.packageName}.fileprovider"
            val uri = FileProvider.getUriForFile(context, authority, file)

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(shareIntent, "Download DOCX via"))
        } catch (e: Exception) {
            e.printStackTrace()
            android.widget.Toast.makeText(context, "Failed to generate DOCX: ${e.localizedMessage}", android.widget.Toast.LENGTH_LONG).show()
        }
    }
}
