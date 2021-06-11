package com.github.rfoltyns;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class RequestLog {

    @JsonProperty("http_verb")
    private final String verb;

    @JsonProperty("uri")
    private final String uri;

    @JsonProperty("query")
    private final String query;

    @JsonProperty("body")
    private final String body;

    @JsonProperty("io_type")
    private final String type;


    public RequestLog(String verb, String uri, String query, String body, String type) {
        this.verb = verb;
        this.uri = uri;
        this.query = query;
        this.body = body;
        this.type = type == null ? "OUT_REQUEST" : type;
    }

    public String getVerb() {
        return verb;
    }

    public String getUri() {
        return uri;
    }

    public String getQuery() {
        return query;
    }

    public String getBody() {
        return body;
    }

    public String getType() {
        return type;
    }
}
