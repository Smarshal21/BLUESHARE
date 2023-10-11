package com.example.sichacks

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Button
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {
    private lateinit var sendActivity: Button
    private lateinit var receiveActivity: Button

    @RequiresApi(Build.VERSION_CODES.S)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        sendActivity = findViewById(R.id.SendAct)
        receiveActivity = findViewById(R.id.ReceiveAct)
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT)
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this, arrayOf(Manifest.permission.BLUETOOTH_CONNECT),
                2
            )
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN)
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this, arrayOf(Manifest.permission.BLUETOOTH_SCAN),
                2
            )
        }
        sendActivity.setOnClickListener {
            val intent: Intent = Intent(this, SendActivity::class.java)
            startActivity(intent)
        }
        receiveActivity.setOnClickListener {
            val intent = Intent(this, ReceiveActivity::class.java)
            startActivity(intent)
        }
    }

}