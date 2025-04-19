package cn.viewcn.nessusrm.gui;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;

public class SetController {
    @FXML
    private Button txApiButton;

    @FXML
    private TextField txApiId;

    @FXML
    private TextField txApiKey;


    @FXML
    private void initialize() {
        configReader();
    }

    @FXML
    private void configReader() {
        Properties properties = new Properties();
        try {
            FileInputStream fileInputStream = new FileInputStream("config.properties");
            properties.load(fileInputStream);
            fileInputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        String id = properties.getProperty("txApiId");
        String key = properties.getProperty("txApiKey");
        txApiId.setText(id);
        txApiKey.setText(key);
    }

    @FXML
    void txApiButton(ActionEvent event) {
        Properties props = new Properties();
        File configFile = new File("config.properties");

        // 如果配置文件已存在，先加载现有配置
        if (configFile.exists()) {
            try (FileInputStream in = new FileInputStream(configFile)) {
                props.load(in);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        // 更新/添加需要修改的配置项
        props.setProperty("txApiId", txApiId.getText());
        props.setProperty("txApiKey", txApiKey.getText());

        // 保存配置（保留注释和原有配置）
        try (FileOutputStream out = new FileOutputStream(configFile)) {
            props.store(out, null);
        } catch (IOException e) {
            e.printStackTrace();
        }

        Stage stage = (Stage) txApiButton.getScene().getWindow();
        stage.close();
    }

}
