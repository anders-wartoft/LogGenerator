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
 *  WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR
 * OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package nu.sitia.loggenerator.inputitems;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import java.util.ArrayList;
import java.util.List;

import java.util.logging.Logger;


public class ElasticInputItem extends AbstractInputItem {
    static final Logger logger = Logger.getLogger(ElasticInputItem.class.getName());
    /** The url to connect to */
    private final String host;

    private final int port;

    /** The API key to use */
    private final String apiKey;

    private final String index;

    /** A JSON formatted query */
    private String query= "{\"query\": { \"query_string\": { \"query\": \"*\" }}, \"_source\": [\"_id\"]}";
    private final String field;

    /** The client to use in the communication */
    private RestClient restClient;

    /** The higher lever api */
//    private ElasticsearchClient esClient;

    private final String certificatePath;

    private static RequestOptions COMMON_OPTIONS;

    private List<String> result = new ArrayList<>();

    private long readPosition = 0;

    /** Trust all hostnames even if they differ from the hostname in the certificate */
    private static final HostnameVerifier HOSTNAME_VERIFIER = (hostname, session) -> true;

    /**
     * Create a new ElasticInputItem
     * @param config The command line arguments
     */
    public ElasticInputItem(Configuration config) {
        super(config);
        this.host = config.getValue("-eih");
        this.port = Integer.parseInt(config.getValue("-eip"));
        this.index = config.getValue("-eii");
        this.apiKey = config.getValue("-eiak");
        this.certificatePath = config.getValue("-eic");
        this.field = config.getValue("-eif");
        if (config.getValue("-eiq") != null) {
            this.query = config.getValue("-eiq");
        }

        RequestOptions.Builder builder = RequestOptions.DEFAULT.toBuilder();
        builder.addHeader("Authorization", "ApiKey " + apiKey);
        builder.addHeader("Content-Type", "application/json");
        builder.setHttpAsyncResponseConsumerFactory(
                new HttpAsyncResponseConsumerFactory
                        .HeapBufferedResponseConsumerFactory(1024 * 1024 * 1024));
        COMMON_OPTIONS = builder.build();

    }

    /**
     * Let the item prepare for reading
     */
    public void setup() throws RuntimeException {
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
                            String value = x.get(this.field).toString();
                            if (value != null && value.length() > 2 && "\"".equals(value.substring(0, 1)) ) {
                                // Remove leading and trailing " from single values
                                result.add(value.substring(1, value.length() - 1));
                            } else {
                                result.add(value);
                            }
                        } catch (Exception e) {
                            logger.warning(String.format("Can't read field %s. Is this field present in the result? %s", this.field, x));
                            throw (e);
                        }
                    } else {
                        // Read everything
                        JsonNode event = rootNode.get("hits").get("hits");
                        result.add(event.toString());
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
        return String.format("ElasticInputItem %s:%d/%s", this.host, this.port, this.index);
    }
}
