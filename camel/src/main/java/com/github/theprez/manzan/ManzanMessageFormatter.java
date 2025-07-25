package com.github.theprez.manzan;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.gson.Gson;

public class ManzanMessageFormatter {

    private final String m_fmtStr;

    public ManzanMessageFormatter(final String _fmtStr) {
        m_fmtStr = _fmtStr;
    }

    public String format(final Map<String, Object> _mappings) {
        String ret = m_fmtStr;
        if (ret == null){
            return "";
        }
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
            while(currentMapping instanceof List){
                if (((List<?>) currentMapping).isEmpty()){
                    return "";
                }
                currentMapping = ((List<?>) currentMapping).get(0);
            }
            if (currentMapping instanceof HashMap){
                if (((HashMap<?, ?>)currentMapping).containsKey(propertyToken)){
                    currentMapping = ((HashMap<?, ?>) currentMapping).get(propertyToken);
                } else {
                    return "";
                }
            }
            else if (currentMapping instanceof Map.Entry){
                if (((Map.Entry<?, ?>) currentMapping).getKey().equals(propertyToken)){
                    currentMapping = ((Map.Entry<?, ?>) currentMapping).getValue();
                } else {
                    return "";
                }
            } else {
                return "";
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
