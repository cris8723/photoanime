package com.animephotostudio.ui;

import com.animephotostudio.licensing.LicenseManager;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.stage.Stage;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public class LicenseInfoController {
    @FXML private Label typeLabel;
    @FXML private Label issuedLabel;
    @FXML private Label pathLabel;
    @FXML private Label statusLabel;
    @FXML private Button deactivateBtn;
    @FXML private Button closeBtn;

    private final LicenseManager licenseManager = LicenseManager.getInstance();

    @FXML
    public void initialize() {
        refresh();
    }

    private void refresh() {
        typeLabel.setText(licenseManager.getLicenseType());
        licenseManager.getIssuedTimestamp().ifPresentOrElse(ts -> {
            String formatted = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
                    .withZone(ZoneId.systemDefault())
                    .format(Instant.ofEpochMilli(ts));
            issuedLabel.setText(formatted);
        }, () -> issuedLabel.setText("N/A"));
        pathLabel.setText(licenseManager.getLicensePath().toString());
        deactivateBtn.setDisable(!licenseManager.hasLicenseFile() || !licenseManager.isPro());
        statusLabel.setText("");
    }

    @FXML
    private void onDeactivate() {
        licenseManager.clearLicense();
        statusLabel.setStyle("-fx-text-fill: green;");
        statusLabel.setText("Licencia eliminada. Reinicia la app si es necesario.");
        refresh();
    }

    @FXML
    private void onClose() {
        Stage s = (Stage) closeBtn.getScene().getWindow();
        s.close();
    }
}