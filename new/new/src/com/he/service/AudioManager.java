package com.he.service;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import java.io.File;

public class AudioManager {
    private static AudioManager instance;
    private Clip bgmClip;

    private AudioManager() {}

    public static synchronized AudioManager getInstance() {
        if (instance == null) {
            instance = new AudioManager();
        }
        return instance;
    }

    // 播放音效（即放即毁）
    public void playSound(String filePath) {
        new Thread(() -> {
            try {
                AudioInputStream ais = AudioSystem.getAudioInputStream(new File(filePath));
                Clip clip = AudioSystem.getClip();
                clip.open(ais);
                clip.start();
                clip.addLineListener(event -> {
                    if (event.getType() == javax.sound.sampled.LineEvent.Type.STOP) {
                        clip.close();
                    }
                });
            } catch (Exception e) {
                System.err.println("音效播放失败: " + filePath);
            }
        }).start();
    }

    // 播放或循环 BGM
    public void playBGM(String filePath, boolean loop) {
        try {
            stopBGM();
            AudioInputStream ais = AudioSystem.getAudioInputStream(new File(filePath));
            bgmClip = AudioSystem.getClip();
            bgmClip.open(ais);
            if (loop) {
                bgmClip.loop(Clip.LOOP_CONTINUOUSLY);
            }
            bgmClip.start();
        } catch (Exception e) {
            System.err.println("BGM播放失败: " + filePath);
        }
    }

    public void stopBGM() {
        if (bgmClip != null) {
            bgmClip.stop();
            bgmClip.close();
            bgmClip = null;
        }
    }
}