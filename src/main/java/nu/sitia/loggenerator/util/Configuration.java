package nu.sitia.loggenerator.util;

import nu.sitia.loggenerator.util.gapdetector.GapDetector;

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
    private int outputBatchSize = 0; // don't care

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
        variableSubstitutions.put("syslog-header", "<{pri:}>{date:MMM dd HH:mm:ss} {oneOf:my-machine,your-machine,localhost,{ipv4:192.168.0.0/16}} {string:a-z0-9/9}[{random:1-65535}]: ");
        variableSubstitutions.put("ip", "{<ipv4:0.0.0.0/0}");
        variableSubstitutions.put("rfc1918","{oneOf:{ipv4:192.168.0.0/16},{ipv4:172.16.0.0/12},{ipv4:10.0.0.0/8}}");
    }

    /** Should the input be treated as a template and variables resolved? In that case, how?*/
    private Template template = Template.NONE;

    /** Should the statistics messages be removed before the data is written to the output? */
    private boolean removeGuards = false;

    /** When the template engine is set to time, this is the duration */
    private long durationSeconds;

    /** Send at most this number of events */
    private long limit = 0;

    /** Gap detection regex */
    private String gapRegex;

    /** The gap detector */
    private GapDetector detector;

    /** How many ms between extra statistics printouts? */
    private long printouts = -1; // not user defined yet

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
    public void setInputBatchSize(Integer value) { this.inputBatchSize = value; }
    public int getOutputBatchSize() {
        return outputBatchSize;
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

    public long getPrintouts() {
        return printouts;
    }

    public void setPrintouts(long printouts) {
        this.printouts = printouts;
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

    public String getGapRegex() {
        return gapRegex;
    }

    public void setGapRegex(String gapRegex) {
        this.gapRegex = gapRegex;
    }

    public GapDetector getDetector() {
        return detector;
    }

    public void setDetector(GapDetector detector) {
        this.detector = detector;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        if (inputType != null)  sb.append("InputType: ").append(inputType).append(System.lineSeparator());
        if (inputName != null) sb.append("InputName: ").append(inputName).append(System.lineSeparator());
        if (outputType != null) sb.append("OutputType: ").append(outputType).append(System.lineSeparator());
        if (outputName != null) sb.append("OutputName: ").append(outputName).append(System.lineSeparator());
        if (inputBatchSize != 0) sb.append("InputBatchSize: ").append(inputBatchSize).append(System.lineSeparator());
        if (outputBatchSize != 0) sb.append("OutputBatchSize: ").append(outputBatchSize).append(System.lineSeparator());
        if (clientName != null) sb.append("ClientName: ").append(clientName).append(System.lineSeparator());
        if (topicName != null) sb.append("TopicName: ").append(topicName).append(System.lineSeparator());
        if (bootstrapServer != null) sb.append("BootstrapServer: ").append(bootstrapServer).append(System.lineSeparator());
        if (header != null) sb.append("Header: ").append(header).append(System.lineSeparator());
        if (regex != null) sb.append("Regex: ").append(regex).append(System.lineSeparator());
        if (value != null) sb.append("Value: ").append(value).append(System.lineSeparator());
        if (statistics) sb.append("Statistics: true\n");
        if (glob != null) sb.append("Glob: ").append(glob).append(System.lineSeparator());
        if (eps != 0) sb.append("EPS: ").append(eps).append(System.lineSeparator());
        if (template != Template.NONE) sb.append("Template: ").append(template).append(System.lineSeparator());
        if (limit != 0) sb.append("Limit: ").append(limit).append(System.lineSeparator());
        sb.append("Remove-guards: ").append(removeGuards).append(System.lineSeparator());
        if (gapRegex != null) sb.append("Gap Regex: ").append(gapRegex).append(System.lineSeparator());
        if (printouts >= 0) sb.append("Statistics printouts every ms: ").append(printouts).append(System.lineSeparator());

        return sb.toString();
    }

}