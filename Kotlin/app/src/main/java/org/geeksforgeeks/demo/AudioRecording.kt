package org.geeksforgeeks.demo

data class AudioRecording(
    // path where the file is located
    val filePath: String,
    // name of the audio file
    val fileName: String,
    // to check whether the audio is playing or not
    var isPlaying: Boolean = false
)