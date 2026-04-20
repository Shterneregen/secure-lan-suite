package com.shterneregen.securelan.audio.service;

public record AudioCallProfile(int sampleRateHz, int channels, boolean echoCancellation, boolean noiseSuppression) {
}
