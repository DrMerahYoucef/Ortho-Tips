package com.example.utils

import android.content.Context
import android.graphics.BitmapFactory
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.pdf.PdfDocument
import com.example.data.local.entity.PatientEntity
import com.example.data.local.entity.SurgeryEntity
import com.example.data.local.model.calculateAge
import com.example.data.prefs.SurgeonProfile
import java.io.File
import java.io.FileOutputStream

enum class ProtocolLanguage { FRENCH, ENGLISH }

/**
 * Renders a one-page-per-overflow operative report using Android's built-in
 * [PdfDocument] — no third-party PDF library needed. Text is laid out manually
 * with word-wrapping and simple pagination (A4 at 72dpi: 595x842 pt).
 */
object OperativeProtocolGenerator {

    private const val PAGE_WIDTH = 595
    private const val PAGE_HEIGHT = 842
    private const val MARGIN = 48f

    fun generate(
        context: Context,
        patient: PatientEntity,
        surgery: SurgeryEntity,
        surgeon: SurgeonProfile,
        language: ProtocolLanguage,
        anonymize: Boolean = false
    ): File {
        val strings = Strings.forLanguage(language)
        val document = PdfDocument()

        var pageNumber = 1
        var page = document.startPage(PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create())
        var canvas = page.canvas
        var y = MARGIN

        val titlePaint = Paint().apply { textSize = 18f; isFakeBoldText = true; isAntiAlias = true }
        val headerPaint = Paint().apply { textSize = 11f; isAntiAlias = true; color = 0xFF444444.toInt() }
        val sectionPaint = Paint().apply { textSize = 13f; isFakeBoldText = true; isAntiAlias = true; color = 0xFF1A5276.toInt() }
        val bodyPaint = Paint().apply { textSize = 11.5f; isAntiAlias = true }

        fun newPageIfNeeded(neededHeight: Float) {
            if (y + neededHeight > PAGE_HEIGHT - MARGIN) {
                document.finishPage(page)
                pageNumber += 1
                page = document.startPage(PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create())
                canvas = page.canvas
                y = MARGIN
            }
        }

        fun drawWrapped(text: String, paint: Paint, lineHeight: Float) {
            if (text.isBlank()) return
            val maxWidth = PAGE_WIDTH - 2 * MARGIN
            val words = text.split(" ")
            var line = StringBuilder()
            words.forEach { word ->
                val trial = if (line.isEmpty()) word else "$line $word"
                if (paint.measureText(trial) > maxWidth) {
                    newPageIfNeeded(lineHeight)
                    canvas.drawText(line.toString(), MARGIN, y, paint)
                    y += lineHeight
                    line = StringBuilder(word)
                } else {
                    line = StringBuilder(trial)
                }
            }
            if (line.isNotEmpty()) {
                newPageIfNeeded(lineHeight)
                canvas.drawText(line.toString(), MARGIN, y, paint)
                y += lineHeight
            }
        }

        fun drawSection(title: String, body: String) {
            if (body.isBlank()) return
            y += 6f
            newPageIfNeeded(20f)
            canvas.drawText(title.uppercase(), MARGIN, y, sectionPaint)
            y += 16f
            drawWrapped(body, bodyPaint, 15f)
        }

        // Letterhead
        canvas.drawText(surgeon.name.ifBlank { strings.unnamedSurgeon }, MARGIN, y, titlePaint)
        y += 20f
        listOf(surgeon.specialty, surgeon.hospital, surgeon.address, surgeon.phone, surgeon.email)
            .filter { it.isNotBlank() }
            .forEach { line -> canvas.drawText(line, MARGIN, y, headerPaint); y += 13f }
        if (surgeon.registrationNumber.isNotBlank()) {
            canvas.drawText("${strings.registrationNumber}: ${surgeon.registrationNumber}", MARGIN, y, headerPaint)
            y += 13f
        }

        y += 10f
        canvas.drawLine(MARGIN, y, PAGE_WIDTH - MARGIN, y, Paint().apply { color = 0xFFCCCCCC.toInt() })
        y += 20f

        canvas.drawText(strings.title, MARGIN, y, titlePaint)
        y += 24f

        // Patient block
        val age = calculateAge(patient.dateOfBirth)
        val patientLine = if (anonymize) {
            "${strings.patient}: ${patient.patientCode}" + (age?.let { " • $it ${strings.years} • ${patient.gender}" } ?: "")
        } else {
            "${strings.patient}: ${patient.fullName} (${patient.patientCode})" + (age?.let { " • $it ${strings.years} • ${patient.gender}" } ?: "")
        }
        canvas.drawText(patientLine, MARGIN, y, bodyPaint)
        y += 15f
        canvas.drawText("${strings.date}: ${DateFormat.formatDate(surgery.surgeryDate)}", MARGIN, y, bodyPaint)
        y += 15f

        drawSection(strings.hospital, surgery.hospitalClinic)
        if (!anonymize) drawSection(strings.surgeon, surgery.surgeon)
        drawSection(strings.assistants, surgery.assistants)
        drawSection(strings.anesthesia, surgery.anesthesiaType)
        drawSection(strings.diagnosis, surgery.diagnosis)
        drawSection(strings.procedure, surgery.procedure)
        drawSection(strings.materials, surgery.materialsUsed)
        drawSection(strings.complications, surgery.complications.ifBlank { strings.none })
        drawSection(strings.postOp, surgery.postOpInstructions)

        // Signature
        if (!anonymize && surgeon.signatureImagePath != null) {
            val signatureFile = File(surgeon.signatureImagePath)
            if (signatureFile.exists()) {
                newPageIfNeeded(90f)
                y += 20f
                val bitmap = BitmapFactory.decodeFile(signatureFile.absolutePath)
                if (bitmap != null) {
                    val targetWidth = 140f
                    val targetHeight = targetWidth * bitmap.height / bitmap.width
                    canvas.drawBitmap(bitmap, null, RectF(MARGIN, y, MARGIN + targetWidth, y + targetHeight), null)
                    y += targetHeight + 4f
                }
                canvas.drawText(surgeon.name, MARGIN, y, bodyPaint)
            }
        }

        document.finishPage(page)

        val outputDir = File(context.filesDir, "protocols").apply { mkdirs() }
        val fileName = "protocol_${surgery.id}_${language.name.lowercase()}${if (anonymize) "_anon" else ""}.pdf"
        val outputFile = File(outputDir, fileName)
        FileOutputStream(outputFile).use { document.writeTo(it) }
        document.close()

        return outputFile
    }

    private data class Strings(
        val title: String,
        val patient: String,
        val date: String,
        val years: String,
        val hospital: String,
        val surgeon: String,
        val assistants: String,
        val anesthesia: String,
        val diagnosis: String,
        val procedure: String,
        val materials: String,
        val complications: String,
        val postOp: String,
        val none: String,
        val registrationNumber: String,
        val unnamedSurgeon: String
    ) {
        companion object {
            fun forLanguage(language: ProtocolLanguage): Strings = when (language) {
                ProtocolLanguage.FRENCH -> Strings(
                    title = "Compte-rendu opératoire",
                    patient = "Patient",
                    date = "Date",
                    years = "ans",
                    hospital = "Établissement",
                    surgeon = "Chirurgien",
                    assistants = "Aides opératoires",
                    anesthesia = "Anesthésie",
                    diagnosis = "Diagnostic",
                    procedure = "Intervention",
                    materials = "Matériel utilisé",
                    complications = "Complications",
                    postOp = "Consignes postopératoires",
                    none = "Aucune",
                    registrationNumber = "N° d'inscription",
                    unnamedSurgeon = "Chirurgien non renseigné"
                )
                ProtocolLanguage.ENGLISH -> Strings(
                    title = "Operative Report",
                    patient = "Patient",
                    date = "Date",
                    years = "yrs",
                    hospital = "Hospital / clinic",
                    surgeon = "Surgeon",
                    assistants = "Assistants",
                    anesthesia = "Anesthesia",
                    diagnosis = "Diagnosis",
                    procedure = "Procedure",
                    materials = "Materials used",
                    complications = "Complications",
                    postOp = "Postoperative instructions",
                    none = "None",
                    registrationNumber = "Registration No.",
                    unnamedSurgeon = "Surgeon not set"
                )
            }
        }
    }
}
