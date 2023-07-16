package com.example.audbot

import android.Manifest
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.media.MediaRecorder
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException

class MainActivity : AppCompatActivity() {
    private lateinit var recordButton: Button
    private var mediaRecorder: MediaRecorder? = null
    private var isRecording = false
    private val RECORD_AUDIO_PERMISSION = Manifest.permission.RECORD_AUDIO
    private val PERMISSION_REQUEST_CODE = 200
    private val REQUEST_SAVE_FILE = 201

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        var apiService = ApiClient.apiService

        recordButton = findViewById(R.id.recordButton)
        recordButton.setOnClickListener {
            if (isRecording) {
                stopRecording()
            } else {
                startRecording()
            }
        }
    }

    private fun startRecording() {
        if (ContextCompat.checkSelfPermission(
                this,
                RECORD_AUDIO_PERMISSION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(RECORD_AUDIO_PERMISSION),
                PERMISSION_REQUEST_CODE
            )
        } else {
            mediaRecorder = MediaRecorder()
            mediaRecorder?.setAudioSource(MediaRecorder.AudioSource.MIC)
            mediaRecorder?.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP)
            mediaRecorder?.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB)
            val audioFilePath = getAudioFilePath()
            mediaRecorder?.setOutputFile(audioFilePath)
            try {
                mediaRecorder?.prepare()
                mediaRecorder?.start()
                isRecording = true
                recordButton.text = "Stop Recording"
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }

    private fun getAudioFilePath(): String {
        val audioDir = getExternalFilesDir(Environment.DIRECTORY_MUSIC)
        val audioFile = File.createTempFile("recording", ".3gp", audioDir)
        return audioFile.absolutePath
    }

    private fun stopRecording() {
        mediaRecorder?.stop()
        mediaRecorder?.release()
        mediaRecorder = null
        isRecording = false
        recordButton.text = "Record Audio"

        val dialog = AlertDialog.Builder(this)
            .setTitle("Upload Audio")
            .setMessage("Do you want to upload this audio file?")
            .setPositiveButton("Yes") { _, _ ->
                sendAudioToAPI(getAudioFilePath())
                Toast.makeText(this, "Clicked Yes", Toast.LENGTH_LONG).show();
            }
            .setNegativeButton("No", null)
            .create()

        dialog.show()
    }

    private fun sendAudioToAPI(audioFilePath: String) {
        val downloadDir = getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)?.absolutePath
        val file = File(audioFilePath)

        val dialog = AlertDialog.Builder(this)
            .setTitle("Download Path")
            .setMessage("Select a download path:")
            .setPositiveButton("Download") { _, _ ->
                showFileChooser(file, downloadDir)
            }
            .setNegativeButton("Cancel", null)
            .create()

        dialog.show()
    }

    private fun showFileChooser(file: File, downloadDir: String?) {
        val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "audio/3gpp"
            putExtra(Intent.EXTRA_TITLE, file.name)
        }

        startActivityForResult(intent, REQUEST_SAVE_FILE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == REQUEST_SAVE_FILE && resultCode == RESULT_OK) {
            data?.data?.let { uri ->
                val outputStream = contentResolver.openOutputStream(uri)
                outputStream?.use { outputStream ->
                    val inputFile = File(getAudioFilePath())
                    val inputStream = FileInputStream(inputFile)
                    inputStream.use { inputStream ->
                        inputStream.copyTo(outputStream)
                    }
                }

                // Process the downloaded file as needed
                val downloadedFilePath = File(uri.path).absolutePath
                Toast.makeText(
                    this@MainActivity,
                    "File received and saved at: $downloadedFilePath",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun callSampleDataApi() {
        val apiService = ApiClient.apiService
        val call = apiService.getSampleData()
        call.enqueue(object : Callback<SampleData> {
            override fun onResponse(call: Call<SampleData>, response: Response<SampleData>) {
                if (response.isSuccessful) {
                    val sampleData = response.body()
                    // Process the sampleData object here
                    Log.d("MainActivity", "Response status: Sample data received is $sampleData")
                } else {
                    // Handle error response
                    Log.d("MainActivity", "Response status: Sample data not received")
                }
            }

            override fun onFailure(call: Call<SampleData>, t: Throwable) {
                // Handle network failure
                Log.d("MainActivity", "Response status: Network failure")
                Log.e("MainActivity", "Network error: ${t.message}")
            }
        })
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST_CODE && grantResults.isNotEmpty()) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startRecording()
            }
        }
    }
}
