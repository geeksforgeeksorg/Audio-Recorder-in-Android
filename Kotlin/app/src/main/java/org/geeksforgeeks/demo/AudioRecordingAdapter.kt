package org.geeksforgeeks.demo

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.SeekBar
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

// Adapter to display and manage a list of audio recordings
class AudioRecordingAdapter(
    private val recordings: MutableList<AudioRecording>,
    private val onPlayClick: (AudioRecording, Int) -> Unit,
    private val onDeleteClick: (AudioRecording, Int) -> Unit,
    private val onShareClick: (AudioRecording) -> Unit
) : RecyclerView.Adapter<AudioRecordingAdapter.AudioViewHolder>() {

    // ViewHolder class holds the views for each list item
    inner class AudioViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val playButton: ImageView = itemView.findViewById(R.id.playPauseButton)
        val fileNameText: TextView = itemView.findViewById(R.id.fileName)
        val seekBar: SeekBar = itemView.findViewById(R.id.seekBar)
        val deleteButton: ImageView = itemView.findViewById(R.id.delete)
        val shareButton: ImageView = itemView.findViewById(R.id.share)
    }

    // Inflates the layout for each item in the list
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AudioViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_audio_recording, parent, false)
        return AudioViewHolder(view)
    }

    // Binds data to each item and sets up click listeners
    override fun onBindViewHolder(holder: AudioViewHolder, position: Int) {
        val recording = recordings[position]

        holder.fileNameText.text = recording.fileName

        // Update play button based on playback state
        holder.playButton.setImageResource(
            if (recording.isPlaying) R.drawable.pause_square else R.drawable.play_square
        )

        // Play button click triggers callback
        holder.playButton.setOnClickListener {
            onPlayClick(recording, holder.adapterPosition)
        }

        // Delete button click triggers callback
        holder.deleteButton.setOnClickListener {
            onDeleteClick(recording, holder.adapterPosition)
        }

        // Share button click triggers callback
        holder.shareButton.setOnClickListener {
            onShareClick(recording)
        }

        // Reset seekBar to the beginning for now
        holder.seekBar.progress = 0
    }

    // Returns the size of the dataset
    override fun getItemCount(): Int = recordings.size
}