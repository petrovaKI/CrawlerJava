package org.parser;

import com.google.gson.Gson;
import com.rabbitmq.client.*;

import entities.Message;

import java.util.List;
import java.util.concurrent.TimeoutException;

import java.io.IOException;

public class RabbitProducer {

    private final Connection connection;
    private final Channel channel;
    public static String PRODUCER_QUEUE_NAME = "producer_queue";

    public RabbitProducer() throws IOException, TimeoutException {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setUsername("rabbitmq");
        factory.setPassword("rabbitmq");
        factory.setVirtualHost("/");
        factory.setHost("127.0.0.1");
        factory.setPort(5672);

        connection = factory.newConnection();
        channel = connection.createChannel();

        channel.queueDeclare(PRODUCER_QUEUE_NAME, false, false, false, null);
    }

    public void sendLinksToRabbitMQ(String startUrl) throws IOException, InterruptedException, TimeoutException {
        List<Message> messagesQueue = Crawler.startCrawling(startUrl);
        Gson gson = new Gson();
        try {
            for (Message message : messagesQueue) {
                String jsonMessage = gson.toJson(message);

                channel.basicPublish("", PRODUCER_QUEUE_NAME, null, jsonMessage.getBytes());
                System.out.println("Link " + message.getLink() + " queued.");
            }
            channel.close();
            connection.close();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}


