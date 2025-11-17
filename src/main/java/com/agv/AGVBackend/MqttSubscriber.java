package com.agv.AGVBackend;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.paho.client.mqttv3.*;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class MqttSubscriber {
    private final TelemetryRepository repo;
    private final SimpMessagingTemplate websocket;
    private MqttClient client;
    private final ObjectMapper mapper = new ObjectMapper();

    @Value("${mqtt.broker.url}")
    private String brokerUrl;

    @PostConstruct
    public void init() {
        connect();
    }

    @PreDestroy
    public void cleanup() {
        disconnect();
    }

    public MqttSubscriber(TelemetryRepository repo, SimpMessagingTemplate websocket) {
        this.repo = repo;
        this.websocket = websocket;
    }

    private void connect() {
        try {
            client = new MqttClient(brokerUrl, MqttClient.generateClientId());

            MqttConnectOptions options = new MqttConnectOptions();
            options.setAutomaticReconnect(true);
            options.setCleanSession(true);
            options.setConnectionTimeout(10);

            client.setCallback(new MqttCallbackExtended() {
                @Override
                public void connectComplete(boolean reconnect, String serverURI) {
                    log.info("Connected to MQTT broker: {}", serverURI);
                    subscribe();
                }

                @Override
                public void connectionLost(Throwable cause) {
                    log.error("MQTT connection lost", cause);
                }

                @Override
                public void messageArrived(String topic, MqttMessage message) {
                    handleMessage(topic, message);
                }

                @Override
                public void deliveryComplete(IMqttDeliveryToken token) {
                    // Not needed for subscriber
                }
            });

            client.connect(options);
            log.info("Connecting to MQTT broker: {}", brokerUrl);

        } catch (MqttException e) {
            log.error("Failed to connect to MQTT broker", e);
            scheduleReconnect();
        }
    }

    private void subscribe() {
        try {
            client.subscribe("agv/telemetry", 1);
            log.info("MQTT subscription established on topic: agv/telemetry");
        } catch (MqttException e) {
            log.error("Failed to subscribe to topic", e);
        }
    }

    private void handleMessage(String topic, MqttMessage message) {
        try {
            String payload = new String(message.getPayload());
            Telemetry telemetry = mapper.readValue(payload, Telemetry.class);
            repo.save(telemetry);
            websocket.convertAndSend("/topic/telemetry", telemetry);
            log.debug("Received and broadcasted: {}", payload);
        } catch (Exception e) {
            log.error("Error processing message", e);
        }
    }

    private void disconnect() {
        try {
            if (client != null && client.isConnected()) {
                client.disconnect();
                client.close();
            }
        } catch (MqttException e) {
            log.error("Error disconnecting from MQTT broker", e);
        }
    }

    private void scheduleReconnect() {
        // Simple implementation - try to reconnect after 5 seconds
        new Thread(() -> {
            try {
                Thread.sleep(5000);
                log.info("Attempting to reconnect...");
                connect();
            } catch (InterruptedException e) {
                log.error("Reconnection interrupted", e);
                Thread.currentThread().interrupt();
            }
        }).start();
    }
}