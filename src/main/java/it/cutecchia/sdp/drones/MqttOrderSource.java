package it.cutecchia.sdp.drones;

import com.google.gson.Gson;
import it.cutecchia.sdp.common.Log;
import it.cutecchia.sdp.common.Order;
import javax.annotation.Nonnull;
import org.eclipse.paho.client.mqttv3.*;

public class MqttOrderSource implements OrderSource, MqttCallback {
  private static final int QOS = 2;
  private static final Gson gson = new Gson();

  private final MqttClient client;
  private final String topic;

  private OrderListener listener;

  public MqttOrderSource(@Nonnull String brokerAddress, int port, @Nonnull String topic)
      throws MqttException {
    String connectionString = String.format("tcp://%s:%d", brokerAddress, port);
    this.client = new MqttClient(connectionString, MqttClient.generateClientId());
    this.topic = topic;
  }

  @Override
  public void start(@Nonnull OrderListener listener) {
    this.listener = listener;

    MqttConnectOptions options = new MqttConnectOptions();
    options.setCleanSession(true);
    client.setCallback(this);
    try {
      client.connect(options);
      client.subscribe(topic, QOS);
      Log.info("Connected successfully to MQTT broker");
    } catch (MqttException e) {
      Log.error("Failed to connect to MQTT broker. The stack trace follows:");
      e.printStackTrace();
    }
  }

  @Override
  public void stop() {
    try {
      client.disconnect();
      Log.info("Disconnected successfully from MQTT broker");
    } catch (MqttException e) {
      Log.error("Failed to disconnect from the MQTT broker. The stack trace follows:");
      e.printStackTrace();
    }
  }

  // ^MqttCallback
  @Override
  public void connectionLost(Throwable cause) {
    Log.error("Connection to the MQTT broker dropped. The stack trace follows");
    cause.printStackTrace();
  }

  @Override
  public void messageArrived(String topic, MqttMessage message) throws Exception {
    assert (topic.equals(this.topic));
    String jsonMessage = new String(message.getPayload());
    Order order = gson.fromJson(jsonMessage, Order.class);

    listener.onOrderReceived(order);
  }

  @Override
  public void deliveryComplete(IMqttDeliveryToken token) {}
}
