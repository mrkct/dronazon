package it.cutecchia.sdp.dronazon;

import java.util.Random;
import java.util.Timer;

public class Main {
    private static final int TIME_BETWEEN_EVERY_ORDER = 5000;
    private static final String MQTT_URL = "tcp://localhost:1883";
    private static final String MQTT_ORDER_TOPIC = "dronazon/smartcity/orders/";

    public static void main(String[] args) {
        try {
            var timer = new Timer();
            var random = new Random();
            var mqttBroker = new MQTTOrderPublisher(MQTT_URL, MQTT_ORDER_TOPIC);
            var orderGenerator = new PeriodicOrderGenerator(TIME_BETWEEN_EVERY_ORDER, timer, random);

            orderGenerator.setOnOrderReceivedListener(mqttBroker::publishOrder);
            orderGenerator.begin();
        } catch(Exception e) {
            // TODO: Handle connection error to mqtt server etc.
            e.printStackTrace();
        }
    }
}
