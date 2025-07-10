package com.github.theprez.manzan.routes;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.apache.camel.Exchange;
import org.apache.camel.builder.RouteBuilder;

import com.github.theprez.jcmdutils.StringUtils;
import com.github.theprez.manzan.ManzanEventType;

public abstract class ManzanRoute extends RouteBuilder {

    protected static final String EVENT_TYPE = "event_type";

    protected static final String SESSION_ID = "SESSION_ID";
    protected static final String MSG_ORDINAL_POSITION = "ORDINAL_POSITION";
    protected static final String HANDLED_TIMESTAMP = "HANDLED_TIMESTAMP";

    protected static final String MSG_MESSAGE_ID = "MESSAGE_ID";
    protected static final String MSG_MESSAGE_TYPE = "MESSAGE_TYPE";
    protected static final String MSG_SEVERITY = "SEVERITY";
    protected static final String JOB = "JOB";
    protected static final String MSG_SENDING_USRPRF = "SENDING_USRPRF";
    protected static final String MSG_SENDING_PROGRAM_NAME = "SENDING_PROGRAM_NAME";
    protected static final String MSG_SENDING_MODULE_NAME = "SENDING_MODULE_NAME";
    protected static final String MSG_SENDING_PROCEDURE_NAME = "SENDING_PROCEDURE_NAME";
    protected static final String MSG_MESSAGE_TIMESTAMP = "MESSAGE_TIMESTAMP";
    protected static final String MSG_MESSAGE = "MESSAGE";

    protected static final String MAJOR_CODE = "MAJOR_CODE";
    protected static final String MINOR_CODE = "MINOR_CODE";
    protected static final String LOG_ID = "LOG_ID";
    protected static final String LOG_TIMESTAMP = "LOG_TIMESTAMP";
    protected static final String ENTRY_TIMESTAMP = "ENTRY_TIMESTAMP";
    protected static final String TDE_NUM = "TDE_NUM";
    protected static final String TASK_NAME = "TASK_NAME";
    protected static final String SERVER_TYPE = "SERVER_TYPE";
    protected static final String EXCEPTION_ID = "EXCEPTION_ID";
    protected static final String THREAD_ID = "THREAD_ID";
    protected static final String MODULE_OFFSET = "MODULE_OFFSET";
    protected static final String MODULE_RU_NAME = "MODULE_RU_NAME";
    protected static final String MODULE_NAME = "MODULE_NAME";
    protected static final String MODULE_ENTRY_POINT_NAME = "MODULE_ENTRY_POINT_NAME";

    protected static final String SYSTEM_REFERENCE_CODE = "SYSTEM_REFERENCE_CODE";
    protected static final String DEVICE_NAME = "DEVICE_NAME";
    protected static final String MODEL = "MODEL";
    protected static final String SERIAL_NUMBER = "SERIAL_NUMBER";
    protected static final String RESOURCE_NAME = "RESOURCE_NAME";
    protected static final String PAL_TIMESTAMP = "PAL_TIMESTAMP";
    protected static final String REFERENCE_CODE = "REFERENCE_CODE";
    protected static final String SECONDARY_CODE = "SECONDARY_CODE";
    protected static final String TABLE_ID = "TABLE_ID";
    protected static final String SEQUENCE_NUM = "SEQUENCE_NUM";

    protected static final int SEVERITY_LIMIT = 29;

    protected ManzanEventType m_eventType;

    protected static String getWatchName(final Exchange exchange) {
        final Object watchNameObject = exchange.getIn().getHeader(SESSION_ID);
        if (null == watchNameObject) {
            throw new RuntimeException("Couldn't figure out watch ID");
        }
        return watchNameObject.toString().toLowerCase().trim();
    }

    protected void injectIntoDataMap(Map<String, Object> data_map, Map<String, String> dataMapInjection){
        for (Map.Entry<String, String> entry: dataMapInjection.entrySet()){
            data_map.put(entry.getKey(), entry.getValue());
        }
    }

    protected final String m_name;
    private String m_recipientList = "";

    public ManzanRoute(final String _name) {
        m_name = _name;
    }

    @Override
    public abstract void configure();

    protected abstract void setEventType(ManzanEventType manzanEventType);

    protected Object get(final Exchange _exchange, final String _attr) {
        final Map<String, Object> data = getDataMap(_exchange);
        final Object val = data.get(_attr);
        if (null == val) {
            throw new RuntimeException("Missing value for " + _attr);
        }
        return val;
    }

    @SuppressWarnings("unchecked")
    protected Map<String, Object> getDataMap(final Exchange _exchange) {
        return (Map<String, Object>) _exchange.getIn().getHeader("data_map");
    }

    protected <T> T getBody(final Exchange _exchange, Class<T> type) {
        return _exchange.getIn().getBody(type);
    }

    protected String getInUri() {
        return "direct:" + m_name;
    }

    protected String getRecipientList() {
        return m_recipientList;
    }

    protected String getString(final Exchange _exchange, final String _attr) {
        return "" + get(_exchange, _attr);
    }

    protected void setRecipientList(final List<String> _destinations) throws IOException {
        m_recipientList = createRecipientList(_destinations);
        if (StringUtils.isEmpty(m_recipientList)) {
            throw new IOException("Message watch for '" + m_name + "' has no valid destinations");
        }
    }

    public static String createRecipientList(final List<String> _destinations) throws IOException {
        String destinationsStr = "";
        for (final String dest : _destinations) {
            if (StringUtils.isEmpty(dest)) {
                continue;
            }
            destinationsStr += "direct:" + dest.toLowerCase().trim() + ",";
        }
        return destinationsStr.replaceFirst(",$", "").trim();
    }
}
