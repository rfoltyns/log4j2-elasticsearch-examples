package com.github.rfoltyns;

import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.lookup.AbstractConfigurationAwareLookup;
import org.apache.logging.log4j.core.lookup.StrLookup;

@Plugin(name = "customLookup", category = StrLookup.CATEGORY)
public class CustomLog4j2LookupPlugin extends AbstractConfigurationAwareLookup {

    @Override
    public String lookup(final LogEvent event, final String key) {
        return "Valuable data";
    }

}
