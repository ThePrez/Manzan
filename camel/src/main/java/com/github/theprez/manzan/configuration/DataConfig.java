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

import com.github.theprez.manzan.routes.event.*;
import org.ini4j.InvalidFileFormatException;

import com.github.theprez.jcmdutils.StringUtils;
import com.github.theprez.manzan.ManzanEventType;
import com.github.theprez.manzan.WatchStarter;
import com.github.theprez.manzan.routes.ManzanRoute;
import com.ibm.as400.access.AS400SecurityException;
import com.ibm.as400.access.ErrorCompletingRequestException;
import com.ibm.as400.access.ObjectDoesNotExistException;
import org.ini4j.Profile;

import static com.github.theprez.manzan.routes.ManzanRoute.createRecipientList;

public class DataConfig extends Config {

    private final static int DEFAULT_INTERVAL = 1000;
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
            } else if (type.equals("watch")) {
                // We will handle the watch events separately as the logic is a bit more complicated
                watchEvents.add(section);
                continue;
            }
            final String name = section;
            final Profile.Section sectionObj = getIni().get(name);
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
                case "table":
                    final String table = getRequiredString(name, "table");
                    final String tableSchema = getRequiredString(name, "schema");
                    int userNumToProcess = getOptionalInt(name, "numToProcess");
                    int numToProcess = userNumToProcess != -1 ? userNumToProcess : DEFAULT_NUM_TO_PROCESS;
                    ret.put(name, new WatchTableEvent(name, format, destinations, tableSchema, table, interval, numToProcess));
                    break;
                case "audit":
                    userNumToProcess = getOptionalInt(name, "numToProcess");
                    numToProcess = userNumToProcess != -1 ? userNumToProcess : DEFAULT_NUM_TO_PROCESS;

                    int fallbackStartTime = getOptionalInt(name, "fallbackStartTime");
                    fallbackStartTime = fallbackStartTime != -1 ? fallbackStartTime : 24;

                    final String userAuditType = getRequiredString(name, "auditType");
                    ret.put(name, new AuditLog(name, format, destinations, interval, numToProcess, userAuditType, fallbackStartTime));
                    break;
                case "sql":
                    final String query = getRequiredString(name, "query");
                    ret.put(name, new WatchSql(name, query, format, destinations, interval));
                    break;
                case "cmd":
                    final String cmd = getRequiredString(name, "cmd");
                    String args = getOptionalString(name, "args");
                    if (args == null) args = "";

                    ret.put(name, new WatchCmd(name, cmd, args, format, destinations, interval));
                    break;
                case "http":
                    final String url = getRequiredString(name, "url");
                    filter = getOptionalString(name, "filter");
                    Map<String, String> headerParams = getUriAndHeaderParameters(name, sectionObj, "url");
                    ret.put(name, new HttpEvent(name, url, format, destinations,filter,  interval, headerParams));
                    break;
                default:
                    throw new RuntimeException("Unknown destination type: " + type);
            }
        }

        // We will create a formatMap to store the format for each watch session, as well
        // as a destMap to store the destinations for each watch session
        // and an eventMap to start the eventType for each watch session
        final Map<String, String> formatMap = new HashMap<>();
        final Map<String, String> destMap = new HashMap<>();
        final Map<String, ManzanEventType> eventMap = new HashMap<>();


        for (int i = 0; i < watchEvents.size(); i++) {
            final String section = watchEvents.get(i);
            final String name = section;

            // Required fields
            String id = getRequiredString(name, "id");
            String strwch = getRequiredString(name, "strwch");

            String userDestinations = getOptionalString(name, "destinations");
            if (userDestinations != null) {
                // Optional fields
                int userNumToProcess = getOptionalInt(name, "numToProcess");
                final int numToProcess = userNumToProcess != -1 ? userNumToProcess : DEFAULT_NUM_TO_PROCESS;
                int userInterval = getOptionalInt(name, "interval");
                final int interval = userInterval != -1 ? userInterval : DEFAULT_INTERVAL;
                final String format = getOptionalString(name, "format");

                // Determine the event type
                ManzanEventType eventType;
                if (strwch.contains("WCHMSGQ")) {
                    eventType = ManzanEventType.WATCH_MSG;
                } else if (strwch.contains("WCHLICLOG")) {
                    eventType = ManzanEventType.WATCH_VLOG;
                } else if (strwch.contains("WCHPAL")) {
                    eventType = ManzanEventType.WATCH_PAL;
                } else {
                    throw new RuntimeException("Watch for message, LIC log entry, or PAL entry not specified");
                }
                eventMap.put(id.toUpperCase(), eventType);

                // Process the destinations
                final List<String> destinations = new LinkedList<String>();
                for (String d : userDestinations.split("\\s*,\\s*")) {
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
                ret.put(sqlRouteName, new WatchMsgEventSql(sqlRouteName, id, format, destinations, schema, eventType,
                        interval, numToProcess));
            }

            // Create the watcher
            WatchStarter ws = new WatchStarter(id, strwch);
            ws.strwch();
        }

        if (watchEvents.size() > 0) {
            // After iterating over the loop, the formatMap and destMap are complete. Now
            // create the route.
            final String routeName = "socketWatcher";
            ret.put(routeName, new WatchMsgEventSockets(routeName, formatMap, destMap, eventMap));
        }
        return m_routes = ret;
    }
}
