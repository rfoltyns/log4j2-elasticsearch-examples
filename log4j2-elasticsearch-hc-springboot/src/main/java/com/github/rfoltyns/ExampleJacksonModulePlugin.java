package com.github.rfoltyns;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.apache.logging.log4j.core.config.Node;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginBuilderFactory;
import org.appenders.log4j2.elasticsearch.JacksonModule;

import static org.appenders.core.logging.InternalLogging.getLogger;

@Plugin(name = ExampleJacksonModulePlugin.PLUGIN_NAME, category = Node.CATEGORY, elementType = "JacksonModule", printObject = true)
public class ExampleJacksonModulePlugin extends SimpleModule implements JacksonModule {

    private static final long serialVersionUID = 1L;

    public static final String PLUGIN_NAME = "ExampleJacksonModule";

    @Override
    public void applyTo(ObjectMapper objectMapper) {
        // Uncomment to register your module
        objectMapper.registerModule(new JavaTimeModule());
        JsonMapper.builder().disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        // ...
        // And log whatever useful info you need. Or not..?
        getLogger().info("{} applied", getClass().getSimpleName());
    }

    @PluginBuilderFactory
    public static ExampleJacksonModulePlugin.Builder newBuilder() {
        return new ExampleJacksonModulePlugin.Builder();
    }

    public static class Builder implements org.apache.logging.log4j.core.util.Builder<ExampleJacksonModulePlugin> {

        @Override
        public ExampleJacksonModulePlugin build() {
            return new ExampleJacksonModulePlugin();
        }

    }

}