package com.github.theprez.manzan;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.gson.Gson;

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
        m_fmtStr = _fmtStr;
    }

    public String format(final Map<String, Object> _mappings) {
        String ret = m_fmtStr;
        ret = ret.replace("\r\n", "\n");
        ret = ret.replace("\r", "");
        ret = ret.replace("\\n", "\n").replace("\\t", "\t");
        String[] replaceProperties = extractDollarPairs(ret);

        String jsonIndicator = "json:";
        for (String replaceProperty: replaceProperties){
            boolean isUsingJsonIndicator = false;
            if (replaceProperty.contains(jsonIndicator)){
                isUsingJsonIndicator = true;
                replaceProperty = replaceProperty.substring(jsonIndicator.length());
            }
            String replaceWith = getNestedValue(_mappings, replaceProperty);
            if(isUsingJsonIndicator) {
                ret = ret.replace("$" + jsonIndicator + replaceProperty + "$" , jsonEncode(replaceWith));
            } else {
                ret = ret.replace("$" + replaceProperty + "$", replaceWith);
            }
        }
        return ret;
    }

    private String[] extractDollarPairs(String input){
        List<String> matches = new ArrayList<>();
        Pattern pattern = Pattern.compile("\\$(.+?)\\$");
        Matcher matcher = pattern.matcher(input);

        while (matcher.find()) {
            matches.add(matcher.group(1)); // group(1) is the text inside the $...$
        }

        String[] result = matches.toArray(new String[0]);
        return result;
    }

    /**
     *
     * @param mapping: The data map
     * @param property: The property to search for
     * @return The value corresponding to the (nested) property if it exists, otherwise empty string
     */
    private String getNestedValue(Object mapping, String property){
        String[] propertyTokens = property.split("\\.");
        Object currentMapping = mapping;
        for (String propertyToken: propertyTokens){
            while(currentMapping instanceof ArrayList){
                if (((ArrayList<?>) currentMapping).isEmpty()){
                    return "";
                }
                currentMapping = ((ArrayList<?>) currentMapping).get(0);
            }
            if (currentMapping instanceof LinkedHashMap){
                if (((LinkedHashMap<?, ?>) currentMapping).containsKey(propertyToken)){
                    currentMapping = ((LinkedHashMap<?, ?>) currentMapping).get(propertyToken);
                } else {
                    return "";
                }
            }
            else if (currentMapping instanceof Map.Entry){
                if (((Map.Entry<?, ?>) currentMapping).getKey() == propertyToken){
                    currentMapping = ((Map.Entry<?, ?>) currentMapping).getValue();
                } else {
                    return "";
                }
            }
        }
        return currentMapping == null ? "" : currentMapping.toString();
    }

    private CharSequence jsonEncode(String _s) {
        Gson gson = new Gson();
        String json = gson.toJson(_s);
        return json;
    }
}
