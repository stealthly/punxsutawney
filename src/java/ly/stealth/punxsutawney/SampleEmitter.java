package ly.stealth.punxsutawney;

import kafka.javaapi.producer.Producer;
import kafka.producer.KeyedMessage;
import kafka.producer.ProducerConfig;

import java.io.IOException;
import java.util.Properties;

public class SampleEmitter {
    public static void main(String[] args) throws IOException {
        String topic = Marathon.App.DEFAULT_ID;

        Properties props = new Properties();
        props.put("metadata.broker.list", "master:9092");
        Producer<byte[], byte[]> producer = new Producer<>(new ProducerConfig(props));

        for (int i = 0; i < 1; i++) {
            Requests requests = new Requests();
            for (int j = 0; j < 1000; j++) {
                Requests.Request request = new Requests.Request();
                request.method = "GET";
                request.url = "http://master:5050";
                requests.add(request);
            }

            KeyedMessage<byte[], byte[]> message = new KeyedMessage<>(topic, requests.toByteArray());
            producer.send(message);
        }

        producer.close();
    }
}
