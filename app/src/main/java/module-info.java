module com.animephotostudio.app {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.swing;
    requires java.desktop;
    requires com.animephotostudio.core;

    opens com.animephotostudio.ui to javafx.fxml;
    exports com.animephotostudio;
    exports com.animephotostudio.ui;
}