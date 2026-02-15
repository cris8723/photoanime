Build & packaging (summary)

Prerequisites:
- Java 17 JDK installed
- Maven 3.8+
- On Windows: build .exe on Windows machine
- On macOS: build .dmg/.pkg on macOS machine
- jpackage must run on target OS (Apple / Windows)

Development:
- mvn clean package
- mvn javafx:run

Create runtime image (manual jlink example):
- Download JavaFX jmods for JavaFX 17 and point to them.

Windows (manual jlink + jpackage example):
```
"%JAVA_HOME%\bin\jlink" --module-path "%JAVA_HOME%\jmods;C:\path\to\javafx-jmods-17" --add-modules com.animephotostudio,javafx.controls,javafx.fxml,javafx.swing --output runtime-image --compress=2 --strip-debug --no-header-files --no-man-pages

"%JAVA_HOME%\bin\jpackage" --type exe --name "AnimePhoto Studio" --app-version 1.0.0 --vendor "Your Company" --icon src/main/resources/assets/icon.ico --input target --main-jar anime-photo-studio-1.0.0.jar --runtime-image runtime-image --win-shortcut --win-menu --dest installer-windows
```

macOS (manual jlink + jpackage example):
```
/Library/Java/JavaVirtualMachines/jdk-17.jdk/Contents/Home/bin/jlink --module-path /Library/Java/JavaVirtualMachines/jdk-17.jdk/Contents/Home/jmods:/path/to/javafx-jmods-17 --add-modules com.animephotostudio,javafx.controls,javafx.fxml,javafx.swing --output runtime-image-mac --compress=2 --strip-debug --no-header-files --no-man-pages

/Library/Java/JavaVirtualMachines/jdk-17.jdk/Contents/Home/bin/jpackage --type dmg --name "AnimePhoto Studio" --app-version 1.0.0 --vendor "Your Company" --icon src/main/resources/assets/icon.icns --input target --main-jar anime-photo-studio-1.0.0.jar --runtime-image runtime-image-mac --dest installer-mac
```

Badass-jlink (Maven helper):
- mvn clean package
- mvn org.beryx:badass-jlink-plugin:2.25.0:jlink
- mvn org.beryx:badass-jlink-plugin:2.25.0:jpackage

Important: build .exe on Windows; build .dmg/.pkg on macOS. jpackage must run on the target OS.
