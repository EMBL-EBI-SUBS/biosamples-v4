package uk.ac.ebi.biosamples.neo;

import java.util.Collections;
import java.util.concurrent.atomic.AtomicLong;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.AMQP.Basic;

import uk.ac.ebi.biosamples.MessageContent;
import uk.ac.ebi.biosamples.Messaging;

@Service
public class Neo4JMessageHandler {
	
	private final NeoMessageBufferTransaction neoMessageBufferTransaction;
	private final RabbitTemplate rabbitTemplate;
	private final AtomicLong lastMessage = new AtomicLong(System.nanoTime());
	
	public Neo4JMessageHandler(RabbitTemplate rabbitTemplate, 
			NeoMessageBufferTransaction neoMessageBufferTransaction) {
		this.rabbitTemplate = rabbitTemplate;
		this.neoMessageBufferTransaction = neoMessageBufferTransaction;
	}

    @RabbitListener(queues=Messaging.queueToBeIndexedNeo4J)
    @Transactional
    public void processMessageContent(MessageContent messageContent) {
		//store the time when we last handled a message
		lastMessage.set(System.nanoTime());
		
		this.neoMessageBufferTransaction.save(Collections.singletonList(messageContent));

		//send on the next queue
		rabbitTemplate.convertAndSend(Messaging.exchangeForIndexingSolr, "", messageContent);

		//store the time when we last handled a message
		lastMessage.set(System.nanoTime());
		
    }
    
    public long getLastMessage() {
    	return lastMessage.get();
    }
}
