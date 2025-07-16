package com.example.myapplication

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

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

            // Aquí es donde tendrías la lógica para subir a Firebase
            Toast.makeText(this, "Datos listos para guardar. Total: $totalCalorias", Toast.LENGTH_LONG).show()

            // TODO: Implementar la subida a Firebase
             val datosCalorias = hashMapOf(
                 "comida1" to cal1,
                 "comida2" to cal2,
                 "comida3" to cal3,
                "comida4" to cal4,
                 "total_calorias" to totalCalorias,
                 "timestamp" to System.currentTimeMillis() // o FieldValue.serverTimestamp() para Firestore
             )
                subirDatosAFirebase(datosCalorias)

        } catch (e: NumberFormatException) {
            Toast.makeText(this, "Por favor, ingrese solo números válidos para las calorías.", Toast.LENGTH_LONG).show()
            txtMostrarSumaCalorias.text = getString(R.string.txtMostrarCalorias) // Resetear texto
        }
    }

     private fun subirDatosAFirebase(datos: Map<String, Any>) {
          //Lógica para Firebase (Realtime Database o Firestore)
          val userId = FirebaseAuth.getInstance().currentUser?.uid
          if (userId == null) {
             Toast.makeText(this, "Usuario no autenticado.", Toast.LENGTH_SHORT).show()
              return
          }
          FirebaseDatabase.getInstance().getReference("calorias_usuarios")
              .child(userId)
              .child("entradas_calorias")
              .push() // Crea un ID único para la entrada
              .setValue(datos)
            .addOnSuccessListener {
                  Toast.makeText(this, "Datos guardados exitosamente.", Toast.LENGTH_SHORT).show()
              }
              .addOnFailureListener {
                  Toast.makeText(this, "Error al guardar datos: ${it.message}", Toast.LENGTH_SHORT).show()
              }
     }
}
