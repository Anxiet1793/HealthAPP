package com.example.myapplication

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
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
        const val GENDER_KEY = "user_gender"
        const val GENDER_SET_KEY = "gender_set"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        sharedPreferences = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        privacyPolicyLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            mostrarDialogoConsentimiento()
        }

        val botonVerPasos = findViewById<Button>(R.id.btnVerPasos)
        botonVerPasos.setOnClickListener {
            if (sharedPreferences.getBoolean(CONSENT_GIVEN_KEY, false)) {
                val intent = Intent(this, PasosDiarios::class.java)
                startActivity(intent)
            } else {
                mostrarDialogoConsentimiento()
            }
        }

        // Check if gender has been set, if not, show dialog
        if (!sharedPreferences.getBoolean(GENDER_SET_KEY, false)) {
            solicitarGenero()
        } //else {
            // Optionally, if consent is given but gender is not set (e.g. older version users),
            // you might want to prompt for gender here too, or after consent.
            // For now, new users will see gender dialog first if not set.
        //}

        // Add a button to navigate to ImcActivity (Example)
        val btnGoToImc = findViewById<Button>(R.id.btnCalcularPeso) // Assuming R.id.btnCalcularPeso is for IMC
        btnGoToImc.setOnClickListener {
            startActivity(Intent(this, ImcActivity::class.java))
        }

        val btnGoToCalorias = findViewById<Button>(R.id.btncalorias)
        btnGoToCalorias.setOnClickListener {
            startActivity(Intent(this, CaloriasActivity::class.java))
        }
    }

    private fun solicitarGenero() {
        val genderOptions = arrayOf(getString(R.string.gender_male), getString(R.string.gender_female))
        val builder = AlertDialog.Builder(this)
        builder.setTitle(getString(R.string.gender_dialog_title))
        builder.setCancelable(false) // User must choose
        builder.setItems(genderOptions) { dialog, which ->
            val selectedGender = genderOptions[which]
            with(sharedPreferences.edit()) {
                putString(GENDER_KEY, selectedGender)
                putBoolean(GENDER_SET_KEY, true)
                apply()
            }
            Toast.makeText(this, getString(R.string.gender_selected_toast, selectedGender), Toast.LENGTH_SHORT).show()
            dialog.dismiss()
            // After gender selection, you might want to proceed to consent dialog if not given
            // or to the main app flow if consent is already handled.
            // For this example, it just dismisses. Consider the full user flow.
        }
        builder.show()
    }

    private fun mostrarDialogoConsentimiento() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle(getString(R.string.consent_dialog_title))
        builder.setMessage(getString(R.string.consent_dialog_message))
        builder.setCancelable(false)

        builder.setPositiveButton(getString(R.string.accept)) { dialog, which ->
            with(sharedPreferences.edit()) {
                putBoolean(CONSENT_GIVEN_KEY, true)
                apply()
            }
            val intent = Intent(this, PasosDiarios::class.java)
            startActivity(intent)
        }

        builder.setNegativeButton(getString(R.string.cancel)) { dialog, which ->
            dialog.dismiss()
        }

        builder.setNeutralButton(getString(R.string.privacy_policy)) { dialog, which ->
            val intent = Intent(this, PrivacyPolicyActivity::class.java)
            privacyPolicyLauncher.launch(intent)
        }

        val dialog = builder.create()
        dialog.show()
    }
}
