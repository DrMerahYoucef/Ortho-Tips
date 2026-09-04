package com.example.utils

import android.content.Context
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.pdf.PdfDocument
import com.example.data.local.entity.PatientEntity
import com.example.data.local.entity.SurgeryEntity
import com.example.data.local.entity.TimelineEventEntity
import com.example.data.local.model.calculateAge
import java.io.File
import java.io.FileOutputStream

/** Which sections the surgeon chose to include when exporting a patient's chart to PDF. */
data class PatientChartPdfOptions(
    val includeContact: Boolean = true,
    val includeInsurance: Boolean = true,
    val includeMedicalHistory: Boolean = true,
    val includeAllergies: Boolean = true,
    val includeChronicDiseases: Boolean = true,
    val includePreviousSurgeries: Boolean = true,
    val includeMedications: Boolean = true,
    val includeNotes: Boolean = true,
    val includeOrthopedicInfo: Boolean = true,
    val includeTimeline: Boolean = true,
    val includeSurgeries: Boolean = true,
    /** Encodes the patient's id — left off by default whenever [PatientChartPdfGenerator.generate]'s
     *  own `anonymize` flag is on, since it would otherwise defeat the point of anonymizing. */
    val includeQrCode: Boolean = true
)

/**
 * Renders the patient's full chart (demographics, medical & orthopedic info, timeline,
 * surgery summaries) as a paginated PDF, using the same manual-layout approach as
 * [OperativeProtocolGenerator] — no third-party PDF library needed.
 */
object PatientChartPdfGenerator {

    private const val PAGE_WIDTH = 595
    private const val PAGE_HEIGHT = 842
    private const val MARGIN = 48f
    private const val QR_SIZE = 64f

    fun generate(
        context: Context,
        patient: PatientEntity,
        timelineEvents: List<TimelineEventEntity>,
        surgeries: List<SurgeryEntity>,
        anonymize: Boolean,
        options: PatientChartPdfOptions = PatientChartPdfOptions()
    ): File {
        val document = PdfDocument()
        var pageNumber = 1
        var page = document.startPage(PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create())
        var canvas = page.canvas
        var y = MARGIN

        val titlePaint = Paint().apply { textSize = 18f; isFakeBoldText = true; isAntiAlias = true }
        val sectionPaint = Paint().apply { textSize = 13f; isFakeBoldText = true; isAntiAlias = true; color = 0xFF1A5276.toInt() }
        val bodyPaint = Paint().apply { textSize = 11.5f; isAntiAlias = true }
        val mutedPaint = Paint().apply { textSize = 10.5f; isAntiAlias = true; color = 0xFF666666.toInt() }

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

        // QR code — top-right of the first page, so the printed chart can be scanned back to
        // this patient. Skipped whenever the chart itself is anonymized: encoding the
        // patient's id would quietly undo the point of anonymizing.
        if (options.includeQrCode && !anonymize) {
            QrCodeGenerator.generate(QrPatientLink.encode(patient.id), sizePx = 240)?.let { qrBitmap ->
                val qrRight = PAGE_WIDTH - MARGIN
                val qrTop = MARGIN
                canvas.drawBitmap(qrBitmap, null, RectF(qrRight - QR_SIZE, qrTop, qrRight, qrTop + QR_SIZE), null)
                qrBitmap.recycle()
            }
        }

        // Title block
        val displayName = if (anonymize) patient.patientCode else "${patient.fullName} (${patient.patientCode})"
        canvas.drawText(displayName, MARGIN, y, titlePaint)
        y += 22f
        val age = calculateAge(patient.dateOfBirth)
        val subLine = listOfNotNull(
            age?.let { "$it yrs" },
            patient.gender.ifBlank { null }
        ).joinToString(" • ")
        if (subLine.isNotBlank()) {
            canvas.drawText(subLine, MARGIN, y, mutedPaint)
            y += 14f
        }
        canvas.drawText("Generated ${DateFormat.formatDate(System.currentTimeMillis())}", MARGIN, y, mutedPaint)
        y += 10f
        canvas.drawLine(MARGIN, y, PAGE_WIDTH - MARGIN, y, Paint().apply { color = 0xFFCCCCCC.toInt() })
        y += 20f

        if (!anonymize) {
            if (options.includeContact) {
                drawSection("Contact", listOfNotNull(
                    patient.phone.ifBlank { null }?.let { "Phone: $it" },
                    patient.email.ifBlank { null }?.let { "Email: $it" },
                    patient.address.ifBlank { null }?.let { "Address: $it" },
                    listOf(patient.emergencyContactName, patient.emergencyContactPhone).filter { it.isNotBlank() }
                        .joinToString(" — ").ifBlank { null }?.let { "Emergency contact: $it" }
                ).joinToString("\n"))
            }
            if (options.includeInsurance) {
                drawSection("Insurance", listOfNotNull(
                    patient.insuranceProvider.ifBlank { null },
                    patient.insuranceNumber.ifBlank { null }
                ).joinToString(" — "))
            }
        }

        if (options.includeMedicalHistory) drawSection("Medical history", patient.medicalHistory)
        if (options.includeAllergies) drawSection("Allergies", patient.allergies)
        if (options.includeChronicDiseases) drawSection("Chronic diseases", patient.chronicDiseases)
        if (options.includePreviousSurgeries) drawSection("Previous surgeries (history)", patient.previousSurgeries)
        if (options.includeMedications) drawSection("Medications", patient.medications)
        if (options.includeNotes) drawSection("Notes", patient.generalNotes)

        if (options.includeOrthopedicInfo) {
            drawSection("Diagnosis", patient.diagnosis)
            drawSection("Body region / side", listOfNotNull(
                patient.bodyRegion.ifBlank { null },
                patient.side.takeIf { it.isNotBlank() && it != "NOT_APPLICABLE" }
            ).joinToString(" — "))
            drawSection("Injury date", DateFormat.formatDate(patient.injuryDate).takeIf { it != "—" } ?: "")
            drawSection("Trauma mechanism", patient.traumaMechanism)
            drawSection("Fracture classification", patient.fractureClassification)
            drawSection("Imaging findings", patient.imagingFindings)
            drawSection("Treatment plan", patient.treatmentPlan)
        }

        if (options.includeTimeline && timelineEvents.isNotEmpty()) {
            y += 6f
            newPageIfNeeded(20f)
            canvas.drawText("TIMELINE", MARGIN, y, sectionPaint)
            y += 16f
            timelineEvents.sortedByDescending { it.eventDate }.forEach { event ->
                newPageIfNeeded(15f)
                canvas.drawText(DateFormat.formatDate(event.eventDate), MARGIN, y, mutedPaint)
                canvas.drawText(event.title, MARGIN + 90f, y, bodyPaint)
                y += 15f
                if (event.description.isNotBlank()) {
                    drawWrapped("   ${event.description}", mutedPaint, 13f)
                }
            }
        }

        if (options.includeSurgeries && surgeries.isNotEmpty()) {
            y += 6f
            newPageIfNeeded(20f)
            canvas.drawText("SURGERIES", MARGIN, y, sectionPaint)
            y += 16f
            surgeries.sortedByDescending { it.surgeryDate }.forEach { surgery ->
                newPageIfNeeded(15f)
                val line = "${DateFormat.formatDate(surgery.surgeryDate)} — ${surgery.procedure.ifBlank { "Untitled procedure" }}"
                drawWrapped(line, bodyPaint, 15f)
                if (!anonymize && surgery.surgeon.isNotBlank()) {
                    drawWrapped("   Surgeon: ${surgery.surgeon}", mutedPaint, 13f)
                }
                y += 4f
            }
        }

        document.finishPage(page)

        val outputDir = File(context.filesDir, "protocols").apply { mkdirs() }
        val fileName = "chart_${patient.id}${if (anonymize) "_anon" else ""}.pdf"
        val outputFile = File(outputDir, fileName)
        FileOutputStream(outputFile).use { document.writeTo(it) }
        document.close()
        return outputFile
    }
}
