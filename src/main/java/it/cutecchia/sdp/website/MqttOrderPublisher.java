package it.cutecchia.sdp.website;

import com.google.gson.Gson;
import it.cutecchia.sdp.common.Order;
import java.nio.charset.StandardCharsets;
import java.util.logging.Logger;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;

public class MqttOrderPublisher implements OrderPublisher {
  private static final Logger log = Logger.getLogger(MqttOrderPublisher.class.getName());

  private final String orderTopic;
  private final MqttConnectOptions mqttConnectOptions;
  private final MqttClient mqttClient;
  private final Gson gson = new Gson();

  public MqttOrderPublisher(MqttClient mqttClient, MqttConnectOptions options, String orderTopic) {
    this.orderTopic = orderTopic;
    this.mqttClient = mqttClient;
    this.mqttConnectOptions = options;
  }

  @Override
  public void publishOrder(Order order) {
    try {
      log.info(String.format("Sending order %s...", order.toString()));
      sendMqttMessage(orderTopic, gson.toJson(order));
    } catch (MqttException e) {
      log.warning("Failed to send an order to the mqtt broker. Here is the stack trace: ");
      e.printStackTrace();
    }
  }

  private void sendMqttMessage(String topic, String message) throws MqttException {
    MqttMessage mqttMessage = new MqttMessage(message.getBytes(StandardCharsets.UTF_8));
    mqttMessage.setQos(0);
    mqttMessage.setRetained(false);

    if (!mqttClient.isConnected()) {
      mqttClient.connect(mqttConnectOptions);
    }

    mqttClient.publish(topic, mqttMessage);
  }
}
