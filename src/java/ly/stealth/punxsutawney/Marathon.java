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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

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

        public String cmd;

        @SuppressWarnings("unchecked")
        private JSONObject toJson() {
            JSONObject obj = new JSONObject();

            obj.put("id", id);
            obj.put("cpus", cpus);
            obj.put("mem", mem);

            obj.put("instances", instances);
            obj.put("ports", ports);
            obj.put("uris", uris);

            obj.put("cmd", cmd);
            return obj;
        }
    }
}

