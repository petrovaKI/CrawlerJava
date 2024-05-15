package org.parser;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

public class Main {
    private static final String BASE_URL = "https://vm.ru";

    public static void main(String[] args) throws IOException, TimeoutException {
        final int NUM_THREADS = 5;
        ThreadRunner threadRunner = new ThreadRunner(NUM_THREADS);

        RabbitProducer rabbitProducer = new RabbitProducer();
        try {
            rabbitProducer.sendLinksToRabbitMQ(BASE_URL);
        } catch (IOException | InterruptedException | TimeoutException e) {
            e.printStackTrace();
        }

        threadRunner.runTasks(() -> {
            try {
                RabbitConsumer rabbitConsumer = new RabbitConsumer();
                rabbitConsumer.getLinksFromRabbitMQ();
                System.out.println("Total links received: " + rabbitConsumer.getTotalLinks());
            } catch (IOException | TimeoutException e) {
                e.printStackTrace();
            }
            return null;
        });

    }
}