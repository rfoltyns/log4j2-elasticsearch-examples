package com.github.rfoltyns;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonUnwrapped;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.apache.logging.log4j.core.impl.MutableLogEvent;
import org.appenders.log4j2.elasticsearch.json.jackson.ExtendedLogEventJacksonJsonMixIn;

@JsonSerialize(as = MutableLogEvent.class)
public abstract class CustomLogEventMixIn extends ExtendedLogEventJacksonJsonMixIn {

    @JsonUnwrapped
    @JsonSerialize(using = RequestLogParamSerializer.class)
    public abstract Object[] getParameters();

    @JsonIgnore
    @Override
    public abstract long getTimeMillis();

}
