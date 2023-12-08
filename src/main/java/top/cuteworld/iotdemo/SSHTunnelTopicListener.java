package top.cuteworld.iotdemo;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.json.JSONArray;
import org.json.JSONObject;
import software.amazon.awssdk.crt.mqtt.MqttClientConnection;
import software.amazon.awssdk.crt.mqtt.QualityOfService;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;

/**
 * 启动监听 $aws/things/${thingName}/tunnels/notify topic来启动localproxy, 以支持SSHTunnel远程调试
 *
 * <a href="https://docs.aws.amazon.com/zh_cn/iot/latest/developerguide/tunneling-tutorial-quick-setup.html">打开隧道并使用基于浏览器的 SSH 访问远程设备</a>
 */
public class SSHTunnelTopicListener implements Runnable {

    private static Log log = LogFactory.getLog(SSHTunnelTopicListener.class);

    public static final char NEW_LINE_CHAR = '\n';

    private final MqttClientConnection mqttClientConnection;
    private final String thingName;
    private final String tunnelNotificationTopic;

    public SSHTunnelTopicListener(MqttClientConnection mqttClientConnection, String thingName) {
        this.mqttClientConnection = mqttClientConnection;
        this.thingName = thingName;
        tunnelNotificationTopic = String.format("$aws/things/%s/tunnels/notify", thingName);
        log.info(" Listen tunnel notification topic: " + tunnelNotificationTopic);
    }

    @Override
    public void run() {
        try {
            CompletableFuture<Integer> subscribed = mqttClientConnection.subscribe(tunnelNotificationTopic, QualityOfService.AT_LEAST_ONCE, (message) -> {
                String payload = new String(message.getPayload(), StandardCharsets.UTF_8);
                log.info("tunnel notification message: " + payload);
                parseMessageToStartLocalProxy(payload);
            });
            subscribed.get();
        } catch (Exception e) {
            log.error("Failed to subscribe topic ->" + tunnelNotificationTopic, e);
        }
    }

    /**
     * 解析CAT并启动本地localproxy程序
     *
     * @param message
     */
    private void parseMessageToStartLocalProxy(String message) {
        try {
            final JSONObject json = new JSONObject(message);

            final String accessToken = json.getString("clientAccessToken");
            final String region = json.getString("region");


            final String clientMode = json.getString("clientMode");
            if (!clientMode.equals("destination")) {
                throw new RuntimeException("Client mode " + clientMode + " in the MQTT message is not expected");
            }

            final JSONArray servicesArray = json.getJSONArray("services");
            if (servicesArray.length() > 1) {
                throw new RuntimeException("Services in the MQTT message has more than 1 service");
            }
            final String service = servicesArray.get(0).toString();
            if (!service.equals("SSH")) {
                throw new RuntimeException("Service " + service + " is not supported");
            }

            // Start the destination local proxy in a separate process to connect to the SSH Daemon listening port 22
//            String localproxy_path = System.getenv("localproxy_path");
            final ProcessBuilder pb = new ProcessBuilder("localproxy", "-t", accessToken, "-r", region, "-d", "localhost:22", "-v", "5");
            log.info("command - ");
            log.info(pb.command().stream().reduce((s, s2) -> s + " " + s2));
            Process localProxyProcess = pb.start();
            try (InputStreamReader isr = new InputStreamReader(localProxyProcess.getInputStream())) {
                int c;

                StringBuffer buffer = new StringBuffer();
                while ((c = isr.read()) >= 0) {
                    buffer.append((char) c);
                    if (NEW_LINE_CHAR == c) {
                        log.debug("---localproxy--->" + buffer.toString());
                        buffer = new StringBuffer();
                    }
                }
            }

            try (InputStreamReader isr = new InputStreamReader(localProxyProcess.getErrorStream())) {
                int c;
                while ((c = isr.read()) >= 0) {
                    log.debug("---localproxy error--->" + (char) c);
                }
            }

        } catch (Exception e) {
            log.error("Failed to start localproxy by tunnel notify message", e);
        }
    }
}
