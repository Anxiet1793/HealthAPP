package com.example.myapplication

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.PermissionController
import androidx.lifecycle.lifecycleScope
import com.example.myapplication.data.HealthManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter

class PasosDiarios : AppCompatActivity() {

    private lateinit var healthManager: HealthManager
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore

    private val TAG = "PasosDiarios"
    private val providerPackageName = "com.google.android.apps.healthdata" // Paquete de Health Connect

    // Contrato para la solicitud de permisos de Health Connect
    private val requestPermissionActivityContract = PermissionController.createRequestPermissionResultContract()

    private lateinit var permisoLauncher: ActivityResultLauncher<Set<String>>

    private fun inicializarPermisoLauncher() {
        permisoLauncher = registerForActivityResult(requestPermissionActivityContract) { grantedPermissions ->
            val requiredPermissions = healthManager.getPermissions() // Obtener los permisos requeridos nuevamente
            if (grantedPermissions.containsAll(requiredPermissions)) {
                Toast.makeText(this, "Permisos de Health Connect concedidos", Toast.LENGTH_SHORT).show()
                Log.d(TAG, "Todos los permisos de Health Connect fueron concedidos.")
                obtenerYSubirPasos()
            } else {
                Log.e(TAG, "Permisos de Health Connect denegados. Concedidos: $grantedPermissions, Requeridos: $requiredPermissions")
                mostrarDialogoPermisosRechazados()
            }
        }
    }


    private fun mostrarDialogoPermisosRechazados() {
        AlertDialog.Builder(this)
            .setTitle("Permisos Requeridos")
            .setMessage("Esta aplicación necesita permisos de Health Connect para leer tus pasos diarios y ofrecerte una experiencia completa. Por favor, acepta los permisos para continuar.")
            .setPositiveButton("Reintentar") { dialog, _ ->
                val requiredPermissions = healthManager.getPermissions()
                permisoLauncher.launch(requiredPermissions)
                dialog.dismiss()
            }
            .setNegativeButton("Cancelar") { dialog, _ ->
                findViewById<TextView>(R.id.tvValorPromedio).text = "Permisos denegados"
                Toast.makeText(this, "Funcionalidad limitada sin permisos de Health Connect.", Toast.LENGTH_LONG).show()
                dialog.dismiss()
                finish() // Considera cerrar la actividad si los permisos son cruciales y denegados
            }
            .setCancelable(false)
            .show()
    }

    private fun mostrarDialogoHealthConnectNoDisponible() {
        AlertDialog.Builder(this)
            .setTitle("Health Connect Requerido")
            .setMessage("Para usar esta funcionalidad, necesitas instalar o actualizar la aplicación Health Connect de Google. ¿Deseas ir a Play Store?")
            .setPositiveButton("Ir a Play Store") { dialog, _ ->
                try {
                    val playStoreIntent = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$providerPackageName"))
                    startActivity(playStoreIntent)
                } catch (e: Exception) {
                    // Si Play Store no está disponible (ej. emulador sin Play Store)
                    Toast.makeText(this, "No se pudo abrir Play Store.", Toast.LENGTH_SHORT).show()
                }
                dialog.dismiss()
                finish() // Cierra PasosDiarios
            }
            .setNegativeButton("Cancelar") { dialog, _ ->
                findViewById<TextView>(R.id.tvValorPromedio).text = "Health Connect no disponible"
                Toast.makeText(this, "Health Connect no está disponible en este dispositivo.", Toast.LENGTH_LONG).show()
                dialog.dismiss()
                finish() // Cierra PasosDiarios
            }
            .setCancelable(false)
            .show()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pasos_diarios)

        // Inicializar HealthManager primero para poder usarlo
        healthManager = HealthManager(this)
        // Inicializar el launcher de permisos después de healthManager
        inicializarPermisoLauncher()


        // Verificar disponibilidad de Health Connect SDK
        val availabilityStatus = HealthConnectClient.getSdkStatus(this, providerPackageName)
        if (availabilityStatus != HealthConnectClient.SDK_AVAILABLE) {
            when (availabilityStatus) {
                HealthConnectClient.SDK_UNAVAILABLE_PROVIDER_UPDATE_REQUIRED -> {
                    Log.e(TAG, "Health Connect necesita actualizarse. Paquete: $providerPackageName")
                }
                HealthConnectClient.SDK_UNAVAILABLE -> {
                    Log.e(TAG, "Health Connect no está disponible. Paquete: $providerPackageName")
                }
                else -> {
                     Log.e(TAG, "Health Connect SDK no está disponible por una razón desconocida. Status: $availabilityStatus")
                }
            }
            mostrarDialogoHealthConnectNoDisponible()
            return // No continuar si Health Connect no está disponible o necesita actualización
        }

        // Si Health Connect está disponible, proceder con la inicialización de Firebase
        firebaseAuth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        if (firebaseAuth.currentUser == null) {
            Log.e(TAG, "Usuario no autenticado. Redirigiendo a Login.")
            Toast.makeText(this, "Usuario no autenticado", Toast.LENGTH_LONG).show()
            findViewById<TextView>(R.id.tvValorPromedio).text = "Usuario no autenticado"
            finish() // Cierra PasosDiarios si el usuario no está autenticado
            return
        }

        // Si Health Connect está disponible y el usuario está autenticado, solicitar permisos
        val requiredPermissions = healthManager.getPermissions()
        Log.d(TAG, "Solicitando permisos: $requiredPermissions")
        lifecycleScope.launch { // Coroutine para verificar permisos concedidos
            val granted = HealthConnectClient.getOrCreate(this@PasosDiarios).permissionController.getGrantedPermissions()
            if (granted.containsAll(requiredPermissions)) {
                Log.d(TAG, "Todos los permisos ya estaban concedidos.")
                obtenerYSubirPasos()
            } else {
                Log.d(TAG, "No todos los permisos están concedidos. Solicitando...")
                permisoLauncher.launch(requiredPermissions)
            }
        }
    }

    private fun obtenerYSubirPasos() {
        val currentUser = firebaseAuth.currentUser
        if (currentUser == null) {
            Log.e(TAG, "Intento de obtener pasos sin usuario autenticado.")
            Toast.makeText(this, "Error: Usuario no disponible", Toast.LENGTH_SHORT).show()
            return
        }

        lifecycleScope.launch {
            val hoy = LocalDate.now()
            val inicio = hoy.atStartOfDay()
            val fin = hoy.atTime(LocalTime.MAX)

            try {
                val pasos = healthManager.leerPasos(inicio, fin)
                val tvValorPasos = findViewById<TextView>(R.id.tvValorPromedio)
                tvValorPasos.text = pasos.toString()
                Log.d(TAG, "Pasos leídos de Health Connect para hoy ($hoy): $pasos")

                val userId = currentUser.uid
                val dateKey = hoy.format(DateTimeFormatter.ISO_LOCAL_DATE)
                val userDocumentRef = firestore.collection("users").document(userId)
                val dailyStepsUpdate = mapOf("dailySteps.$dateKey" to pasos)

                userDocumentRef.set(mapOf("lastLogin" to com.google.firebase.Timestamp.now()), com.google.firebase.firestore.SetOptions.merge())
                    .addOnSuccessListener { Log.d(TAG, "Marca de tiempo 'lastLogin' actualizada para $userId") }
                    .addOnFailureListener { e -> Log.w(TAG, "Error al actualizar 'lastLogin' para $userId", e) }

                userDocumentRef.set(dailyStepsUpdate, com.google.firebase.firestore.SetOptions.merge())
                    .addOnSuccessListener {
                        Log.d(TAG, "Pasos ($pasos) para el día $dateKey subidos exitosamente a Firestore para el usuario $userId.")
                    }
                    .addOnFailureListener { e ->
                        Log.e(TAG, "Error al subir pasos a Firestore para $userId:", e)
                        tvValorPasos.text = "Error al guardar"
                    }

            } catch (e: SecurityException) {
                Log.e(TAG, "Error de seguridad al leer pasos (posiblemente permisos revocados): ${e.message}", e)
                findViewById<TextView>(R.id.tvValorPromedio).text = "Error de permisos"
                // Aquí podrías querer solicitar los permisos de nuevo si fueron revocados
                 val requiredPermissions = healthManager.getPermissions()
                 permisoLauncher.launch(requiredPermissions)
            } catch (e: Exception) {
                Log.e(TAG, "Error al leer o subir pasos: ${e.message}", e)
                findViewById<TextView>(R.id.tvValorPromedio).text = "Error al leer pasos"
            }
        }
    }
}
