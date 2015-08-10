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

import java.io.*;
import java.util.*;

public class Requests implements Iterable<Requests.Request>, Serializable {
    private long timeout = 30000;
    private List<Request> requests = new ArrayList<>();

    public Requests() {}

    @SuppressWarnings("unchecked")
    public Requests(byte[] bytes) {
        try { requests = (List<Request>) new ObjectInputStream(new ByteArrayInputStream(bytes)).readObject(); }
        catch (IOException | ClassNotFoundException e) { throw new IOError(e); }
    }

    public long getTimeout() { return timeout; }
    public void setTimeout(long timeout) { this.timeout = timeout; }

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

