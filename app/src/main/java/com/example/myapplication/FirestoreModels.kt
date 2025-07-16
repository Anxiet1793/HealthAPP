package com.example.myapplication

import com.google.firebase.firestore.PropertyName
import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

data class UserProfile(
    val uid: String = "", // Usually matches FirebaseAuth.currentUser.uid
    val name: String? = null,
    val email: String? = null,
    @get:PropertyName("birth_date") @set:PropertyName("birth_date") var birthDate: String? = null,
    val gender: String? = null,
    @get:PropertyName("height_cm") @set:PropertyName("height_cm") var heightCm: Int? = null,
    @get:PropertyName("weight_kg") @set:PropertyName("weight_kg") var weightKg: Double? = null,
    @get:PropertyName("total_score") @set:PropertyName("total_score") var totalScore: Int = 0,
    @get:PropertyName("registration_date") @set:PropertyName("registration_date") @ServerTimestamp var registrationDate: Date? = null
)

data class BmiRecord(
    var documentId: String? = null, // For local tracking if needed
    val date: String = "", // YYYY-MM-DD
    @get:PropertyName("weight_kg") @set:PropertyName("weight_kg") var weightKg: Double = 0.0,
    @get:PropertyName("height_cm") @set:PropertyName("height_cm") var heightCm: Int = 0,
    val bmi: Double = 0.0,
    val classification: String = ""
)

data class DailyData(
    var documentId: String? = null, // For local tracking, typically date YYYY-MM-DD
    val date: String = "", // YYYY-MM-DD
    val steps: Int = 0,
    @get:PropertyName("calories_morning") @set:PropertyName("calories_morning") var caloriesMorning: Int = 0,
    @get:PropertyName("calories_afternoon") @set:PropertyName("calories_afternoon") var caloriesAfternoon: Int = 0,
    @get:PropertyName("calories_evening") @set:PropertyName("calories_evening") var caloriesEvening: Int = 0,
    @get:PropertyName("total_calories") @set:PropertyName("total_calories") var totalCalories: Int = 0,
    // Future metrics
    @get:PropertyName("sleep_hours") @set:PropertyName("sleep_hours") var sleepHours: Double? = null,
    @get:PropertyName("water_liters") @set:PropertyName("water_liters") var waterLiters: Double? = null,
    @ServerTimestamp var lastUpdated: Date? = null
)

data class Achievement(
    var documentId: String? = null,
    val name: String = "",
    val description: String = "",
    val criteria: Map<String, Any>? = null, // Example: mapOf("type" to "steps", "threshold" to 10000)
    val score: Int = 0
)

data class UserMission(
    var documentId: String? = null,
    @get:PropertyName("userId") @set:PropertyName("userId") var userId: String = "",
    @get:PropertyName("achievementId") @set:PropertyName("achievementId") var achievementId: String = "",
    @get:PropertyName("completed_at") @set:PropertyName("completed_at") @ServerTimestamp var completedAt: Date? = null,
    @get:PropertyName("score_awarded") @set:PropertyName("score_awarded") var scoreAwarded: Int = 0,
    var status: String = "pending" // "pending", "in_progress", "completed"
)

data class AchievementType(
    var documentId: String? = null, // Matches the 'id' field in your JSON
    val name: String = "",
    val unit: String = "",
    val description: String = ""
)
