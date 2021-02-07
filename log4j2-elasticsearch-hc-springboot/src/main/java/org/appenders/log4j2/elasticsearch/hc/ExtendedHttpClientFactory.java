package org.appenders.log4j2.elasticsearch.hc;

import com.github.rfoltyns.ExtendedHCRequestFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.impl.nio.client.CloseableHttpAsyncClient;
import org.apache.http.nio.conn.NoopIOSessionStrategy;
import org.apache.http.nio.conn.ssl.SSLIOSessionStrategy;

public class ExtendedHttpClientFactory extends HttpClientFactory {

    public ExtendedHttpClientFactory(Builder httpClientFactoryBuilder) {
        super(httpClientFactoryBuilder);
    }

    @Override
    protected HttpClient createConfiguredClient(CloseableHttpAsyncClient asyncHttpClient, ServerPool serverPool, HttpAsyncResponseConsumerFactory asyncResponseConsumerFactory) {
        return new HttpClient(
                asyncHttpClient,
                serverPool,
                new ExtendedHCRequestFactory(),
                asyncResponseConsumerFactory
        );
    }

    public static class Builder extends HttpClientFactory.Builder {

        @Override
        public HttpClientFactory build() {

            if (this.sslSocketFactory == null) {
                this.sslSocketFactory = SSLConnectionSocketFactory.getSocketFactory();
            }
            if (this.plainSocketFactory == null) {
                this.plainSocketFactory = PlainConnectionSocketFactory.getSocketFactory();
            }
            if (this.httpIOSessionStrategy == null) {
                this.httpIOSessionStrategy = NoopIOSessionStrategy.INSTANCE;
            }
            if (this.httpsIOSessionStrategy == null) {
                this.httpsIOSessionStrategy = SSLIOSessionStrategy.getSystemDefaultStrategy();
            }

            return new ExtendedHttpClientFactory(this);
        }
    }

}
