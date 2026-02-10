package com.softclass.fingerprint;

import javax.sound.sampled.*;
import java.io.BufferedInputStream;
import java.io.InputStream;

public class SoundUtil {

    private static void play(String resourcePath) {
        try (InputStream audioSrc = SoundUtil.class.getResourceAsStream(resourcePath);
             InputStream bufferedIn = new BufferedInputStream(audioSrc)) {
            AudioInputStream audioStream = AudioSystem.getAudioInputStream(bufferedIn);
            Clip clip = AudioSystem.getClip();
            clip.open(audioStream);
            clip.start();
        } catch (Exception e) {
            System.err.println("⚠ No se pudo reproducir sonido: " + e.getMessage());
        }
    }

    /** Sonido para registro exitoso (entrada/salida) */
    public static void playSuccess() {
        play("/sounds/success.wav");
    }

    /** Sonido para error (huella no reconocida o fallida) */
    public static void playError() {
        play("/sounds/error.wav");
    }

    /** Sonido simple (beep del sistema, multiplataforma) */
    public static void beep() {
        java.awt.Toolkit.getDefaultToolkit().beep();
    }
}
