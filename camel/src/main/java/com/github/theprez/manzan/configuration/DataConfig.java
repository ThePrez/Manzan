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
import com.github.theprez.manzan.InstanceContext;
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

    /**
     * Get DataConfig for a specific instance.
     */
    public static DataConfig get(final InstanceContext ctx, final Set<String> destinations)
            throws InvalidFileFormatException, IOException {
        return new DataConfig(getConfigFile(ctx, "data.ini"), ctx, destinations);
    }

    /**
     * Get DataConfig for the default instance.
     *
     * @deprecated Use {@link #get(InstanceContext, Set)} for multi-instance support.
     */
    @Deprecated
    public static DataConfig get(final Set<String> destinations) throws InvalidFileFormatException, IOException {
        return get(InstanceContext.getDefault(), destinations);
    }

    private final InstanceContext m_ctx;
    private final Set<String> m_destinations;

    private Map<String, ManzanRoute> m_routes = null;

    private DataConfig(final File _f, final InstanceContext ctx, final Set<String> _destinations)
            throws InvalidFileFormatException, IOException {
        super(_f);
        m_ctx = ctx;
        m_destinations = _destinations;
    }

    public synchronized Map<String, ManzanRoute> getRoutes() throws IOException, AS400SecurityException,
            ErrorCompletingRequestException, InterruptedException, PropertyVetoException, ObjectDoesNotExistException {
        if (null != m_routes) {
            return m_routes;
        }
        final Map<String, ManzanRoute> ret = new LinkedHashMap<String, ManzanRoute>();
        final List<String> watchEvents = new ArrayList<>();
        final ApplicationConfig appConfig = ApplicationConfig.get(m_ctx);
        final String schema = appConfig.getLibrary();
        // Read the per-instance socket port once so it can be passed to
        // WatchMsgEventSockets without touching the global env var.
        final int socketPort = appConfig.getSocketPort();

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
            final Map<String, String> dataMapInjections = getDataMapInjections(name);

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
                    ret.put(name, new FileEvent(name, file, format, destinations, filter, interval, dataMapInjections));
                    break;
                case "table":
                    final String table = getRequiredString(name, "table");
                    final String tableSchema = getRequiredString(name, "schema");
                    int userNumToProcess = getOptionalInt(name, "numToProcess");
                    int numToProcess = userNumToProcess != -1 ? userNumToProcess : DEFAULT_NUM_TO_PROCESS;
                    ret.put(name, new WatchTableEvent(name, format, destinations, tableSchema, table, interval, numToProcess, dataMapInjections));
                    break;
                case "audit":
                    userNumToProcess = getOptionalInt(name, "numToProcess");
                    numToProcess = userNumToProcess != -1 ? userNumToProcess : DEFAULT_NUM_TO_PROCESS;

                    int fallbackStartTime = getOptionalInt(name, "fallbackStartTime");
                    fallbackStartTime = fallbackStartTime != -1 ? fallbackStartTime : 24;

                    final String userAuditType = getRequiredString(name, "auditType");
                    ret.put(name, new AuditLog(m_ctx, name, format, destinations, interval, numToProcess, userAuditType, fallbackStartTime, dataMapInjections));
                    break;
                case "sql":
                    final String query = getRequiredString(name, "query");
                    ret.put(name, new WatchSql(name, query, format, destinations, interval, dataMapInjections));
                    break;
                case "cmd":
                    final String cmd = getRequiredString(name, "cmd");
                    String args = getOptionalString(name, "args");
                    if (args == null) args = "";

                    ret.put(name, new WatchCmd(name, cmd, args, format, destinations, interval, dataMapInjections));
                    break;
                case "http":
                    final String url = getRequiredString(name, "url");
                    filter = getOptionalString(name, "filter");
                    Map<String, String> headerParams = getUriAndHeaderParameters(name, sectionObj, "url");
                    ret.put(name, new HttpEvent(name, url, format, destinations,filter,  interval, headerParams, dataMapInjections));
                    break;
                case "joblog":
                    final String jobs = getRequiredString(name, "jobs");
                    final List<String> jobIdentifiers = new LinkedList<>();
                    for (String jobId : jobs.split("\\s*,\\s*")) {
                        jobId = jobId.trim();
                        if (StringUtils.isNonEmpty(jobId)) {
                            jobIdentifiers.add(jobId);
                        }
                    }
                    if (jobIdentifiers.isEmpty()) {
                        throw new RuntimeException("No valid job identifiers specified for joblog data source '" + name + "'");
                    }
                    ret.put(name, new WatchJobLog(name, jobIdentifiers, format, destinations, interval, dataMapInjections));
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
        final Map<String, Map<String, String>> dataMapInjectionsMap = new HashMap<>();



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
                final Map<String, String> dataMapInjections = getDataMapInjections(name);

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

                // Create the watcher first so the generated SESSION_ID is known before
                // populating the maps. The socket listener and SQL route both look up
                // messages by the session ID that STRWCH/HANDLER actually writes —
                // which is the generated ID, not the raw 'id' from data.ini.
                WatchStarter ws = new WatchStarter(m_ctx, id, strwch);
                final String sessionId = ws.getSessionId().trim().toUpperCase();

                // Build the maps keyed by the generated session ID so that
                // WatchMsgEventSockets can match incoming socket messages correctly.
                String destString = createRecipientList(destinations);
                formatMap.put(sessionId, format);
                destMap.put(sessionId, destString);
                eventMap.put(sessionId, eventType);
                dataMapInjectionsMap.put(sessionId, dataMapInjections);

                String sqlRouteName = name + "sql";
                ret.put(sqlRouteName, new WatchMsgEventSql(sqlRouteName, ws.getSessionId(), format, destinations, schema, eventType,
                        interval, numToProcess, dataMapInjections));
                ws.strwch();
                continue;
            }

            WatchStarter ws = new WatchStarter(m_ctx, id, strwch);
            ws.strwch();
        }

        if (watchEvents.size() > 0) {
            // After iterating over the loop, the formatMap and destMap are complete. Now
            // create the route.
            final String routeName = "socketWatcher";
            ret.put(routeName, new WatchMsgEventSockets(routeName, socketPort, formatMap, destMap, eventMap, dataMapInjectionsMap));
        }
        return m_routes = ret;
    }
}
