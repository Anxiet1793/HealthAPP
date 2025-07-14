package com.example.myapplication.ui

import android.os.Bundle
import android.webkit.WebView
import androidx.appcompat.app.AppCompatActivity
import com.example.myapplication.R

class PrivacyPolicyActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_privacy_policy)

        val webView: WebView = findViewById(R.id.webViewPrivacy)
        webView.loadUrl("file:///android_asset/privacy_policy.html")
    }
}
