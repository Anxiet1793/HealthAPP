package com.example.myapplication

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.example.myapplication.databinding.ActivityLoginBinding
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private lateinit var googleSignInClient: GoogleSignInClient
    private lateinit var firebaseAuth: FirebaseAuth

    private val TAG = "LoginActivity"
    // Necesitarás obtener este ID de tu consola de Firebase/Google Cloud
    // Ve a Firebase Console -> Project Settings -> General -> Web API Key (si usas autenticación de Firebase)
    // O desde Google Cloud Console -> APIs & Services -> Credentials -> OAuth 2.0 Client IDs
    private val WEB_CLIENT_ID ="852657501886-knvo2ls1hs28ilar393ev4uiupd0ltvd.apps.googleusercontent.com" // <-- IMPORTANTE: REEMPLAZA ESTO

    private val googleSignInLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
                val account = task.getResult(ApiException::class.java)
                Log.d(TAG, "Firebase auth con Google: ${account.id}")
                firebaseAuthWithGoogle(account)
            } catch (e: ApiException) {
                Log.w(TAG, "Error en el inicio de sesión con Google", e)
                Toast.makeText(this, "Falló el inicio de sesión con Google", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Configurar Google Sign-In Options
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(WEB_CLIENT_ID) // Necesario para la autenticación con Firebase
            .requestEmail()
            .build()

        googleSignInClient = GoogleSignIn.getClient(this, gso)
        firebaseAuth = FirebaseAuth.getInstance()

        // Comprobar si el usuario ya ha iniciado sesión
        val currentUser = firebaseAuth.currentUser
        if (currentUser != null) {
            navigateToMainActivity()
        }

        binding.signInButton.setOnClickListener {
            signIn()
        }
    }

    private fun signIn() {
        val signInIntent = googleSignInClient.signInIntent
        googleSignInLauncher.launch(signInIntent)
    }

    private fun firebaseAuthWithGoogle(account: GoogleSignInAccount) {
        val credential = GoogleAuthProvider.getCredential(account.idToken, null)
        firebaseAuth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    Log.d(TAG, "signInWithCredential exitoso")

                    val user = firebaseAuth.currentUser
                    val db = FirebaseFirestore.getInstance()

                    user?.let {
                        val userId = it.uid
                        val userRef = db.collection("users").document(userId)

                        userRef.get().addOnSuccessListener { document ->
                            if (!document.exists()) {
                                val name = account.displayName ?: ""
                                val email = account.email ?: ""
                                val registrationDate = Timestamp.now()

                                val userData = hashMapOf(
                                    "name" to name,
                                    "email" to email,
                                    "birth_date" to "",
                                    "gender" to "",
                                    "height_cm" to 0,
                                    "weight_kg" to 0,
                                    "total_score" to 0,
                                    "registration_date" to registrationDate
                                )

                                userRef.set(userData)
                                    .addOnSuccessListener {
                                        Log.d(TAG, "Usuario agregado a Firestore")
                                        navigateToMainActivity()
                                    }
                                    .addOnFailureListener { e ->
                                        Log.e(TAG, "Error al agregar usuario", e)
                                        Toast.makeText(this, "Error al registrar usuario", Toast.LENGTH_SHORT).show()
                                    }
                            } else {
                                Log.d(TAG, "Usuario ya existe en Firestore")
                                navigateToMainActivity()
                            }
                        }.addOnFailureListener { e ->
                            Log.e(TAG, "Error al verificar existencia de usuario", e)
                            Toast.makeText(this, "Error verificando usuario", Toast.LENGTH_SHORT).show()
                        }
                    }
                } else {
                    Log.w(TAG, "signInWithCredential fallido", task.exception)
                    Toast.makeText(this, "Falló la autenticación con Firebase", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun navigateToMainActivity() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}
