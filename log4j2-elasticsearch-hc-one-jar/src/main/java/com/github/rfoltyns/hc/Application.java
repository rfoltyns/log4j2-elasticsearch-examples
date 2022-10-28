package com.github.rfoltyns.hc;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.util.MinimalPrettyPrinter;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MappingJsonFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.introspect.VisibilityChecker;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.UnpooledByteBufAllocator;
import org.appenders.core.logging.InternalLogging;
import org.appenders.core.logging.Logger;
import org.appenders.log4j2.elasticsearch.AsyncBatchDelivery;
import org.appenders.log4j2.elasticsearch.BatchDelivery;
import org.appenders.log4j2.elasticsearch.ByteBufBoundedSizeLimitPolicy;
import org.appenders.log4j2.elasticsearch.ByteBufPooledObjectOps;
import org.appenders.log4j2.elasticsearch.ClientObjectFactory;
import org.appenders.log4j2.elasticsearch.ExtendedObjectMapper;
import org.appenders.log4j2.elasticsearch.FailoverPolicy;
import org.appenders.log4j2.elasticsearch.IndexTemplate;
import org.appenders.log4j2.elasticsearch.ItemSource;
import org.appenders.log4j2.elasticsearch.JacksonSerializer;
import org.appenders.log4j2.elasticsearch.MillisFormatter;
import org.appenders.log4j2.elasticsearch.NoopFailoverPolicy;
import org.appenders.log4j2.elasticsearch.OperationFactory;
import org.appenders.log4j2.elasticsearch.PooledItemSourceFactory;
import org.appenders.log4j2.elasticsearch.RollingMillisFormatter;
import org.appenders.log4j2.elasticsearch.ValueResolver;
import org.appenders.log4j2.elasticsearch.backoff.BackoffPolicy;
import org.appenders.log4j2.elasticsearch.backoff.BatchLimitBackoffPolicy;
import org.appenders.log4j2.elasticsearch.failover.ChronicleMapRetryFailoverPolicy;
import org.appenders.log4j2.elasticsearch.failover.KeySequenceSelector;
import org.appenders.log4j2.elasticsearch.failover.SingleKeySequenceSelector;
import org.appenders.log4j2.elasticsearch.hc.BatchItemResult;
import org.appenders.log4j2.elasticsearch.hc.BatchItemResultMixIn;
import org.appenders.log4j2.elasticsearch.hc.BatchRequest;
import org.appenders.log4j2.elasticsearch.hc.BatchResult;
import org.appenders.log4j2.elasticsearch.hc.BatchResultMixIn;
import org.appenders.log4j2.elasticsearch.hc.ElasticsearchBulkAPI;
import org.appenders.log4j2.elasticsearch.hc.ElasticsearchOperationFactory;
import org.appenders.log4j2.elasticsearch.hc.ErrorMixIn;
import org.appenders.log4j2.elasticsearch.hc.HCBatchOperations;
import org.appenders.log4j2.elasticsearch.hc.HCHttp;
import org.appenders.log4j2.elasticsearch.hc.HttpClientFactory;
import org.appenders.log4j2.elasticsearch.hc.HttpClientProvider;
import org.appenders.log4j2.elasticsearch.hc.SyncStepProcessor;

import java.util.Collections;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;

public class Application {

    static {
        InternalLogging.setLogger(createLogger());
    }

    private static int batchSize = 1000;
    private static int concurrentBatches = 3;
    private static int estimatedItemSizeInBytes = 1024;

    public static void main(String[] args) {
        System.out.println("Start");

        // index name formatter
        final MillisFormatter indexNameFormatter = createIndexNameFormatter();
        System.out.println("IndexNameFormatter created");

        // batch handling
        final BatchDelivery batchDelivery = createBatchDelivery();
        System.out.println("BatchDelivery created");

        // event serialisation
        final PooledItemSourceFactory<Event, ByteBuf> itemSourceFactory = createItemSourcePool("itemPool");
        System.out.println("ItemSourceFactory created");

        final ObjectWriter writer = createWriter();

        // handle lifecycle
        itemSourceFactory.start();
        batchDelivery.start();

        final JacksonSerializer<Event> serializer = new JacksonSerializer<>(writer);

        final Event event = new Event();

        // add to batch
        batchDelivery.add(indexNameFormatter.format(event.getTimestamp()), itemSourceFactory.create(event, serializer));

        System.out.println("Event added to batch");

        // wait for delivery
        LockSupport.parkNanos(TimeUnit.SECONDS.toNanos(5));

        // handle lifecycle
        batchDelivery.stop();
        itemSourceFactory.stop();

        System.out.println("Done");

    }

    private static Logger createLogger() {
        return new ConsoleLogger();
    }

    private static MillisFormatter createIndexNameFormatter() {
        return new RollingMillisFormatter.Builder()
                    .withPrefix("log4j2-elasticsearch-one-jar")
                    .withSeparator(".")
                    .withPattern("yyyy-MM-dd-ss")
                    .build();
    }

    private static ObjectWriter createWriter() {
        return new ExtendedObjectMapper(new MappingJsonFactory()).writer(new MinimalPrettyPrinter());
    }

    private static PooledItemSourceFactory<Event, ByteBuf> createItemSourcePool(String poolName) {
        return createItemSourcePool(poolName, concurrentBatches * batchSize, estimatedItemSizeInBytes);
    }

    private static BatchDelivery<ItemSource<ByteBuf>> createBatchDelivery() {
        final PooledItemSourceFactory<Event, ByteBuf> batchItemSourceFactory =
                createItemSourcePool("batchPool", concurrentBatches, estimatedItemSizeInBytes * batchSize);
        BackoffPolicy<BatchRequest> backoffPolicy = new BatchLimitBackoffPolicy<>(concurrentBatches);
        return createBatchDelivery(
                batchSize,
                createClientObjectFactory(batchItemSourceFactory, backoffPolicy),
                createIndexTemplate(),
                createNoopFailoverPolicy()
        );
    }

    private static FailoverPolicy createNoopFailoverPolicy() {
        return new NoopFailoverPolicy();
    }

    private static IndexTemplate createIndexTemplate() {
        return new IndexTemplate.Builder()
                .withApiVersion(7)
                .withName("log4j2-elasticsearch-programmatic-test-template")
                .withPath("classpath:indexTemplate.json")
                .build();
    }

    private static ClientObjectFactory createClientObjectFactory(PooledItemSourceFactory itemSourceFactory, BackoffPolicy<BatchRequest> backoffPolicy) {

        final ElasticsearchBulkAPI builderFactory = new ElasticsearchBulkAPI(null);

        final HttpClientProvider clientProvider = new HttpClientProvider(new HttpClientFactory.Builder()
                .withServerList(Collections.singletonList("http://localhost:9200"))
                .withConnTimeout(500)
                .withReadTimeout(20000)
                .withIoThreadCount(concurrentBatches)
                .withMaxTotalConnections(concurrentBatches));

        return new HCHttp.Builder()
                .withClientProvider(clientProvider)
                .withBatchOperations(new HCBatchOperations(itemSourceFactory, builderFactory))
                .withBackoffPolicy(backoffPolicy)
                .withOperationFactory(createSetupOperationFactory(clientProvider))
                .build();
    }

    private static OperationFactory createSetupOperationFactory(final HttpClientProvider clientProvider) {

        final ObjectReader objectReader = JacksonMappers.responseMapper()
                .readerFor(BatchResult.class);

        final ValueResolver valueResolver = getValueResolver();

        return new ElasticsearchOperationFactory(
                new SyncStepProcessor(clientProvider, objectReader),
                valueResolver);

    }

    private static ValueResolver getValueResolver() {
        return ValueResolver.NO_OP;
    }

    private static class JacksonMappers {

        public static ObjectMapper responseMapper() {
            return new ObjectMapper()
                    .setVisibility(VisibilityChecker.Std.defaultInstance().with(JsonAutoDetect.Visibility.ANY))
                    .setSerializationInclusion(JsonInclude.Include.NON_EMPTY)
                    .configure(SerializationFeature.CLOSE_CLOSEABLE, false)
                    .configure(DeserializationFeature.ACCEPT_EMPTY_ARRAY_AS_NULL_OBJECT, true)
                    .addMixIn(BatchResult.class, BatchResultMixIn.class)
                    .addMixIn(java.lang.Error.class, ErrorMixIn.class)
                    .addMixIn(BatchItemResult.class, BatchItemResultMixIn.class);
        }

    }

    private static KeySequenceSelector createKeySequenceSelector() {
        return new SingleKeySequenceSelector(1);
    }

    private static PooledItemSourceFactory<Event, ByteBuf> createItemSourcePool(String poolName, int initialPoolSize, int estimatedItemSizeInBytes) {
        return new PooledItemSourceFactory.Builder<Event, ByteBuf>()
                .withPoolName(poolName)
                .withInitialPoolSize(initialPoolSize)
                .withPooledObjectOps(new ByteBufPooledObjectOps(UnpooledByteBufAllocator.DEFAULT, new ByteBufBoundedSizeLimitPolicy(estimatedItemSizeInBytes, estimatedItemSizeInBytes)))
                .withMonitored(true)
                .withMonitorTaskInterval(10000)
                .build();
    }

    private static BatchDelivery createBatchDelivery(int batchSize, ClientObjectFactory httpObjectFactory, IndexTemplate indexTemplate, FailoverPolicy failoverPolicy) {
        return AsyncBatchDelivery.newBuilder()
                    .withClientObjectFactory(httpObjectFactory)
                    .withBatchSize(batchSize)
                    .withDeliveryInterval(1000)
                    .withSetupOpSources(indexTemplate)
                    .withFailoverPolicy(failoverPolicy)
                    .withShutdownDelayMillis(10000)
                    .build();
    }

    private static ChronicleMapRetryFailoverPolicy createChronicleMapFailoverPolicy(KeySequenceSelector keySequenceSelector) {
        return new ChronicleMapRetryFailoverPolicy.Builder()
                .withKeySequenceSelector(keySequenceSelector)
                .withFileName("failedItems.chronicleMap")
                .withNumberOfEntries(100000)
                .withAverageValueSize(2048)
                .withBatchSize(5000)
                .withRetryDelay(4000)
                .withMonitored(true)
                .withMonitorTaskInterval(1000)
                .build();
    }

    static class Event {

        private final long timestamp = System.currentTimeMillis();

        private final String name = "fatjar-event";

        @JsonProperty("timeMillis")
        public long getTimestamp() {
            return timestamp;
        }

        @JsonProperty("message")
        public String getName() {
            return name;
        }

    }
}
