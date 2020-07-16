package com.vmware.vipclient.i18n.messages.api.opt;

import org.json.simple.JSONObject;

public interface PatternOpt {
    public JSONObject getPatterns(String locale);
    public JSONObject getPatterns(String language, String region);
}
