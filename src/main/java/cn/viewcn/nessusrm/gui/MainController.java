package cn.viewcn.nessusrm.gui;

import java.io.File;
import cn.viewcn.nessusrm.core.MakeReport;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.application.Platform;
import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javafx.scene.control.ListView;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import javafx.concurrent.Task;
import java.util.stream.Collectors;
import tech.tablesaw.api.Table;


public class MainController {

    private static final Logger logger = LoggerFactory.getLogger(MainController.class);
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    @FXML
    public VBox mainVbox;

    @FXML
    public MenuItem txTranKeyMenu;

    @FXML
    private TextField systemName, createPerson, checkPerson, permitPerson;

    // 注入 CheckBox 容器
    @FXML
    private HBox riskSelect;

    @FXML
    private DatePicker createDate, checkDate, permitDate, startDate, endDate;

    @FXML
    private TextField unitName, unitAddress, customContacts, projectName, customEmail, customPhone, ourContact, ourEmail, ourPhone, ourTestPerson;

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
        // 验证输入
        if (systemName.getText().isEmpty()) {
            showAlert("错误", "系统名称必须填写", Alert.AlertType.ERROR);
            return;
        }
        if (fileList.isEmpty()) {
            showAlert("错误", "请至少添加一个CSV文件", Alert.AlertType.ERROR);
            return;
        }

        // 收集表单数据
        Map<String, Object> formData = collectFormData();
        System.out.println("[+]获取表单数据：" + formData);

        try {
            // 加载进度窗口FXML
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/ProgressDialog.fxml"));
            Parent progressRoot = loader.load();
            ProgressDialogController progressController = loader.getController();

            // 配置进度窗口Stage
            Stage progressStage = new Stage();
            progressStage.setTitle("生成报告");
            progressStage.initOwner(((Node) event.getSource()).getScene().getWindow());
            progressStage.initModality(Modality.APPLICATION_MODAL);
            progressStage.setScene(new Scene(progressRoot, 400, 200));
            progressController.setDialogStage(progressStage);

            // 创建后台任务
            Task<Void> reportTask = new Task<Void>() {
                private Path savedPath;

                @Override
                protected Void call() throws Exception {
                    try {
                        MakeReport makeReport = new MakeReport(formData);
                        // 步骤1: 合并CSV文件
                        updateMessage("合并CSV文件...");
                        updateProgress(0, 1);
                        List<File> files = new ArrayList<>(fileList);
                        Table mergedTable = makeReport.mergeCsvFiles(files);
                        System.out.println("[+]合并后的CSV：" + mergedTable);
                        Thread.sleep(1000);

                        // 步骤2: 清洗过滤CSV数据
                        updateMessage("清洗过滤CSV文件...");
                        updateProgress(0.1, 1);
                        Table cleanedTable = makeReport.cleanCsvFiles(mergedTable);
                        System.out.println("[+]清理过滤后的CSV：" + cleanedTable);
                        Thread.sleep(1000);

                        // 步骤3: 翻译漏洞信息
                        updateMessage("翻译漏洞信息...");
                        updateProgress(0.2, 1);
                        Table translatedTable = makeReport.translateVulnerabilities(mergedTable);
                        Thread.sleep(1000);

                        // 步骤4: 分析数据
                        updateMessage("分析漏洞数据...");
                        updateProgress(0.6, 1);
//                        Map<String, Object> analysisResults = makeReport.analyzeVulnerabilityData(translatedTable);
                        Thread.sleep(1000);

                        // 步骤5: 生成报告
                        updateMessage("生成报告...");
                        updateProgress(0.8, 1);
//                        byte[] zipBytes = makeReport.generateZipReport(translatedTable, analysisResults, formData);
                        Thread.sleep(1000);

                        // 步骤6: 保存报告
                        updateMessage("保存报告...");
                        updateProgress(0.95, 1);
//                        savedPath = makeReport.saveReport(zipBytes, formData.get("system_name"));
                        Thread.sleep(1000);
                        // 完成
                        updateMessage("报告生成完成");
                        updateProgress(1, 1);
                        return null;
                    } catch (Exception e) {
                        logger.error("报告生成失败", e);
                        throw e; // 触发failed()回调
                    }
                }

                @Override
                protected void succeeded() {
                    progressController.setCompleted();
                    showAlert("成功", "报告生成完成！\n已保存到: " + savedPath, Alert.AlertType.INFORMATION);
                }

                @Override
                protected void failed() {
                    Throwable e = getException();
                    progressController.setFailed(extractErrorMessage(e));
                    showAlert("错误", "报告生成失败:\n" + extractErrorMessage(e), Alert.AlertType.ERROR);
                }

                @Override
                protected void cancelled() {
                    progressStage.close();
                    showAlert("取消", "操作已取消", Alert.AlertType.INFORMATION);
                }

                private String extractErrorMessage(Throwable e) {
                    if (e instanceof IOException) return "文件处理错误: " + e.getMessage();
                    if (e instanceof IllegalArgumentException) return "参数错误: " + e.getMessage();
                    return e.getCause() != null ? extractErrorMessage(e.getCause()) : e.getMessage();
                }
            };

            // 绑定任务属性到进度窗口
            progressController.bindProgress(reportTask.progressProperty());
            progressController.bindMessage(reportTask.messageProperty());
            progressController.setOnCancel(() -> {
                if (!reportTask.isDone()) reportTask.cancel();
            });

            // 启动任务并显示窗口
            new Thread(reportTask).start();
            progressStage.show();

        } catch (IOException e) {
            logger.error("加载进度窗口失败", e);
            showAlert("错误", "进度窗口加载失败", Alert.AlertType.ERROR);
        }
    }

    public List<String> getSelectedRiskLevels() {
        List<String> selectedLevels = new ArrayList<>();
        riskSelect.getChildren().stream()
                .filter(node -> node instanceof CheckBox)
                .map(node -> (CheckBox) node)
                .filter(CheckBox::isSelected)
                .forEach(checkBox -> selectedLevels.add(checkBox.getUserData().toString()));

        return selectedLevels;
    }

    private Map<String, Object> collectFormData() {
        Map<String, Object> formData = new HashMap<>();

        // 基本信息（字符串）
        formData.put("system_name", systemName.getText());
        formData.put("create", createPerson.getText());
        formData.put("audit", checkPerson.getText());
        formData.put("permit", permitPerson.getText());
        formData.put("create_date", createDate.getValue().format(DATE_FORMAT));
        formData.put("audit_date", checkDate.getValue().format(DATE_FORMAT));
        formData.put("permit_date", permitDate.getValue().format(DATE_FORMAT));

        // 风险级别（直接存储列表）
        formData.put("risk", getSelectedRiskLevels());

        // 单位信息（字符串）
        formData.put("company", unitName.getText());
        formData.put("address", unitAddress.getText());
        formData.put("contact", customContacts.getText());
        formData.put("project", projectName.getText());
        formData.put("email", customEmail.getText());
        formData.put("phone", customPhone.getText());

        // 我方信息（字符串）
        // formData.put("our_company", unitName.getText());
        formData.put("our_email", ourEmail.getText());
        formData.put("our_contact", ourContact.getText());
        formData.put("our_phone", ourPhone.getText());
        formData.put("our_test_person", ourTestPerson.getText());

        // 测试日期（字符串）
        formData.put("start_date", startDate.getValue().format(DATE_FORMAT));
        formData.put("end_date", endDate.getValue().format(DATE_FORMAT));

        return formData;
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

    @FXML
    @Deprecated
    private void handleClearButtonAction(ActionEvent event) {
        systemName.clear();
        createPerson.clear();
        checkPerson.clear();
        permitPerson.clear();
        fileList.clear();
    }

    private void showAlert(String title, String message, Alert.AlertType type) {
        Platform.runLater(() -> {
            Alert alert = new Alert(type);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.showAndWait();
        });
    }

}