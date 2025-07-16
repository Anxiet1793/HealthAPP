package com.example.myapplication

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
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
        setContentView(R.layout.activity_imc)

        etHeight = findViewById(R.id.etHeight)
        etWeight = findViewById(R.id.etWeight)
        btnCalculateImc = findViewById(R.id.btnCalculateImc)
        tvImcResultValue = findViewById(R.id.tvImcResultValue)
        tvImcClassificationValue = findViewById(R.id.tvImcClassificationValue)

        firestore = FirebaseFirestore.getInstance()
        firebaseAuth = FirebaseAuth.getInstance()

        btnCalculateImc.setOnClickListener {
            calculateAndSaveImc()
        }
    }

    private fun calculateAndSaveImc() {
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

        val classification = getBmiClassification(bmiRounded)

        tvImcResultValue.text = String.format("%.2f", bmiRounded)
        tvImcClassificationValue.text = classification

        saveImcToFirestore(heightCm, weightKg, bmiRounded, classification)
    }

    private fun getBmiClassification(bmi: Double): String {
        return when {
            bmi < 18.5 -> "Bajo peso"
            bmi < 24.9 -> "Normal"
            bmi < 29.9 -> "Sobrepeso"
            else -> "Obesidad"
        }
    }

    private fun saveImcToFirestore(heightCm: Int, weightKg: Double, bmi: Double, classification: String) {
        val currentUser = firebaseAuth.currentUser
        if (currentUser == null) {
            Toast.makeText(this, "Usuario no autenticado", Toast.LENGTH_SHORT).show()
            // Potentially redirect to LoginActivity
            return
        }

        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val currentDate = sdf.format(Date())

        val bmiRecord = BmiRecord(
            date = currentDate,
            weightKg = weightKg,
            heightCm = heightCm,
            bmi = bmi,
            classification = classification
        )

        firestore.collection("users").document(currentUser.uid)
            .collection("imc")
            .add(bmiRecord)
            .addOnSuccessListener {
                Toast.makeText(this, "IMC guardado correctamente", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error al guardar IMC: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }
}
