package com.softclass.fingerprint;

import SecuGen.FDxSDKPro.jni.*;
import java.util.Base64;

public class FingerprintService {

    private final JSGFPLib sgfplib;
    private final SGDeviceInfoParam deviceInfo = new SGDeviceInfoParam();

    public FingerprintService() throws Exception {
        sgfplib = new JSGFPLib();

        // --- Inicializa SDK ---
        long err = sgfplib.Init(SGFDxDeviceName.SG_DEV_AUTO);
        if (err != SGFDxErrorCode.SGFDX_ERROR_NONE) {
            throw new RuntimeException("Error inicializando SDK: " + err);
        }

        // --- Abre el dispositivo automáticamente ---
        err = sgfplib.OpenDevice(SGPPPortAddr.AUTO_DETECT);
        if (err != SGFDxErrorCode.SGFDX_ERROR_NONE) {
            throw new RuntimeException("Error abriendo dispositivo: " + err);
        }

        // --- Configura formato y obtiene info ---
        sgfplib.SetTemplateFormat(SGFDxTemplateFormat.TEMPLATE_FORMAT_SG400);
        sgfplib.GetDeviceInfo(deviceInfo);

        System.out.println("✅ Lector inicializado correctamente");
        System.out.println("  Serie: " + deviceInfo.deviceSN());
        System.out.println("  Resolución: " + deviceInfo.imageWidth + "x" + deviceInfo.imageHeight);
    }

    /** Captura una huella y devuelve el template codificado en Base64 */
    public String enrollFingerprint() throws Exception {
        int imgSize = deviceInfo.imageWidth * deviceInfo.imageHeight;
        byte[] imageBuffer = new byte[imgSize];
        byte[] template = new byte[400];

        System.out.println("👉 Coloca el dedo en el lector...");

        // Captura de imagen
        long err = sgfplib.GetImage(imageBuffer);
        if (err != SGFDxErrorCode.SGFDX_ERROR_NONE)
            throw new RuntimeException("Error capturando imagen: " + err);

        // Calidad
        int[] quality = new int[1];
        sgfplib.GetImageQuality(deviceInfo.imageWidth, deviceInfo.imageHeight, imageBuffer, quality);
        System.out.println("📊 Calidad de imagen: " + quality[0]);
        if (quality[0] < 50) throw new RuntimeException("Huella de baja calidad");

        // Crear template
        SGFingerInfo fingerInfo = new SGFingerInfo();
        fingerInfo.FingerNumber = 1;
        fingerInfo.ImageQuality = quality[0];
        fingerInfo.ImpressionType = SGImpressionType.SG_IMPTYPE_LP;
        fingerInfo.ViewNumber = 1;

        err = sgfplib.CreateTemplate(fingerInfo, imageBuffer, template);
        if (err != SGFDxErrorCode.SGFDX_ERROR_NONE)
            throw new RuntimeException("Error creando template: " + err);

        return Base64.getEncoder().encodeToString(template);
    }

    /** Verifica dos huellas (Base64) */
    public boolean match(String storedTemplateBase64, String liveTemplateBase64) throws Exception {
        byte[] stored = Base64.getDecoder().decode(storedTemplateBase64);
        byte[] live = Base64.getDecoder().decode(liveTemplateBase64);

        boolean[] matched = new boolean[1];
        long err = sgfplib.MatchTemplate(stored, live, SGFDxSecurityLevel.SL_NORMAL, matched);
        if (err != SGFDxErrorCode.SGFDX_ERROR_NONE)
            throw new RuntimeException("Error en comparación: " + err);

        return matched[0];
    }

    public void close() {
        try {
            sgfplib.CloseDevice();
            System.out.println("🔒 Dispositivo cerrado correctamente");
        } catch (Exception e) {
            System.err.println("⚠️ Error al cerrar dispositivo: " + e.getMessage());
        }
    }
}
