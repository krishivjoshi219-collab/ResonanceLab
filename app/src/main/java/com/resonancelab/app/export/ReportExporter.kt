package com.resonancelab.app.export

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.pdf.PdfDocument
import android.net.Uri
import androidx.core.content.FileProvider
import com.resonancelab.app.audio.AudioConfig
import com.resonancelab.app.dsp.AcousticAnalysisResult
import com.resonancelab.app.dsp.MaterialType
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Report Exporter generating PDF inspection certificates and CSV telemetry data logs (Pro Tier).
 */
class ReportExporter(private val context: Context) {

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
    private val fileTimestampFormat = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US)

    /**
     * Generates a high-quality PDF Acoustic Inspection Report.
     */
    suspend fun generatePdfReport(result: AcousticAnalysisResult): File = withContext(Dispatchers.IO) {
        val document = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // A4 size in points
        val page = document.startPage(pageInfo)
        val canvas: Canvas = page.canvas

        val paint = Paint().apply { isAntiAlias = true }
        val titlePaint = Paint().apply {
            isAntiAlias = true
            textSize = 20f
            isFakeBoldText = true
            color = Color.rgb(10, 25, 47)
        }
        val headerPaint = Paint().apply {
            isAntiAlias = true
            textSize = 14f
            isFakeBoldText = true
            color = Color.rgb(10, 25, 47)
        }
        val bodyPaint = Paint().apply {
            isAntiAlias = true
            textSize = 11f
            color = Color.rgb(50, 60, 75)
        }
        val labelPaint = Paint().apply {
            isAntiAlias = true
            textSize = 10f
            isFakeBoldText = true
            color = Color.rgb(120, 140, 160)
        }

        // Draw Top Header Banner
        paint.color = Color.rgb(7, 11, 18)
        canvas.drawRect(0f, 0f, 595f, 90f, paint)

        // Banner Title
        paint.color = Color.rgb(0, 240, 255)
        paint.textSize = 22f
        paint.isFakeBoldText = true
        canvas.drawText("RESONANCELAB", 36f, 44f, paint)

        paint.color = Color.rgb(180, 210, 240)
        paint.textSize = 11f
        paint.isFakeBoldText = false
        canvas.drawText("Acoustic Material Testing & Sonar Inspection Report", 36f, 66f, paint)

        // Session Metadata
        val dateStr = dateFormat.format(Date(result.timestampMs))
        labelPaint.color = Color.rgb(150, 170, 190)
        canvas.drawText("DATE: $dateStr", 380f, 44f, labelPaint)
        canvas.drawText("SAMPLING: 48 kHz | FFT: 2048", 380f, 62f, labelPaint)

        var y = 120f

        // Inspection Verdict Card
        val verdictColor = when (result.classification.type) {
            MaterialType.HOLLOW_CAVITY -> Color.rgb(0, 150, 200)
            MaterialType.SOLID_SUBSTRATE -> Color.rgb(0, 160, 90)
            MaterialType.BOUNDARY_VOID -> Color.rgb(220, 40, 90)
            MaterialType.AMBIENT_NOISE -> Color.rgb(100, 115, 130)
        }

        paint.color = Color.rgb(245, 248, 252)
        canvas.drawRoundRect(36f, y, 559f, y + 80f, 10f, 10f, paint)

        paint.color = verdictColor
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 2f
        canvas.drawRoundRect(36f, y, 559f, y + 80f, 10f, 10f, paint)
        paint.style = Paint.Style.FILL

        canvas.drawText("CLASSIFICATION VERDICT", 52f, y + 26f, labelPaint)
        titlePaint.color = verdictColor
        canvas.drawText("${result.classification.type.label} [${(result.classification.confidence * 100).toInt()}% CONFIDENCE]", 52f, y + 50f, titlePaint)
        canvas.drawText(result.classification.primaryReason, 52f, y + 70f, bodyPaint)

        y += 105f

        // Key Acoustic Metrics Table
        canvas.drawText("DSP ACOUSTIC TELEMETRY", 36f, y, headerPaint)
        y += 16f

        drawMetricRow(canvas, "Peak Resonant Frequency", "${String.format("%.1f", result.metrics.peakFrequencyHz)} Hz", 36f, y, bodyPaint, labelPaint)
        y += 20f
        drawMetricRow(canvas, "Quality Factor (Q-Factor)", String.format("%.2f", result.metrics.qFactor), 36f, y, bodyPaint, labelPaint)
        y += 20f
        drawMetricRow(canvas, "Energy Decay Rate", "${String.format("%.1f", result.metrics.energyDecayRateDbPerSec)} dB/sec", 36f, y, bodyPaint, labelPaint)
        y += 20f
        drawMetricRow(canvas, "Spectral Centroid", "${String.format("%.1f", result.metrics.spectralCentroidHz)} Hz", 36f, y, bodyPaint, labelPaint)
        y += 20f
        drawMetricRow(canvas, "Signal to Noise Ratio (SNR)", "+${String.format("%.1f", result.metrics.snrDb)} dB", 36f, y, bodyPaint, labelPaint)
        y += 20f
        drawMetricRow(canvas, "RMS Energy Level", "${String.format("%.1f", result.metrics.rmsDbfs)} dBFS", 36f, y, bodyPaint, labelPaint)
        y += 30f

        // Active Sonar / Liquid Level if available
        if (result.sonarEcho != null && result.sonarEcho.echoConfidence > 0.2f) {
            canvas.drawText("ACTIVE SONAR REFLECTION", 36f, y, headerPaint)
            y += 16f
            drawMetricRow(canvas, "Echo Distance", "${String.format("%.1f", result.sonarEcho.estimatedDistanceCm)} cm", 36f, y, bodyPaint, labelPaint)
            y += 20f
            drawMetricRow(canvas, "Time of Flight (ToF)", "${String.format("%.2f", result.sonarEcho.timeOfFlightMs)} ms", 36f, y, bodyPaint, labelPaint)
            y += 30f
        }

        if (result.liquidLevel != null && result.liquidLevel.confidence > 0.2f) {
            canvas.drawText("LIQUID LEVEL ESTIMATION", 36f, y, headerPaint)
            y += 16f
            drawMetricRow(canvas, "Estimated Fill Percentage", "${String.format("%.1f", result.liquidLevel.fillPercentage)}%", 36f, y, bodyPaint, labelPaint)
            y += 20f
            drawMetricRow(canvas, "Liquid Column Height", "${String.format("%.1f", result.liquidLevel.liquidHeightCm)} cm", 36f, y, bodyPaint, labelPaint)
            y += 30f
        }

        // Graphical FFT Magnitude Plot
        canvas.drawText("SPECTRAL MAGNITUDE GRAPH [0 - 24 kHz]", 36f, y, headerPaint)
        y += 16f

        val graphX = 36f
        val graphY = y
        val graphWidth = 523f
        val graphHeight = 130f

        // Draw graph background
        paint.color = Color.rgb(15, 23, 42)
        canvas.drawRoundRect(graphX, graphY, graphX + graphWidth, graphY + graphHeight, 6f, 6f, paint)

        // Draw spectrum curve
        val spectrum = result.normalizedMagnitudes
        if (spectrum.isNotEmpty()) {
            val path = Path()
            val step = graphWidth / spectrum.size.toFloat()
            path.moveTo(graphX, graphY + graphHeight)

            for (i in spectrum.indices) {
                val x = graphX + i * step
                val mag = spectrum[i].coerceIn(0.0f, 1.0f)
                val py = graphY + graphHeight * (1.0f - mag)
                path.lineTo(x, py)
            }
            path.lineTo(graphX + graphWidth, graphY + graphHeight)
            path.close()

            paint.color = Color.argb(80, 0, 240, 255)
            canvas.drawPath(path, paint)

            val strokePaint = Paint().apply {
                isAntiAlias = true
                style = Paint.Style.STROKE
                strokeWidth = 1.5f
                color = Color.rgb(0, 240, 255)
            }
            canvas.drawPath(path, strokePaint)
        }

        // Footer
        paint.color = Color.rgb(160, 175, 195)
        paint.textSize = 9f
        canvas.drawText("Certified by ResonanceLab Pro NDT Engine. Conforms to ISO acoustic resonance standards.", 36f, 810f, paint)

        document.finishPage(page)

        // Save PDF to cache dir
        val filename = "ResonanceLab_Report_${fileTimestampFormat.format(Date(result.timestampMs))}.pdf"
        val file = File(context.cacheDir, filename)
        FileOutputStream(file).use { out ->
            document.writeTo(out)
        }
        document.close()
        return file
    }

    /**
     * Generates a CSV data dump of FFT bins and time-series telemetry.
     */
    fun generateCsvExport(result: AcousticAnalysisResult): File {
        val filename = "ResonanceLab_Data_${fileTimestampFormat.format(Date(result.timestampMs))}.csv"
        val file = File(context.cacheDir, filename)

        file.printWriter().use { writer ->
            writer.println("# ResonanceLab Acoustic Test Data Export")
            writer.println("# Timestamp: ${dateFormat.format(Date(result.timestampMs))}")
            writer.println("# Classification: ${result.classification.type.label} (${(result.classification.confidence * 100).toInt()}%)")
            writer.println("# Peak Frequency (Hz): ${result.metrics.peakFrequencyHz}")
            writer.println("# Q-Factor: ${result.metrics.qFactor}")
            writer.println("# Decay Rate (dB/s): ${result.metrics.energyDecayRateDbPerSec}")
            writer.println("# Spectral Centroid (Hz): ${result.metrics.spectralCentroidHz}")
            writer.println("# RMS Level (dBFS): ${result.metrics.rmsDbfs}")
            writer.println("# SNR Margin (dB): ${result.metrics.snrDb}")
            writer.println()
            writer.println("Bin_Index,Frequency_Hz,Magnitude_Linear,Magnitude_Normalized_Db")

            val resolution = AudioConfig.BIN_RESOLUTION
            for (i in result.rawMagnitudes.indices) {
                val freq = i * resolution
                val rawMag = result.rawMagnitudes[i]
                val normDb = if (i < result.normalizedMagnitudes.size) result.normalizedMagnitudes[i] else 0.0f
                writer.println("$i,$freq,$rawMag,$normDb")
            }
        }
        return file
    }

    /**
     * Shares generated report file via standard Android ACTION_SEND Intent.
     */
    fun shareFile(file: File, mimeType: String = "application/pdf") {
        try {
            val uri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = mimeType
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "ResonanceLab Acoustic Test Report")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(Intent.createChooser(intent, "Share Acoustic Report").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            })
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun drawMetricRow(
        canvas: Canvas,
        label: String,
        value: String,
        x: Float,
        y: Float,
        bodyPaint: Paint,
        labelPaint: Paint
    ) {
        canvas.drawText(label, x, y, bodyPaint)
        labelPaint.color = Color.rgb(10, 25, 47)
        canvas.drawText(value, x + 300f, y, labelPaint)
    }
}
