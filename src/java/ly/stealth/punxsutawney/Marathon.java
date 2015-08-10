/**
 * Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements. See the NOTICE
 * file distributed with this work for additional information regarding copyright ownership. The ASF licenses this file
 * to You under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the
 * License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */

package ly.stealth.punxsutawney;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.*;

public class Marathon {
    public static String url = "http://master:8080";

    public static void startApp(App app) throws IOException, InterruptedException {
        sendRequest("/v2/apps", "POST", app.toJson());

        for(;;) {
            @SuppressWarnings("unchecked")
            List<JSONObject> tasks = getTasks(app.id);
            if (tasks == null) throw new IllegalStateException("App " + app.id + " not found");

            int started = 0;
            for (JSONObject task : tasks)
                if (task.get("startedAt") != null) started ++;

            if (started == app.instances) break;
            Thread.sleep(1000);
        }
    }

    public static boolean hasApp(String app) throws IOException {
        return getApp(app) != null;
    }

    public static void stopApp(String app) throws IOException {
        sendRequest("/v2/apps/" + app, "DELETE", null);
    }

    public static List<String> getEndpoints(String app) throws IOException {
        @SuppressWarnings("unchecked") List<JSONObject> tasks = getTasks(app);
        if (tasks == null) return Collections.emptyList();

        List<String> endpoints = new ArrayList<>();
        for (JSONObject task : tasks)
            endpoints.add(task.get("host") + ":" + ((JSONArray)task.get("ports")).get(0));

        return endpoints;
    }

    private static JSONArray getTasks(String app) throws IOException {
        JSONObject app_ = getApp(app);
        return app_ != null ? (JSONArray) app_.get("tasks") : null;
    }

    private static JSONObject getApp(String app) throws IOException {
        JSONObject response = sendRequest("/v2/apps/" + app, "GET", null);
        return response != null ? (JSONObject) response.get("app") : null;
    }

    private static JSONObject sendRequest(String uri, String method, JSONObject json) throws IOException {
        URL url = new URL(Marathon.url + uri);
        HttpURLConnection c = (HttpURLConnection) url.openConnection();
        try {
            c.setRequestMethod(method);

            if (method.equalsIgnoreCase("POST")) {
                byte[] body = json.toString().getBytes("utf-8");
                c.setDoOutput(true);
                c.setRequestProperty("Content-Type", "application/json");
                c.setRequestProperty("Content-Length", "" + body.length);
                c.getOutputStream().write(body);
            }

            return (JSONObject) JSONValue.parse(new InputStreamReader(c.getInputStream(), "utf-8"));
        } catch (IOException e) {
            if (c.getResponseCode() == 404 && method.equals("GET"))
                return null;

            ByteArrayOutputStream response = new ByteArrayOutputStream();
            InputStream err = c.getErrorStream();
            if (err == null) throw e;

            Util.copyAndClose(err, response);
            IOException ne = new IOException(e.getMessage() + ": " + response.toString("utf-8"));
            ne.setStackTrace(e.getStackTrace());
            throw ne;
        } finally {
            c.disconnect();
        }
    }

    public static class App {
        public static final String DEFAULT_ID = "punxsutawney";

        public String id = DEFAULT_ID;
        public double cpus = 1.0;
        public int mem = 128;

        public int instances = 1;
        public List<Integer> ports = new ArrayList<>();
        public List<String> uris = new ArrayList<>();
        public Map<String, String> env = new HashMap<>();

        public String cmd;

        @SuppressWarnings("unchecked")
        private JSONObject toJson() {
            JSONObject obj = new JSONObject();

            obj.put("id", id);
            obj.put("cpus", cpus);
            obj.put("mem", mem);

            obj.put("instances", instances);
            if (!ports.isEmpty()) obj.put("ports", ports);
            if (!uris.isEmpty()) obj.put("uris", uris);
            if (!env.isEmpty()) obj.put("env", env);

            obj.put("cmd", cmd);
            return obj;
        }
    }
}

