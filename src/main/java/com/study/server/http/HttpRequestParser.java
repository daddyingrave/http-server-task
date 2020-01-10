package com.study.server.http;

import com.study.server.exceptions.BadRequestException;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class HttpRequestParser {
    private static Pattern mainString = Pattern.compile("(?<method>[\\x41-\\x5A]+)( )" +
            "((?<path>[\\x41-\\x5A[\\x61-\\x7A[\\x30-\\x39[./]]]]+)" +
            "((\\?)(?<parameters>[\\x41-\\x5A[\\x61-\\x7A[\\x30-\\x39[,.=&]]]]+))?)? (?<protocol>HTTP/[\\d].[\\d])");
    private static Pattern pairsPattern = Pattern.compile("(?<key>[a-zA-Z\\d]+)=(?<value>[a-zA-Z\\d]+)");
    private static Pattern headersPattern = Pattern.compile("(?<key>[\\x20-\\x7D&&[^:]]+)" +
            ":(?<value>[\\x20-\\x7D]+)");
    private static Pattern hostPattern = Pattern.compile("(?<host>[\\x20-\\x7D&&[^:]]+)(:)?(?<port>\\d+)?");

    private HttpRequestParser() {
    }

    public static HttpRequest parse(InputStream in) {
        var br = new BufferedReader(new InputStreamReader(in));
        var builder = new HttpRequest.Builder();

        try {
            var curLine = br.readLine();
            var matcher = mainString.matcher(curLine);

            var method = methodParse(matcher);
            builder.setMethod(method);

            var path = pathParse(curLine, matcher);
            builder.setPath(path);

            Map<String, String> queryParameters;
            if (curLine.contains("?")) {
                queryParameters = queryParse(matcher);
                builder.setQueryParameters(queryParameters);
            }

            var protocol = protocolParse(matcher);
            builder.setProtocol(protocol);

            curLine = br.readLine();
            Map<String, String> headers = new HashMap<>();
            while (!curLine.equals("")) {
                String[] pair = headersParse(curLine);
                headers.put(pair[0], pair[1]);
                curLine = br.readLine();
            }
            builder.setHeaders(headers);

            var host = hostParse(headers);
            builder.setHost(host);

            var port = portParse(headers);
            builder.setPort(port);
        } catch (Exception e) {
            throw new BadRequestException("Can't parse request");
        }
        return builder.build();
    }

    private static String methodParse(Matcher matcher) {
        matcher.find();
        var method = matcher.group("method");
        var methodIsSupported = false;

        for (HttpMethods elem : HttpMethods.values()) {
            if (elem.name().equals(method)) {
                methodIsSupported = true;
                break;
            }
        }

        if (methodIsSupported) {
            return method;
        } else {
            throw new BadRequestException("Can't parse request");
        }
    }

    private static String pathParse(String curLine, Matcher matcher) {

        if (curLine.contains(" /")) {
            return matcher.group("path");
        } else {
            return "";
        }
    }

    private static Map<String, String> queryParse(Matcher matcher) {
        Map<String, String> queryParameters = new HashMap<>();
        var parameters = matcher.group("parameters");
        Matcher pairsMatcher = pairsPattern.matcher(parameters);

        while (pairsMatcher.find()) {
            var key = pairsMatcher.group("key").toLowerCase();
            var value = pairsMatcher.group("value").toLowerCase();
            queryParameters.put(key, value);
        }

        return queryParameters;
    }

    private static String protocolParse(Matcher matcher) {
        var defaultProtocol = "HTTP/1.1";
        var protocol = matcher.group("protocol");
        if (defaultProtocol.equals(protocol)) {
            return protocol;
        } else {
            throw new BadRequestException("Can't parse request");
        }
    }

    private static String[] headersParse(String curLine) {
        var matcher = headersPattern.matcher(curLine);
        var headers = new String[2];
        matcher.find();
        headers[0] = matcher.group("key").toLowerCase();
        headers[1] = matcher.group("value").trim().toLowerCase();

        if (headers[0].equals("") || headers[1].equals("")) {
            throw new BadRequestException("Can't parse request");
        } else {
            return headers;
        }
    }

    private static String hostParse(Map<String, String> headers) {
        var hostLine = headers.get("host");
        var matcher = hostPattern.matcher(hostLine);
        matcher.find();

        return matcher.group("host");
    }

    private static String portParse(Map<String, String> headers) {
        var hostLine = headers.get("host");
        var matcher = hostPattern.matcher(hostLine);
        matcher.find();

        if (matcher.group("port") == null) {
            return "80";
        } else {
            return matcher.group("port");
        }
    }
}