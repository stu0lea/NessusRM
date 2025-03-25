package cn.viewcn.nessusrm.gui;

import cn.viewcn.nessusrm.api.TxTransApi;
import com.alibaba.fastjson.JSONObject;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Properties;

import javafx.scene.control.ListView;
import javafx.stage.Modality;
import javafx.stage.Stage;

public class MainController {


    @FXML
    public VBox mainVbox;

    @FXML
    public MenuItem txTranKeyMenu;

    @FXML
    private TextField systemName, createPerson, checkPerson, permitPerson;

    @FXML
    private DatePicker createDate, checkDate, permitDate, startDate, endDate;

    @FXML
    private TextField unitName, unitAddress, customContacts, projectName, customEmail, customPhone, ourPerson, ourEmail, ourPhone, ourTestPerson;

    @FXML
    private ListView<File> fileListView;

    private ObservableList<File> fileList;

    @FXML
    private void initialize() {
        LocalDate now_date = LocalDate.now();
        createDate.setValue(now_date); // 初始化日期
        checkDate.setValue(now_date);
        permitDate.setValue(now_date);
        startDate.setValue(now_date);
        endDate.setValue(now_date);
        fileList = fileListView.getItems();
        fileListView.setCellFactory(param -> new ListCell<File>() {
            @Override
            protected void updateItem(File file, boolean empty) {
                super.updateItem(file, empty);
                if (empty || file == null) {
                    setText(null);
                } else {
                    setText(file.getName());
                }
            }
        });
    }

    @FXML
    @Deprecated
    private void handleAddFile(ActionEvent event) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Add File");
        // 创建文件过滤器，只接受.csv文件
        FileChooser.ExtensionFilter extFilter = new FileChooser.ExtensionFilter("csv文件(*.csv)", "*.csv");
        fileChooser.getExtensionFilters().add(extFilter);

        List<File> selectedFiles = fileChooser.showOpenMultipleDialog(((Node) event.getSource()).getScene().getWindow());
        if (selectedFiles != null) { // 检查返回值是否为 null
            for (File file : selectedFiles) {
                if (!fileList.contains(file)) {
                    fileList.add(file);
                }
            }
        }
    }

    @FXML
    @Deprecated
    private void handleRemoveFile(ActionEvent event) {
        fileList.removeAll(fileListView.getSelectionModel().getSelectedItems());
    }

    @FXML
    @Deprecated
    private void handleSubmitAction(ActionEvent event) {
//         TODO: Submit files
//        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
//        String dateString = checkDate.getValue().format(formatter);
//        System.out.println(dateString); // 输出格式为 yyyy-MM-dd 的日期字符
        TxTransApi t = new TxTransApi();
        try {
            JSONObject res = t.translate("test");
            System.out.println(res.toJSONString());
        } catch (Exception e) {
            System.err.println("Error occurred while calling translation API: " + e.getMessage());
        }
        fileList.clear();
    }

    @FXML
    @Deprecated
    private void handleClearButtonAction(ActionEvent event) {
        systemName.clear();
        createPerson.clear();
        checkPerson.clear();
        permitPerson.clear();
        fileList.clear();
    }

    @FXML
    public void setTxTranKey(ActionEvent event) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/views/SetView.fxml"));
        Stage stage = new Stage();
        Scene scene = new Scene(fxmlLoader.load(), 400, 150);
        // 获取当前Stage
        Scene main_scene = mainVbox.getScene();
        Stage main_stage = (Stage) main_scene.getWindow();
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.initOwner(main_stage); //避免最大化窗口影响子窗口
        stage.setTitle("API设置");
        stage.setScene(scene);
        stage.show();
    }

}