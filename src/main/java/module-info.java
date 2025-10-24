module com.softclass.fingerprint {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.sql;
    requires FDxSDKPro;
    requires org.apache.pdfbox;
    requires org.apache.poi.poi;
    requires org.apache.poi.ooxml;
    requires itextpdf;

    opens com.softclass.fingerprint to javafx.fxml;
    exports com.softclass.fingerprint;
}