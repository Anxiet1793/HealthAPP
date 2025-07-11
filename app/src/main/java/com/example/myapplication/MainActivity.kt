package com.example.myapplication

import android.content.Intent // Importar Intent
import android.os.Bundle
import android.widget.Button // Importar Button
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        // Aplicar WindowInsets al layout principal
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // --- INICIO DE LA LÓGICA PARA EL BOTÓN ---

        // 1. Obtener una referencia al botón btnVerPasos
        //    Asegúrate de que el ID 'btnVerPasos' existe en tu activity_main.xml
        val botonVerPasos = findViewById<Button>(R.id.btnVerPasos)

        // 2. Configurar un OnClickListener para el botón
        botonVerPasos.setOnClickListener {
            // 3. Crear un Intent para iniciar PasosDiariosActivity
            //    Asegúrate de que la clase PasosDiarios existe y está declarada en tu AndroidManifest.xml
            val intent = Intent(this, PasosDiarios::class.java)

            // 4. Iniciar la actividad
            startActivity(intent)
        }

        // --- FIN DE LA LÓGICA PARA EL BOTÓN ---
    }
}