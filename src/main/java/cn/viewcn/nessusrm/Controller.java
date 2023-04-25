package cn.viewcn.nessusrm;

import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.stage.FileChooser;

import java.io.File;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.List;

import javafx.scene.control.ListView;

public class Controller {
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
    private void handleAddFile(ActionEvent event) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Add File");
        // 创建文件过滤器，只接受 .txt 和 .pdf 文件
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
    private void handleRemoveFile(ActionEvent event) {
        fileList.removeAll(fileListView.getSelectionModel().getSelectedItems());
    }

    @FXML
    private void handleSubmitAction(ActionEvent event) {
        // TODO: Submit files
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        String dateString = checkDate.getValue().format(formatter);
        System.out.println(dateString); // 输出格式为 yyyy-MM-dd 的日期字符
        fileList.clear();
    }

    @FXML
    private void handleClearButtonAction(ActionEvent event) {
        systemName.clear();
        createPerson.clear();
        checkPerson.clear();
        permitPerson.clear();
        fileList.clear();
    }

}