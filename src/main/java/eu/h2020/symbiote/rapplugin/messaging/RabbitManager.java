package eu.h2020.symbiote.rapplugin.messaging;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessagePropertiesBuilder;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.support.CorrelationData;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

/**
 * Bean used to manage internal communication using RabbitMQ. It is responsible
 * for declaring exchanges and using routing keys from centralized config
 * server.
 *
 * @author mateuszl, Petar Krivic, Mario Kusek
 */
@Component
public class RabbitManager {
    private static final Logger LOG = LoggerFactory.getLogger(RabbitManager.class);
    private static final int REPLY_TIMEOUT = 20_000;

    private RabbitTemplate rabbitTemplate;

    public RabbitManager(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(SerializationFeature.INDENT_OUTPUT, true);
        mapper.setSerializationInclusion(JsonInclude.Include.NON_EMPTY);
        Jackson2JsonMessageConverter messageConverter = new Jackson2JsonMessageConverter(mapper);
        rabbitTemplate.setMessageConverter(messageConverter);
    }

    /**
     * Method publishes given message to the given exchange and routing key. Props
     * are set for correct message handle on the receiver side.
     *
     * @param exchange
     *            name of the proper Rabbit exchange, adequate to topic of the
     *            communication
     * @param routingKey
     *            name of the proper Rabbit routing key, adequate to topic of the
     *            communication
     * @param message
     *            message content in JSON String format
     */
    public void sendMessage(String exchange, String routingKey, String message) {
        rabbitTemplate.send(exchange, routingKey, new Message(message.getBytes(StandardCharsets.UTF_8),
            MessagePropertiesBuilder.newInstance()
                .setContentType("plain/text")
                .setHeader("__TypeId__", String.class.getName())
                .build()));
    }

    /**
     * Method publishes given message to the given exchange and routing key. Props
     * are set for correct message handle on the receiver side.
     *
     * @param exchange
     *            name of the proper Rabbit exchange, adequate to topic of the
     *            communication
     * @param routingKey
     *            name of the proper Rabbit routing key, adequate to topic of the
     *            communication
     * @param obj
     *            object content is mapped to JSON String by using Jackson2 and send
     *            as payload
     */
    public void sendMessage(String exchange, String routingKey, Object obj) {
        rabbitTemplate.convertAndSend(exchange, routingKey, obj);
    }

    /**
     * Method used to send message via RPC (Remote Procedure Call) pattern. In this
     * implementation it covers asynchronous Rabbit communication with synchronous
     * one, as it is used by conventional REST facade. Before sending a message, a
     * temporary response queue is declared and its name is passed along with the
     * message. When a consumer handles the message, it returns the result via the
     * response queue. Since this is a synchronous pattern, it uses timeout of 20
     * seconds. If the response doesn't come in that time, the method returns with
     * null result.
     *
     * @param exchange
     *            name of the exchange to send message to
     * @param routingKey
     *            routing key to send message to
     * @param stringMessage
     *            message to be sent
     * @return response from the consumer or null if timeout occurs
     */
    public String sendRpcMessage(String exchange, String routingKey, String stringMessage) {
        LOG.info("Sending RPC message: {}", stringMessage);

        String correlationId = UUID.randomUUID().toString();
        Message sendMessage = new Message(stringMessage.getBytes(StandardCharsets.UTF_8),
            MessagePropertiesBuilder.newInstance()
                .setContentType("plain/text")
                .setHeader("__TypeId__", String.class.getName())
                .setCorrelationIdString(correlationId)
                .build()
            );
        rabbitTemplate.setReplyTimeout(REPLY_TIMEOUT);
        Message receivedMessage = rabbitTemplate.sendAndReceive(exchange, routingKey, sendMessage);
        if(receivedMessage == null) {
            LOG.info("Timeout in RPC receiving. Send: {}", sendMessage);
            return null;
        }

        LOG.info("RPC Response received: " + receivedMessage);

        byte[] body = receivedMessage.getBody();
        LOG.info("client received: {}", body);
        return new String(body, StandardCharsets.UTF_8);
    }

    /**
     * Method used to send message via RPC (Remote Procedure Call) pattern. In this
     * implementation it covers asynchronous Rabbit communication with synchronous
     * one, as it is used by conventional REST facade. Before sending a message, a
     * temporary response queue is declared and its name is passed along with the
     * message. When a consumer handles the message, it returns the result via the
     * response queue. Since this is a synchronous pattern, it uses timeout of 20
     * seconds. If the response doesn't come in that time, the method returns with
     * null result.
     *
     * @param exchange
     *            name of the exchange to send message to
     * @param routingKey
     *            routing key to send message to
     * @param obj
     *            object content is mapped to JSON String by using Jackson2 and send
     *            as payload
     * @return response from the consumer or null if timeout occurs
     */
    public Object sendRpcMessage(String exchange, String routingKey, Object obj) {
        LOG.info("Sending RPC obj: {}", obj);

        String correlationId = UUID.randomUUID().toString();
        rabbitTemplate.setReplyTimeout(REPLY_TIMEOUT);
        Object receivedObj = rabbitTemplate.convertSendAndReceive(exchange, routingKey, obj, new CorrelationData(correlationId));
        if(receivedObj == null) {
            LOG.info("Timeout in RPC receiving obj. Send: {}", obj);
            return null;
        }

        LOG.info("RPC Response received obj: " + receivedObj);

        return receivedObj;
    }
}
