package ly.stealth.punxsutawney;

import kafka.javaapi.producer.Producer;
import kafka.producer.KeyedMessage;
import kafka.producer.ProducerConfig;
import org.apache.log4j.BasicConfigurator;

import java.io.IOException;
import java.util.Properties;

public class SampleEmitter {
    public static void main(String[] args) throws IOException {
        BasicConfigurator.configure();
        String topic = Marathon.App.DEFAULT_ID;

        Properties props = new Properties();
        props.put("metadata.broker.list", "master:9092");
        Producer<byte[], byte[]> producer = new Producer<>(new ProducerConfig(props));

        for (int i = 0; i < 10; i++) {
            Requests requests = new Requests();
            for (int j = 0; j < 10000; j++) {
                Requests.Request request = new Requests.Request();
                request.method = "GET";
                request.url = "http://192.168.3.1:80";
                requests.add(request);
            }

            KeyedMessage<byte[], byte[]> message = new KeyedMessage<>(topic, (i + "").getBytes(), requests.toByteArray());
            producer.send(message);
        }

        producer.close();
    }
}
