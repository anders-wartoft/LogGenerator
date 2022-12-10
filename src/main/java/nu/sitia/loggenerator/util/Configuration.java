package nu.sitia.loggenerator.util;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class Configuration {
    /** template possible values */
    public  enum Template {
        CONTINUOUS,
        TIME,
        FILE,
        NONE
    }
    /** Guard sent as an event for each transmission if statistics is enabled */
    public static final String BEGIN_TRANSACTION_TEXT = "--------BEGIN_TRANSACTION--------";
    /** Guard sent as an event for each file if statistics is enabled */
    public static final String BEGIN_FILE_TEXT = "--------BEGIN_FILE--------";

    /** Guard sent as an end event for each transmission if statistics is enabled */
    public static final String END_TRANSACTION_TEXT = "--------END_TRANSACTION--------";
    /** Guard sent as an end event for each file if statistics is enabled */
    public static final String END_FILE_TEXT = "--------END_FILE--------";
    /** Start of transaction */
    public static final List<String> BEGIN_TRANSACTION = new ArrayList<>();
    /** Start of file */
    public static final List<String> BEGIN_FILE = new ArrayList<>();
    /** End of transaction */
    public static final List<String> END_TRANSACTION = new ArrayList<>();
    /** End of file */
    public static final List<String> END_FILE = new ArrayList<>();

    static {
        BEGIN_TRANSACTION.add(BEGIN_TRANSACTION_TEXT);
        BEGIN_FILE.add(BEGIN_FILE_TEXT);
        END_TRANSACTION.add(END_TRANSACTION_TEXT);
        END_FILE.add(END_FILE_TEXT);
    }


    /** Name of the input module */
    private String inputType;
    /** Name of the output module */
    private String outputType;

     /** Name for the input module to load */
    private String inputName;
    /** Name the output module use to send */
    private String outputName;

    /** How many rows to read before sending to the proxy? */
    private int inputBatchSize = 1;
    /** How many rows to send in each batch */
    private int outputBatchSize = 1;

    /** The name to use to identify to e.g., kafka */
    private String clientName;

    /** The Kafka Topic Name */
    private String topicName;

    /** The Kafka Bootstrap Server */
    private String bootstrapServer;

    /** A header to add to all events */
    private String header;

    /** A Regex to find and change to value */
    private String regex;

    /** The value to add instead of the regex */
    private String value;

    /** Default variable substitutions */
    private final Map<String, String> variableSubstitutions = new LinkedHashMap<>();

    /** Statistics for i/o */
    private boolean statistics = false;

    /** Directory glob */
    private String glob;

    /** Preferred eps */
    private long eps = 0;

    public Configuration() {
        variableSubstitutions.put("syslog-header", "<{pri:}>{date:MMM dd HH:mm:ss} {oneOf:mymachine,yourmachine,localhost,{ipv4:192.168.0.0/16}} {string:a-z0-9/9}[{random:1-65535}]: ");
        variableSubstitutions.put("ip", "{<ipv4:0.0.0.0/0}");
    }

    /** Should the input be treated as a template and variables resolved? In that case, how?*/
    private Template template = Template.NONE;

    /** Should the statistics messages be removed before the data is written to the output? */
    private boolean removeGuards = true;

    /** When the template enginge is set to time, this is the duration */
    private long durationSeconds;

    /** Send at most this number of events */
    private long limit = 0;

    public String getInputName() {
        return inputName;
    }

    public void setInputName(String inputName) {
        this.inputName = inputName;
    }

    public String getOutputName() {
        return outputName;
    }

    public void setOutputName(String outputName) {
        this.outputName = outputName;
    }

    public String getInputType() {
        return inputType;
    }

    public void setInputType(String inputType) {
        this.inputType = inputType;
    }

    public String getOutputType() {
        return outputType;
    }

    public void setOutputType(String outputType) {
        this.outputType = outputType;
    }

    public String getClientName() {
        return clientName;
    }

    public void setClientName(String clientName) {
        this.clientName = clientName;
    }

    public String getTopicName() {
        return topicName;
    }

    public void setTopicName(String topicName) {
        this.topicName = topicName;
    }
    public void setInputBatchSize(Integer value) {
        this.inputBatchSize = value;
    }

    public void setOutputBatchSize(Integer value) {
        this.outputBatchSize = value;
    }

    public int getInputBatchSize() {
        return inputBatchSize;
    }

    public String getBootstrapServer() {
        return bootstrapServer;
    }

    public void setBootstrapServer(String bootstrapServer) {
        this.bootstrapServer = bootstrapServer;
    }

    public String getHeader() {
        return header;
    }

    public void setHeader(String header) {
        this.header = header;
    }

    public String getRegex() {
        return regex;
    }

    public void setRegex(String regex) {
        this.regex = regex;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public boolean isStatistics() {
        return statistics;
    }

    public void setStatistics(boolean statistics) {
        this.statistics = statistics;
    }

    public String getGlob() {
        return glob;
    }

    public void setGlob(String glob) {
        this.glob = glob;
    }

    public long getEps() {
        return eps;
    }

    public void setEps(long eps) {
        this.eps = eps;
    }

    public Template getTemplate() {
        return template;
    }

    public long getDurationMilliSeconds() {
        return durationSeconds;
    }

    public long getLimit() {
        return limit;
    }

    public void setLimit(long limit) {
        this.limit = limit;
    }

    public Map<String, String> getVariableSubstitutions() {
        return this.variableSubstitutions;
    }


    public void setTemplate(String template) {
        if (template != null) {
            if ("continuous".equalsIgnoreCase(template)) {
                this.template = Template.CONTINUOUS;
            } else if ("file".equalsIgnoreCase(template)) {
                this.template = Template.FILE;
            } else if ("none".equalsIgnoreCase(template)) {
                this.template = Template.NONE;
            } else if (template.startsWith("time:")) {
                String [] parts = template.split(":");
                if (parts.length != 2) {
                    throw new RuntimeException("Illegal template time value. Set as time:30 for 30 seconds. Value was: " + template);
                }
                this.durationSeconds = Long.parseLong(parts[1]);
                this.template = Template.TIME;
            } else {
                throw new RuntimeException("Illegal template value. Legal values are: continuous, file, time or none. Value was: " + template);
            }
        } else { // null
            this.template = Template.NONE;
        }
    }

    public boolean isRemoveGuards() {
        return removeGuards;
    }

    public void setRemoveGuards(boolean removeGuards) {
        this.removeGuards = removeGuards;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        if (inputType != null)  sb.append("InputType: ").append(inputType).append("\n");
        if (inputName != null) sb.append("InputName: ").append(inputName).append("\n");
        if (outputType != null) sb.append("OutputType: ").append(outputType).append("\n");
        if (outputName != null) sb.append("OutputName: ").append(outputName).append("\n");
        if (inputBatchSize != 0) sb.append("InputBatchSize: ").append(inputBatchSize).append("\n");
        if (outputBatchSize != 0) sb.append("OutputBatchSize: ").append(outputBatchSize).append("\n");
        if (clientName != null) sb.append("ClientName: ").append(clientName).append("\n");
        if (topicName != null) sb.append("TopicName: ").append(topicName).append("\n");
        if (bootstrapServer != null) sb.append("BootstrapServer: ").append(bootstrapServer).append("\n");
        if (header != null) sb.append("Header: ").append(header).append("\n");
        if (regex != null) sb.append("Regex: ").append(regex).append("\n");
        if (value != null) sb.append("Value: ").append(value).append("\n");
        if (statistics) sb.append("Statistics: true\n");
        if (glob != null) sb.append("Glob: ").append(glob).append("\n");
        if (eps != 0) sb.append("EPS: ").append(eps).append("\n");
        if (template != Template.NONE) sb.append("Template: ").append(template).append("\n");
        if (limit != 0) sb.append("Limit: ").append(limit).append("\n");
        sb.append("Remove-guards: ").append(removeGuards).append("\n");

        return sb.toString();
    }

}