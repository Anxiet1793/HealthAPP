package com.example.myapplication

import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.util.*

class MisionesActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    private lateinit var puntosText: TextView
    private lateinit var btnVolver: Button

    private lateinit var statusPeso: TextView
    private lateinit var statusPasos: TextView
    private lateinit var statusCalorias: TextView

    private var puntosTotales = 0
    private val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_misiones)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        puntosText = findViewById(R.id.puntosText)
        btnVolver = findViewById(R.id.btnVolver)

        statusPeso = findViewById(R.id.statusPesoIdeal)
        statusPasos = findViewById(R.id.statusPasos)
        statusCalorias = findViewById(R.id.statusCalorias)



        btnVolver.setOnClickListener { finish() }

        obtenerPuntos()
        verificarMisiones()
    }
    private fun verificarMisiones() {
        val userId = auth.currentUser?.uid ?: return
        val userRef = db.collection("users").document(userId)
        val hoyfecha = LocalDate.now()
        val hoy = LocalDate.now().toString()
        val mes = LocalDate.now().monthValue.toString().padStart(2, '0')

        userRef.get().addOnSuccessListener { userDoc ->
            val puntosActuales = userDoc.getLong("total_points") ?: 0
            val targetCalories = userDoc.getLong("target_calories")?.toInt() ?: return@addOnSuccessListener

            // --- PESO IDEAL ---
            db.collection("users").document(userId)
                .collection("imc").document(hoy)
                .get()
                .addOnSuccessListener { imcDoc ->
                    if (imcDoc.exists()) {
                        val clasificacion = imcDoc.getString("classification") ?: ""
                        val fecha = imcDoc.getString("date") ?: ""
                        val diasDesdeUltima = ChronoUnit.DAYS.between(LocalDate.parse(fecha),hoyfecha)

                        yaRecibioPuntos(userId, "peso_ideal", fecha) { yaOtorgado ->
                            if (!yaOtorgado && clasificacion == "Normal") {
                                val nuevosPuntos = puntosActuales + 3000
                                userRef.update("total_points", nuevosPuntos)
                                registrarMision(userId, "peso_ideal", fecha, 3000)
                                statusPeso.text = "✅ Cumplido (3000 pts)"
                                puntosText.text = "🎯 Puntos actuales: $nuevosPuntos"
                            } else {
                                statusPeso.text = "❌ No cumplido"
                            }
                        }
                    } else {
                        statusPeso.text = "❌ Sin datos"
                    }
                }

            // --- PASOS Y CALORÍAS ---
            db.collection("users").document(userId)
                .collection("daily_data").document(hoy)
                .get()
                .addOnSuccessListener { doc ->
                    val pasos = doc.getLong("steps") ?: 0
                    val caloriasHoy = doc.getDouble("total_calories")?.toInt() ?: -1

                    // PASOS
                    yaRecibioPuntos(userId, "pasos", hoy) { yaOtorgado ->
                        if (!yaOtorgado && pasos >= 6000) {
                            val nuevosPuntos = puntosActuales + 300
                            userRef.update("total_points", nuevosPuntos)
                            registrarMision(userId, "pasos", hoy, 300)
                            statusPasos.text = "✅ $pasos pasos (+300 pts)"
                            puntosText.text = "🎯 Puntos actuales: $nuevosPuntos"
                        } else {
                            statusPasos.text = "❌ $pasos pasos"
                        }
                    }

                    // CALORÍAS
                    if (caloriasHoy != -1) {
                        val rangoMin = (targetCalories * 0.9).toInt()
                        val rangoMax = (targetCalories * 1.1).toInt()

                        yaRecibioPuntos(userId, "calorias", hoy) { yaOtorgado ->
                            if (!yaOtorgado && caloriasHoy in rangoMin..rangoMax) {
                                val nuevosPuntos = puntosActuales + 500
                                userRef.update("total_points", nuevosPuntos)
                                registrarMision(userId, "calorias", hoy, 500)
                                statusCalorias.text = "✅ $caloriasHoy kcal (+500 pts)"
                                puntosText.text = "🎯 Puntos actuales: $nuevosPuntos"
                            } else {
                                statusCalorias.text = "❌ $caloriasHoy kcal"
                            }
                        }
                    }
                }.addOnFailureListener {
                    statusPasos.text = "❌ Error pasos"
                    statusCalorias.text = "❌ Error calorías"
                }

        }.addOnFailureListener {
            Log.e("Misiones", "❌ Error general", it)
        }
    }





    private fun obtenerPuntos() {
        val userId = auth.currentUser?.uid ?: return

        db.collection("users").document(userId).get()
            .addOnSuccessListener { doc ->
                puntosTotales = doc.getLong("total_points")?.toInt() ?: 0
                puntosText.text = "🎯 Puntos actuales: $puntosTotales"
            }
            .addOnFailureListener {
                Toast.makeText(this, "Error al obtener puntos", Toast.LENGTH_SHORT).show()
            }
    }
    private fun yaRecibioPuntos(userId: String, mision: String, fecha: String, callback: (Boolean) -> Unit) {
        db.collection("users").document(userId)
            .collection("misiones_log")
            .whereEqualTo("mission", mision)
            .whereEqualTo("date", fecha)
            .get()
            .addOnSuccessListener { docs ->
                callback(!docs.isEmpty)  // true si YA recibió puntos
            }
            .addOnFailureListener {
                Log.e("Misiones", "❌ Error al verificar log", it)
                callback(false)
            }
    }
    private fun registrarMision(userId: String, mission: String, date: String, puntos: Int) {
        val logRef = db.collection("users")
            .document(userId)
            .collection("misiones_log")
            .document("$mission-$date")

        val data = mapOf(
            "mission" to mission,
            "date" to date,
            "points" to puntos,
            "timestamp" to FieldValue.serverTimestamp()
        )

        logRef.set(data)
            .addOnSuccessListener {
                Log.i("Misiones", "✅ Misión registrada: $mission ($date)")
            }
            .addOnFailureListener {
                Log.e("Misiones", "❌ Error al registrar misión", it)
            }
    }



}
