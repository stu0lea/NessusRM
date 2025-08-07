package cn.viewcn.nessusrm.orm;
import java.util.Map;
import com.j256.ormlite.field.DataType;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

@DatabaseTable(tableName = "nessus_trans_plugin")
public class PluginTranslation {
    @DatabaseField(id = true, columnName = "plugin_id")
    private String pluginId;

    @DatabaseField(columnName = "cve", dataType = DataType.LONG_STRING)
    private String cve;

    @DatabaseField(columnName = "cvss", dataType = DataType.LONG_STRING)
    private String cvss;

    @DatabaseField(columnName = "risk", dataType = DataType.LONG_STRING)
    private String risk;

    @DatabaseField(columnName = "risk_cn", dataType = DataType.LONG_STRING)
    private String riskCn;

    @DatabaseField(columnName = "plugin_name", dataType = DataType.LONG_STRING)
    private String pluginName;

    @DatabaseField(columnName = "plugin_name_cn", dataType = DataType.LONG_STRING)
    private String pluginNameCn;

    @DatabaseField(columnName = "synopsis", dataType = DataType.LONG_STRING)
    private String synopsis;

    @DatabaseField(columnName = "synopsis_cn", dataType = DataType.LONG_STRING)
    private String synopsisCn;

    @DatabaseField(columnName = "description", dataType = DataType.LONG_STRING)
    private String description;

    @DatabaseField(columnName = "description_cn", dataType = DataType.LONG_STRING)
    private String descriptionCn;

    @DatabaseField(columnName = "solution", dataType = DataType.LONG_STRING)
    private String solution;

    @DatabaseField(columnName = "solution_cn", dataType = DataType.LONG_STRING)
    private String solutionCn;

    @DatabaseField(columnName = "upload_date", dataType = DataType.LONG_STRING)
    private String uploadDate;

    // 无参构造函数 (OrmLite要求)
    public PluginTranslation() {}

    // 带参数的构造函数
    public PluginTranslation(Map<String, Object> data) {
        this.pluginId = (String) data.get("plugin_id");
        this.cve = (String) data.get("cve");
        this.cvss = (String) data.get("cvss");
        this.risk = (String) data.get("risk");
        this.pluginName = (String) data.get("plugin_name");
        this.synopsis = (String) data.get("synopsis");
        this.description = (String) data.get("description");
        this.solution = (String) data.get("solution");
        this.uploadDate = (String) data.get("upload_date");
        this.pluginNameCn = (String) data.get("plugin_name_cn");
        this.riskCn = (String) data.get("risk_cn");
        this.synopsisCn = (String) data.get("synopsis_cn");
        this.descriptionCn = (String) data.get("description_cn");
        this.solutionCn = (String) data.get("solution_cn");
    }

    // ============ Getter 方法 ============
    public String getPluginId() {
        return pluginId;
    }

    public String getCve() {
        return cve;
    }

    public String getCvss() {
        return cvss;
    }

    public String getRisk() {
        return risk;
    }

    public String getPluginName() {
        return pluginName;
    }

    public String getSynopsis() {
        return synopsis;
    }

    public String getDescription() {
        return description;
    }

    public String getSolution() {
        return solution;
    }

    public String getUploadDate() {
        return uploadDate;
    }

    public String getPluginNameCn() {
        return pluginNameCn;
    }

    public String getRiskCn() {
        return riskCn;
    }

    public String getSynopsisCn() {
        return synopsisCn;
    }

    public String getDescriptionCn() {
        return descriptionCn;
    }

    public String getSolutionCn() {
        return solutionCn;
    }

    // ============ Setter 方法 ============
    public void setPluginId(String pluginId) {
        this.pluginId = pluginId;
    }

    public void setCve(String cve) {
        this.cve = cve;
    }

    public void setCvss(String cvss) {
        this.cvss = cvss;
    }

    public void setRisk(String risk) {
        this.risk = risk;
    }

    public void setPluginName(String pluginName) {
        this.pluginName = pluginName;
    }

    public void setSynopsis(String synopsis) {
        this.synopsis = synopsis;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setSolution(String solution) {
        this.solution = solution;
    }

    public void setUploadDate(String uploadDate) {
        this.uploadDate = uploadDate;
    }

    public void setPluginNameCn(String pluginNameCn) {
        this.pluginNameCn = pluginNameCn;
    }

    public void setRiskCn(String riskCn) {
        this.riskCn = riskCn;
    }

    public void setSynopsisCn(String synopsisCn) {
        this.synopsisCn = synopsisCn;
    }

    public void setDescriptionCn(String descriptionCn) {
        this.descriptionCn = descriptionCn;
    }

    public void setSolutionCn(String solutionCn) {
        this.solutionCn = solutionCn;
    }

}