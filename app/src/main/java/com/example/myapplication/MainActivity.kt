package com.example.myapplication

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.widget.Button
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.myapplication.ui.PrivacyPolicyActivity

class MainActivity : AppCompatActivity() {

    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var privacyPolicyLauncher: ActivityResultLauncher<Intent>

    companion object {
        const val PREFS_NAME = "MyPrefsFile"
        const val CONSENT_GIVEN_KEY = "health_consent_given"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // enableEdgeToEdge() // Se comenta si causa problemas o no es estrictamente necesario para el ejemplo
        setContentView(R.layout.activity_main)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        sharedPreferences = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        privacyPolicyLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            // Este bloque se ejecuta cuando PrivacyPolicyActivity regresa.
            // Volvemos a mostrar el diálogo de consentimiento para que el usuario pueda actuar.
            mostrarDialogoConsentimiento()
        }

        val botonVerPasos = findViewById<Button>(R.id.btnVerPasos)

        botonVerPasos.setOnClickListener {
            if (sharedPreferences.getBoolean(CONSENT_GIVEN_KEY, false)) {
                // El consentimiento ya fue otorgado, iniciar PasosDiarios directamente
                val intent = Intent(this, PasosDiarios::class.java)
                startActivity(intent)
            } else {
                // Mostrar el diálogo de consentimiento
                mostrarDialogoConsentimiento()
            }
        }
    }

    private fun mostrarDialogoConsentimiento() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle(getString(R.string.consent_dialog_title))
        builder.setMessage(getString(R.string.consent_dialog_message))
        builder.setCancelable(false) // Evitar que se cierre al tocar fuera o con botón atrás

        builder.setPositiveButton(getString(R.string.accept)) { dialog, which ->
            // Guardar que el consentimiento fue otorgado
            with(sharedPreferences.edit()) {
                putBoolean(CONSENT_GIVEN_KEY, true)
                apply()
            }
            // Iniciar PasosDiarios Activity
            val intent = Intent(this, PasosDiarios::class.java)
            startActivity(intent)
        }

        builder.setNegativeButton(getString(R.string.cancel)) { dialog, which ->
            // El usuario canceló. Puedes mostrar un Toast o no hacer nada.
            // Podrías considerar si quieres cerrar la app o deshabilitar funciones.
            dialog.dismiss()
        }

        builder.setNeutralButton(getString(R.string.privacy_policy)) { dialog, which ->
            // Abrir PrivacyPolicyActivity usando el launcher
            val intent = Intent(this, PrivacyPolicyActivity::class.java)
            privacyPolicyLauncher.launch(intent)
            // El diálogo se cerrará automáticamente al iniciar otra actividad.
            // El launcher se encargará de reabrir el diálogo al volver.
        }

        val dialog = builder.create()
        dialog.show()
    }
}
