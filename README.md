# punxsutawney
An tool that allows for replaying load over and over and over (and over) again.

It uses Mesos/Marathon for running its HTTP clients.
It uses Kafka to read traffic data.

Primary components are:
- CLI - CLI to start HttpClients on Mesos (see Main);
- HttpClient - client running on Mesos, that generates HTTP traffic;
- Emitter - emitter (see SampleEmitter), producing traffic to Kafka topic (consumed by HttpClient(s));

# Running
In order to run the tool following steps should be executed:
1. Build via `./gradlew jar`
2. Start `./punxsutawney.sh`
3. Verify app is running via Mesos & Marathon UIs
4. Start emitting traffic to Kafka topic `punxsutawney` (see SampleEmitter)

Note: if running N HttpClient instances, Kafka topic should contain at least N partitions,
in order for HttpClients to be able to consume concurrently.

# Traffic generation
HttpClient consumes Kafka messages, containing serialized Requests instances.
Each Requests instance is designed to carry a batch of HTTP requests.
Emitter should batch 1k - 10k requests together to populate Requests object.