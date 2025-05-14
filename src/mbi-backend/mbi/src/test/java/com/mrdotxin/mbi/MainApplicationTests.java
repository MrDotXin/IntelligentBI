package com.mrdotxin.mbi;

import com.mrdotxin.mbi.bizmq.MBIMQProducer;
import com.mrdotxin.mbi.manager.RedisLimitManager;
import com.mrdotxin.mbi.ai.AIChartService;
import com.mrdotxin.mbi.utils.ExcelUtils;
import com.rabbitmq.client.*;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeoutException;

/**
 * 主类测试
 *
 */
@Nested
@SpringBootTest
class MainApplicationTests {

    @Resource
    private AIChartService aiChartService;

    @Resource
    private RedisLimitManager redisLimitManager;

    @Resource
    private MBIMQProducer mbimqProducer;

    static final String QUEUE_NAME = "HELLO";

    @Value("${MQ.ChartGenKey}")
    private String chartGenKey;

    @Value("${MQ.ChartGenQueue}")
    private String chartGenQueue;

    @Value("${MQ.MBIExchange}")
    private String mbiExchange;

    @Test
    void contextLoads() {
    }


    @Test
    void testAIPort() throws IOException {
        ClassPathResource resource = new ClassPathResource("test_excel.xlsx");
        InputStream inputStream = resource.getInputStream();

        // 创建 MockMultipartFile 对象
        MultipartFile multipartFile = new MockMultipartFile(
            "file", // 文件名
            "test_excel.xlsx", // 原始文件名
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", // 文件类型
            inputStream // 文件内容
        );

        String content = ExcelUtils.convertToCSV(multipartFile);
        String outcome = aiChartService.doAIChart(
                Arrays.asList(
                        "data:\"",
                        content,
                        "\"}",
                        "goal: \"分析增长趋势\"",
                        "chartType: \"Line Chart"
                )
        );

        System.out.println(outcome);
    }

    @Test
    void testRedisson() {
        String userId = "1234";
        for (int i = 0; i < 10; i ++) {
            redisLimitManager.doRateLimit(userId);
            System.out.println("通过 " + (i + 1));
        }
    }

    @Test
    void testMQ() {

        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("localhost");
        try {
            Connection connection = factory.newConnection();
            Channel channel = connection.createChannel();
            channel.queueDeclare(QUEUE_NAME, false, false, false, null);

            String message = "Hello World";
            channel.basicPublish("", QUEUE_NAME, null, message.getBytes());
            System.out.println(" [x] Sent '" + message + "'");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    void testMQConsumer() {
        ConnectionFactory connectionFactory = new ConnectionFactory();
        connectionFactory.setHost("localhost");

        try {
            Connection connection = connectionFactory.newConnection();
            Channel channel = connection.createChannel();
            channel.queueDeclare(QUEUE_NAME, false, false, false, null);

            System.out.println("等待消息");
            DeliverCallback deliverCallback = (consumerTag, delivery) -> {
                String message = new String(delivery.getBody(), StandardCharsets.UTF_8);
                System.out.println("[x] Received '" + message + "'");
            };

            channel.basicConsume(QUEUE_NAME, true, deliverCallback, consumerTag -> {});
        } catch(Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    void testMQMultiTests() {
        final String name = "Multi_Consumer";
        ConnectionFactory connectionFactory = new ConnectionFactory();
        connectionFactory.setHost("localhost");

        try (Connection connection = connectionFactory.newConnection()) {

            Channel channel = connection.createChannel();
            channel.queueDeclare(name, true, false, false, null);
            List<String> messages = Arrays.asList("h1", "h2");
            for (String message : messages) {
                channel.basicPublish(
                        "",
                        name,
                        MessageProperties.PERSISTENT_TEXT_PLAIN,
                        message.getBytes(StandardCharsets.UTF_8)
                );
            }

        } catch (IOException | TimeoutException e) {
            e.printStackTrace();
        }
    }

    @Test
    void testMultiConsumer() {
        final String name = "Multi_Consumer";
        ConnectionFactory connectionFactory = new ConnectionFactory();
        connectionFactory.setHost("localhost");

        try (Connection connection = connectionFactory.newConnection()) {
            for (int i = 0; i < 2; i ++) {
                Channel channel = connection.createChannel();
                channel.queueDeclare(name, true, false, false, null);
                channel.basicQos(1); // 控制每个消费者最多同时处理的消息数量
                DeliverCallback deliverCallback = (consumerTag, delivery) -> {
                    String message = new String(delivery.getBody(), StandardCharsets.UTF_8);

                    try {
                        System.out.println("[x] " + "  " + channel + " Received '" + message + "'");
                        Thread.sleep(10000);
                    } catch (Exception e) {
                        e.printStackTrace();
                        channel.basicNack(delivery.getEnvelope().getDeliveryTag(), false, false);
                    } finally {
                        System.out.println("[x] " + " " + channel + " Done wih message '" + message + "'");
                        channel.basicAck(delivery.getEnvelope().getDeliveryTag(), false);
                    }
                };

                channel.basicConsume(name, false, deliverCallback, consumerTag -> {});
            }
            Thread.sleep(100000);
        } catch (IOException | TimeoutException | InterruptedException e) {
            e.printStackTrace();
        }

    }


    @Test
    void testFanoutProducer() {
        final String FANOUT_NAME = "Fanout_Producer";
        ConnectionFactory connectionFactory = new ConnectionFactory();
        connectionFactory.setHost("localhost");

        try (Connection connection = connectionFactory.newConnection()) {
            Channel channel = connection.createChannel();

            channel.exchangeDeclare(FANOUT_NAME, "fanout", false);
            List<String> messages = Arrays.asList("h1", "h2", "h3");

            for (String message : messages) {
                channel.basicPublish(
                        FANOUT_NAME,
                        "",
                        null,
                        message.getBytes(StandardCharsets.UTF_8)
                );
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    void testFanoutConsumer() {
        final String FANOUT_NAME = "Fanout_Producer";

        ConnectionFactory connectionFactory = new ConnectionFactory();
        connectionFactory.setHost("localhost");

        try (Connection connection = connectionFactory.newConnection()) {
            for (int i = 0; i < 2; i ++) {
                final String queueName = "fanout_queue_" + i;
                Channel channel = connection.createChannel();
                channel.exchangeDeclare(FANOUT_NAME, "fanout", false);
                channel.queueDeclare(queueName, true, false, false, null);
                channel.queueBind(queueName, FANOUT_NAME , "");

                DeliverCallback deliverCallback = (consumerTag, delivery) -> {
                    String message = new String(delivery.getBody(), StandardCharsets.UTF_8);

                    System.out.println("[x] " + "  " + queueName +  " Received '" + message + "'");
                };

                channel.basicConsume(queueName, true, deliverCallback, consumerTag -> {});
            }
        } catch(Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    void testMBIMQInterface() {
        mbimqProducer.sendMessage(mbiExchange, chartGenKey, "你好");
    }

     @Test
    void testDirectConsumer() {
        ConnectionFactory connectionFactory = new ConnectionFactory();
        connectionFactory.setHost("localhost");

        try (Connection connection = connectionFactory.newConnection()) {
            Channel channel = connection.createChannel();
            channel.exchangeDeclare(mbiExchange, "direct", true);
            channel.queueDeclare(chartGenQueue, true, false, false, null);
            channel.queueBind(chartGenQueue, mbiExchange , chartGenKey);

            DeliverCallback deliverCallback = (consumerTag, delivery) -> {
                String message = new String(delivery.getBody(), StandardCharsets.UTF_8);

                System.out.println("[x] " + "  " + chartGenQueue +  " Received '" + message + "'");
                channel.basicAck(delivery.getEnvelope().getDeliveryTag(), false);
            };

            channel.basicConsume(chartGenQueue, false, deliverCallback, consumerTag -> {});
        } catch(Exception e) {
            e.printStackTrace();
        }
    }
}
