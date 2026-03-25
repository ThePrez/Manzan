package com.github.theprez.manzan.routes.event;

import java.io.IOException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.*;

import com.github.theprez.jcmdutils.StringUtils;
import com.github.theprez.manzan.ManzanEventType;
import com.github.theprez.manzan.ManzanMessageFormatter;
import com.github.theprez.manzan.routes.ManzanRoute;

public class WatchJobLog extends ManzanRoute {
    private final int m_interval;
    private final ManzanMessageFormatter m_formatter;
    private final List<String> m_jobIdentifiers;
    private final Map<String, Timestamp> m_lastCheckTimestamps;
    private final Map<String, String> dataMapInjection;

    public WatchJobLog(final String _name, final List<String> _jobIdentifiers, final String _format,
                       final List<String> _destinations, final int _interval, 
                       final Map<String, String> _dataMapInjection) throws IOException {
        super(_name);
        m_interval = _interval;
        m_formatter = StringUtils.isEmpty(_format) ? null : new ManzanMessageFormatter(_format);
        m_jobIdentifiers = _jobIdentifiers;
        m_lastCheckTimestamps = new HashMap<>();
        dataMapInjection = _dataMapInjection;
        
        // Initialize timestamps for each job to current time minus 1 minute
        Timestamp initialTimestamp = Timestamp.from(Instant.now().minusSeconds(60));
        for (String jobId : m_jobIdentifiers) {
            m_lastCheckTimestamps.put(jobId, initialTimestamp);
        }
        
        super.setRecipientList(_destinations);
        setEventType(ManzanEventType.JOBLOG);
    }

    protected void setEventType(ManzanEventType eventType) {
        m_eventType = eventType;
    }

    /**
     * Build SQL query to fetch job log entries for all monitored jobs
     * Uses QSYS2.JOBLOG_INFO table function
     */
    private String buildJobLogQuery() {
        StringBuilder sql = new StringBuilder();
        
        for (int i = 0; i < m_jobIdentifiers.size(); i++) {
            String jobId = m_jobIdentifiers.get(i);
            Timestamp lastCheck = m_lastCheckTimestamps.get(jobId);
            
            if (i > 0) {
                sql.append(" UNION ALL ");
            }
            
            sql.append("SELECT ");
            sql.append("'").append(jobId).append("' AS JOB_FULL, ");
            sql.append("MESSAGE_ID, ");
            sql.append("MESSAGE_TYPE, ");
            sql.append("SEVERITY, ");
            sql.append("MESSAGE_TIMESTAMP, ");
            sql.append("MESSAGE_TEXT, ");
            sql.append("MESSAGE_SECOND_LEVEL_TEXT, ");
            sql.append("FROM_PROGRAM, ");
            sql.append("FROM_LIBRARY, ");
            sql.append("FROM_MODULE, ");
            sql.append("FROM_PROCEDURE, ");
            sql.append("MESSAGE_KEY ");
            sql.append("FROM TABLE(QSYS2.JOBLOG_INFO('").append(jobId).append("')) ");
            sql.append("WHERE MESSAGE_TIMESTAMP > '").append(lastCheck.toString()).append("' ");
        }
        
        if (m_jobIdentifiers.size() > 0) {
            sql.append(" ORDER BY MESSAGE_TIMESTAMP");
        }
        
        return sql.toString();
    }

    /**
     * Update the last check timestamp for a job based on the latest message timestamp
     */
    private void updateLastCheckTimestamp(String jobId, Timestamp messageTimestamp) {
        Timestamp current = m_lastCheckTimestamps.get(jobId);
        if (current == null || messageTimestamp.after(current)) {
            m_lastCheckTimestamps.put(jobId, messageTimestamp);
        }
    }

    @Override
    public void configure() {
        from("timer://foo?synchronous=true&period=" + m_interval)
                .routeId("manzan_joblog:" + m_name)
                .process(exchange -> {
                    String sql = buildJobLogQuery();
                    exchange.getIn().setBody(sql);
                })
                .to("jdbc:jt400?outputType=StreamList")
                .split(body()).streaming().parallelProcessing()
                .process(exchange -> {
                    Map<String, Object> dataMap = exchange.getIn().getBody(Map.class);
                    
                    // Update last check timestamp for this job
                    String jobId = (String) dataMap.get("JOB_FULL");
                    Object timestampObj = dataMap.get("MESSAGE_TIMESTAMP");
                    if (timestampObj instanceof Timestamp) {
                        updateLastCheckTimestamp(jobId, (Timestamp) timestampObj);
                    }
                    
                    // Parse job identifier into components
                    String[] jobParts = jobId.split("/");
                    if (jobParts.length == 3) {
                        dataMap.put("JOB_NUMBER", jobParts[0]);
                        dataMap.put("JOB_USER", jobParts[1]);
                        dataMap.put("JOB_NAME", jobParts[2]);
                    }
                    
                    // Inject custom data
                    injectIntoDataMap(dataMap, dataMapInjection);
                    exchange.getIn().setHeader("data_map", dataMap);
                    exchange.getIn().setBody(dataMap);
                })
                .setHeader(EVENT_TYPE, constant(m_eventType))
                .marshal().json(true) // TODO: skip this if we are applying a format
                .setBody(simple("${body}\n"))
                .process(exchange -> {
                    if (null != m_formatter) {
                        exchange.getIn().setBody(m_formatter.format(getDataMap(exchange)));
                    }
                })
                .recipientList(constant(getRecipientList()))
                .parallelProcessing()
                .stopOnException()
                .end()
                .end();
    }
}

