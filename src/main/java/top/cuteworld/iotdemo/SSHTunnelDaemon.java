/**
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0.
 */

package top.cuteworld.iotdemo;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import software.amazon.awssdk.crt.CRT;
import software.amazon.awssdk.crt.CrtResource;
import software.amazon.awssdk.crt.CrtRuntimeException;
import software.amazon.awssdk.crt.mqtt.MqttClientConnection;
import software.amazon.awssdk.crt.mqtt.MqttClientConnectionEvents;
import software.amazon.awssdk.crt.mqtt.QualityOfService;
import software.amazon.awssdk.iot.iotjobs.model.RejectedError;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;

public class SSHTunnelDaemon {

    private static Log log = LogFactory.getLog(SSHTunnelDaemon.class);

    // When run normally, we want to exit nicely even if something goes wrong
    // When run from CI, we want to let an exception escape which in turn causes the
    // exec:java task to return a non-zero exit code
    static String ciPropValue = System.getProperty("aws.crt.ci");
    static boolean isCI = ciPropValue != null && Boolean.valueOf(ciPropValue);

    static String topic = "sdk/test/java";
    static String message = "Hello World!";
    static int messagesToPublish = 10;

    static CommandLineUtils cmdUtils;

    static void onRejectedError(RejectedError error) {
        log.error("Request rejected: " + error.code.toString() + ": " + error.message);
    }

    /*
     * When called during a CI run, throw an exception that will escape and fail the exec:java task
     * When called otherwise, print what went wrong (if anything) and just continue (return from main)
     */
    static void onApplicationFailure(Throwable cause) {
        if (isCI) {
            throw new RuntimeException("BasicPubSub execution failure", cause);
        } else if (cause != null) {
            log.error("Exception encountered: " + cause.toString());
        }
    }

    public static void main(String[] args) {
//        System.out.println(new File("test.pgk").getAbsoluteFile());
        cmdUtils = new CommandLineUtils();
        cmdUtils.registerProgramName("SSHTunnelDaemon");
        cmdUtils.addCommonMQTTCommands();
        cmdUtils.addCommonTopicMessageCommands();
        cmdUtils.registerCommand("key", "<path>", "Path to your key in PEM format.");
        cmdUtils.registerCommand("cert", "<path>", "Path to your client certificate in PEM format.");
        cmdUtils.registerCommand("client_id", "<int>", "Client id to use (optional, default='test-*').");
        cmdUtils.registerCommand("port", "<int>", "Port to connect to on the endpoint (optional, default='8883').");
        cmdUtils.registerCommand("count", "<int>", "Number of messages to publish (optional, default='10').");
        cmdUtils.sendArguments(args);

//        topic = cmdUtils.getCommandOrDefault("topic", topic);
        String thing = cmdUtils.getCommandRequired("thing", "Please give thing name registered at IoT Core");

//        final String tunnelNotificationTopic = String.format("$aws/things/%s/tunnels/notify", thing);
        message = cmdUtils.getCommandOrDefault("message", message);
        messagesToPublish = Integer.parseInt(cmdUtils.getCommandOrDefault("count", String.valueOf(messagesToPublish)));

        MqttClientConnectionEvents callbacks = new MqttClientConnectionEvents() {
            @Override
            public void onConnectionInterrupted(int errorCode) {
                if (errorCode != 0) {
                    log.warn("Connection interrupted: " + errorCode + ": " + CRT.awsErrorString(errorCode));
                } else {
                    log.warn("Connection interrupted zero: " + errorCode + ": " + CRT.awsErrorString(errorCode));
                }
            }

            @Override
            public void onConnectionResumed(boolean sessionPresent) {
                log.warn("Connection resumed: " + (sessionPresent ? "existing session" : "clean session"));
            }
        };

        try {

            /**
             * 通过证书等做MQTT的连接
             */
            MqttClientConnection mqttConnection = cmdUtils.buildMQTTConnection(callbacks);
            if (mqttConnection == null) {
                onApplicationFailure(new RuntimeException("MQTT mqttConnection creation failed!"));
                throw new RuntimeException("MQTT mqttConnection creation failed!");
            }

            CompletableFuture<Boolean> connected = mqttConnection.connect();
            try {
                boolean sessionPresent = connected.get();
                log.info("Connected to " + (!sessionPresent ? "new" : "existing") + " session!");
            } catch (Exception ex) {
                throw new RuntimeException("Exception occurred during connect", ex);
            }

            CountDownLatch receivedSignal = new CountDownLatch(10);
            //启动健康心跳发送线程
//            startHealthProbeThread(mqttConnection);
            //启动一个线程做独立的topic接收测试
            startTestReceiveThread(mqttConnection);
            //启动一个线程接收tunnel notification消息并启动localproxy
            startSShTunnelListenThread(mqttConnection, thing);

            // await forever to make program resident in backend
            receivedSignal.await();

            CompletableFuture<Void> disconnected = mqttConnection.disconnect();
            disconnected.get();

            // Close the mqttConnection now that we are completely done with it.
            mqttConnection.close();

        } catch (CrtRuntimeException | InterruptedException | ExecutionException ex) {
            onApplicationFailure(ex);
//            System.exit(-1);//fail to connect to mqtt quit
        }

        CrtResource.waitForNoResources();
        log.info("Complete!");
    }

    private static void startHealthProbeThread(MqttClientConnection mqttConnection) {
        new Thread(new HealthProbeRunnable(mqttConnection, "$aws/thing/ap-southeast-1/health")).start();
    }

    private static void startSShTunnelListenThread(MqttClientConnection mqttConnection, String thingName) {
        new Thread(new SSHTunnelTopicListener(mqttConnection, thingName)).start();
    }

    private static void startTestReceiveThread(MqttClientConnection mqttConnection) {
        new Thread(() -> {
            try {
                String topic = "sdk/test/java";
                log.info("Test subscribe for topic,  " + topic);
                CompletableFuture<Integer> subscribed = mqttConnection.subscribe(topic, QualityOfService.AT_LEAST_ONCE, (message) -> {
                    String payload = new String(message.getPayload(), StandardCharsets.UTF_8);
                    if (log.isInfoEnabled()) {
                        log.info("Topic [" + topic + "]");
                        log.info("message: " + payload);
                    }

                });
                Integer integer = subscribed.get();
//                System.out.println("future id --- > " + integer);
                log.info("Test subscribe for " + topic + " is ok");
            } catch (software.amazon.awssdk.crt.mqtt.MqttException exception) {
                log.error("Failed to subscribe topic for with " + topic, exception);
//                System.exit(-1);//fail to sub
            } catch (Exception e) {
                log.error("Failed to handle topic for with " + topic, e);
            }
        }).start();
    }
}
