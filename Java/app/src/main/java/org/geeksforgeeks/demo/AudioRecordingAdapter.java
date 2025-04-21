package org.geeksforgeeks.demo;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

// Adapter to display and manage a list of audio recordings
public class AudioRecordingAdapter extends RecyclerView.Adapter<AudioRecordingAdapter.AudioViewHolder> {

    private final List<AudioRecording> recordings;
    private final OnPlayClickListener onPlayClick;
    private final OnDeleteClickListener onDeleteClick;
    private final OnShareClickListener onShareClick;

    public AudioRecordingAdapter(List<AudioRecording> recordings,
                                 OnPlayClickListener onPlayClick,
                                 OnDeleteClickListener onDeleteClick,
                                 OnShareClickListener onShareClick) {
        this.recordings = recordings;
        this.onPlayClick = onPlayClick;
        this.onDeleteClick = onDeleteClick;
        this.onShareClick = onShareClick;
    }

    // ViewHolder class holds the views for each list item
    public static class AudioViewHolder extends RecyclerView.ViewHolder {
        ImageView playButton;
        TextView fileNameText;
        SeekBar seekBar;
        ImageView deleteButton;
        ImageView shareButton;

        public AudioViewHolder(@NonNull View itemView) {
            super(itemView);
            playButton = itemView.findViewById(R.id.playPauseButton);
            fileNameText = itemView.findViewById(R.id.fileName);
            seekBar = itemView.findViewById(R.id.seekBar);
            deleteButton = itemView.findViewById(R.id.delete);
            shareButton = itemView.findViewById(R.id.share);
        }
    }

    // Inflates the layout for each item in the list
    @NonNull
    @Override
    public AudioViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_audio_recording, parent, false);
        return new AudioViewHolder(view);
    }

    // Binds data to each item and sets up click listeners
    @Override
    public void onBindViewHolder(@NonNull AudioViewHolder holder, int position) {
        AudioRecording recording = recordings.get(position);

        holder.fileNameText.setText(recording.getFileName());

        // Update play button based on playback state
        holder.playButton.setImageResource(
                recording.isPlaying() ? R.drawable.pause_square : R.drawable.play_square
        );

        // Play button click triggers callback
        holder.playButton.setOnClickListener(v -> onPlayClick.onPlayClick(recording, holder.getAdapterPosition()));

        // Delete button click triggers callback
        holder.deleteButton.setOnClickListener(v -> onDeleteClick.onDeleteClick(recording, holder.getAdapterPosition()));

        // Share button click triggers callback
        holder.shareButton.setOnClickListener(v -> onShareClick.onShareClick(recording));

        // Reset seekBar to the beginning for now
        holder.seekBar.setProgress(0);
    }

    // Returns the size of the dataset
    @Override
    public int getItemCount() {
        return recordings.size();
    }

    // Interfaces for click callbacks
    public interface OnPlayClickListener {
        void onPlayClick(AudioRecording recording, int position);
    }

    public interface OnDeleteClickListener {
        void onDeleteClick(AudioRecording recording, int position);
    }

    public interface OnShareClickListener {
        void onShareClick(AudioRecording recording);
    }
}