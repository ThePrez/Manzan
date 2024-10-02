package com.github.theprez.manzan.configuration;

import java.beans.PropertyVetoException;
import java.io.File;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.ini4j.InvalidFileFormatException;

import com.github.theprez.jcmdutils.StringUtils;
import com.github.theprez.manzan.ManzanEventType;
import com.github.theprez.manzan.WatchStarter;
import com.github.theprez.manzan.routes.ManzanRoute;
import com.github.theprez.manzan.routes.event.FileEvent;
import com.github.theprez.manzan.routes.event.WatchMsgEvent;
import com.ibm.as400.access.AS400SecurityException;
import com.ibm.as400.access.ErrorCompletingRequestException;
import com.ibm.as400.access.ObjectDoesNotExistException;

public class DataConfig extends Config {

    private final static int DEFAULT_INTERVAL = 5;
    private final static int DEFAULT_NUM_TO_PROCESS = 1000;

    public static DataConfig get(final Set<String> _destinations) throws InvalidFileFormatException, IOException {
        return new DataConfig(getConfigFile("data.ini"), _destinations);
    }

    private final Set<String> m_destinations;

    private Map<String, ManzanRoute> m_routes = null;

    private DataConfig(final File _f, final Set<String> _destinations) throws InvalidFileFormatException, IOException {
        super(_f);
        m_destinations = _destinations;
    }

    public synchronized Map<String, ManzanRoute> getRoutes() throws IOException, AS400SecurityException,
            ErrorCompletingRequestException, InterruptedException, PropertyVetoException, ObjectDoesNotExistException {
        if (null != m_routes) {
            return m_routes;
        }
        final Map<String, ManzanRoute> ret = new LinkedHashMap<String, ManzanRoute>();
        for (final String section : getIni().keySet()) {
            final String type = getIni().get(section, "type");
            if (StringUtils.isEmpty(type)) {
                throw new RuntimeException("Type not specified for data source [" + section + "]");
            }
            if ("false".equalsIgnoreCase(getIni().get(section, "enabled"))) {
                continue;
            }
            final String name = section;
            final String schema = ApplicationConfig.get().getLibrary();
            final String format = getOptionalString(name, "format");
            int userInterval = getOptionalInt(name, "interval");
            final int interval = userInterval != -1 ? userInterval : DEFAULT_INTERVAL;
            int userNumToProcess = getOptionalInt(name, "numToProcess");
            final int numToProcess = userNumToProcess != -1 ? userNumToProcess : DEFAULT_NUM_TO_PROCESS;
            final List<String> destinations = new LinkedList<String>();
            for (String d : getRequiredString(name, "destinations").split("\\s*,\\s*")) {
                d = d.trim();
                if (!m_destinations.contains(d)) {
                    throw new RuntimeException(
                            "No destination configured named '" + d + "' for data source '" + name + "'");
                }
                if (StringUtils.isNonEmpty(d)) {
                    destinations.add(d);
                }
            }
            switch (type) {
                case "watch":
                    String id = getRequiredString(name, "id");
                    String strwch = getRequiredString(name, "strwch");
                    boolean isValidStrwch = false;
                    if (strwch.contains("WCHMSGQ")) {
                        ret.put(name, new WatchMsgEvent(name, id, format, destinations, schema,
                                ManzanEventType.WATCH_MSG, interval, numToProcess));
                        isValidStrwch = true;
                    }
                    if (strwch.contains("WCHLICLOG")) {
                        ret.put(name, new WatchMsgEvent(name, id, format, destinations, schema,
                                ManzanEventType.WATCH_VLOG, interval, numToProcess));
                        isValidStrwch = true;
                    }
                    if (strwch.contains("WCHPAL")) {
                        ret.put(name, new WatchMsgEvent(name, id, format, destinations, schema,
                                ManzanEventType.WATCH_PAL, interval, numToProcess));
                        isValidStrwch = true;
                    }
                    if (isValidStrwch && StringUtils.isNonEmpty(strwch)) {
                        WatchStarter ws = new WatchStarter(id, strwch);
                        ws.strwch();
                    } else {
                        throw new RuntimeException("Watch for message, LIC log entry, or PAL entry not specified");
                    }
                    break;
                case "file":
                    String file = getRequiredString(name, "file");
                    String filter = getOptionalString(name, "filter");
                    ret.put(name, new FileEvent(name, file, format, destinations, filter, interval));
                    break;
                default:
                    throw new RuntimeException("Unknown destination type: " + type);
            }
        }
        return m_routes = ret;
    }

}
