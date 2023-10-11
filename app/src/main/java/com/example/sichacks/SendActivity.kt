package com.example.sichacks

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Base64
import android.util.Log
import android.widget.Button
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.runBlocking
import java.io.ByteArrayOutputStream


class SendActivity : AppCompatActivity() {
    private lateinit var select_image_button: Button
    private lateinit var imageview: ImageView
    private lateinit var camerabtn: Button
    private var uri: String = ""
    private var bitmap: Bitmap? = null
    private var encodedImage: String? = null

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == 250) {
            imageview.setImageURI(data?.data)

            var uuri: Uri? = data?.data
            uri = uuri.toString()
            bitmap = MediaStore.Images.Media.getBitmap(this.contentResolver, uuri)
            val targetWidth = 200
            val targetHeight = 250

            val width = bitmap!!.width
            val height = bitmap!!.height

// Calculate the scale factors to fit the bitmap within the target dimensions
            val scaleWidth = targetWidth.toFloat() / width
            val scaleHeight = targetHeight.toFloat() / height

// Use the smaller scale factor to maintain the aspect ratio
            val scaleFactor = minOf(scaleWidth, scaleHeight)

// Calculate the new dimensions
            val scaledWidth = (width * scaleFactor).toInt()
            val scaledHeight = (height * scaleFactor).toInt()

// Create the scaled bitmap
            val scaledBitmap = scaleBitmap(bitmap!!,bitmap!!.width,bitmap!!.height)
               val sam =  Bitmap.createScaledBitmap(scaledBitmap!!, scaledWidth, scaledHeight, true)

            val imageBytes = runBlocking {
                val baos = ByteArrayOutputStream()
               sam.compress(Bitmap.CompressFormat.JPEG, 60, baos)
                baos.toByteArray()
            }
            encodedImage = Base64.encodeToString(imageBytes, Base64.DEFAULT)
            val intent = Intent(this, ReceiveActivity::class.java)
            intent.putExtra("image", encodedImage)
            startActivity(intent)

        } else if (requestCode == 200 && resultCode == Activity.RESULT_OK) {
            bitmap = data?.extras?.get("data") as Bitmap
            imageview.setImageBitmap(bitmap)
        }
    }
    fun scaleBitmap(bitmap: Bitmap, newWidth: Int, newHeight: Int): Bitmap? {
        val scaledBitmap = Bitmap.createBitmap(newWidth, newHeight, Bitmap.Config.ARGB_8888)
        val scaleX = newWidth / bitmap.width.toFloat()
        val scaleY = newHeight / bitmap.height.toFloat()
        val pivotX = 0f
        val pivotY = 0f
        val scaleMatrix = Matrix()
        scaleMatrix.setScale(scaleX, scaleY, pivotX, pivotY)
        val canvas = Canvas(scaledBitmap)
        canvas.setMatrix(scaleMatrix)
        canvas.drawBitmap(bitmap, 0f, 0f, Paint(Paint.FILTER_BITMAP_FLAG))
        return scaledBitmap
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_send)

        select_image_button = findViewById(R.id.selectbtn)
        imageview = findViewById(R.id.imageview)
        camerabtn = findViewById(R.id.capturebtn)
        checkandGetpermissions()
        select_image_button.setOnClickListener {
            Log.d("msg", "button pressed")
            var intent: Intent = Intent(Intent.ACTION_GET_CONTENT)
            intent.type = "image/*"
            startActivityForResult(intent, 250)
        }

        camerabtn.setOnClickListener {
            var camera = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            startActivityForResult(camera, 200)
        }
    }

    fun checkandGetpermissions() {
        if (checkSelfPermission(android.Manifest.permission.CAMERA) == PackageManager.PERMISSION_DENIED) {
            requestPermissions(arrayOf(android.Manifest.permission.CAMERA), 100)
        } else {
            Toast.makeText(this, "Camera permission granted", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 100 && grantResults.isNotEmpty()) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Camera permission granted", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Permission Denied", Toast.LENGTH_SHORT).show()
            }
        }
    }
}