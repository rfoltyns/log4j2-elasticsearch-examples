package com.github.rfoltyns;

import org.apache.http.client.methods.HttpUriRequest;
import org.appenders.log4j2.elasticsearch.hc.HCRequestFactory;
import org.appenders.log4j2.elasticsearch.hc.Request;

import java.io.IOException;

public class ExtendedHCRequestFactory extends HCRequestFactory {

    @Override
    public HttpUriRequest create(String url, Request request) throws IOException {
        HttpUriRequest httpUriRequest = super.create(url, request);

        // your headers can be set here
        httpUriRequest.addHeader("Custom-Header", "Custom-Header-Value");

        return httpUriRequest;
    }

}
