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
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ElasticOutputItem extends AbstractOutputItem implements SendListener {
    static final Logger logger = Logger.getLogger(ElasticOutputItem.class.getName());
    /** The url to connect to */
    private final String host;

    private final int port;

    /** The API key to use */
    private final String apiKey;

    private final String index;

    private final Pattern pattern;

    /** The client to use in the communication */
    private RestClient restClient;

    private final String idRegex;

    private final String id;

    private final String certificatePath;

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
        addTransactionMessages = config.isStatistics();
        this.host = config.getValue("-eoh");
        this.port = Integer.parseInt(config.getValue("-eop"));
        this.index = config.getValue("-eoi");
        this.id = config.getValue("-eoid");
        this.idRegex = config.getValue("-eoidre");
        this.apiKey = config.getValue("-eoak");
        this.certificatePath = config.getValue("-eoc");


        this.pattern = Pattern.compile(idRegex);
        RequestOptions.Builder builder = RequestOptions.DEFAULT.toBuilder();
        builder.addHeader("Authorization", "ApiKey " + apiKey);
        builder.addHeader("Content-Type", "application/json");
        builder.setHttpAsyncResponseConsumerFactory(
                new HttpAsyncResponseConsumerFactory
                        .HeapBufferedResponseConsumerFactory(1024 * 1024 * 1024));
        COMMON_OPTIONS = builder.build();
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
                                Arrays.stream(headers).toList().forEach(s -> builder.append(s.toString()).append(" "));
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
                            new HttpHost(this.host, this.port, "https"))
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
        return String.format("ElasticOutputItem %s:%d/%s", this.host, this.port, this.index);
    }
}
