/*
 * Copyright 2019 VMware, Inc.
 * SPDX-License-Identifier: EPL-2.0
 */
package com.vmware.vipclient.i18n.messages.api.opt.server;

import com.vmware.vipclient.i18n.VIPCfg;
import com.vmware.vipclient.i18n.base.HttpRequester;
import com.vmware.vipclient.i18n.base.cache.PatternCacheItem;
import com.vmware.vipclient.i18n.l2.common.PatternKeys;
import com.vmware.vipclient.i18n.messages.api.opt.PatternOpt;
import com.vmware.vipclient.i18n.messages.api.url.URLUtils;
import com.vmware.vipclient.i18n.messages.api.url.V2URL;
import com.vmware.vipclient.i18n.util.ConstantsKeys;
import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.HttpURLConnection;
import java.util.List;
import java.util.Map;

public class RemotePatternOpt extends L2RemoteBaseOpt implements PatternOpt{
    Logger logger = LoggerFactory.getLogger(RemotePatternOpt.class);

    public void getPatterns(String locale, PatternCacheItem cacheItem) {
        logger.debug("Look for pattern from Singleton Service for locale [{}]!", locale);
        HttpRequester httpRequester = VIPCfg.getInstance().getVipService().getHttpRequester();
        getPatternsFromRemote(V2URL.getPatternURL(locale,
                    httpRequester.getBaseURL()), ConstantsKeys.GET, null, cacheItem);
    }

    public void getPatterns(String language, String region, PatternCacheItem cacheItem) {
        logger.debug("Look for pattern from Singleton Service for language [{}], region [{}]!", language, region);
        HttpRequester httpRequester = VIPCfg.getInstance().getVipService().getHttpRequester();
        getPatternsFromRemote(V2URL.getPatternURL(language, region,
                    httpRequester.getBaseURL()), ConstantsKeys.GET, null, cacheItem);
    }

    private void getPatternsFromRemote(String url, String method, Object requestData, PatternCacheItem cacheItem) {

        Map<String, Object> response = (Map<String, Object>) getResponse(url, method, requestData, cacheItem);

        Integer responseCode = (Integer) response.get(URLUtils.RESPONSE_CODE);

        if (responseCode != null && (responseCode.equals(HttpURLConnection.HTTP_OK) ||
                responseCode.equals(HttpURLConnection.HTTP_NOT_MODIFIED))) {
            long timestamp = 0;
            String etag = null;
            Long maxAgeMillis = null;

            if (response.get(URLUtils.RESPONSE_TIMESTAMP) != null)
                timestamp = (long) response.get(URLUtils.RESPONSE_TIMESTAMP);
            if (response.get(URLUtils.HEADERS) != null)
                etag = URLUtils.createEtagString((Map<String, List<String>>) response.get(URLUtils.HEADERS));
            if (response.get(URLUtils.MAX_AGE_MILLIS) != null)
                maxAgeMillis = (Long) response.get(URLUtils.MAX_AGE_MILLIS);

            if (responseCode.equals(HttpURLConnection.HTTP_OK)) {
                try {
                    String responseBody = (String) response.get(URLUtils.BODY);
                    Map<String, Object> patterns = getPatternsFromResponse(responseBody);
                    if (patterns != null) {
                        cacheItem.set(patterns, etag, timestamp, maxAgeMillis);
                    }
                } catch (Exception e) {
                    logger.error("Failed to get pattern data from remote!");
                }
            }else{
                cacheItem.set(etag, timestamp, maxAgeMillis);
            }
        }
    }

    private Map<String, Object> getPatternsFromResponse(String responseBody) {
        Map<String, Object> categoriesObj = null;
        Map<String, Object> dataObj = (Map<String, Object>) getDataFromResponse(responseBody);
        if (dataObj != null && dataObj instanceof JSONObject) {
            Object obj = ((JSONObject) dataObj).get(PatternKeys.CATEGORIES);
            if (obj != null && obj instanceof JSONObject) {
                categoriesObj = (Map<String, Object>) obj;
            }
        }
        return categoriesObj;
    }
}
