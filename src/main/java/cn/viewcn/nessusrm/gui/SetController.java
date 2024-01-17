package cn.viewcn.nessusrm.gui;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;

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
            FileInputStream fileInputStream = new FileInputStream("config.ini");
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

    // 创建一个Properties对象
        Properties props = new Properties();
        // 设置txApiId和txApiKey的值
        props.setProperty("txApiId", txApiId.getText());
        props.setProperty("txApiKey", txApiKey.getText());
        // 将Properties对象写入config.ini文件
        try {
            props.store(new FileOutputStream("config.ini"), "TX API Config");
        } catch (IOException e) {
            e.printStackTrace();
        }
        Stage stage = (Stage) txApiButton.getScene().getWindow();
        stage.close();
    }

}
