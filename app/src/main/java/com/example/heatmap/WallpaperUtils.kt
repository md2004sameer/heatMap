package com.example.heatmap

import android.app.WallpaperManager
import android.content.Context
import android.graphics.*
import android.text.TextPaint
import android.util.Log
import android.widget.Toast
import androidx.core.graphics.createBitmap
import androidx.core.graphics.toColorInt
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters

object WallpaperUtils {
    private const val TAG = "WallpaperUtils"
    private val applyMutex = Mutex()

    /**
     * Applies the LeetCode heatmap as a wallpaper.
     * Hardened for concurrency and memory safety.
     */
    suspend fun applyWallpaper(context: Context, data: LeetCodeData, target: Int = 0) {
        applyMutex.withLock {
            withContext(Dispatchers.Default) {
                try {
                    val wm = WallpaperManager.getInstance(context)
                    
                    val metrics = context.resources.displayMetrics
                    val screenWidth = metrics.widthPixels
                    val screenHeight = metrics.heightPixels
                    
                    var width = wm.desiredMinimumWidth
                    var height = wm.desiredMinimumHeight
                    
                    if (width <= 0 || height <= 0 || width > screenWidth * 2 || height > screenHeight * 2) {
                        width = screenWidth
                        height = screenHeight
                    }

                    val bitmap = renderToBitmap(data, width, height)

                    val flag = if (target == 0) {
                        WallpaperManager.FLAG_SYSTEM or WallpaperManager.FLAG_LOCK
                    } else {
                        target
                    }
                    
                    try {
                        wm.setBitmap(bitmap, null, true, flag)
                        Log.d(TAG, "Wallpaper set successfully")
                        withContext(Dispatchers.Main) {
                            Toast.makeText(context, "Wallpaper set successfully", Toast.LENGTH_SHORT).show()
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to apply wallpaper with flags $flag", e)
                        wm.setBitmap(bitmap, null, true, WallpaperManager.FLAG_SYSTEM)
                        Log.d(TAG, "Wallpaper set successfully (fallback to system)")
                        withContext(Dispatchers.Main) {
                            Toast.makeText(context, "Wallpaper set successfully", Toast.LENGTH_SHORT).show()
                        }
                    }
                } catch (e: OutOfMemoryError) {
                    Log.e(TAG, "OOM while applying wallpaper", e)
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to apply wallpaper", e)
                }
            }
        }
    }

    private fun renderToBitmap(data: LeetCodeData, width: Int, height: Int): Bitmap {
        val bitmap = createBitmap(width, height, Bitmap.Config.RGB_565)
        val canvas = Canvas(bitmap)

        // 1. Solid Black Background
        canvas.drawColor(Color.BLACK)

        val user = data.matchedUser ?: return bitmap
        val today = LocalDate.now()

        // 2. Main Box (Dark Gray Card)
        val boxWidth = width * 0.85f
        val boxHeight = height * 0.42f
        val boxLeft = (width - boxWidth) / 2f
        val boxTop = height * 0.22f

        val boxPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = "#121212".toColorInt()
            style = Paint.Style.FILL
        }
        val boxBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            alpha = 15 // Very subtle border
            style = Paint.Style.STROKE
            strokeWidth = 2f
        }
        
        val cornerRadius = 64f
        canvas.drawRoundRect(boxLeft, boxTop, boxLeft + boxWidth, boxTop + boxHeight, cornerRadius, cornerRadius, boxPaint)
        canvas.drawRoundRect(boxLeft, boxTop, boxLeft + boxWidth, boxTop + boxHeight, cornerRadius, cornerRadius, boxBorderPaint)

        // 3. "MONTHLY PROGRESS" Label
        val labelPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            alpha = 80
            textSize = 28f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
            letterSpacing = 0.4f
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("MONTHLY PROGRESS", width / 2f, boxTop + 80f, labelPaint)

        // 4. Heatmap
        val hPadding = 60f
        val vPadding = 130f
        drawHeatMap(
            canvas, 
            user.userCalendar.submissionCalendar, 
            boxLeft + hPadding, 
            boxTop + vPadding, 
            boxWidth - (hPadding * 2), 
            boxHeight - vPadding - 40f
        )

        // 5. Date and Countdown
        var currentY = boxTop + boxHeight + 110f
        
        val datePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            alpha = 140
            textSize = 36f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
            letterSpacing = 0.2f
            textAlign = Paint.Align.CENTER
        }
        val dateStr = today.format(DateTimeFormatter.ofPattern("EEEE, MMMM dd")).uppercase()
        canvas.drawText(dateStr, width / 2f, currentY, datePaint)

        currentY += 60f
        val nextYear = today.year + 1
        val firstOfNextYear = LocalDate.of(nextYear, 1, 1)
        val daysLeft = ChronoUnit.DAYS.between(today, firstOfNextYear)

        val countdownPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            alpha = 100
            textSize = 28f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
            letterSpacing = 0.2f
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("$daysLeft DAYS UNTIL $nextYear", width / 2f, currentY, countdownPaint)

        return bitmap
    }

    private fun drawHeatMap(canvas: Canvas, calendarJson: String, left: Float, top: Float, maxWidth: Float, maxHeight: Float) {
        val submissionByDate = try {
            val type = object : TypeToken<Map<String, Int>>() {}.type
            val rawMap = Gson().fromJson<Map<String, Int>>(calendarJson, type)
            rawMap.entries.associate { (tsStr, count) ->
                val ts = tsStr.toLong()
                val date = if (tsStr.length > 10) {
                    Instant.ofEpochMilli(ts).atZone(ZoneId.systemDefault()).toLocalDate()
                } else {
                    Instant.ofEpochSecond(ts).atZone(ZoneId.systemDefault()).toLocalDate()
                }
                date to count
            }
        } catch (_: Exception) {
            emptyMap()
        }

        val today = LocalDate.now()
        val firstOfMonth = today.withDayOfMonth(1)
        val lastOfMonth = today.with(TemporalAdjusters.lastDayOfMonth())

        var gridStart = firstOfMonth
        while (gridStart.dayOfWeek != java.time.DayOfWeek.SUNDAY) {
            gridStart = gridStart.minusDays(1)
        }

        val weeks = mutableListOf<List<LocalDate>>()
        var currentWeekStart = gridStart
        while (currentWeekStart.isBefore(lastOfMonth) || currentWeekStart.isEqual(lastOfMonth)) {
            val week = (0..6).map { currentWeekStart.plusDays(it.toLong()) }
            weeks.add(week)
            currentWeekStart = currentWeekStart.plusWeeks(1)
        }

        val numWeeks = weeks.size
        val spacing = 24f

        val cellByWidth = (maxWidth - (numWeeks + 1) * spacing) / (numWeeks + 1)
        val cellByHeight = (maxHeight - (7 * spacing)) / 7
        val cellSize = minOf(cellByWidth, cellByHeight).coerceAtLeast(10f)

        val actualHeatmapHeight = 7 * cellSize + 6 * spacing
        val actualTop = top + (maxHeight - actualHeatmapHeight) / 2f

        val dayLabels = listOf("S", "M", "T", "W", "T", "F", "S")
        val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            alpha = 40
            textSize = cellSize * 0.3f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }

        dayLabels.forEachIndexed { index, day ->
            val y = actualTop + index * (cellSize + spacing) + cellSize / 2f + labelPaint.textSize / 3f
            canvas.drawText(day, left + cellSize / 2f, y, labelPaint)
        }

        val cellPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
        }

        weeks.forEachIndexed { wIdx, week ->
            val x = left + (wIdx + 1) * (cellSize + spacing)
            week.forEachIndexed { dIdx, date ->
                if (date.month == today.month) {
                    val y = actualTop + dIdx * (cellSize + spacing)
                    val count = submissionByDate[date] ?: 0
                    
                    cellPaint.color = getHeatmapColor(count).toInt()
                    canvas.drawRoundRect(x, y, x + cellSize, y + cellSize, 20f, 20f, cellPaint)

                    if (date == today) {
                        borderPaint.color = Color.WHITE
                        borderPaint.alpha = 180
                        borderPaint.strokeWidth = 4f
                    } else {
                        borderPaint.color = Color.WHITE
                        borderPaint.alpha = 15
                        borderPaint.strokeWidth = 2f
                    }
                    canvas.drawRoundRect(x, y, x + cellSize, y + cellSize, 20f, 20f, borderPaint)
                }
            }
        }
    }

    private fun getHeatmapColor(count: Int): Long {
        return when {
            count == 0 -> 0xFF161B22
            count <= 2 -> 0xFF0E4429
            count <= 5 -> 0xFF006D32
            count <= 10 -> 0xFF26A641
            else -> 0xFF39D353
        }
    }
}
