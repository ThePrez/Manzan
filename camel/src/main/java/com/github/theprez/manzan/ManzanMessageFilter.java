package com.github.theprez.manzan;

import java.util.regex.Pattern;

import com.github.theprez.jcmdutils.StringUtils;

public class ManzanMessageFilter {

    private final String m_filterStr;
    private final boolean m_isRegex;
    private final Pattern m_pattern;

    public ManzanMessageFilter(String _filterStr) {
        m_filterStr = StringUtils.isEmpty(_filterStr)?null: _filterStr.trim();
        m_isRegex = m_filterStr.startsWith("re:");
        if(m_isRegex) {
            String regex = m_filterStr.substring(3);
            m_pattern = Pattern.compile(regex, Pattern.CASE_INSENSITIVE);//TODO: handle invalid regex
        } else {
            m_pattern = null;
        }
    }

    public boolean matches(final String _data) {
        if(null == m_pattern) {
            return StringUtils.isEmpty(m_filterStr) ? true : _data.toLowerCase().contains(m_filterStr.toLowerCase());
        }
        return m_pattern.matcher(_data).find();
    }
}
