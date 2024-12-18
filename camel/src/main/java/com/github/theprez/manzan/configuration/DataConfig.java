package com.github.theprez.manzan.configuration;

import java.beans.PropertyVetoException;
import java.io.File;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.ini4j.InvalidFileFormatException;

import com.github.theprez.jcmdutils.StringUtils;
import com.github.theprez.manzan.ManzanEventType;
import com.github.theprez.manzan.ManzanEventType;
import com.github.theprez.manzan.WatchStarter;
import com.github.theprez.manzan.routes.ManzanRoute;
import com.github.theprez.manzan.routes.event.FileEvent;
import com.github.theprez.manzan.routes.event.WatchMsgEventSockets;
import com.github.theprez.manzan.routes.event.WatchMsgEventSql;
import com.ibm.as400.access.AS400SecurityException;
import com.ibm.as400.access.ErrorCompletingRequestException;
import com.ibm.as400.access.ObjectDoesNotExistException;

import static com.github.theprez.manzan.routes.ManzanRoute.createRecipientList;

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
        final List<String> watchEvents = new ArrayList<>();
        final String schema = ApplicationConfig.get().getLibrary();

        for (final String section : getIni().keySet()) {
            final String type = getIni().get(section, "type");
            if (StringUtils.isEmpty(type)) {
                throw new RuntimeException("Type not specified for data source [" + section + "]");
            }
            if ("false".equalsIgnoreCase(getIni().get(section, "enabled"))) {
                continue;
            } else if ( type.equals("watch")){
                // We will handle the watch events separately as the logic is a bit more complicated
                watchEvents.add(section);
                continue;
            }
            final String name = section;
            final String format = getOptionalString(name, "format");
            int userInterval = getOptionalInt(name, "interval");
            final int interval = userInterval != -1 ? userInterval : DEFAULT_INTERVAL;
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
                case "file":
                    String file = getRequiredString(name, "file");
                    String filter = getOptionalString(name, "filter");
                    ret.put(name, new FileEvent(name, file, format, destinations, filter, interval));
                    break;
                default:
                    throw new RuntimeException("Unknown destination type: " + type);
            }
        }

        // We will create a formatMap to store the format for each watch session, as well
        // as a destMap to store the destinations for each watch session
        final Map<String, String> formatMap = new HashMap<>();
        final Map<String, String> destMap = new HashMap<>();

        for (int i = 0; i < watchEvents.size(); i++) {
            final String section = watchEvents.get(i);
            final String name = section;
            int userNumToProcess = getOptionalInt(name, "numToProcess");
            final int numToProcess = userNumToProcess != -1 ? userNumToProcess : DEFAULT_NUM_TO_PROCESS;
            final String format = getOptionalString(name, "format");
            String strwch = getRequiredString(name, "strwch");
            String id = getRequiredString(name, "id");

            ManzanEventType eventType;
            if(strwch.contains("WCHMSGQ")) {
                eventType = ManzanEventType.WATCH_MSG;
            } else if(strwch.contains("WCHLICLOG")) {
                eventType = ManzanEventType.WATCH_VLOG;
            } else if(strwch.contains("WCHPAL")) {
                eventType = ManzanEventType.WATCH_PAL;
            } else {
                throw new RuntimeException("Watch for message, LIC log entry, or PAL entry not specified");
            }

            int userInterval = getOptionalInt(name, "interval");
            final int interval = userInterval != -1 ? userInterval : DEFAULT_INTERVAL;
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

            // Build the maps
            String destString = createRecipientList(destinations);
            formatMap.put(id.toUpperCase(), format);
            destMap.put(id.toUpperCase(), destString);

            String sqlRouteName = name + "sql";
            ret.put(sqlRouteName, new WatchMsgEventSql(sqlRouteName, id, format, destinations, schema, eventType, interval, numToProcess));
            WatchStarter ws = new WatchStarter(id, strwch);
            ws.strwch();
        }

        if (watchEvents.size() > 0){
            // After iterating over the loop, the formatMap and destMap are complete. Now create the route.
            final String routeName = "socketWatcher";
            ret.put(routeName, new WatchMsgEventSockets(routeName, formatMap, destMap));
        }
        return m_routes = ret;
    }
}
