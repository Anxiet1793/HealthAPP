package com.example.myapplication.data

import android.content.Context
import androidx.health.connect.client.HealthConnectClient
// Import HealthPermission
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.*
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import java.time.*
import kotlin.math.pow

class HealthManager(private val context: Context) {

    private val client = HealthConnectClient.getOrCreate(context)

    // Updated getPermissions function
    fun getPermissions(): Set<String> = setOf(
        HealthPermission.getReadPermission(StepsRecord::class),
        HealthPermission.getWritePermission(StepsRecord::class),
        HealthPermission.getReadPermission(WeightRecord::class),
        HealthPermission.getReadPermission(HeightRecord::class),
        HealthPermission.getReadPermission(SleepSessionRecord::class)
    )

    suspend fun leerPasos(inicio: LocalDateTime, fin: LocalDateTime): Long { // Changed return type to Long for steps
        val timeRangeFilter = TimeRangeFilter.between(inicio, fin)
        val request = ReadRecordsRequest(
            recordType = StepsRecord::class,
            timeRangeFilter = timeRangeFilter
        )
        val response = client.readRecords(request)
        return response.records.sumOf { it.count }
    }

    suspend fun leerPeso(inicio: LocalDateTime, fin: LocalDateTime): Double? { // Return Double? to handle no data
        val timeRangeFilter = TimeRangeFilter.between(inicio, fin)
        val request = ReadRecordsRequest(
            recordType = WeightRecord::class,
            timeRangeFilter = timeRangeFilter
        )
        val response = client.readRecords(request)
        return response.records.lastOrNull()?.weight?.inKilograms
    }

    suspend fun leerAltura(inicio: LocalDateTime, fin: LocalDateTime): Double? { // Return Double? and store in meters
        val timeRangeFilter = TimeRangeFilter.between(inicio, fin)
        val request = ReadRecordsRequest(
            recordType = HeightRecord::class,
            timeRangeFilter = timeRangeFilter
        )
        val response = client.readRecords(request)
        // Storing height in meters is standard. Conversion to cm can be done in UI if needed.
        return response.records.lastOrNull()?.height?.inMeters
    }

    suspend fun leerSueno(inicio: LocalDateTime, fin: LocalDateTime): Double { // Return Double for hours
        val timeRangeFilter = TimeRangeFilter.between(inicio, fin)
        val request = ReadRecordsRequest(
            recordType = SleepSessionRecord::class,
            timeRangeFilter = timeRangeFilter
        )
        val response = client.readRecords(request)
        val minutos = response.records.sumOf {
            Duration.between(it.startTime, it.endTime).toMinutes()
        }
        return minutos.toDouble() / 60.0 // Use Double for more precision
    }

    fun calcularIMC(pesoKg: Double, alturaMetros: Double): Double { // Use Double for parameters
        return if (alturaMetros > 0) pesoKg / alturaMetros.pow(2) else 0.0
    }
}
