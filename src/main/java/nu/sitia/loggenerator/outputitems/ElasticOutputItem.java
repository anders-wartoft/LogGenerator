/*
 * Copyright 2022 sitia.nu https://github.com/anders-wartoft/LogGenerator
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit
 * persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the
 * Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE
 *  WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NON-INFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR
 * OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package nu.sitia.loggenerator.outputitems;

import nu.sitia.loggenerator.Configuration;
import org.apache.http.Header;
import org.apache.http.HttpHost;
import org.apache.http.message.BasicHeader;
import org.apache.http.ssl.SSLContextBuilder;
import org.apache.http.ssl.SSLContexts;
import org.apache.http.util.EntityUtils;
import org.elasticsearch.client.*;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.KeyStore;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ElasticOutputItem extends AbstractOutputItem implements SendListener {
    static final Logger logger = Logger.getLogger(ElasticOutputItem.class.getName());
    /** The url to connect to */
    private String host;

    private String port;

    /** The API key to use */
    private String apiKey;

    private String index;

    private Pattern pattern;

    /** The client to use in the communication */
    private RestClient restClient;

    private String idRegex;

    private String id;

    private String certificatePath;

    private static RequestOptions COMMON_OPTIONS;

    /** Trust all hostnames even if they differ from the hostname in the certificate */
    private static final HostnameVerifier HOSTNAME_VERIFIER = (hostname, session) -> true;

    /**
     * Constructor. Add the callback method from this class.
     * @param config The command line arguments
     */
    public ElasticOutputItem(Configuration config) {
        super(config);
        super.addListener(this);
    }

    @Override
    public boolean setParameter(String key, String value) {
        if (key != null && (key.equalsIgnoreCase("--help") || key.equalsIgnoreCase("-h"))) {
            System.out.println("ElasticOutputItem. Write events to an Elastic instance\n" +
                    "Parameters:\n" +
                    "--hostname <hostname> (-h <hostname>)\n" +
                    "  The hostname of the Elastic server\n" +
                    "--port <port> (-p <port>)\n" +
                    "  The port of the Elastic server\n" +
                    "--index <index> (-i <index>)\n" +
                    "  The index to read from\n" +
                    "--api-key <api-key> (-ak <api-key>)\n" +
                    "  The API key to use\n" +
                    "--certificate-path <certificate-path> (-cp <certificate-path>)\n" +
                    "  The path to the certificate to use\n" +
                    "--id <id> (-id <id>)\n" +
                    "  The id to use when writing to Elastic. " +
                    "--regex <regex> (-r <regex>)\n" +
                    "  Regex to use to find the event id from the incoming logs. " +
                    "Used as the variable ${id} in the --id variable above\n");
            System.exit(1);
        }
        if (super.setParameter(key, value)) {
            return true;
        }
        if (key != null && (key.equalsIgnoreCase("--hostname") || key.equalsIgnoreCase("-h"))) {
            this.host = value;
            logger.fine("host " + value);
            return true;
        }
        if (key != null && (key.equalsIgnoreCase("--port") || key.equalsIgnoreCase("-p"))) {
            this.port = value;
            logger.fine("port " + value);
            return true;
        }
        if (key != null && (key.equalsIgnoreCase("--index") || key.equalsIgnoreCase("-i"))) {
            this.index = value;
            logger.fine("index " + value);
            return true;
        }
        if (key != null && (key.equalsIgnoreCase("--api-key") || key.equalsIgnoreCase("-ak"))) {
            this.apiKey = value;
            logger.fine("apiKey " + value);
            return true;
        }
        if (key != null && (key.equalsIgnoreCase("--certificate-path") || key.equalsIgnoreCase("-cp"))) {
            this.certificatePath = value;
            logger.fine("certificatePath " + value);
            return true;
        }
        if (key != null && (key.equalsIgnoreCase("--id") || key.equalsIgnoreCase("-id"))) {
            this.id = value;
            logger.fine("id " + value);
            return true;
        }
        if (key != null && (key.equalsIgnoreCase("--regex") || key.equalsIgnoreCase("-r"))) {
            this.idRegex = value;
            logger.fine("idRegex " + value);
            return true;
        }
        return false;
    }

    @Override
    public boolean afterPropertiesSet() {
        if (null == this.host) {
            throw new RuntimeException("Missing --hostname");
        }
        if (null == this.port) {
            throw new RuntimeException("Missing --port");
        }
        if (null == this.index) {
            throw new RuntimeException("Missing --index");
        }
        if (null == this.apiKey) {
            throw new RuntimeException("Missing --api-key");
        }
        if (null == this.certificatePath) {
            throw new RuntimeException("Missing --certificate-path");
        }
        if (null == this.id) {
            throw new RuntimeException("Missing --id");
        }
        if (null == this.idRegex) {
            throw new RuntimeException("Missing --regex");
        }

        final Pattern check = Pattern.compile("^\\d{1,5}$");
        final Matcher matcher = check.matcher(this.port);
        if (!matcher.matches()) {
            throw new RuntimeException("Field port contains illegal characters: " + this.port);
        }

        // Create the pattern to use to find the id
        this.pattern = Pattern.compile(this.idRegex);

        RequestOptions.Builder builder = RequestOptions.DEFAULT.toBuilder();
        builder.addHeader("Authorization", "ApiKey " + apiKey);
        builder.addHeader("Content-Type", "application/json");
        builder.setHttpAsyncResponseConsumerFactory(
                new HttpAsyncResponseConsumerFactory
                        .HeapBufferedResponseConsumerFactory(1024 * 1024 * 1024));
        COMMON_OPTIONS = builder.build();
        return true;
    }

    /**
     * Write to Elasticsearch
     * @param elements The element to write
     * @throws RuntimeException Not thrown here
     */
    @Override
    public void write(List<String> elements) throws RuntimeException {
        super.write(elements);
    }

    /**
     * Callback. What to do when the cache is full.
     * Writes to hostname:port
     * @param toSend String to send
     */
    @Override
    public void send(List<String> toSend) {
        logger.info("Sending " + toSend.size() + " messages to " + this.host);
        if (toSend.size() > 0) {
            try {
                SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
                toSend.forEach(e -> {
                    // Get the id:
                    Matcher matcher = pattern.matcher(e);
                    if (matcher.find()) {
                        String id = matcher.group(1);
                        logger.finest("Found log id: " + id);
                        String newId = this.id.replace("${id}", id);
                        logger.finest("Created new id: " + newId);
                        String date = format.format(new Date());
                        String content = "{  \"@timestamp\": \"" + date + "\", \"message\": \"" + e + "\"}";
                        Request request = new Request(
                                    "PUT",
                                    "/" + this.index + "/_create/" + newId);
                        request.setJsonEntity(content);
                        request.setOptions(COMMON_OPTIONS);

                        Response response;
                        try {
                            response = restClient.performRequest(request);
                            int statusCode = response.getStatusLine().getStatusCode();
                            Header[] headers = response.getHeaders();
                            try {
                                String responseBody = EntityUtils.toString(response.getEntity());
                                StringBuilder builder = new StringBuilder();
                                List<Header> list = new ArrayList<>();
                                Collections.addAll(list, headers);
                                for (Header s : list) {
                                    builder.append(s.toString()).append(" ");
                                }
                                logger.fine(statusCode + ":" + builder + " : " + responseBody);
                            } catch (IOException ex) {
                                throw new RuntimeException(ex);
                            }
                        } catch (IOException ex) {
                            throw new RuntimeException(ex);
                        }
                        logger.fine("Created new item with id: " + newId);
                    } else {
                        logger.warning("The regex '" + this.idRegex + "' did not match the log entry: '" + e + "'");
                    }
                });
                logger.finer("Sent message without exception");
            } catch (Exception e) {
                logger.warning("Exception sending message: \n" + e.getMessage() + "\n");
            }
        }
    }

    @Override
    public void setup() throws RuntimeException {
        super.setup();
        try {
            int portNumber = Integer.parseInt(this.port);
            Path caCertificatePath = Paths.get(this.certificatePath);
            CertificateFactory factory = CertificateFactory.getInstance("X.509");
            Certificate trustedCa;
            try (InputStream is = Files.newInputStream(caCertificatePath)) {
                trustedCa = factory.generateCertificate(is);
            }
            KeyStore trustStore = KeyStore.getInstance("pkcs12");
            trustStore.load(null, null);
            trustStore.setCertificateEntry("ca", trustedCa);
            SSLContextBuilder sslContextBuilder = SSLContexts.custom()
                    .loadTrustMaterial(trustStore, null);
            final SSLContext sslContext = sslContextBuilder.build();
            this.restClient = RestClient.builder(
                            new HttpHost(this.host, portNumber, "https"))
                    .setHttpClientConfigCallback(httpClientBuilder -> httpClientBuilder.setSSLContext(sslContext).setSSLHostnameVerifier(HOSTNAME_VERIFIER))
                    .setDefaultHeaders(new Header[]{
                            new BasicHeader("Authorization", "ApiKey " + apiKey)
                    })
                    .build();
            logger.info("Connected to Elastic  " + this.host + ":" + this.port);

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void teardown() {
        super.teardown();
        if (this.restClient != null) {
            try {
                this.restClient.close();
            } catch (IOException e) {
                // Ignore, we are shutting down...
            } finally {
                this.restClient = null;
            }
        }
    }

    /**
     * Print the configuration
     * @return A printable string of the current configuration
     */
    @Override
    public String toString() {
        return String.format("ElasticOutputItem %s:%s/%s", this.host, this.port, this.index);
    }
}
