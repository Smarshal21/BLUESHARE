package com.example.sichacks

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothServerSocket
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.os.Handler
import android.os.Message
import android.util.Base64
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ImageView
import android.widget.ListView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.UUID

class ReceiveActivity : AppCompatActivity() {

    private lateinit var listenButton: Button
    private lateinit var sendButton: Button
    private lateinit var listDevicesButton: Button
    private lateinit var listView: ListView
    private lateinit var status: TextView
    private lateinit var encodedImage: String
    lateinit var imageView: ImageView
    lateinit var bluetoothAdapter: BluetoothAdapter
    private lateinit var btArray: Array<BluetoothDevice?>
    lateinit var sendReceive: SendReceive
    private lateinit var imagelistview: ListView
    private val STATE_LISTENING = 1
    private val STATE_CONNECTING = 2
    private val STATE_CONNECTED = 3
    private val STATE_CONNECTION_FAILED = 4
    private val STATE_MESSAGE_RECEIVED = 5
    private val REQUEST_ENABLE_BLUETOOTH = 1
    private val APP_NAME = "BTChat"
    private val receivedImagesList = ArrayList<Bitmap>()
private lateinit var adapter: ArrayAdapter<Bitmap>
    private val MY_UUID: UUID = UUID.fromString("8ce255c0-223a-11e0-ac64-0803450c9a66")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_receive)
        listenButton = findViewById(R.id.listen)
        sendButton = findViewById(R.id.send)
        listDevicesButton = findViewById(R.id.listDevices)
        listView = findViewById(R.id.listview)
        status = findViewById(R.id.status)
        imageView = findViewById(R.id.imageView)
        imagelistview = findViewById(R.id.imagelistview)
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()

         adapter = ArrayAdapter<Bitmap>(this, android.R.layout.simple_list_item_1, receivedImagesList)
        imagelistview.adapter = adapter
        val sharedPreferences = applicationContext.getSharedPreferences("MyImages", Context.MODE_PRIVATE)
        val imageStrings = sharedPreferences.getStringSet("imageStrings", setOf())

        for (imageString in imageStrings!!) {
            val image = decodeImageFromBase64(imageString)
            if (image != null) {
                receivedImagesList.add(image)
            }
        }


        imagelistview.onItemClickListener = AdapterView.OnItemClickListener { _, _, position, _ ->
            // Handle item click here
            val selectedImage = receivedImagesList[position]
            displayImageInFullScreen(selectedImage)
        }


        if (bluetoothAdapter == null) {
        } else {
            if (!bluetoothAdapter.isEnabled) {
                val enableIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                startActivityForResult(enableIntent, REQUEST_ENABLE_BLUETOOTH)
            }
        }

        implementListeners()
        var receivedIntent = intent

        if (receivedIntent != null) {
            var dataValue = receivedIntent.getStringExtra("image")
            if (dataValue != null) {
                encodedImage = dataValue
            }
        }

    }
    private fun displayImageInFullScreen(image: Bitmap) {
        val intent = Intent(this, FullScreenImageActivity::class.java)
        intent.putExtra("selectedImage", encodeImageToBase64(image))
        startActivity(intent)
    }
    private fun encodeImageToBase64(image: Bitmap): String {
        val byteArrayOutputStream = ByteArrayOutputStream()
        image.compress(Bitmap.CompressFormat.PNG, 100, byteArrayOutputStream)
        val bytes = byteArrayOutputStream.toByteArray()
        return Base64.encodeToString(bytes, Base64.DEFAULT)
    }

    private fun decodeImageFromBase64(encodedImage: String): Bitmap? {
        val imageBytes = Base64.decode(encodedImage, Base64.DEFAULT)
        return BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
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
                val arrayAdapter =
                    ArrayAdapter(applicationContext, android.R.layout.simple_list_item_1, strings)
                listView.adapter = arrayAdapter
            }
        }
        listenButton.setOnClickListener {
            val serverClass = ServerClass()
            serverClass.start()
        }

        listView.onItemClickListener =
            AdapterView.OnItemClickListener { adapterView, view, i, l ->
                val clientClass = ClientClass(
                    btArray[i]
                )

                clientClass.start()
                status.text = "Connecting"
            }

        sendButton.setOnClickListener { view: View? ->
            sendReceive.writeImage()
        }
    }

    private val handler = Handler { msg ->
        when (msg.what) {
            STATE_LISTENING -> status.text = "Listening"
            STATE_CONNECTING -> status.text = "Connecting"
            STATE_CONNECTED -> status.text = "Connected"
            STATE_CONNECTION_FAILED -> status.text = "Connection Failed"
            STATE_MESSAGE_RECEIVED -> {
                val receivedData = msg.obj as ByteArray
                val receivedBitmap = sendReceive.decodeImage(receivedData)
                imageView.setImageBitmap(receivedBitmap)
            }
        }
        true
    }

    private inner class ServerClass : Thread() {
        private var serverSocket: BluetoothServerSocket? = null

        init {
            try {
                serverSocket =
                    bluetoothAdapter.listenUsingRfcommWithServiceRecord(APP_NAME, MY_UUID)
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
                        Log.d("Received Data", receivedString)
                        if (receivedString.endsWith("\n")) {
                            val base64Image = receivedString.trim()
                            val image = decodeImage(base64Image.toByteArray())
                            if (image != null) {
                                receivedImagesList.add(image)
                                saveImagesToSharedPreferences()
                                runOnUiThread { imageView.setImageBitmap(image)
                                    updateListView() }
                            }
                            dataBuffer.reset()
                        }
                    }
                } catch (e: IOException) {
                    e.printStackTrace()
                    break
                }
            }
        }
        private fun saveImagesToSharedPreferences() {
            val sharedPreferences = applicationContext.getSharedPreferences("MyImages", Context.MODE_PRIVATE)
            val editor = sharedPreferences.edit()
            val imageStrings = receivedImagesList.map { encodeImageToBase64(it) }
            editor.putStringSet("imageStrings", imageStrings.toSet())
            editor.apply()
        }

        private fun encodeImageToBase64(image: Bitmap): String {
            val byteArrayOutputStream = ByteArrayOutputStream()
            image.compress(Bitmap.CompressFormat.PNG, 100, byteArrayOutputStream)
            val bytes = byteArrayOutputStream.toByteArray()
            return Base64.encodeToString(bytes, Base64.DEFAULT)
        }

        private fun updateListView() {
            val adapter = ArrayAdapter<Bitmap>(applicationContext, android.R.layout.simple_list_item_1, receivedImagesList)
            imagelistview.adapter = adapter
        }

        fun writeImage() {
            write(encodedImage.toByteArray())
        }

        fun decodeImage(encodedImage: ByteArray): Bitmap? {
            if (encodedImage.isEmpty()) {
                Log.e("Decode Image", "Received null or empty data")
                return null
            }

            try {
                val imageBytes = Base64.decode(encodedImage, Base64.DEFAULT)
                Log.e("Encoded Image", encodedImage.toString())
                if (imageBytes.isNotEmpty()) {
                    return BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
                } else {
                    return null
                }
            } catch (e: IllegalArgumentException) {
                e.printStackTrace()
                return null
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