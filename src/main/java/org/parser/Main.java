package org.parser;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class Main {
    private static final String BASE_URL = "https://vm.ru";
    private static final Logger logger = LogManager.getLogger(Main.class);
    public static void main(String[] args) throws IOException, TimeoutException {
        final int NUM_THREADS = 5;
        ThreadRunner threadRunner = new ThreadRunner(NUM_THREADS);

        RabbitProducer rabbitProducer = new RabbitProducer();
        try {
            rabbitProducer.sendLinksToRabbitMQ(BASE_URL);
        } catch (IOException | InterruptedException | TimeoutException e) {
            logger.error(e.getMessage());
        }

        threadRunner.runTasks(() -> {
            try {
                RabbitConsumer rabbitConsumer = new RabbitConsumer();
                rabbitConsumer.getLinksFromRabbitMQ();
                System.out.println("Total links received: " + rabbitConsumer.getTotalLinks());
            } catch (IOException | TimeoutException e) {
                logger.error(e.getMessage());
            }
            return null;
        });

    }
}