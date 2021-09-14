package it.cutecchia.sdp.website;

import java.util.Random;
import java.util.Timer;
import java.util.logging.Logger;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;

public class Main {
  private static final Logger log = Logger.getLogger(Main.class.getName());

  private static final int TIME_BETWEEN_EVERY_ORDER = 5000;

  private static final String MQTT_USERNAME = "mrkct";
  private static final String MQTT_PASSWORD = "segreto";
  private static final String MQTT_CONNECT_STRING = "tcp://localhost:8000";
  private static final String MQTT_ORDERS_TOPIC = "dronazon/smartcity/orders";

  public static void main(String[] args) {
    try {
      Timer timer = new Timer();
      Random random = new Random();
      OrderPublisher orderPublisher = getOrderPublisher();
      PeriodicOrderGenerator orderGenerator =
          new PeriodicOrderGenerator(TIME_BETWEEN_EVERY_ORDER, timer, random);

      orderGenerator.setOnOrderReceivedListener(orderPublisher::publishOrder);
      orderGenerator.begin();
    } catch (MqttException mqttError) {
      log.warning(String.format("Failed to connect to the MQTT broker: %s", mqttError));
      mqttError.printStackTrace();
    }
  }

  private static OrderPublisher getOrderPublisher() throws MqttException {
    MqttConnectOptions mqttConnectOptions = new MqttConnectOptions();
    mqttConnectOptions.setUserName(MQTT_USERNAME);
    mqttConnectOptions.setPassword(MQTT_PASSWORD.toCharArray());

    MqttClient mqttClient = new MqttClient(MQTT_CONNECT_STRING, MqttClient.generateClientId());
    return new MqttOrderPublisher(mqttClient, mqttConnectOptions, MQTT_ORDERS_TOPIC);
  }
}
