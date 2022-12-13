package com.github.theprez.manzan;

import java.util.Map;
import java.util.Map.Entry;

public class ManzanMessageFormatter {

    private final String m_fmtStr;

    public ManzanMessageFormatter(String _fmtStr) {
        m_fmtStr = _fmtStr;
    }

    public String format(Map<String, Object> _mappings) {
        String ret = m_fmtStr;
        for (Entry<String, Object> repl : _mappings.entrySet()) {
            ret = ret.replace("$" + repl.getKey() + "$", "" + repl.getValue());
        }
        return ret;
    }

}
