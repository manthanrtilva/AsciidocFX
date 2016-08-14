package com.kodcu.config;

import com.kodcu.controller.ApplicationController;
import com.kodcu.other.IOHelper;
import com.kodcu.service.ThreadService;
import javafx.animation.FadeTransition;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.event.ActionEvent;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonStructure;
import javax.json.JsonWriter;
import javax.json.stream.JsonGenerator;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by usta on 19.07.2015.
 */
public abstract class ConfigurationBase {

    private final ApplicationController controller;
    private final ThreadService threadService;

    public abstract String formName();

    @Value("${application.config.folder}")
    private String userHomeConfigFolder;

    public static ObjectProperty<Path> configRootLocation = new SimpleObjectProperty<>();

    public Path getConfigRootLocation() {

        String userHome = System.getProperty("user.home");

        Path userHomeConfigPath = Paths.get(userHome).resolve(userHomeConfigFolder);

        IOHelper.createDirectories(userHomeConfigPath);

        setConfigRootLocation(userHomeConfigPath);

        return userHomeConfigPath;
    }

    public ObjectProperty<Path> configRootLocationProperty() {
        return configRootLocation;
    }

    public void setConfigRootLocation(Path configRootLocation) {
        this.configRootLocation.set(configRootLocation);
    }

    private Logger logger = LoggerFactory.getLogger(ConfigurationBase.class);

    public ConfigurationBase(ApplicationController controller, ThreadService threadService) {
        this.controller = controller;
        this.threadService = threadService;
    }

    public abstract VBox createForm();

    public Path resolveConfigPath(String fileName) {

        Path configRootLocation = getConfigRootLocation();

        Path configPath = null;

        configPath = configRootLocation.resolve(fileName);

        if (Files.notExists(configPath)) {
            Path defaultConfigPath = getConfigDirectory().resolve(fileName);
            IOHelper.copy(defaultConfigPath, configPath);
        }

        return configPath;
    }

    public Path getConfigDirectory() {
        Path configPath = controller.getConfigPath();
        return configPath;
    }


    protected void fadeOut(Label label, String text) {
        threadService.runActionLater(() -> {
            label.setText(text);
            FadeTransition fadeTransition = new FadeTransition(Duration.millis(2000), label);
            fadeTransition.setFromValue(1);
            fadeTransition.setToValue(0);
            fadeTransition.playFromStart();
        });
    }

    protected void saveJson(JsonStructure jsonStructure) {
        Map<String, Object> properties = new HashMap<>(1);
        properties.put(JsonGenerator.PRETTY_PRINTING, true);

        try (FileOutputStream fos = new FileOutputStream(getConfigPath().toFile());
             OutputStreamWriter fileWriter = new OutputStreamWriter(fos, "UTF-8");
             JsonWriter jsonWriter = Json.createWriterFactory(properties).createWriter(fileWriter);) {
            jsonWriter.write(jsonStructure);
        } catch (Exception e) {
            logger.error("Problem occured while saving {}", this.getClass().getSimpleName(), e);
        }
    }

    public abstract Path getConfigPath();

    public abstract void load(ActionEvent... actionEvent);

    public abstract void save(ActionEvent... actionEvent);

    public abstract JsonObject getJSON();
}
