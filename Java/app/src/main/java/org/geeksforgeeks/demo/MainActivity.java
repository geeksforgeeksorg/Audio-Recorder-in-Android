package org.geeksforgeeks.demo;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.widget.*;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.masoudss.lib.WaveformSeekBar;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    // UI components
    private Button startStopRecordingButton;
    private ImageView playStopButton;
    private ImageView micImage;
    private TextView statusText;
    private WaveformSeekBar waveformView;
    private RecyclerView recyclerView;

    // Media components
    private MediaRecorder mRecorder;
    private MediaPlayer mPlayer;
    private String audioFilePath;

    // Flags
    private boolean isRecording = false;
    private boolean isPlaying = false;

    // List of saved recordings
    private final List<AudioRecording> recordings = new ArrayList<>();
    private AudioRecordingAdapter adapter;

    // State for currently playing item in the list
    private MediaPlayer playingPlayer = null;
    private int playingIndex = -1;
    private final Handler seekHandler = new Handler();

    // Called when the activity is first created
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize UI components
        micImage = findViewById(R.id.micImage);
        playStopButton = findViewById(R.id.playStopButton);
        startStopRecordingButton = findViewById(R.id.startStopRecordingButton);
        statusText = findViewById(R.id.statusText);
        waveformView = findViewById(R.id.waveformView);
        recyclerView = findViewById(R.id.recordingsRecyclerView);

        // Set up RecyclerView with linear layout and adapter
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new AudioRecordingAdapter(
                recordings,
                this::playRecording,
                this::deleteRecording,
                this::shareRecording
        );
        recyclerView.setAdapter(adapter);

        // Load any saved recordings from storage
        loadRecordings();

        // Ask for mic permission if not already granted
        if (!checkPermissions()) requestPermissions();

        // Set click listener to start or stop recording
        startStopRecordingButton.setOnClickListener(v -> {
            if (!isRecording) startRecording();
            else stopRecording();
        });

        // Set click listener to play or stop last recorded file
        playStopButton.setOnClickListener(v -> {
            if (!isPlaying) playAudio();
            else stopAudio();
        });
    }

    // Generate a unique file path for each new recording
    private String getNewFilePath() {
        File dir = getExternalFilesDir(null);
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.getDefault());
        String timeStamp = formatter.format(new Date());
        return new File(dir, "Recording_" + timeStamp + ".3gp").getAbsolutePath();
    }

    // Start recording using the MediaRecorder API
    private void startRecording() {
        if (!checkPermissions()) {
            requestPermissions();
            return;
        }

        audioFilePath = getNewFilePath();

        mRecorder = new MediaRecorder();
        mRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        mRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
        mRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
        mRecorder.setOutputFile(audioFilePath);

        try {
            mRecorder.prepare();
            mRecorder.start();
            isRecording = true;
            startStopRecordingButton.setText("Stop Recording");
            micImage.setImageResource(R.drawable.mic_open);
            statusText.setText("Recording started");
        } catch (IOException e) {
            Log.e("MainActivity", "Recording failed: " + e.getLocalizedMessage());
            statusText.setText("Recording failed");
        }
    }

    // Stop recording and save the file to list
    private void stopRecording() {
        if (mRecorder != null) {
            mRecorder.stop();
            mRecorder.release();
        }
        mRecorder = null;
        isRecording = false;
        startStopRecordingButton.setText("Start Recording");
        micImage.setImageResource(R.drawable.mic_close);
        statusText.setText("Recording saved");

        String fileName = new File(audioFilePath).getName();
        recordings.add(new AudioRecording(audioFilePath, fileName));
        adapter.notifyItemInserted(recordings.size() - 1);
    }

    // Play the most recent audio and show waveform animation
    private void playAudio() {
        if (audioFilePath == null || !new File(audioFilePath).exists()) {
            Toast.makeText(this, "No recording found to play", Toast.LENGTH_SHORT).show();
            return;
        }

        mPlayer = new MediaPlayer();
        try {
            mPlayer.setDataSource(audioFilePath);
            mPlayer.prepare();
            mPlayer.start();
            waveformView.setSampleFrom(audioFilePath);
            isPlaying = true;
            playStopButton.setImageResource(R.drawable.pause);
            statusText.setText("Playing...");
            seekHandler.post(updateWaveformRunnable);

            mPlayer.setOnCompletionListener(mp -> {
                stopAudio();
                statusText.setText("Playback complete");
            });
        } catch (IOException e) {
            Log.e("MainActivity", "Playback failed: " + e.getLocalizedMessage());
            statusText.setText("Playback failed");
        }
    }

    // Stop current audio playback
    private void stopAudio() {
        if (mPlayer != null) {
            mPlayer.release();
            mPlayer = null;
        }
        isPlaying = false;
        playStopButton.setImageResource(R.drawable.play);
        statusText.setText("Playback stopped");
        seekHandler.removeCallbacks(updateWaveformRunnable);
        waveformView.setProgress(0f);
    }

    // Periodically updates waveform progress while audio plays
    private final Runnable updateWaveformRunnable = new Runnable() {
        @Override
        public void run() {
            if (mPlayer != null && mPlayer.isPlaying()) {
                float progress = (mPlayer.getCurrentPosition() * 100f) / mPlayer.getDuration();
                waveformView.setProgress(progress);
                seekHandler.postDelayed(this, 100);
            }
        }
    };

    // Play audio from a recording in the list (by index)
    private void playRecording(AudioRecording recording, int index) {
        // Stop previously playing audio if different
        if (playingPlayer != null && playingIndex != index) stopPlaying();

        if (recording.isPlaying()) {
            stopPlaying();
        } else {
            playingPlayer = new MediaPlayer();
            try {
                playingPlayer.setDataSource(recording.getFilePath());
                playingPlayer.prepare();
                playingPlayer.start();
                recording.setPlaying(true);
                playingIndex = index;
                adapter.notifyItemChanged(index);

                playingPlayer.setOnCompletionListener(mp -> {
                    recording.setPlaying(false);
                    adapter.notifyItemChanged(index);
                    playingPlayer = null;
                    playingIndex = -1;
                });

                updateSeekBar(index);

            } catch (IOException e) {
                Log.e("MainActivity", "Playback failed: " + e.getLocalizedMessage());
            }
        }
    }

    // Stops currently playing list item
    private void stopPlaying() {
        if (playingPlayer != null) {
            playingPlayer.release();
            playingPlayer = null;
        }
        if (playingIndex >= 0 && playingIndex < recordings.size()) {
            recordings.get(playingIndex).setPlaying(false);
            adapter.notifyItemChanged(playingIndex);
            playingIndex = -1;
        }
    }

    // Updates the progress bar (seek bar) for the item being played
    private void updateSeekBar(int index) {
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                if (playingPlayer != null && playingPlayer.isPlaying() && index == playingIndex) {
                    int percent = (100 * playingPlayer.getCurrentPosition()) / playingPlayer.getDuration();
                    AudioRecordingAdapter.AudioViewHolder holder = (AudioRecordingAdapter.AudioViewHolder) recyclerView.findViewHolderForAdapterPosition(index);
                    if (holder != null) {
                        holder.seekBar.setProgress(percent);
                    }
                    seekHandler.postDelayed(this, 500);
                }
            }
        };
        seekHandler.post(runnable);
    }

    // Loads all previously recorded .3gp files from app storage
    private void loadRecordings() {
        File dir = getExternalFilesDir(null);
        if (dir != null) {
            File[] files = dir.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.getName().endsWith(".3gp")) {
                        recordings.add(new AudioRecording(file.getAbsolutePath(), file.getName()));
                    }
                }
            }
        }
        adapter.notifyDataSetChanged();
    }

    // Deletes the selected recording from list and file system
    private void deleteRecording(AudioRecording recording, int index) {
        File file = new File(recording.getFilePath());
        if (file.exists()) file.delete();

        recordings.remove(index);
        adapter.notifyItemRemoved(index);
        Toast.makeText(this, "Recording deleted", Toast.LENGTH_SHORT).show();
    }

    // Shares the selected audio file using external apps
    private void shareRecording(AudioRecording recording) {
        File file = new File(recording.getFilePath());
        if (!file.exists()) return;

        Uri uri = FileProvider.getUriForFile(
                this,
                getPackageName() + ".provider",
                file
        );

        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("audio/*");
        shareIntent.putExtra(Intent.EXTRA_STREAM, uri);
        shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

        startActivity(Intent.createChooser(shareIntent, "Share recording via"));
    }

    // Checks if microphone permission is granted
    private boolean checkPermissions() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED;
    }

    // Requests microphone permission
    private void requestPermissions() {
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO}, REQUEST_AUDIO_PERMISSION_CODE);
    }

    // Handles result from permission request dialog
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_AUDIO_PERMISSION_CODE) {
            statusText.setText(grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED ? "Permission granted" : "Permission denied");
        }
    }

    // Constant for permission request code
    public static final int REQUEST_AUDIO_PERMISSION_CODE = 200;
}