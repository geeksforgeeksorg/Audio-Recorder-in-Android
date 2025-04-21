package org.geeksforgeeks.demo;

// Model class representing an audio recording
public class AudioRecording {

    // path where the file is located
    private final String filePath;

    // name of the audio file
    private final String fileName;

    // to check whether the audio is playing or not
    private boolean isPlaying;

    public AudioRecording(String filePath, String fileName) {
        this.filePath = filePath;
        this.fileName = fileName;
        this.isPlaying = false;
    }

    public String getFilePath() {
        return filePath;
    }

    public String getFileName() {
        return fileName;
    }

    public boolean isPlaying() {
        return isPlaying;
    }

    public void setPlaying(boolean playing) {
        isPlaying = playing;
    }
}