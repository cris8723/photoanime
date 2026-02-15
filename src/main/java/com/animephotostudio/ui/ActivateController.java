package com.animephotostudio.ui;

import com.animephotostudio.licensing.LicenseManager;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

public class ActivateController {
    @FXML private TextField keyField;
    @FXML private Button activateBtn;
    @FXML private Button cancelBtn;
    @FXML private Label statusLabel;

    private final LicenseManager licenseManager = LicenseManager.getInstance();

    @FXML
    private void onActivateKey() {
        String key = keyField.getText();
        if (key == null || key.trim().isEmpty()) {
            statusLabel.setText("Introduce un código válido.");
            return;
        }
        activateBtn.setDisable(true);
        boolean ok = licenseManager.activate(key.trim());
        activateBtn.setDisable(false);
        if (ok) {
            statusLabel.setStyle("-fx-text-fill: green;");
            statusLabel.setText("Activación correcta — PRO");
            // close dialog
            Stage s = (Stage) activateBtn.getScene().getWindow();
            s.close();
        } else {
            statusLabel.setStyle("-fx-text-fill: red;");
            statusLabel.setText("Código inválido o error.");
        }
    }

    @FXML
    private void onCancel() {
        Stage s = (Stage) cancelBtn.getScene().getWindow();
        s.close();
    }
}