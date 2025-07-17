package com.example.myapplication

import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.libraries.places.api.Places
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
class MapaActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private val client = OkHttpClient()

    private val apiKey = "AIzaSyA1yunZstzWMzvfefQ2nzDPcEN3S8nXmuI"
    private var ubicacionActual: LatLng = LatLng(-12.0464, -77.0428) // default: Lima

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_mapa)

        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        // Inicializar Places si aún no está
        if (!Places.isInitialized()) {
            Places.initialize(applicationContext, apiKey)
        }

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        // Configurar el spinner
        val spinner = findViewById<Spinner>(R.id.spinnerFiltros)
        val opciones = listOf("Gimnasios", "Restaurantes", "Tiendas Fit")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, opciones)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinner.adapter = adapter

        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                val tipo = when (position) {
                    0 -> "gym"
                    1 -> "restaurant"
                    2 -> "store"
                    else -> "store"
                }
                buscarLugaresCercanos(tipo)
            }

            override fun onNothingSelected(parent: AdapterView<*>) {}
        }
    }
    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            mMap.isMyLocationEnabled = true
        }

        obtenerUbicacion()
    }

    private fun obtenerUbicacion() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 1001)
            return
        }

        fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
            location?.let {
                ubicacionActual = LatLng(it.latitude, it.longitude)
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(ubicacionActual, 15f))
                mMap.addMarker(MarkerOptions().position(ubicacionActual).title("Estás aquí"))
            }
        }
    }


    private fun buscarLugaresCercanos(tipo: String) {
        mMap.clear()

        val keyword = when (tipo) {
            "gym" -> "gimnasio"
            "restaurant" -> "comida saludable"
            "store" -> "suplementos"
            else -> "salud"
        }

        val url = "https://maps.googleapis.com/maps/api/place/nearbysearch/json" +
                "?location=${ubicacionActual.latitude},${ubicacionActual.longitude}" +
                "&radius=2000" +
                "&type=$tipo" +
                "&keyword=${keyword.replace(" ", "%20")}"+
                "&key=$apiKey"

        println("🔍 URL: $url") // Para debug en Logcat

        val request = Request.Builder().url(url).build()

        client.newCall(request).enqueue(object : okhttp3.Callback {
            override fun onFailure(call: okhttp3.Call, e: java.io.IOException) {
                runOnUiThread {
                    Toast.makeText(applicationContext, "Error al buscar lugares", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onResponse(call: okhttp3.Call, response: okhttp3.Response) {
                val data = response.body?.string() ?: return
                val json = JSONObject(data)

                val status = json.getString("status")
                if (status != "OK") {
                    val errorMessage = json.optString("error_message", "Sin mensaje de error")
                    runOnUiThread {
                        Toast.makeText(applicationContext, "Error: $status\n$errorMessage", Toast.LENGTH_LONG).show()
                    }
                    println("❌ Error API: $status - $errorMessage")
                    return
                }

                val results = json.getJSONArray("results")
                runOnUiThread {
                    for (i in 0 until results.length()) {
                        val lugar = results.getJSONObject(i)
                        val name = lugar.getString("name")
                        val geometry = lugar.getJSONObject("geometry").getJSONObject("location")
                        val lat = geometry.getDouble("lat")
                        val lng = geometry.getDouble("lng")
                        val posicion = LatLng(lat, lng)
                        mMap.addMarker(MarkerOptions().position(posicion).title(name))
                    }
                }
            }
        })

    }



    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 1001 && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            obtenerUbicacion()
        } else {
            Toast.makeText(this, "Permiso de ubicación denegado", Toast.LENGTH_SHORT).show()
        }
    }
}