package com.github.rfoltyns;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.appenders.log4j2.elasticsearch.json.jackson.ExtendedLogEventJacksonJsonMixIn;

public abstract class CustomLogEventJacksonJsonMixIn extends ExtendedLogEventJacksonJsonMixIn {

    private static final long serialVersionUID = 1L;

    @JsonProperty("@timestamp")
    @Override
    public abstract long getTimeMillis();

}

