package com.github.theprez.manzan;

import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

public class ManzanMessageFilter {

    private final String m_filterStr;
    private final boolean m_isRegex;
    private final Pattern m_pattern;

    public ManzanMessageFilter(final String _filterStr) {
        m_filterStr = _filterStr;
        m_isRegex = m_filterStr.startsWith("re:");
        if (m_isRegex) {
            final String regex = m_filterStr.substring(3);
            if (regex.isEmpty()){
                throw new PatternSyntaxException("No pattern was specified", "", 0);
            }
            m_pattern = Pattern.compile(regex, Pattern.DOTALL);
        } else {
            m_pattern = null;
        }
    }

    public boolean matches(final String _data) {
        if (null == m_pattern) {
            return m_filterStr.isEmpty() ? true : _data.contains(m_filterStr);
        }
        return m_pattern.matcher(_data).find();
    }
}
