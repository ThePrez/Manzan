package com.github.theprez.manzan;

import java.util.Map;
import java.util.Map.Entry;

public class ManzanMessageFormatter {

    private final Map<String, Object> m_mappings;

    public ManzanMessageFormatter(Map<String, Object> _mappings) {
m_mappings = _mappings;
    }
    public String format(String _fmtStr) {
        String ret = _fmtStr;
        for(Entry<String, Object> repl:m_mappings.entrySet()) {
            ret = ret.replace("$"+repl.getKey()+"$", ""+repl.getValue());
        }
        return ret;
    }
    
}
