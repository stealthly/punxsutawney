package ly.stealth.punxsutawney;

import java.io.*;
import java.util.*;

public class Requests implements Iterable<Requests.Request>, Serializable {
    private List<Request> requests = new ArrayList<>();

    public Requests() {}

    @SuppressWarnings("unchecked")
    public Requests(byte[] bytes) {
        try { requests = (List<Request>) new ObjectInputStream(new ByteArrayInputStream(bytes)).readObject(); }
        catch (IOException | ClassNotFoundException e) { throw new IOError(e); }
    }

    public Request add(Request request) { requests.add(request); return request; }
    public void remove(Request request) { requests.remove(request); }
    public int size() { return requests.size(); }

    public Iterator<Request> iterator() { return requests.iterator(); }

    public byte[] toByteArray() {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try { new ObjectOutputStream(bytes).writeObject(requests); }
        catch (IOException e) { throw new IOError(e); }
        return bytes.toByteArray();
    }

    public static class Request implements Serializable {
        public String url;
        public String method = "GET";

        public Map<String, String> headers = new HashMap<>();
        public byte[] body;
    }
}

