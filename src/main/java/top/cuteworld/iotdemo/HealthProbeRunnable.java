package top.cuteworld.iotdemo;

import software.amazon.awssdk.crt.mqtt.MqttClientConnection;
import software.amazon.awssdk.crt.mqtt.MqttMessage;
import software.amazon.awssdk.crt.mqtt.QualityOfService;

import java.nio.charset.StandardCharsets;

/**
 * 发送健康
 */
public class HealthProbeRunnable implements Runnable {

    private final MqttClientConnection mqttClientConnection;
    private final String healthCheckTopic;


    public HealthProbeRunnable(MqttClientConnection mqttClientConnection, String healthCheckTopic) {
        this.mqttClientConnection = mqttClientConnection;
        this.healthCheckTopic = healthCheckTopic;
    }

    @Override
    public void run() {
        while (true) {
            mqttClientConnection.publish(new MqttMessage(healthCheckTopic, composeHealthStatus().getBytes(StandardCharsets.UTF_8), QualityOfService.AT_LEAST_ONCE));
        }
    }

    public String composeHealthStatus() {
        return "1";//TODO 这里做真实的健康状态信息收集
    }
}
