package nu.sitia.loggenerator;
import nu.sitia.loggenerator.filter.FilterFactory;
import nu.sitia.loggenerator.filter.GuardFilter;
import nu.sitia.loggenerator.filter.ProcessFilter;
import nu.sitia.loggenerator.inputitems.InputItem;
import nu.sitia.loggenerator.inputitems.InputItemFactory;
import nu.sitia.loggenerator.outputitems.OutputItem;
import nu.sitia.loggenerator.outputitems.OutputItemFactory;
import org.apache.commons.cli.*;
import nu.sitia.loggenerator.util.Configuration;

import java.util.List;
import java.util.ArrayList;
import java.util.logging.Logger;

/**
 * Hello world!
 *
 */
public class App 
{
    static Logger logger = Logger.getLogger(App.class.getName());

    public static void main( String[] args )
    {
        // -i file -n test/data/test.txt -o cmd
        Options options = new Options();

        Option input = new Option("i", "input", true, "Input module name (udp, tcp, kafka or file");
        input.setRequired(true);
        options.addOption(input);

        Option output = new Option("o", "output", true, "Output module name (cmd, udp, tcp, kafka or file)");
        output.setRequired(true);
        options.addOption(output);

        Option inputName = new Option("in", "input-name", true, "Input file name");
        inputName.setRequired(false);
        options.addOption(inputName);

        Option outputName = new Option("on", "output-name", true, "Output details");
        outputName.setRequired(false);
        options.addOption(outputName);

        Option inputBatchSize = new Option("ib", "input-batch-size", true, "How many rows to read before sending to processing");
        inputBatchSize.setRequired(false);
        options.addOption(inputBatchSize);

        Option outputBatchSize = new Option("ob", "output-batch-size", true, "How many rows to send in each sending");
        outputBatchSize.setRequired(false);
        options.addOption(outputBatchSize);

        Option clientName = new Option("cn", "client-name", true, "The Client ID to use in Kafka input and output items");
        clientName.setRequired(false);
        options.addOption(clientName);

        Option topicName = new Option("tn", "topic-name", true, "The Topic Name to use in Kafka input and output items");
        topicName.setRequired(false);
        options.addOption(topicName);

        Option bootstrapServer = new Option("bs", "bootstrap-server", true, "The address (host:port) to Kafka input and output items");
        bootstrapServer.setRequired(false);
        options.addOption(bootstrapServer);

        Option header = new Option("he", "header", true, "String to add to the beginning of each entry. May contain variables.");
        header.setRequired(false);
        options.addOption(header);

        Option regex = new Option("r", "regex", true, "Regex to search for and replace with a value.");
        regex.setRequired(false);
        options.addOption(regex);

        Option value = new Option("v", "value", true, "Value to replace the regex with.");
        value.setRequired(false);
        options.addOption(value);

        Option statistics = new Option("s", "statistics", true, "Add statistics messages and printouts");
        statistics.setRequired(false);
        options.addOption(statistics);

        Option glob = new Option("g", "glob", true, "Filename glob for directories");
        glob.setRequired(false);
        options.addOption(glob);

        Option eps = new Option("e", "eps", true, "Max eps. Throttle if above.");
        eps.setRequired(false);
        options.addOption(eps);

        Option template = new Option("t", "template", true, "Should the input be regarded as a template and variables resolved?");
        template.setRequired(false);
        options.addOption(template);

        Option removeGuards = new Option("rg", "remove-guards", true, "Drop messages that is used for statistics before we send them?");
        removeGuards.setRequired(false);
        options.addOption(removeGuards);

        Option limit = new Option("l", "limit", true, "Only send this number of messages");
        limit.setRequired(false);
        options.addOption(limit);

        CommandLineParser parser = new DefaultParser();
        HelpFormatter formatter = new HelpFormatter();
        CommandLine cmd = null; //not a good practice, it serves it purpose

        try {
            cmd = parser.parse(options, args);
        } catch (ParseException e) {
            System.out.println(e.getMessage());
            formatter.printHelp("utility-name", options);
            System.exit(1);
        }
        // Create a configuration object
        Configuration config = new Configuration();
        config.setInputType(cmd.getOptionValue("input"));
        config.setInputName(cmd.getOptionValue("input-name"));
        config.setOutputType(cmd.getOptionValue("output"));
        config.setOutputName(cmd.getOptionValue("output-name"));
        config.setClientName(cmd.getOptionValue("client-name"));
        config.setTopicName(cmd.getOptionValue("topic-name"));
        config.setBootstrapServer(cmd.getOptionValue("bootstrap-server"));
        config.setHeader(cmd.getOptionValue("header"));
        config.setRegex(cmd.getOptionValue("regex"));
        config.setValue(cmd.getOptionValue("value"));
        config.setStatistics(cmd.getOptionValue("statistics") != null && cmd.getOptionValue("statistics").equalsIgnoreCase("true"));
        config.setGlob(cmd.getOptionValue(glob));


        if (cmd.getOptionValue("eps") != null) {
            if (!config.isStatistics()) {
                throw new RuntimeException("eps requires statistics");
            }
            config.setEps(Long.parseLong(cmd.getOptionValue("eps")));
        }

        if (cmd.getOptionValue("limit") != null) {
            config.setLimit(Long.parseLong(cmd.getOptionValue("limit")));
        }

        int inputBatchSizeInt = 1;
        if (cmd.getOptionValue(inputBatchSize) != null) {
            inputBatchSizeInt = Integer.parseInt(cmd.getOptionValue(inputBatchSize));
        }
        config.setInputBatchSize(inputBatchSizeInt);

        int outputBatchSizeInt = 1;
        if (cmd.getOptionValue(outputBatchSize) != null) {
            outputBatchSizeInt = Integer.parseInt(cmd.getOptionValue(outputBatchSize));
        }
        config.setOutputBatchSize(outputBatchSizeInt);

        if (cmd.getOptionValue("template") != null) {
            config.setTemplate(cmd.getOptionValue("template"));
        }

        if (cmd.getOptionValue("remove-guards") != null) {
            config.setRemoveGuards(cmd.getOptionValue("remove-guards").equalsIgnoreCase("true"));
        }

        // Print the variables
        logger.config(config.toString());

        // Create a list of filters
        List<ProcessFilter> filterList = new ArrayList<>();
        if (config.isRemoveGuards()) {
            // The sender will send control messages, like --------BEGIN_...
            // remove those
            filterList.add(new GuardFilter(config));
        }
        if (config.getRegex() != null) {
            filterList.add(FilterFactory.createFilter("regex", config));
        }
        if (config.getHeader() != null) {
            filterList.add(FilterFactory.createFilter("header", config));
        }
        if (config.getRegex() != null || config.getHeader() != null || config.getTemplate() != Configuration.Template.NONE) {
            // {date: etc
            filterList.add(FilterFactory.createFilter("substitution", config));
        }

        // Now create the input and output items
        InputItem inputItem = InputItemFactory.create(config);
        OutputItem outputItem = OutputItemFactory.create(config);

        // And the proxy that mediates the flow
        ItemProxy proxy = new ItemProxy(inputItem, outputItem, filterList, config);

        // Start pumping messages
        proxy.pump();
    }
}
