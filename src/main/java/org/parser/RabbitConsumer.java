package org.parser;

import com.google.gson.Gson;
import com.rabbitmq.client.*;
import entities.Message;
import entities.News;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

import static org.parser.Parser.startParsing;

public class RabbitConsumer {
    private final Connection connection;
    private final Channel channel;
    private AtomicInteger totalLinks = new AtomicInteger(0);
    public static String CONSUMER_QUEUE_NAME = "consumer_queue";
    private final Object lock = new Object();


    public RabbitConsumer() throws IOException, TimeoutException {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setUsername("rabbitmq");
        factory.setPassword("rabbitmq");
        factory.setVirtualHost("/");
        factory.setHost("127.0.0.1");
        factory.setPort(5672);

        connection = factory.newConnection();
        channel = connection.createChannel();

        channel.queueDeclare(RabbitProducer.PRODUCER_QUEUE_NAME, false, false, false, null);
        channel.queueDeclare(CONSUMER_QUEUE_NAME, false, false, false, null);
    }

    public void getLinksFromRabbitMQ() {
        try {
            Gson gson = new Gson();
            long timeoutMillis = 60000;
            long lastMessageTime = System.currentTimeMillis();

            while (true) {
                GetResponse response = channel.basicGet(RabbitProducer.PRODUCER_QUEUE_NAME, false);
                if (response == null) {
                    if (System.currentTimeMillis() - lastMessageTime > timeoutMillis) {
                        System.out.println("No messages received within timeout. Exiting...");
                        break;
                    }
                    Thread.sleep(1000);
                    continue;
                }

                lastMessageTime = System.currentTimeMillis();

                byte[] body = response.getBody();
                long tag = response.getEnvelope().getDeliveryTag();
                String message = new String(body, StandardCharsets.UTF_8);

                System.err.println("Message received " + message);

                Message msg = gson.fromJson(message, Message.class);
                if (msg == null || msg.getLink() == null) {
                    System.err.println("Invalid message format or missing link");
                    channel.basicNack(tag, false, false);
                    continue;
                }

                try {
                    String link = msg.getLink();
                    News news = startParsing(link);
                    if (news != null) {
                            synchronized (lock){
                            String newsJson = gson.toJson(news);
                            channel.basicPublish("", CONSUMER_QUEUE_NAME, null, newsJson.getBytes());
                            System.out.println("News sent to queue: " + news.getLink());
                        }
                    }
                    channel.basicAck(tag, false);
                    totalLinks.incrementAndGet();
                    System.err.println("Message deleted " + message);
                } catch (Exception ex) {
                    ex.printStackTrace(System.err);
                    try {
                        channel.basicNack(tag, false, true);
                    } catch (IOException ioException) {
                        ioException.printStackTrace(System.err);
                    }
                }
            }

            channel.close();
            connection.close();

        } catch (Exception e) {
            e.printStackTrace(System.err);
        }
    }
    public int getTotalLinks() {
        return totalLinks.get();
    }
}

