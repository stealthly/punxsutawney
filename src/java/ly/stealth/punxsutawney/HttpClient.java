package ly.stealth.punxsutawney;

import kafka.consumer.Consumer;
import kafka.consumer.ConsumerConfig;
import kafka.consumer.ConsumerIterator;
import kafka.consumer.KafkaStream;
import kafka.javaapi.consumer.ConsumerConnector;
import kafka.message.MessageAndMetadata;
import org.eclipse.jetty.client.api.Request;
import org.eclipse.jetty.client.api.Response;
import org.eclipse.jetty.client.api.Result;
import org.eclipse.jetty.client.util.BytesContentProvider;
import org.eclipse.jetty.http.HttpMethod;

import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

import static java.lang.System.out;

public class HttpClient {
    private static String id = System.getenv("HC_ID");
    private static String zk = System.getenv("HC_ZK");

    public static void main(String[] args) throws Exception {
        if (id == null) throw new IllegalStateException("Undefined HC_ID");
        if (zk == null) throw new IllegalStateException("Undefined HC_ZK");

        out.println("Starting " + HttpClient.class.getSimpleName());
        out.println("Using zk:" + zk + ", id:" + id);

        Properties props = new Properties();
        props.put("zookeeper.connect", zk);
        props.put("group.id", id);
        props.put("zookeeper.session.timeout.ms", "400");
        props.put("zookeeper.sync.time.ms", "200");

        ConsumerConnector consumer = Consumer.createJavaConsumerConnector(new ConsumerConfig(props));
        KafkaStream<byte[],byte[]> stream = consumer.createMessageStreams(Collections.singletonMap(id, 1)).get(id).get(0);

        consume(consumer, stream);
    }

    @SuppressWarnings("InfiniteLoopStatement")
    private static void consume(ConsumerConnector consumer, KafkaStream<byte[], byte[]> stream) throws Exception {
        for(ConsumerIterator<byte[],byte[]> i = stream.iterator();;) {
            out.println("Waiting for requests on \"" + id + "\" topic ...");

            MessageAndMetadata<byte[],byte[]> message = i.next();
            Requests requests = new Requests(message.message());

            sendRequests(requests);
            consumer.commitOffsets();
        }
    }

    static void sendRequests(Requests requests) throws Exception {
        org.eclipse.jetty.client.HttpClient client = new org.eclipse.jetty.client.HttpClient();
        client.setConnectTimeout(requests.getTimeout());
        client.setMaxRequestsQueuedPerDestination(requests.size());
        client.setMaxConnectionsPerDestination(256);

        final CountDownLatch completed = new CountDownLatch(requests.size());

        final AtomicInteger failed = new AtomicInteger();
        final List<Throwable> failures = new ArrayList<>();

        Response.CompleteListener listener = new Response.CompleteListener() {
            public void onComplete(Result result) {
                if (result.isFailed()) {
                    failed.incrementAndGet();
                    if (failures.size() < 3) failures.add(result.getFailure());
                }
                completed.countDown();
            }
        };

        long start = System.currentTimeMillis();
        client.start();

        out.println("Sending " + requests.size() + " requests ...");
        for (Requests.Request request : requests) {
            Request r = client.newRequest(request.url);
            r.method(HttpMethod.valueOf(request.method));

            for (Map.Entry<String, String> entry: request.headers.entrySet())
                r.header(entry.getKey(), entry.getValue());

            if (request.body != null) r.content(new BytesContentProvider(request.body));
            r.send(listener);
        }

        completed.await();
        client.stop();
        long time = System.currentTimeMillis() - start;
        double rps = Math.floor(requests.size() * 10d * 1000 / time) / 10;

        out.println("Failed:" + failed.get() + "/" + requests.size() + ", speed:" + rps + "rps");

        if (!failures.isEmpty()) {
            out.println("Failures (" + failures.size() + "/" + failed.get() + "):");
            for (Throwable failure : failures)
                failure.printStackTrace(out);
        }
    }
}
