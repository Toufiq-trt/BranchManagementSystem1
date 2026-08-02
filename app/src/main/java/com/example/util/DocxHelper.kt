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
            val currentMonthUpper = SimpleDateFormat("MMMM", Locale.ENGLISH).format(Date()).uppercase()
            val currentDateStr = SimpleDateFormat("dd/MM/yyyy", Locale.ENGLISH).format(Date())

            val docXmlBuilder = StringBuilder()
            docXmlBuilder.append("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>""")
            docXmlBuilder.append("""<w:document xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main">""")
            docXmlBuilder.append("<w:body>")

            items.forEachIndexed { index, item ->
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

                docXmlBuilder.append("""
                    <w:p>
                        <w:r><w:rPr><w:rFonts w:ascii="Times New Roman" w:hAnsi="Times New Roman"/><w:sz w:val="22"/></w:rPr><w:t>Ref: SMBPLC/7003/$itemTypeTag/2026/$currentMonthUpper/</w:t></w:r>
                    </w:p>
                    <w:p>
                        <w:r><w:rPr><w:rFonts w:ascii="Times New Roman" w:hAnsi="Times New Roman"/><w:sz w:val="22"/></w:rPr><w:t>Date: $currentDateStr</w:t></w:r>
                    </w:p>
                    <w:tbl>
                        <w:tblPr>
                            <w:tblW w:w="5000" w:type="pct"/>
                            <w:tblBorders>
                                <w:top w:val="single" w:sz="4" w:space="0" w:color="000000"/>
                                <w:left w:val="single" w:sz="4" w:space="0" w:color="000000"/>
                                <w:bottom w:val="single" w:sz="4" w:space="0" w:color="000000"/>
                                <w:right w:val="single" w:sz="4" w:space="0" w:color="000000"/>
                                <w:insideH w:val="single" w:sz="4" w:space="0" w:color="000000"/>
                                <w:insideV w:val="single" w:sz="4" w:space="0" w:color="000000"/>
                            </w:tblBorders>
                        </w:tblPr>
                        <w:tr>
                            <w:tc><w:p><w:r><w:rPr><w:rFonts w:ascii="Times New Roman" w:hAnsi="Times New Roman"/><w:b/><w:sz w:val="22"/></w:rPr><w:t>Name</w:t></w:r></w:p></w:tc>
                            <w:tc><w:p><w:r><w:rPr><w:rFonts w:ascii="Times New Roman" w:hAnsi="Times New Roman"/><w:b/><w:sz w:val="22"/></w:rPr><w:t>${item.customerName}</w:t></w:r></w:p></w:tc>
                        </w:tr>
                        <w:tr>
                            <w:tc><w:p><w:r><w:rPr><w:rFonts w:ascii="Times New Roman" w:hAnsi="Times New Roman"/><w:b/><w:sz w:val="22"/></w:rPr><w:t>Father Name</w:t></w:r></w:p></w:tc>
                            <w:tc><w:p><w:r><w:rPr><w:rFonts w:ascii="Times New Roman" w:hAnsi="Times New Roman"/><w:b/><w:sz w:val="22"/></w:rPr><w:t></w:t></w:r></w:p></w:tc>
                        </w:tr>
                        <w:tr>
                            <w:tc><w:p><w:r><w:rPr><w:rFonts w:ascii="Times New Roman" w:hAnsi="Times New Roman"/><w:b/><w:sz w:val="22"/></w:rPr><w:t>Address</w:t></w:r></w:p></w:tc>
                            <w:tc><w:p><w:r><w:rPr><w:rFonts w:ascii="Times New Roman" w:hAnsi="Times New Roman"/><w:b/><w:sz w:val="22"/></w:rPr><w:t>${if (item.address.isBlank()) "CHIRIRBANDAR, DINAJPUR" else item.address}</w:t></w:r></w:p></w:tc>
                        </w:tr>
                        <w:tr>
                            <w:tc><w:p><w:r><w:rPr><w:rFonts w:ascii="Times New Roman" w:hAnsi="Times New Roman"/><w:b/><w:sz w:val="22"/></w:rPr><w:t>Reg no</w:t></w:r></w:p></w:tc>
                            <w:tc><w:p><w:r><w:rPr><w:rFonts w:ascii="Times New Roman" w:hAnsi="Times New Roman"/><w:b/><w:sz w:val="22"/></w:rPr><w:t>${item.regNo}</w:t></w:r></w:p></w:tc>
                        </w:tr>
                        <w:tr>
                            <w:tc><w:p><w:r><w:rPr><w:rFonts w:ascii="Times New Roman" w:hAnsi="Times New Roman"/><w:b/><w:sz w:val="22"/></w:rPr><w:t>Mobile No</w:t></w:r></w:p></w:tc>
                            <w:tc><w:p><w:r><w:rPr><w:rFonts w:ascii="Times New Roman" w:hAnsi="Times New Roman"/><w:b/><w:sz w:val="22"/></w:rPr><w:t>${item.phoneNumber}</w:t></w:r></w:p></w:tc>
                        </w:tr>
                    </w:tbl>
                    <w:p/>
                    <w:p>
                        <w:r><w:rPr><w:rFonts w:ascii="Times New Roman" w:hAnsi="Times New Roman"/><w:b/><w:sz w:val="22"/></w:rPr><w:t>Subject: Collection of $itemTypeTag.</w:t></w:r>
                    </w:p>
                    <w:p/>
                    <w:p/>
                    <w:p>
                        <w:r><w:rPr><w:rFonts w:ascii="Times New Roman" w:hAnsi="Times New Roman"/><w:b/><w:sz w:val="22"/></w:rPr><w:t>Dear Sir / Madam,</w:t></w:r>
                    </w:p>
                    <w:p/>
                    <w:p>
                        <w:pPr><w:jc w:val="both"/></w:pPr>
                        <w:r><w:rPr><w:rFonts w:ascii="Times New Roman" w:hAnsi="Times New Roman"/><w:sz w:val="22"/></w:rPr><w:t>Thank you for choosing Shimanto Bank PLC as your banking partner.</w:t></w:r>
                    </w:p>
                    <w:p/>
                    <w:p>
                        <w:pPr><w:jc w:val="both"/></w:pPr>
                        <w:r><w:rPr><w:rFonts w:ascii="Times New Roman" w:hAnsi="Times New Roman"/><w:sz w:val="22"/></w:rPr><w:t>This is to inform you that your $itemTypeTagBody of account number </w:t></w:r>
                        <w:r><w:rPr><w:rFonts w:ascii="Times New Roman" w:hAnsi="Times New Roman"/><w:b/><w:i/><w:sz w:val="22"/></w:rPr><w:t>“${item.accountNumber}”</w:t></w:r>
                        <w:r><w:rPr><w:rFonts w:ascii="Times New Roman" w:hAnsi="Times New Roman"/><w:sz w:val="22"/></w:rPr><w:t> have not been collected from our Chirirbandar Branch. You are hereby requested to collect the $itemTypeTagBody within 30 days from the issuing date of this letter.</w:t></w:r>
                    </w:p>
                    <w:p/>
                    <w:p>
                        <w:pPr><w:jc w:val="both"/></w:pPr>
                        <w:r><w:rPr><w:rFonts w:ascii="Times New Roman" w:hAnsi="Times New Roman"/><w:sz w:val="22"/></w:rPr><w:t>Please note as per bank’s laid down process, we will destroy your $itemTypeTagBody if not collected within the stipulated time frame and a destruction charge will be deducted from your account as per our published schedule of charges.</w:t></w:r>
                    </w:p>
                    <w:p/>
                    <w:p>
                        <w:pPr><w:jc w:val="both"/></w:pPr>
                        <w:r><w:rPr><w:rFonts w:ascii="Times New Roman" w:hAnsi="Times New Roman"/><w:sz w:val="22"/></w:rPr><w:t>Hence, kindly consider this as the final reminder from the bank to collect your $itemTypeTagBody within the mentioned time. We appreciate your cooperation in helping us to serve you better.</w:t></w:r>
                    </w:p>
                    <w:p/>
                    <w:p>
                        <w:r><w:rPr><w:rFonts w:ascii="Times New Roman" w:hAnsi="Times New Roman"/><w:b/><w:sz w:val="22"/></w:rPr><w:t>Please feel free to contact us for any further assistance. 09610870230-34</w:t></w:r>
                    </w:p>
                    <w:p/>
                    <w:p/>
                    <w:p>
                        <w:r><w:rPr><w:rFonts w:ascii="Times New Roman" w:hAnsi="Times New Roman"/><w:b/><w:sz w:val="22"/></w:rPr><w:t>Yours Sincerely,</w:t></w:r>
                    </w:p>
                    <w:p/>
                    <w:p/>
                    <w:p/>
                    <w:p>
                        <w:r><w:rPr><w:rFonts w:ascii="Times New Roman" w:hAnsi="Times New Roman"/><w:b/><w:sz w:val="22"/></w:rPr><w:t>_________________                                                       _________________</w:t></w:r>
                    </w:p>
                    <w:p>
                        <w:r><w:rPr><w:rFonts w:ascii="Times New Roman" w:hAnsi="Times New Roman"/><w:b/><w:sz w:val="22"/></w:rPr><w:t>Authorised signatory                                                       Authorised signatory</w:t></w:r>
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

            // 1-inch margins section property (1440 twips = 1 inch)
            docXmlBuilder.append("""
                <w:sectPr>
                    <w:pgMar w:top="1440" w:right="1440" w:bottom="1440" w:left="1440"/>
                </w:sectPr>
            """.trimIndent())

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
