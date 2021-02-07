package org.appenders.log4j2.elasticsearch.hc;

import org.apache.logging.log4j.core.config.Node;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginBuilderFactory;
import org.appenders.log4j2.elasticsearch.ClientObjectFactory;
import org.appenders.log4j2.elasticsearch.ClientProvider;

import java.util.Arrays;

@Plugin(name = "CustomHCHttp", category = Node.CATEGORY, elementType = ClientObjectFactory.ELEMENT_TYPE, printObject = true)
public class ExtendedHCHttp extends HCHttp {

    private final ClientProvider<HttpClient> clientProvider;

    public ExtendedHCHttp(Builder builder, ClientProvider<HttpClient> clientProvider) {
        super(builder);
        this.clientProvider = clientProvider;
    }

    @PluginBuilderFactory
    public static Builder newExtendedBuilder() {
        return new Builder();
    }

    @Override
    public HttpClient createClient() {
        return clientProvider.createClient();
    }

    public static class Builder extends HCHttp.Builder implements org.apache.logging.log4j.core.util.Builder<HCHttp> {

        @Override
        public HCHttp build() {
            super.validate();

            ClientProvider<HttpClient> clientProvider = new ClientProvider<HttpClient>() {

                private HttpClient httpClient;

                @Override
                public HttpClient createClient() {
                    if (httpClient == null) {
                        ExtendedHttpClientFactory.Builder builder = (ExtendedHttpClientFactory.Builder) new ExtendedHttpClientFactory.Builder()
                                .withServerList(Arrays.asList(serverUris.split(";")))
                                .withConnTimeout(connTimeout)
                                .withReadTimeout(readTimeout)
                                .withMaxTotalConnections(maxTotalConnections)
                                .withIoThreadCount(ioThreadCount)
                                .withPooledResponseBuffers(pooledResponseBuffers)
                                .withPooledResponseBuffersSizeInBytes(pooledResponseBuffersSizeInBytes);
                        if (Builder.this.auth != null) {
                            auth.configure(builder);
                        }
                        httpClient = builder.build().createInstance();
                    }
                    return httpClient;
                }

            };
            return new ExtendedHCHttp(this, clientProvider);

        }

    }

}
