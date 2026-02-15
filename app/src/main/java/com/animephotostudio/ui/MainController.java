package com.animephotostudio.ui;

import com.animephotostudio.core.ImageProcessor;
import com.animephotostudio.licensing.LicenseManager;
import com.animephotostudio.utils.ImageUtils;
import javafx.embed.swing.SwingFXUtils;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.concurrent.Task;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;

public class MainController {
    @FXML private ImageView originalView;
    @FXML private ImageView processedView;
    @FXML private Button loadBtn;
    @FXML private Button applyBtn;
    @FXML private Button exportBtn;
    @FXML private Label licenseLabel;
    @FXML private Label statusLabel;

    private BufferedImage originalImage;
    private BufferedImage processedImage;
    private final ImageProcessor processor = new ImageProcessor();
    private final LicenseManager licenseManager = LicenseManager.getInstance();

    @FXML
    public void initialize() {
        statusLabel.setText("Listo");
        applyBtn.setDisable(true);
        exportBtn.setDisable(true);
        updateLicenseLabel();
    }

    private void updateControls() {
        applyBtn.setDisable(originalImage == null);
        exportBtn.setDisable(processedImage == null);
        licenseLabel.setText(licenseManager.isPro() ? "PRO" : "FREE");
    }

    @FXML
    private void onLoad() {
        FileChooser chooser = new FileChooser();
        chooser.getExtensionFilters().addAll(new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg", "*.bmp"));
        File f = chooser.showOpenDialog(originalView.getScene().getWindow());
        if (f == null) return;
        try {
            originalImage = ImageIO.read(f);
            originalView.setImage(SwingFXUtils.toFXImage(originalImage, null));
            processedView.setImage(null);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void onApply() {
        if (originalImage == null) return;
        // run processing off the FX thread
        applyBtn.setDisable(true);
        loadBtn.setDisable(true);
        statusLabel.setText("Procesando...");

        Task<BufferedImage> task = new Task<>() {
            @Override
            protected BufferedImage call() throws Exception {
                return processor.applyAnimeEffect(originalImage);
            }
        };

        task.setOnSucceeded(evt -> {
            processedImage = task.getValue();
            // show preview (watermarked for FREE) without mutating processedImage
            BufferedImage preview = ImageUtils.copy(processedImage);
            if (!licenseManager.isPro()) {
                ImageUtils.applyWatermark(preview, "AnimePhoto Studio • FREE (preview)");
            }
            processedView.setImage(SwingFXUtils.toFXImage(preview, null));
            statusLabel.setText("Listo");
            loadBtn.setDisable(false);
            updateControls();
        });

        task.setOnFailed(evt -> {
            statusLabel.setText("Error al procesar");
            loadBtn.setDisable(false);
            updateControls();
        });

        Thread t = new Thread(task, "imgproc");
        t.setDaemon(true);
        t.start();
    }

    @FXML
    private void onExport() {
        if (processedImage == null) return;
        FileChooser chooser = new FileChooser();
        chooser.getExtensionFilters().addAll(new FileChooser.ExtensionFilter("PNG", "*.png"), new FileChooser.ExtensionFilter("JPEG", "*.jpg"));
        File f = chooser.showSaveDialog(originalView.getScene().getWindow());
        if (f == null) return;
        try {
            // copy so watermark doesn't mutate the in-memory processedImage
            BufferedImage toSave = com.animephotostudio.utils.ImageUtils.copy(processedImage);
            if (!licenseManager.isPro()) {
                ImageUtils.applyWatermark(toSave, "AnimePhoto Studio • FREE");
            }
            ImageIO.write(toSave, getExtension(f.getName()), f);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void onActivate() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/activation.fxml"));
            Parent root = loader.load();
            Stage dialog = new Stage();
            dialog.initOwner(originalView.getScene().getWindow());
            dialog.initModality(Modality.APPLICATION_MODAL);
            dialog.setTitle("Activar licencia");
            dialog.setScene(new Scene(root));
            dialog.showAndWait();
            // refresh license label after the dialog closes
            updateLicenseLabel();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void onManageLicense() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/license_info.fxml"));
            Parent root = loader.load();
            Stage dialog = new Stage();
            dialog.initOwner(originalView.getScene().getWindow());
            dialog.initModality(Modality.APPLICATION_MODAL);
            dialog.setTitle("Información de licencia");
            dialog.setScene(new Scene(root));
            dialog.showAndWait();
            // refresh main UI license indicator after dialog
            updateLicenseLabel();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String getExtension(String name) {
        String n = name.toLowerCase();
        if (n.endsWith(".png")) return "png";
        return "jpg";
    }

    private void updateLicenseLabel() {
        licenseLabel.setText(licenseManager.isPro() ? "PRO" : "FREE");
        updateControls();
        if (processedImage != null) {
            BufferedImage preview = ImageUtils.copy(processedImage);
            if (!licenseManager.isPro()) {
                ImageUtils.applyWatermark(preview, "AnimePhoto Studio • FREE (preview)");
            }
            processedView.setImage(SwingFXUtils.toFXImage(preview, null));
        }
    }
}