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
