package com.mrdotxin.mbi.bizmq.init;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

public class InitMBIQueue {


    public static void main(String[] args) {
        ConnectionFactory connectionFactory = new ConnectionFactory();
        connectionFactory.setHost("localhost");

        try (Connection connection = connectionFactory.newConnection()) {
            Channel channel = connection.createChannel();

            channel.exchangeDeclare("MBIDirectExchange", "direct", true);
            channel.queueDeclare("ChartGenQueue", true, false, false, null);
            channel.queueBind("ChartGenQueue", "MBIDirectExchange", "GeneratingChart");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
