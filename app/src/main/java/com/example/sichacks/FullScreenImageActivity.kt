package com.example.sichacks

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Base64
import android.widget.ImageView

class FullScreenImageActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_full_screen_image)

        val imageView = findViewById<ImageView>(R.id.fullScreenImageView)
        val imageString = intent.getStringExtra("selectedImage")

        if (imageString != null) {
            val selectedImage = decodeImageFromBase64(imageString)
            imageView.setImageBitmap(selectedImage)
        }
    }
    private fun decodeImageFromBase64(encodedImage: String): Bitmap? {
        val imageBytes = Base64.decode(encodedImage, Base64.DEFAULT)
        return BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
    }

}
