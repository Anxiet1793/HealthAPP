package com.example.myapplication

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*

class CaloriasActivity : AppCompatActivity() {

    private lateinit var txtCantidadCalorias1: EditText
    private lateinit var txtCantidadCalorias2: EditText
    private lateinit var txtCantidadCalorias3: EditText
    private lateinit var txtCantidadCalorias4: EditText
    private lateinit var btnGuardarCalorias: Button
    private lateinit var txtMostrarSumaCalorias: TextView // Para mostrar la suma
    private var targetCalories: Int = 2200 // Valor por defecto

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_calorias)



        txtCantidadCalorias1 = findViewById(R.id.txtCantidadCalorias1)
        txtCantidadCalorias2 = findViewById(R.id.txtCantidadCalorias2)
        txtCantidadCalorias3 = findViewById(R.id.txtCantidadCalorias3)
        btnGuardarCalorias = findViewById(R.id.btnGuardarCalorias)
        txtMostrarSumaCalorias = findViewById(R.id.txtMostrarSumaCalorias) // Referencia al TextView

        btnGuardarCalorias.setOnClickListener {
            recogerYProcesarDatos()
        }
        val btnInicio = findViewById<Button>(R.id.btnInicio)
        btnInicio.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
        }
        val txtMetaCalorica = findViewById<TextView>(R.id.txtMetaCalorica)
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        if (userId != null) {
            val db = FirebaseFirestore.getInstance()
            db.collection("users").document(userId).get()
                .addOnSuccessListener { document ->
                    targetCalories = document.getLong("target_calories")?.toInt() ?: 2200
                    txtMetaCalorica.text = "Meta calórica diaria: $targetCalories kcal"
                }
                .addOnFailureListener {
                    txtMetaCalorica.text = "Meta calórica no disponible"
                }
        }

    }

    private fun recogerYProcesarDatos() {
        val cal1Str = txtCantidadCalorias1.text.toString()
        val cal2Str = txtCantidadCalorias2.text.toString()
        val cal3Str = txtCantidadCalorias3.text.toString()

        if (cal1Str.isBlank() || cal2Str.isBlank() || cal3Str.isBlank()) {
            Toast.makeText(this, "Por favor, ingrese todas las cantidades de calorías.", Toast.LENGTH_LONG).show()
            txtMostrarSumaCalorias.text = getString(R.string.txtMostrarCalorias) // Resetear texto
            return
        }

        try {
            val cal1 = cal1Str.toInt()
            val cal2 = cal2Str.toInt()
            val cal3 = cal3Str.toInt()

            val totalCalorias = cal1 + cal2 + cal3
            txtMostrarSumaCalorias.text = "Total Calorías: $totalCalorias" // Mostrar suma

            Toast.makeText(this, "Datos listos para guardar. Total: $totalCalorias", Toast.LENGTH_LONG).show()

            subirDatosAFirebase(cal1, cal2, cal3,  totalCalorias)

        } catch (e: NumberFormatException) {
            Toast.makeText(this, "Por favor, ingrese solo números válidos para las calorías.", Toast.LENGTH_LONG).show()
            txtMostrarSumaCalorias.text = getString(R.string.txtMostrarCalorias) // Resetear texto
        }
    }

    private fun subirDatosAFirebase(cal1: Int, cal2: Int, cal3: Int, totalCalorias: Int) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        if (userId == null) {
            Toast.makeText(this, "Usuario no autenticado.", Toast.LENGTH_SHORT).show()
            return
        }

        val db = FirebaseFirestore.getInstance()
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val currentDate = sdf.format(Date())

        // Calcular diferencia con la meta calórica ya cargada
        val diferencia = totalCalorias - targetCalories
        val mensaje = when {
            diferencia > 100 -> "🔺 Exceso: +$diferencia kcal sobre lo recomendado"
            diferencia < -200 -> "🔻 Déficit: ${-diferencia} kcal por debajo"
            else -> "✅ Equilibrio calórico"
        }

        txtMostrarSumaCalorias.text = "Total Calorías: $totalCalorias\n$mensaje"

        // Cambiar color del texto según resultado
        val color = when {
            diferencia > 100 || diferencia < -100 -> android.graphics.Color.RED
            else -> android.graphics.Color.parseColor("#4CAF50") // Verde
        }
        txtMostrarSumaCalorias.setTextColor(color)

        // Guardar en Firestore
        val data = hashMapOf(
            "date" to currentDate,
            "steps" to 0,
            "calories_morning" to cal1,
            "calories_afternoon" to cal2,
            "calories_evening" to cal3,
            "total_calories" to totalCalorias
        )

        db.collection("users")
            .document(userId)
            .collection("daily_data")
            .document(currentDate)
            .set(data)
            .addOnSuccessListener {
                Toast.makeText(this, "Calorías guardadas correctamente.", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error al guardar: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }



}
