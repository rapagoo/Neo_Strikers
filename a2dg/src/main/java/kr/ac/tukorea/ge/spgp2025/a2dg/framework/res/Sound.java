package kr.ac.tukorea.ge.spgp2025.a2dg.framework.res;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.media.SoundPool;

import java.util.HashMap;

public class Sound {
    private static SoundPool soundPool;
    private static final int MAX_STREAMS = 10;
    private static HashMap<Integer, Integer> soundIdMap = new HashMap<>();

    private static MediaPlayer mediaPlayer;

    public static void init(Context context) {
        AudioAttributes audioAttributes = new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build();
        soundPool = new SoundPool.Builder()
                .setAudioAttributes(audioAttributes)
                .setMaxStreams(MAX_STREAMS)
                .build();
    }

    public static void load(Context context, int resId) {
        int soundId = soundPool.load(context, resId, 1);
        soundIdMap.put(resId, soundId);
    }

    public static void play(int resId, float volume) {
        Integer soundId = soundIdMap.get(resId);
        if (soundId != null) {
            soundPool.play(soundId, volume, volume, 0, 0, 1.0f);
        }
    }
    
    public static void play(int resId) {
        play(resId, 1.0f);
    }

    public static void playMusic(Context context, int resId) {
        if (mediaPlayer != null) {
            mediaPlayer.stop();
        }
        mediaPlayer = MediaPlayer.create(context, resId);
        mediaPlayer.setLooping(true);
        mediaPlayer.start();
    }

    public static void stopMusic() {
        if (mediaPlayer != null) {
            mediaPlayer.stop();
            mediaPlayer.release();
            mediaPlayer = null;
        }
    }

    public static void pauseMusic() {
        if (mediaPlayer != null && mediaPlayer.isPlaying()) {
            mediaPlayer.pause();
        }
    }

    public static void resumeMusic() {
        if (mediaPlayer != null && !mediaPlayer.isPlaying()) {
            mediaPlayer.start();
        }
    }
} 