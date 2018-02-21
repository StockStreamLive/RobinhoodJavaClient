package com.cheddar.http;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.CookieSpecs;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicHeader;
import org.apache.http.message.BasicNameValuePair;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Slf4j
public class HTTPClient {

    public Optional<HTTPResult> executeHTTPPostRequest(final HTTPQuery httpQuery) {
        HttpPost httpPost = new HttpPost(httpQuery.getUrl());

        List<NameValuePair> params = new ArrayList<>();
        for (final Map.Entry<String, String> paramEntry : httpQuery.getParameters().entrySet()) {
            params.add(new BasicNameValuePair(paramEntry.getKey(), paramEntry.getValue()));
        }

        try {
            httpPost.setEntity(new UrlEncodedFormEntity(params, "utf-8"));
        } catch (final UnsupportedEncodingException e) {
            log.warn(e.getMessage(), e);
        }

        return executeHTTPRequest(httpPost, httpQuery.getHeaders());
    }


    public Optional<HTTPResult> executeHTTPGetRequest(final HTTPQuery httpQuery) {
        String paramString = "";
        if (httpQuery.getParameters().size() > 0) {
            List<NameValuePair> params = new ArrayList<>();
            for (final Map.Entry<String, String> paramEntry : httpQuery.getParameters().entrySet()) {
                final String urlParameter;
                try {
                    urlParameter = URLEncoder.encode(paramEntry.getKey(), "UTF-8");
                } catch (UnsupportedEncodingException e) {
                    log.warn("Cannot encode parameter {}!", paramEntry.getKey());
                    continue;
                }
                if (httpQuery.getUrl().contains(urlParameter+"=")) {
                    continue;
                }
                params.add(new BasicNameValuePair(paramEntry.getKey(), paramEntry.getValue()));
            }
            if (params.size() > 0) {
                paramString = "?" + URLEncodedUtils.format(params, "utf-8");
            }
        }
        HttpGet httpGet = new HttpGet(httpQuery.getUrl() + paramString);

        return executeHTTPRequest(httpGet, httpQuery.getHeaders());
    }

    private Optional<HTTPResult> executeHTTPRequest(final HttpUriRequest httpRequest,
                                                    final Map<String, String> headers) {

        final List<Header> headerList = new ArrayList<>();
        headers.forEach((key, value) -> headerList.add(new BasicHeader(key, value)));

        final HttpClient httpClient = HttpClients.custom()
                                                 .setDefaultRequestConfig(RequestConfig.custom().setCookieSpec(CookieSpecs.STANDARD).build())
                                                 .setDefaultHeaders(headerList).setConnectionTimeToLive(2000, TimeUnit.MILLISECONDS).build();

        Optional<HTTPResult> httpResultOptional = Optional.empty();

        try {
            HttpResponse response = httpClient.execute(httpRequest);
            HttpEntity entity = response.getEntity();

            final Map<String, String> responseHeader = new HashMap<>();
            for (final Header header : response.getAllHeaders()) {
                responseHeader.put(header.getName(), header.getValue());
            }

            final String pageContents = IOUtils.toString(entity.getContent(), Charset.defaultCharset());

            HTTPResult result = new HTTPResult(response.getStatusLine().getStatusCode(), responseHeader, pageContents);

            httpResultOptional = Optional.of(result);
        } catch (final IOException e) {
            log.warn(e.getMessage(), e);
        }

        return httpResultOptional;
    }

}
