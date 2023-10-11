package com.example.sichacks

import android.Manifest
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothServerSocket
import android.bluetooth.BluetoothSocket
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Message
import android.provider.MediaStore
import android.util.Base64
import android.util.Log
import android.view.View
import android.widget.AdapterView.OnItemClickListener
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ImageView
import android.widget.ListView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import kotlinx.coroutines.runBlocking
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.UUID
import android.content.Intent as Intent1

class MainActivity : AppCompatActivity() {
    private lateinit var select_image_button: Button
    private lateinit var imageview: ImageView
    private lateinit var camerabtn: Button
    private var uri: String = ""
    private var bitmap: Bitmap? = null
    private var encodedImage: String? = null
    lateinit var listenButton: Button
    lateinit var sendButton: Button
    lateinit var listDevicesButton: Button
    lateinit var listView: ListView
    lateinit var status: TextView
    lateinit var imageView: ImageView
    lateinit var bluetoothAdapter: BluetoothAdapter
    lateinit var btArray: Array<BluetoothDevice?>
    lateinit var sendReceive: SendReceive
    private val STATE_LISTENING = 1
    private val STATE_CONNECTING = 2
    private val STATE_CONNECTED = 3
    private val STATE_CONNECTION_FAILED = 4
    private val STATE_MESSAGE_RECEIVED = 5
    private val REQUEST_ENABLE_BLUETOOTH = 1
    private val APP_NAME = "BTChat"
    private val MY_UUID: UUID = UUID.fromString("8ce255c0-223a-11e0-ac64-0803450c9a66")



    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent1?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == 250) {
            imageview.setImageURI(data?.data)

            var uuri: Uri? = data?.data
            uri = uuri.toString()
            bitmap = MediaStore.Images.Media.getBitmap(this.contentResolver, uuri)
            val scaledBitmap = Bitmap.createScaledBitmap(bitmap!!, 200, 200, true)

            val imageBytes = runBlocking {
                val baos = ByteArrayOutputStream()
                scaledBitmap?.compress(Bitmap.CompressFormat.JPEG,30 , baos)
                baos.toByteArray()
            }
            encodedImage = Base64.encodeToString(imageBytes, Base64.DEFAULT)

        } else if (requestCode == 200 && resultCode == Activity.RESULT_OK) {
            bitmap = data?.extras?.get("data") as Bitmap
            imageview.setImageBitmap(bitmap)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        select_image_button = findViewById(R.id.selectbtn)
        imageview = findViewById(R.id.imageview)
        camerabtn = findViewById(R.id.capturebtn)
        listenButton = findViewById(R.id.listen)
        sendButton = findViewById(R.id.send)
        listDevicesButton = findViewById(R.id.listDevices)
        listView = findViewById(R.id.listview)
        status = findViewById(R.id.status)
        imageView = findViewById(R.id.imageView)
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT)
            != PackageManager.PERMISSION_GRANTED
        ) {
            // Permission not granted, request it
            ActivityCompat.requestPermissions(
                this, arrayOf(Manifest.permission.BLUETOOTH_CONNECT),
                2
            )
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN)
            != PackageManager.PERMISSION_GRANTED
        ) {
            // Permission not granted, request it
            ActivityCompat.requestPermissions(
                this, arrayOf(Manifest.permission.BLUETOOTH_SCAN),
                2
            )
        }
        checkandGetpermissions()
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()

        if (bluetoothAdapter == null) {
            // Device does not support Bluetooth, handle accordingly
        } else {
            if (!bluetoothAdapter.isEnabled) {
                val enableIntent = Intent1(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                startActivityForResult(enableIntent, REQUEST_ENABLE_BLUETOOTH)
            }
        }

        implementListeners()

        select_image_button.setOnClickListener {
            Log.d("msg", "button pressed")
            var intent: Intent1 = Intent1(Intent1.ACTION_GET_CONTENT)
            intent.type = "image/*"
            startActivityForResult(intent, 250)
        }

        camerabtn.setOnClickListener {
            var camera = Intent1(MediaStore.ACTION_IMAGE_CAPTURE)
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

    private fun implementListeners() {
        listDevicesButton.setOnClickListener { view ->
            val bt: Set<BluetoothDevice?> = bluetoothAdapter.bondedDevices
            val strings = Array(bt.size) { "" }
            btArray = Array(bt.size) { null }
            var index = 0

            if (bt.size > 0) {
                for (device in bt) {
                    btArray[index] = device
                    strings[index] = device!!.name
                    index++
                }
                val arrayAdapter = ArrayAdapter(applicationContext, android.R.layout.simple_list_item_1, strings)
                listView.adapter = arrayAdapter
            }
        }
        listenButton.setOnClickListener(View.OnClickListener {
            val serverClass:MainActivity.ServerClass = ServerClass()
            serverClass.start()
        })

        listView.onItemClickListener =
            OnItemClickListener { adapterView, view, i, l ->
                val clientClass:MainActivity.ClientClass = ClientClass(
                    btArray[i]
                )

                clientClass.start()
                status.text = "Connecting"
            }

        sendButton.setOnClickListener(View.OnClickListener { view: View? ->
        sendReceive.writeImage()
        })
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


    private val handler = Handler(Handler.Callback { msg ->
        when (msg.what) {
            STATE_LISTENING -> status.text = "Listening"
            STATE_CONNECTING -> status.text = "Connecting"
            STATE_CONNECTED -> status.text = "Connected"
            STATE_CONNECTION_FAILED -> status.text = "Connection Failed"
            STATE_MESSAGE_RECEIVED -> {
                val receivedData = msg.obj as ByteArray
                val receivedBitmap = sendReceive.decodeImage(receivedData)
              ///  receivedBitmap?.compress(Bitmap.CompressFormat.JPEG, 1, ByteArrayOutputStream())
                imageView.setImageBitmap(receivedBitmap)
            }
        }
        true
    })

    private inner class ServerClass : Thread() {
        private var serverSocket: BluetoothServerSocket? = null

        init {
            try {
                serverSocket = bluetoothAdapter.listenUsingRfcommWithServiceRecord(APP_NAME, MY_UUID)
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }

        override fun run() {
            var socket: BluetoothSocket? = null

            while (socket == null) {
                try {
                    val message = Message.obtain()
                    message.what = STATE_CONNECTING
                    handler.sendMessage(message)

                    socket = serverSocket?.accept()
                } catch (e: IOException) {
                    e.printStackTrace()
                    val message = Message.obtain()
                    message.what = STATE_CONNECTION_FAILED
                    handler.sendMessage(message)
                }

                if (socket != null) {
                    val message = Message.obtain()
                    message.what = STATE_CONNECTED
                    handler.sendMessage(message)

                    sendReceive = SendReceive(socket)
                    sendReceive.start()
                    break
                }
            }
        }
    }

    private inner class ClientClass(private val device: BluetoothDevice?) : Thread() {
        private var socket: BluetoothSocket? = null

        init {
            try {
                socket = device!!.createRfcommSocketToServiceRecord(MY_UUID)
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }

        override fun run() {
            try {
                socket?.connect()
                val message = Message.obtain()
                message.what = STATE_CONNECTED
                handler.sendMessage(message)

                sendReceive = SendReceive(socket!!)
                sendReceive.start()
            } catch (e: IOException) {
                e.printStackTrace()
                val message = Message.obtain()
                message.what = STATE_CONNECTION_FAILED
                handler.sendMessage(message)
            }
        }
    }

    inner class SendReceive(private val bluetoothSocket: BluetoothSocket) : Thread() {
        private val inputStream: InputStream
        private val outputStream: OutputStream

        init {
            var tempIn: InputStream? = null
            var tempOut: OutputStream? = null

            try {
                tempIn = bluetoothSocket.inputStream
                tempOut = bluetoothSocket.outputStream
            } catch (e: IOException) {
                e.printStackTrace()
            }

            inputStream = tempIn!!
            outputStream = tempOut!!
        }

        override fun run() {
            val buffer = ByteArray(1024)
            var bytes: Int

            val dataBuffer = ByteArrayOutputStream()

            while (true) {
                try {
                    bytes = inputStream.read(buffer)
                    if (bytes > 0) {
                        dataBuffer.write(buffer, 0, bytes)
                        val receivedData = dataBuffer.toByteArray()
                        val receivedString = String(receivedData, Charsets.UTF_8)
                        Log.d("Received Data", receivedString) // Log the received data
                        if (receivedString.endsWith("\n")) {
                            // We've received a complete message (assuming it ends with a newline)
                            val base64Image = receivedString.trim()
                            Log.d("Base64 Image", base64Image) // Log the Base64 data
                            val image = decodeImage(base64Image.toByteArray())
                            if (image != null) {
                                // Now you can use the 'image' Bitmap for your desired operations.
                                // For example, you can display it in an ImageView:
                                runOnUiThread { imageView.setImageBitmap(image) } // Assuming 'imageView' is your ImageView
                            }
                            dataBuffer.reset() // Clear the buffer for the next message
                        }
                    }
                } catch (e: IOException) {
                    e.printStackTrace()
                    break
                }
            }
        }

        fun writeImage() {
//            val outputStream = ByteArrayOutputStream()
//            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
//            val imageBytes = outputStream.toByteArray()
//            val base64Image = Base64.encodeToString(imageBytes, Base64.DEFAULT)
        //    Log.d("Main Base64", encodedImage) // Log the received base64 string
            write(encodedImage!!.toByteArray())
        }

        fun decodeImage(encodedImage: ByteArray): Bitmap? {
            if (encodedImage.isEmpty()) {
                // Handle null or empty data gracefully
                Log.e("Decode Image", "Received null or empty data")
                return null // You can return a placeholder image or handle the error as needed
            }

            try {
                val imageBytes = Base64.decode(encodedImage, Base64.DEFAULT)
                Log.e("Encoded Image", encodedImage.toString())
                if (imageBytes.isNotEmpty()) {
                    return BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
                } else {
                    Log.e("Decode Image", "Decoded imageBytes is null or empty")
                    return null // Handle the error
                }
            } catch (e: IllegalArgumentException) {
                e.printStackTrace()
                Log.e("Decode Image", "Error decoding Base64 data: " + e.message)
                return null // Handle the error
            }
        }

        fun write(bytes: ByteArray) {
            try {
                outputStream.write(bytes)
                outputStream.flush()
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }


}