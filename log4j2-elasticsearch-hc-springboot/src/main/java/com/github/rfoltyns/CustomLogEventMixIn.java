package com.github.rfoltyns;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.JsonUnwrapped;
import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.time.Instant;
import org.appenders.log4j2.elasticsearch.json.jackson.ExtendedLogEventJacksonJsonMixIn;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

@JsonPropertyOrder({ "instant", "loggerName", "level", "marker", "message", "thrown", "threadName"})
@JsonSerialize(as = LogEvent.class)
public abstract class CustomLogEventMixIn extends ExtendedLogEventJacksonJsonMixIn {

    @JsonIgnore
    @Override
    public abstract long getTimeMillis();

    @JsonProperty("@timestamp")
    @JsonIgnore(value = false)
    @JsonUnwrapped
    @JsonSerialize(using = InstantSerializer.class)
    @Override
    public abstract Instant getInstant();

    public static class InstantSerializer extends JsonSerializer<Instant> {

        private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ISO_DATE_TIME;

        @Override
        public void serialize(Instant value, JsonGenerator gen, SerializerProvider serializers) throws IOException {

            final java.time.Instant instant = java.time.Instant.ofEpochMilli(value.getEpochMillisecond());
            final LocalDateTime dateTime = LocalDateTime.ofInstant(instant, ZoneId.of("UTC"));
            gen.writeString(FORMATTER.format(dateTime));

        }

    }

}
