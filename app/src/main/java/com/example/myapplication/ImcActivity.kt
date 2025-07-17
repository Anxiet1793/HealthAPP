package com.example.myapplication

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.myapplication.R.*
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ImcActivity : AppCompatActivity() {

    private lateinit var etHeight: TextInputEditText
    private lateinit var etWeight: TextInputEditText
    private lateinit var btnCalculateImc: Button
    private lateinit var tvImcResultValue: TextView
    private lateinit var tvImcClassificationValue: TextView

    private lateinit var firestore: FirebaseFirestore
    private lateinit var firebaseAuth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(layout.activity_imc)

        etHeight = findViewById(id.etHeight)
        etWeight = findViewById(id.etWeight)
        btnCalculateImc = findViewById(id.btnCalculateImc)
        tvImcResultValue = findViewById(id.tvImcResultValue)
        tvImcClassificationValue = findViewById(id.tvImcClassificationValue)

        firestore = FirebaseFirestore.getInstance()
        firebaseAuth = FirebaseAuth.getInstance()

        btnCalculateImc.setOnClickListener {
            calculateAndSaveImc()
        }
        val btnInicio = findViewById<Button>(R.id.btnInicioIMC)
        btnInicio.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
        }
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        val db = FirebaseFirestore.getInstance()

    }

    private fun calculateAndSaveImc() {
        val currentUser = firebaseAuth.currentUser
        if (currentUser == null) {
            Toast.makeText(this, "Usuario no autenticado", Toast.LENGTH_SHORT).show()
            return
        }

        firestore.collection("users").document(currentUser.uid)
            .get()
            .addOnSuccessListener { document ->
                val gender =
                    document.getString("gender")?.lowercase() ?: "male" // por defecto hombre
                procesarImc(gender)
            }
            .addOnFailureListener {
                Toast.makeText(this, "Error al obtener el género", Toast.LENGTH_SHORT).show()
            }
    }

    private fun procesarImc(gender: String) {
        val heightStr = etHeight.text.toString()
        val weightStr = etWeight.text.toString()

        if (heightStr.isEmpty() || weightStr.isEmpty()) {
            Toast.makeText(this, "Por favor, ingresa altura y peso", Toast.LENGTH_SHORT).show()
            return
        }

        val heightCm = heightStr.toIntOrNull()
        val weightKg = weightStr.toDoubleOrNull()

        if (heightCm == null || heightCm <= 0 || weightKg == null || weightKg <= 0) {
            Toast.makeText(this, "Valores de altura o peso inválidos", Toast.LENGTH_SHORT).show()
            return
        }

        val heightM = heightCm / 100.0
        val bmi = weightKg / (heightM * heightM)
        val bmiRounded = String.format("%.2f", bmi).toDouble()

        val classification = getBmiClassification(bmiRounded, gender)
        tvImcResultValue.text = bmiRounded.toString()
        tvImcClassificationValue.text = classification

        saveImcToFirestore(heightCm, weightKg, bmiRounded, classification)
        guardarCaloriasObjetivo(classification)
    }


    private fun guardarCaloriasObjetivo(clasificacion: String) {
        val userId = firebaseAuth.currentUser?.uid ?: return
        val targetCalories = when (clasificacion) {
            "Bajo peso" -> 2500 // superávit
            "Normal" -> 2200 // mantenimiento
            "Sobrepeso" -> 1800
            "Obesidad I" -> 1600
            "Obesidad II" -> 1400
            "Obesidad mórbida" -> 1200
            else -> 2200
        }

        val updates = mapOf("target_calories" to targetCalories)

        firestore.collection("users").document(userId)
            .update(updates)
            .addOnSuccessListener {
                Toast.makeText(this, "Meta calórica actualizada: $targetCalories kcal", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {
                Toast.makeText(this, "No se pudo guardar el objetivo calórico", Toast.LENGTH_SHORT).show()
            }
    }


    private fun getBmiClassification(bmi: Double, gender: String): String {
            return if (gender == "female") {
                when {
                    bmi < 18.0 -> "Bajo peso"
                    bmi < 23.9 -> "Normal"
                    bmi < 28.9 -> "Sobrepeso"
                    bmi < 34.9 -> "Obesidad I"
                    bmi < 39.9 -> "Obesidad II"
                    else -> "Obesidad mórbida"
                }
            } else {
                when {
                    bmi < 18.5 -> "Bajo peso"
                    bmi < 24.9 -> "Normal"
                    bmi < 29.9 -> "Sobrepeso"
                    bmi < 34.9 -> "Obesidad I"
                    bmi < 39.9 -> "Obesidad II"
                    else -> "Obesidad mórbida"
                }
            }
        }


    private fun saveImcToFirestore(heightCm: Int, weightKg: Double, bmi: Double, classification: String) {
            val userId = firebaseAuth.currentUser?.uid ?: return
            if (userId == null) {
                Toast.makeText(this, "Usuario no autenticado", Toast.LENGTH_SHORT).show()
                // Potentially redirect to LoginActivity
                return
            }
            val db = FirebaseFirestore.getInstance()
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val currentDate = sdf.format(Date())


            val data = hashMapOf(
                "date" to currentDate,
                "weightKg" to weightKg,
                "heigthCm" to heightCm,
                "bmi" to bmi,
                "classification" to classification
            )

            db.collection("users")
                .document(userId)
                .collection("imc")
                .document(currentDate)
                .set(data)
                .addOnSuccessListener {
                    Toast.makeText(this, "IMC actualizado correctamente", Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Error al guardar: ${e.message}", Toast.LENGTH_SHORT).show()
                }

        }

}



