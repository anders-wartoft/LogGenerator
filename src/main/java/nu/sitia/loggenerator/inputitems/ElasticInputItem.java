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

package nu.sitia.loggenerator.inputitems;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import nu.sitia.loggenerator.Configuration;
import nu.sitia.loggenerator.util.JsonUtil;
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
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class ElasticInputItem extends AbstractInputItem {
    static final Logger logger = Logger.getLogger(ElasticInputItem.class.getName());
    /** The url to connect to */
    private String host;

    private String port;

    /** The API key to use */
    private String apiKey;

    private String index;

    /** A JSON formatted query */
    private String query= "{\"query\": { \"query_string\": { \"query\": \"*\" }}, \"_source\": [\"_id\"]}";
    private String field;

    /** The client to use in the communication */
    private RestClient restClient;

    private String certificatePath;

    private static RequestOptions COMMON_OPTIONS;

    private List<String> result = new ArrayList<>();

    private long readPosition = 0;

    private final static int DEFAULT_BATCHSIZE = 100;

    /** Trust all hostnames even if they differ from the hostname in the certificate */
    private static final HostnameVerifier HOSTNAME_VERIFIER = (hostname, session) -> true;

    /**
     * Create a new ElasticInputItem
     */
    public ElasticInputItem(Configuration config) {
        super(config);
        this.batchSize = DEFAULT_BATCHSIZE;
    }

    @Override
    public boolean setParameter(String key, String value) {
        if (key != null && (key.equalsIgnoreCase("--help") || key.equalsIgnoreCase("-h"))) {
            System.out.println("ElasticInputItem. Read events from an Elastic query\n" +
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
                    "--field <field> (-f <field>)\n" +
                    "  The field to read from\n" +
                    "--query <query> (-q <query>)\n" +
                    "  The query to use\n");
            super.setParameter(key, value);
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
        if (key != null && (key.equalsIgnoreCase("--field") || key.equalsIgnoreCase("-f"))) {
            this.field = value;
            logger.fine("field " + value);
            return true;
        }
        if (key != null && (key.equalsIgnoreCase("--query") || key.equalsIgnoreCase("-q"))) {
            this.query = value;
            logger.fine("query " + value);
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
        if (null == this.field) {
            throw new RuntimeException("Missing --field");
        }
        if (null == this.query) {
            throw new RuntimeException("Missing --query");
        }

        final Pattern pattern = Pattern.compile("^\\d{1,5}$");
        final Matcher matcher = pattern.matcher(this.port);
        if (!matcher.matches()) {
            throw new RuntimeException("Field port contains illegal characters: " + this.port);
        }

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
     * Let the item prepare for reading
     */
    public void setup() throws RuntimeException {
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

    private List<String> getHits() {
        try {
            // pagination. Dirty hack. Change { to { "from": pos, "size": size
            String paginatedQuery = this.query.replaceFirst("\\{",
                    "{ \"from\": " + this.readPosition +
                            ", \"size\": " + this.batchSize + ", ");
            logger.fine(paginatedQuery);
            Request request = new Request(
                    "POST",
                    this.index + "/_search");
            request.setJsonEntity(paginatedQuery);
            request.setOptions(COMMON_OPTIONS);

            Response response = restClient.performRequest(request);
            int statusCode = response.getStatusLine().getStatusCode();
            if (statusCode != 200) {
                logger.warning(String.format("Got return code: %d. %s", statusCode, response.getStatusLine().getReasonPhrase()));
                return new ArrayList<>();
            }
            try {
                String responseBody = EntityUtils.toString(response.getEntity());
                ObjectMapper objectMapper = new ObjectMapper();
                JsonNode rootNode = objectMapper.readTree(responseBody);
                int hits = rootNode.get("hits").get("total").get("value").asInt();
                logger.fine(String.format("StatusCode: %d, hits: %d", statusCode, hits));
                ArrayList<String> result = new ArrayList<>();
                for (JsonNode x : rootNode.get("hits").get("hits")) {
                    if (this.field != null) {
                        try {
                            result.addAll(new JsonUtil().matchPath(x, this.field));
                        } catch (Exception e) {
                            logger.warning(String.format("Can't read field %s. Is this field present in the result? %s", this.field, x));
                            throw (e);
                        }
                    } else {
                        // Read everything
                        result.add(x.toString());
                    }
                }
                this.readPosition += result.size();
                return result;
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    /**
     * Are there more messages to read?
     * We'll just wait if there are no messages now...
     * @return True iff there are more messages
     */
    public boolean hasNext() {
        if (this.result.size() > 0) {
            return true;
        }
        this.result = this.getHits();
        return this.result.size() > 0;
    }

    /**
     * Read input one line at a time and return
     * {batchSize} elements.
     * @return The read input
     */
    public List<String> next() {
        List<String> toReturn = new ArrayList<>();
        for (int i=0; i<batchSize && this.result.size() > 0; i++) {
            toReturn.add(this.result.get(0));
            this.result.remove(0);
        }
        return toReturn;
    }

    /**
     * Let the item teardown after reading.
     * Will be called after Ctrl-C
     */
    public void teardown() {
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
        return String.format("ElasticInputItem %s:%s/%s, %d", this.host, this.port, this.index, this.batchSize);
    }
}
