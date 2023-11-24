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
import org.appenders.log4j2.elasticsearch.JacksonDeserializer;
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
import org.appenders.log4j2.elasticsearch.metrics.BasicMetricOutputsRegistry;
import org.appenders.log4j2.elasticsearch.metrics.BasicMetricsRegistry;
import org.appenders.log4j2.elasticsearch.metrics.MetricConfigFactory;
import org.appenders.log4j2.elasticsearch.metrics.MetricLog;
import org.appenders.log4j2.elasticsearch.metrics.MetricType;
import org.appenders.log4j2.elasticsearch.metrics.ScheduledMetricsProcessor;

import java.time.Clock;
import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;

public class NonStopApplication {

    public static final String MODULE_NAME = "log4j2-elasticsearch-hc-one-jar";

    static {
        InternalLogging.setLogger(createLogger());
    }

    // TODO: configure?
    private static final int BATCH_SIZE = 1024;
    private static final int CONCURRENT_BATCHES = 1;
    private static final int ESTIMATED_ITEM_SIZE_IN_BYTES = 1024;

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
                    .withPrefix(MODULE_NAME)
                    .withSeparator(".")
                    .withPattern("yyyy-MM-dd-ss")
                    .build();
    }

    private static ObjectWriter createWriter() {
        return new ExtendedObjectMapper(new MappingJsonFactory()).writer(new MinimalPrettyPrinter());
    }

    private static PooledItemSourceFactory<Event, ByteBuf> createItemSourcePool(String poolName) {
        return createItemSourcePool(poolName, CONCURRENT_BATCHES * BATCH_SIZE, ESTIMATED_ITEM_SIZE_IN_BYTES);
    }

    private static BatchDelivery<ItemSource<ByteBuf>> createBatchDelivery() {
        final PooledItemSourceFactory<Event, ByteBuf> batchItemSourceFactory =
                createItemSourcePool("batchPool", CONCURRENT_BATCHES, ESTIMATED_ITEM_SIZE_IN_BYTES * BATCH_SIZE);
        BackoffPolicy<BatchRequest> backoffPolicy = new BatchLimitBackoffPolicy<>(CONCURRENT_BATCHES);
        return createBatchDelivery(
                BATCH_SIZE,
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
                .withApiVersion(7) // use 8 for ES8
                .withName(MODULE_NAME)
                .withPath("classpath:indexTemplate.json")
                .build();
    }

    private static ClientObjectFactory createClientObjectFactory(PooledItemSourceFactory itemSourceFactory, BackoffPolicy<BatchRequest> backoffPolicy) {

        final ElasticsearchBulkAPI builderFactory = new ElasticsearchBulkAPI();

        final HttpClientProvider clientProvider = new HttpClientProvider(new HttpClientFactory.Builder()
                .withServerList(Collections.singletonList("http://localhost:9200"))
                .withConnTimeout(500)
                .withReadTimeout(20000)
                .withIoThreadCount(CONCURRENT_BATCHES)
                .withMaxTotalConnections(CONCURRENT_BATCHES));

        return new HCHttp.Builder()
                .withClientProvider(clientProvider)
                .withBatchOperations(new HCBatchOperations(itemSourceFactory, builderFactory))
                .withBackoffPolicy(backoffPolicy)
                .withOperationFactory(createSetupOperationFactory(clientProvider))
                .withName("http-main")
                .withMetricConfigs(HCHttp.metricConfigs(true))
                .build();
    }

    private static OperationFactory createSetupOperationFactory(final HttpClientProvider clientProvider) {

        final ObjectReader objectReader = JacksonMappers.responseMapper()
                .readerFor(BatchResult.class);

        final ValueResolver valueResolver = getValueResolver();

        return new ElasticsearchOperationFactory(
                new SyncStepProcessor(clientProvider, new JacksonDeserializer<>(objectReader)),
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
                    .addMixIn(Error.class, ErrorMixIn.class)
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
                .withMetricConfigs(Arrays.asList(
                        MetricConfigFactory.createSuppliedConfig(MetricType.COUNT, true, "available"),
                        MetricConfigFactory.createSuppliedConfig(MetricType.COUNT, true, "total")
                ))
                .build();
    }

    private static BatchDelivery createBatchDelivery(int batchSize, ClientObjectFactory httpObjectFactory, IndexTemplate indexTemplate, FailoverPolicy failoverPolicy) {

        final BasicMetricOutputsRegistry metricOutputs = new BasicMetricOutputsRegistry();
        metricOutputs.register(new MetricLog("example-metrics", new ConsoleLogger()));

        return AsyncBatchDelivery.newBuilder()
                .withClientObjectFactory(httpObjectFactory)
                .withBatchSize(batchSize)
                .withDeliveryInterval(1000)
                .withSetupOpSources(indexTemplate)
                .withFailoverPolicy(failoverPolicy)
                .withShutdownDelayMillis(10000)
                .withMetricProcessor(new ScheduledMetricsProcessor(500, 5000, Clock.systemDefaultZone(), new BasicMetricsRegistry(), metricOutputs))
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

        private final String name = "Hello, World!";

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
