package cn.viewcn.nessusrm.gui;

import javafx.application.Platform;
import javafx.beans.property.ReadOnlyDoubleProperty;
import javafx.beans.property.ReadOnlyStringProperty;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.stage.Stage;
import java.util.concurrent.atomic.AtomicReference;

public class ProgressDialogController {

    @FXML private Label messageLabel;
    @FXML private ProgressBar progressBar;
    @FXML private Button cancelButton;

    private Stage dialogStage;
    private Runnable cancelAction;
    private final AtomicReference<ReadOnlyDoubleProperty> boundProgress = new AtomicReference<>();
    private final AtomicReference<ReadOnlyStringProperty> boundMessage = new AtomicReference<>();

    // 初始化方法（FXML加载后自动调用）
    @FXML
    public void initialize() {
        // 设置初始状态
        messageLabel.setText("处理中，请稍候...");
        progressBar.setProgress(0.0);
    }

    // 设置对话框Stage（由外部调用）
    public void setDialogStage(Stage stage) {
        this.dialogStage = stage;
    }

    // ===== 进度和消息更新 =====

    /**
     * 绑定外部进度属性（如Task的progressProperty）
     */
    public void bindProgress(ReadOnlyDoubleProperty progressProperty) {
        unbindProgress(); // 先解除旧绑定
        boundProgress.set(progressProperty);
        progressBar.progressProperty().bind(progressProperty);
    }

    /**
     * 绑定外部消息属性（如Task的messageProperty）
     */
    public void bindMessage(ReadOnlyStringProperty messageProperty) {
        unbindMessage(); // 先解除旧绑定
        boundMessage.set(messageProperty);
        messageLabel.textProperty().bind(messageProperty);
    }

    /**
     * 手动更新进度（会自动解除绑定）
     */
    public void updateProgress(double progress) {
        Platform.runLater(() -> {
            unbindProgress();
            if (progress < 0) {
                progressBar.setProgress(ProgressBar.INDETERMINATE_PROGRESS);
            } else {
                progressBar.setProgress(progress);
            }
        });
    }

    /**
     * 手动更新主消息（会自动解除绑定）
     */
    public void updateMessage(String message) {
        Platform.runLater(() -> {
            unbindMessage();
            messageLabel.setText(message);
        });
    }

    /**
     * 更新状态详情文本
     */

    // ===== 绑定管理 =====

    private void unbindProgress() {
        ReadOnlyDoubleProperty bound = boundProgress.getAndSet(null);
        if (bound != null) {
            progressBar.progressProperty().unbind();
        }
    }

    private void unbindMessage() {
        ReadOnlyStringProperty bound = boundMessage.getAndSet(null);
        if (bound != null) {
            messageLabel.textProperty().unbind();
        }
    }

    // ===== 取消按钮事件 =====

    @FXML
    private void handleCancel() {
        if (cancelAction != null) {
            cancelAction.run();
        }
        if (dialogStage != null) {
            dialogStage.close();
        }
    }

    // 设置取消回调
    public void setOnCancel(Runnable action) {
        this.cancelAction = action;
    }

    // ===== 完成/失败状态 =====

    /**
     * 标记操作成功完成
     */
    public void setCompleted() {
        Platform.runLater(() -> {
            unbindAll();
            messageLabel.setText("操作完成");
            progressBar.setProgress(1.0);
            cancelButton.setText("关闭");

            // 3秒后自动关闭
            new Thread(() -> {
                try {
                    Thread.sleep(2500);
                    Platform.runLater(() -> dialogStage.close());
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }).start();
        });
    }

    /**
     * 标记操作失败
     */
    public void setFailed(String errorMessage) {
        Platform.runLater(() -> {
            unbindAll();
            messageLabel.setText("操作失败");
            progressBar.setProgress(ProgressBar.INDETERMINATE_PROGRESS);
            cancelButton.setText("关闭");

            // 3秒后自动关闭
            new Thread(() -> {
                try {
                    Thread.sleep(2500);
                    Platform.runLater(() -> dialogStage.close());
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }).start();
        });
    }

    // 解除所有绑定
    private void unbindAll() {
        unbindProgress();
        unbindMessage();
    }
}