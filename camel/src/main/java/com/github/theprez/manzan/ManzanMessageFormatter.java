package com.github.theprez.manzan;

import java.util.Map;
import java.util.Map.Entry;

import com.google.gson.Gson;

import java.util.WeakHashMap;

public class ManzanMessageFormatter {

    private static final Map<String, ManzanMessageFormatter> s_formatterCache = new WeakHashMap<String, ManzanMessageFormatter>();

    public static String format(final String _in, final Map<String, Object> _mappings) {
        ManzanMessageFormatter formatter = s_formatterCache.get(_in);
        if (null == formatter) {
            formatter = new ManzanMessageFormatter(_in);
            s_formatterCache.put(_in, formatter);
        }
        return formatter.format(_mappings);
    }

    public String getM_fmtStr() {
        return m_fmtStr;
    }

    private final String m_fmtStr;

    public ManzanMessageFormatter(final String _fmtStr) {
        StringBuilder result = new StringBuilder();
        
        // Use a boolean array to avoid final constraint
        boolean[] keyOpened = {false};
        _fmtStr.chars().forEach(c -> {
            char next = (char) c;
            if (c == '$'){
                keyOpened[0] = !keyOpened[0];
                result.append(next);
            }
            else if (keyOpened[0]){
                result.append(Character.toUpperCase(next));
            } else {
                result.append(next);
            }
        });
        m_fmtStr = result.toString();
    }

    public String format(final Map<String, Object> _mappings) {
        String ret = m_fmtStr;
        ret = ret.replace("\r\n", "\n");
        ret = ret.replace("\r", "");
        ret = ret.replace("\\n", "\n").replace("\\t", "\t");

        for (final Entry<String, Object> repl : _mappings.entrySet()) {
            final String key = repl.getKey().toUpperCase();
            ret = ret.replace("$" + key + "$", "" + repl.getValue());
            String jsonIndicator ="$json:" + key + "$";
            if(ret.contains(jsonIndicator)) {
                ret = ret.replace(jsonIndicator, jsonEncode("" + repl.getValue()));
            }
        }
        return ret;
    }

    private CharSequence jsonEncode(String _s) {
        Gson gson = new Gson();
        String json = gson.toJson(_s);
        return json;
    }
}
