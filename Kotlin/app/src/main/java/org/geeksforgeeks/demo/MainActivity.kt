package org.geeksforgeeks.demo

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.masoudss.lib.WaveformSeekBar
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : AppCompatActivity() {

    // UI components
    private lateinit var startStopRecordingButton: Button
    private lateinit var playStopButton: ImageView
    private lateinit var micImage: ImageView
    private lateinit var statusText: TextView
    private lateinit var waveformView: WaveformSeekBar
    private lateinit var recyclerView: RecyclerView

    // Media components
    private var mRecorder: MediaRecorder? = null
    private var mPlayer: MediaPlayer? = null
    private lateinit var audioFilePath: String

    // Flags
    private var isRecording = false
    private var isPlaying = false

    // List of saved recordings
    private val recordings = mutableListOf<AudioRecording>()
    private lateinit var adapter: AudioRecordingAdapter

    // State for currently playing item in the list
    private var playingPlayer: MediaPlayer? = null
    private var playingIndex = -1
    private val seekHandler = Handler()

    // Called when the activity is first created
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Initialize UI components
        micImage = findViewById(R.id.micImage)
        playStopButton = findViewById(R.id.playStopButton)
        startStopRecordingButton = findViewById(R.id.startStopRecordingButton)
        statusText = findViewById(R.id.statusText)
        waveformView = findViewById(R.id.waveformView)
        recyclerView = findViewById(R.id.recordingsRecyclerView)

        // Set up RecyclerView with linear layout and adapter
        recyclerView.layoutManager = LinearLayoutManager(this)
        adapter = AudioRecordingAdapter(
            recordings,
            onPlayClick = { recording, index -> playRecording(recording, index) },
            onDeleteClick = { recording, index -> deleteRecording(recording, index) },
            onShareClick = { recording -> shareRecording(recording) }
        )
        recyclerView.adapter = adapter

        // Load any saved recordings from storage
        loadRecordings()

        // Ask for mic permission if not already granted
        if (!checkPermissions()) requestPermissions()

        // Set click listener to start or stop recording
        startStopRecordingButton.setOnClickListener {
            if (!isRecording) startRecording() else stopRecording()
        }

        // Set click listener to play or stop last recorded file
        playStopButton.setOnClickListener {
            if (!isPlaying) playAudio() else stopAudio()
        }
    }

    // Generate a unique file path for each new recording
    private fun getNewFilePath(): String {
        val dir = getExternalFilesDir(null)
        val formatter = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.getDefault())
        val timeStamp = formatter.format(Date())
        return File(dir, "Recording_$timeStamp.3gp").absolutePath
    }

    // Start recording using the MediaRecorder API
    private fun startRecording() {
        if (!checkPermissions()) {
            requestPermissions()
            return
        }

        audioFilePath = getNewFilePath()

        mRecorder = MediaRecorder().apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP)
            setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB)
            setOutputFile(audioFilePath)

            try {
                prepare()
                start()
                isRecording = true
                startStopRecordingButton.text = "Stop Recording"
                micImage.setImageResource(R.drawable.mic_open)
                statusText.text = "Recording started"
            } catch (e: IOException) {
                Log.e("MainActivity", "Recording failed: ${e.localizedMessage}")
                statusText.text = "Recording failed"
            }
        }
    }

    // Stop recording and save the file to list
    private fun stopRecording() {
        mRecorder?.apply {
            stop()
            release()
        }

        mRecorder = null
        isRecording = false
        startStopRecordingButton.text = "Start Recording"
        micImage.setImageResource(R.drawable.mic_close)
        statusText.text = "Recording saved"

        val fileName = File(audioFilePath).name
        recordings.add(AudioRecording(audioFilePath, fileName))
        adapter.notifyItemInserted(recordings.size - 1)
    }

    // Play the most recent audio and show waveform animation
    private fun playAudio() {
        if (!::audioFilePath.isInitialized || !File(audioFilePath).exists()) {
            Toast.makeText(this, "No recording found to play", Toast.LENGTH_SHORT).show()
            return
        }

        mPlayer = MediaPlayer().apply {
            try {
                setDataSource(audioFilePath)
                prepare()
                start()
                waveformView.setSampleFrom(audioFilePath)
                this@MainActivity.isPlaying = true
                playStopButton.setImageResource(R.drawable.pause)
                statusText.text = "Playing..."
                handler.post(updateWaveformRunnable)

                setOnCompletionListener {
                    stopAudio()
                    statusText.text = "Playback complete"
                }

            } catch (e: IOException) {
                Log.e("MainActivity", "Playback failed: ${e.localizedMessage}")
                statusText.text = "Playback failed"
            }
        }
    }

    // Stop current audio playback
    private fun stopAudio() {
        mPlayer?.release()
        mPlayer = null
        isPlaying = false
        playStopButton.setImageResource(R.drawable.play)
        statusText.text = "Playback stopped"
        handler.removeCallbacks(updateWaveformRunnable)
        waveformView.progress = 0f
    }

    // Periodically updates waveform progress while audio plays
    private val handler = Handler()
    private val updateWaveformRunnable = object : Runnable {
        override fun run() {
            mPlayer?.let {
                if (it.isPlaying) {
                    val progress = (it.currentPosition.toFloat() / it.duration) * 100
                    waveformView.progress = progress
                    handler.postDelayed(this, 100)
                }
            }
        }
    }

    // Play audio from a recording in the list (by index)
    private fun playRecording(recording: AudioRecording, index: Int) {
        // Stop previously playing audio if different
        if (playingPlayer != null && playingIndex != index) stopPlaying()

        if (recording.isPlaying) {
            stopPlaying()
        } else {
            playingPlayer = MediaPlayer().apply {
                try {
                    setDataSource(recording.filePath)
                    prepare()
                    start()
                    recording.isPlaying = true
                    playingIndex = index
                    adapter.notifyItemChanged(index)

                    setOnCompletionListener {
                        recording.isPlaying = false
                        adapter.notifyItemChanged(index)
                        playingPlayer = null
                        playingIndex = -1
                    }

                    updateSeekBar(index)

                } catch (e: IOException) {
                    Log.e("MainActivity", "Playback failed: ${e.localizedMessage}")
                }
            }
        }
    }

    // Stops currently playing list item
    private fun stopPlaying() {
        playingPlayer?.release()
        playingPlayer = null
        if (playingIndex >= 0 && playingIndex < recordings.size) {
            recordings[playingIndex].isPlaying = false
            adapter.notifyItemChanged(playingIndex)
            playingIndex = -1
        }
    }

    // Updates the progress bar (seek bar) for the item being played
    private fun updateSeekBar(index: Int) {
        val runnable = object : Runnable {
            override fun run() {
                val player = playingPlayer ?: return
                if (player.isPlaying && index == playingIndex) {
                    val percent = (100 * player.currentPosition) / player.duration
                    val holder = recyclerView.findViewHolderForAdapterPosition(index)
                            as? AudioRecordingAdapter.AudioViewHolder
                    holder?.seekBar?.progress = percent
                    seekHandler.postDelayed(this, 500)
                }
            }
        }
        seekHandler.post(runnable)
    }

    // Loads all previously recorded .3gp files from app storage
    private fun loadRecordings() {
        val dir = getExternalFilesDir(null)
        dir?.listFiles()?.filter { it.name.endsWith(".3gp") }?.sortedBy { it.name }?.forEach {
            recordings.add(AudioRecording(it.absolutePath, it.name))
        }
        adapter.notifyDataSetChanged()
    }

    // Deletes the selected recording from list and file system
    private fun deleteRecording(recording: AudioRecording, index: Int) {
        val file = File(recording.filePath)
        if (file.exists()) file.delete()

        recordings.removeAt(index)
        adapter.notifyItemRemoved(index)
        Toast.makeText(this, "Recording deleted", Toast.LENGTH_SHORT).show()
    }

    // Shares the selected audio file using external apps
    private fun shareRecording(recording: AudioRecording) {
        val file = File(recording.filePath)
        if (!file.exists()) return

        val uri: Uri = FileProvider.getUriForFile(
            this,
            "${packageName}.provider",
            file
        )

        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "audio/*"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        startActivity(Intent.createChooser(shareIntent, "Share recording via"))
    }

    // Checks if microphone permission is granted
    private fun checkPermissions(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
    }

    // Requests microphone permission
    private fun requestPermissions() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.RECORD_AUDIO),
            REQUEST_AUDIO_PERMISSION_CODE
        )
    }

    // Handles result from permission request dialog
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_AUDIO_PERMISSION_CODE) {
            statusText.text = if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)
                "Permission granted" else "Permission denied"
        }
    }

    // Constant for permission request code
    companion object {
        const val REQUEST_AUDIO_PERMISSION_CODE = 200
    }
}