module com.animephotostudio {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.swing;
    requires java.desktop;

    opens com.animephotostudio.ui to javafx.fxml;
    exports com.animephotostudio;
    exports com.animephotostudio.ui;
    exports com.animephotostudio.core;
    exports com.animephotostudio.licensing;
    exports com.animephotostudio.utils;
    exports com.animephotostudio.config;
}