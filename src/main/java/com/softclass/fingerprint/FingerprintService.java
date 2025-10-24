package com.softclass.fingerprint;

import SecuGen.FDxSDKPro.jni.*;
import java.util.Base64;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Servicio del lector SecuGen.
 * Compatible con las llamadas existentes:
 *  - enrollFingerprint()  <-- usado por EmployeeView/AttendanceController para enrolamiento manual
 *  - captureTemplateBase64() <-- alias, mismo comportamiento
 *  - match(storedBase64, liveBase64)
 *
 * Además añade:
 *  - startContinuousMode(FingerprintListener)
 *  - stopContinuousMode()
 *
 * Nota: no cambia la lógica externa; solo agrega el modo continuo.
 */
public class FingerprintService {

    private final JSGFPLib sgfplib;
    private final SGDeviceInfoParam deviceInfo = new SGDeviceInfoParam();

    // Modo continuo
    private final AtomicBoolean continuousMode = new AtomicBoolean(false);
    private Thread listenerThread;

    public FingerprintService() throws Exception {
        sgfplib = new JSGFPLib();

        long err = sgfplib.Init(SGFDxDeviceName.SG_DEV_AUTO);
        if (err != SGFDxErrorCode.SGFDX_ERROR_NONE)
            throw new RuntimeException("Error inicializando SDK: " + err);

        err = sgfplib.OpenDevice(SGPPPortAddr.AUTO_DETECT);
        if (err != SGFDxErrorCode.SGFDX_ERROR_NONE)
            throw new RuntimeException("Error abriendo dispositivo: " + err);

        // ajustar formato recomendado por el SDK
        sgfplib.SetTemplateFormat(SGFDxTemplateFormat.TEMPLATE_FORMAT_SG400);
        sgfplib.GetDeviceInfo(deviceInfo);

        System.out.println("✅ Lector inicializado correctamente");
        System.out.println("  Serie: " + deviceInfo.deviceSN());
        System.out.println("  Resolución: " + deviceInfo.imageWidth + "x" + deviceInfo.imageHeight);
    }

    // ---------------------------------------------------------------------
    //  ENROLL / CAPTURE (compatibilidad)
    // ---------------------------------------------------------------------

    /**
     * Método original que esperaba tu código: enrola (captura + template) y devuelve Base64.
     * Mantener este nombre para no romper EmployeeView / AttendanceController.
     */
    public String enrollFingerprint() throws Exception {
        // delegamos a captureTemplateBase64 para evitar duplicar lógica
        return captureTemplateBase64();
    }

    /**
     * Captura la imagen, comprueba calidad, crea template y devuelve Base64.
     * Este método también está disponible por si otras partes lo llaman.
     */
    public String captureTemplateBase64() throws Exception {
        int imgSize = deviceInfo.imageWidth * deviceInfo.imageHeight;
        byte[] imageBuffer = new byte[imgSize];
        byte[] template = new byte[400];

        // Intentos para capturar (si quieres más tolerancia, aumenta reintentos)
        int attempts = 5;
        long lastErr = -1;
        boolean captured = false;
        for (int i = 0; i < attempts; i++) {
            long err = sgfplib.GetImage(imageBuffer);
            if (err == SGFDxErrorCode.SGFDX_ERROR_NONE) {
                captured = true;
                break;
            }
            lastErr = err;
            try { Thread.sleep(300); } catch (InterruptedException ignored) {}
        }
        if (!captured) {
            throw new RuntimeException("Error capturando imagen: " + lastErr);
        }

        int[] quality = new int[1];
        sgfplib.GetImageQuality(deviceInfo.imageWidth, deviceInfo.imageHeight, imageBuffer, quality);
        System.out.println("📊 Calidad de imagen: " + quality[0]);
        if (quality[0] < 30) { // umbral conservador, ajustar según pruebas
            throw new RuntimeException("Huella de baja calidad (" + quality[0] + ")");
        }

        SGFingerInfo fingerInfo = new SGFingerInfo();
        fingerInfo.FingerNumber = 1;
        fingerInfo.ImageQuality = quality[0];
        fingerInfo.ImpressionType = SGImpressionType.SG_IMPTYPE_LP;
        fingerInfo.ViewNumber = 1;

        long err = sgfplib.CreateTemplate(fingerInfo, imageBuffer, template);
        if (err != SGFDxErrorCode.SGFDX_ERROR_NONE)
            throw new RuntimeException("Error creando template: " + err);

        return Base64.getEncoder().encodeToString(template);
    }

    // ---------------------------------------------------------------------
    //  MATCH
    // ---------------------------------------------------------------------

    /**
     * Compara dos templates (en Base64) y devuelve true si coinciden.
     * Firma compatible con el código actual.
     */
    public boolean match(String storedTemplateBase64, String liveTemplateBase64) throws Exception {
        byte[] stored = Base64.getDecoder().decode(storedTemplateBase64);
        byte[] live = Base64.getDecoder().decode(liveTemplateBase64);

        boolean[] matched = new boolean[1];
        long err = sgfplib.MatchTemplate(stored, live, SGFDxSecurityLevel.SL_NORMAL, matched);
        if (err != SGFDxErrorCode.SGFDX_ERROR_NONE) {
            throw new RuntimeException("Error en comparación: " + err);
        }
        return matched[0];
    }

    // ---------------------------------------------------------------------
    //  MODO CONTINUO (no rompe la funcionalidad manual)
    // ---------------------------------------------------------------------

    /**
     * Inicia escucha continua en un hilo (callback en el thread consumidor).
     * El listener recibirá templates (Base64) cada vez que se detecte una huella.
     */
    public void startContinuousMode(FingerprintListener listener) {
        if (continuousMode.get()) return;
        continuousMode.set(true);

        listenerThread = new Thread(() -> {
            int imgSize = deviceInfo.imageWidth * deviceInfo.imageHeight;
            byte[] imageBuffer = new byte[imgSize];
            byte[] template = new byte[400];
            SGFingerInfo fingerInfo = new SGFingerInfo();

            while (continuousMode.get()) {
                try {
                    // usamos GetImage con intentos cortos para no bloquear indefinidamente
                    long err = sgfplib.GetImage(imageBuffer);
                    if (err == SGFDxErrorCode.SGFDX_ERROR_NONE) {
                        int[] quality = new int[1];
                        sgfplib.GetImageQuality(deviceInfo.imageWidth, deviceInfo.imageHeight, imageBuffer, quality);
                        if (quality[0] >= 20) { // si es razonable, crear template y notificar
                            fingerInfo.FingerNumber = 1;
                            fingerInfo.ImageQuality = quality[0];
                            fingerInfo.ImpressionType = SGImpressionType.SG_IMPTYPE_LP;
                            fingerInfo.ViewNumber = 1;

                            long cErr = sgfplib.CreateTemplate(fingerInfo, imageBuffer, template);
                            if (cErr == SGFDxErrorCode.SGFDX_ERROR_NONE) {
                                String base64 = Base64.getEncoder().encodeToString(template);
                                try {
                                    listener.onFingerprintDetected(base64);
                                } catch (Throwable t) {
                                    System.err.println("Error en callback listener: " + t.getMessage());
                                }
                                // evitar lecturas duplicadas inmediatas
                                try { Thread.sleep(1500); } catch (InterruptedException ie) { /* ignore */ }
                            }
                        }
                    } else {
                        // si no hay imagen, dormir corto para bajar consumo CPU
                        try { Thread.sleep(200); } catch (InterruptedException ie) { /* ignore */ }
                    }
                } catch (Throwable e) {
                    System.err.println("Error en escucha continua: " + e.getMessage());
                    try { Thread.sleep(500); } catch (InterruptedException ignored) {}
                }
            }
        }, "Fingerprint-Listener-Thread");

        listenerThread.setDaemon(true);
        listenerThread.start();
        System.out.println("🎧 Modo continuo iniciado");
    }

    /** Detiene la escucha continua */
    public void stopContinuousMode() {
        continuousMode.set(false);
        if (listenerThread != null) {
            listenerThread.interrupt();
            listenerThread = null;
        }
        System.out.println("🛑 Modo continuo detenido");
    }

    /** Interfaz para recibir detecciones en modo continuo */
    public interface FingerprintListener {
        void onFingerprintDetected(String templateBase64);
    }

    // ---------------------------------------------------------------------
    //  CERRAR
    // ---------------------------------------------------------------------
    public void close() {
        stopContinuousMode();
        try {
            sgfplib.CloseDevice();
        } catch (Exception e) {
            System.err.println("⚠️ Error al cerrar dispositivo: " + e.getMessage());
        }
    }
}
