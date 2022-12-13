package com.github.theprez.manzan.configuration;

import java.io.File;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.ini4j.InvalidFileFormatException;

import com.github.theprez.jcmdutils.StringUtils;
import com.github.theprez.manzan.routes.ManzanRoute;
import com.github.theprez.manzan.routes.event.FileEvent;
import com.github.theprez.manzan.routes.event.WatchMsgEvent;

public class DataConfig extends Config {

    private final Set<String> m_destinations;
    private Map<String, ManzanRoute> m_routes = null;

    public DataConfig(final File _f, Set<String> _destinations) throws InvalidFileFormatException, IOException {
        super(_f);
        m_destinations = _destinations;
    }

    public synchronized Map<String, ManzanRoute> getRoutes() throws IOException {
        if(null != m_routes) {
            return m_routes;
        }
        final Map<String, ManzanRoute> ret = new LinkedHashMap<String, ManzanRoute>();
        for (final String section : getIni().keySet()) {
            final String type = getIni().get(section, "type");
            if (StringUtils.isEmpty(type)) {
                throw new RuntimeException("type not specified for data source [" + section + "]");
            }
            if("false".equalsIgnoreCase(getIni().get(section, "enabled"))) {
                continue;
            }
            final String name = section;
            String schema = "JESSEG"; //TODO: get from configuration
            int interval = 5; //TODO: get from configuration
            int numToProcess = 1000; //TODO: get from configuration
            List<String> destinations = new LinkedList<String>();
            for(String d: getRequiredString(name, "destinations").split("\\s*,\\s*")) {
                d=d.trim();
                if(!m_destinations.contains(d)) {
                    throw new RuntimeException("no destination configured named '"+d+"' for data source '"+name+"'");
                }
                if(StringUtils.isNonEmpty(d)) destinations.add(d);
            }
            switch (type) {
                case "watch":
                    ret.put(name, new WatchMsgEvent(name, getRequiredString(name, "id"), destinations, schema, interval, numToProcess));
                    break;
                case "file":
                    ret.put(name, new FileEvent(name, getRequiredString(name, "file"), destinations, getOptionalString(name,"filter")));
                    break;
                default:
                    throw new RuntimeException("Unknown destination type: " + type);
            }
        }
        return m_routes = ret;
    }

}
