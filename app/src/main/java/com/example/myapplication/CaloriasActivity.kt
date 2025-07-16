package com.example.myapplication

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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_calorias)

        txtCantidadCalorias1 = findViewById(R.id.txtCantidadCalorias1)
        txtCantidadCalorias2 = findViewById(R.id.txtCantidadCalorias2)
        txtCantidadCalorias3 = findViewById(R.id.txtCantidadCalorias3)
        txtCantidadCalorias4 = findViewById(R.id.txtCantidadCalorias4)
        btnGuardarCalorias = findViewById(R.id.btnGuardarCalorias)
        txtMostrarSumaCalorias = findViewById(R.id.txtMostrarSumaCalorias) // Referencia al TextView

        btnGuardarCalorias.setOnClickListener {
            recogerYProcesarDatos()
        }
    }

    private fun recogerYProcesarDatos() {
        val cal1Str = txtCantidadCalorias1.text.toString()
        val cal2Str = txtCantidadCalorias2.text.toString()
        val cal3Str = txtCantidadCalorias3.text.toString()
        val cal4Str = txtCantidadCalorias4.text.toString()

        if (cal1Str.isBlank() || cal2Str.isBlank() || cal3Str.isBlank() || cal4Str.isBlank()) {
            Toast.makeText(this, "Por favor, ingrese todas las cantidades de calorías.", Toast.LENGTH_LONG).show()
            txtMostrarSumaCalorias.text = getString(R.string.txtMostrarCalorias) // Resetear texto
            return
        }

        try {
            val cal1 = cal1Str.toInt()
            val cal2 = cal2Str.toInt()
            val cal3 = cal3Str.toInt()
            val cal4 = cal4Str.toInt()

            val totalCalorias = cal1 + cal2 + cal3 + cal4
            txtMostrarSumaCalorias.text = "Total Calorías: $totalCalorias" // Mostrar suma

            Toast.makeText(this, "Datos listos para guardar. Total: $totalCalorias", Toast.LENGTH_LONG).show()

            subirDatosAFirebase(cal1, cal2, cal3, cal4, totalCalorias)

        } catch (e: NumberFormatException) {
            Toast.makeText(this, "Por favor, ingrese solo números válidos para las calorías.", Toast.LENGTH_LONG).show()
            txtMostrarSumaCalorias.text = getString(R.string.txtMostrarCalorias) // Resetear texto
        }
    }

    private fun subirDatosAFirebase(cal1: Int, cal2: Int, cal3: Int, cal4: Int, totalCalorias: Int) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        if (userId == null) {
            Toast.makeText(this, "Usuario no autenticado.", Toast.LENGTH_SHORT).show()
            return
        }

        val db = FirebaseFirestore.getInstance()

        // Obtener la fecha actual en formato yyyy-MM-dd
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val currentDate = sdf.format(Date())

        val data = hashMapOf(
            "date" to currentDate,
            "steps" to 0, // Puedes actualizarlo luego si tienes un contador de pasos
            "calories_morning" to cal1,
            "calories_afternoon" to cal2,
            "calories_evening" to cal3,
            "total_calories" to totalCalorias
        )

        // Guardar con ID de documento basado en la fecha
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
