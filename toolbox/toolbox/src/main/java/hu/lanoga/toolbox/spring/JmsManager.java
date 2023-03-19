package hu.lanoga.toolbox.spring;

import java.lang.ref.WeakReference;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

import org.springframework.jms.annotation.JmsListenerConfigurer;
import org.springframework.jms.config.JmsListenerEndpointRegistrar;
import org.springframework.jms.config.SimpleJmsListenerEndpoint;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.jms.listener.MessageListenerContainer;
import org.springframework.stereotype.Component;

import hu.lanoga.toolbox.ToolboxSysKeys.JmsDestinationMode;
import hu.lanoga.toolbox.exception.ToolboxGeneralException;
import hu.lanoga.toolbox.util.ToolboxAssert;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class JmsManager implements JmsListenerConfigurer {

	private static JmsListenerEndpointRegistrar jmsListenerEndpointRegistrar;

	private JmsManager() {
		//
	}

	/**
	 * ne hívd meg kézzel!
	 */
	@Override
	public void configureJmsListeners(final JmsListenerEndpointRegistrar endpointRegistrar) {
		JmsManager.jmsListenerEndpointRegistrar = endpointRegistrar;
	}
	
	/**
	 * (lényeg az, hogy azok a feliratokozók kapják meg az üzenteket, amelyeknek a destStr értékük egyezik a küldésnél használttal)
	 * 
	 * @param destinationMode
	 * @param idStr
	 * 		userId, tenantId stb.
	 * @param subStr
	 * 		finomabb azonosító, description stb. (pl. új megrendelés/vásárlás "new-order")
	 * @return
	 */
	public static String buildDestStr(final JmsDestinationMode destinationMode, final String idStr, final String subStr) {
		
		ToolboxAssert.notNull(idStr);
		ToolboxAssert.isTrue(!idStr.contains("<->"));
		
		if (subStr != null) {
			
			ToolboxAssert.isTrue(!subStr.contains("<->"));
			
			return destinationMode.name() + "<->" + idStr + "<->" + subStr;
		}
		
		return destinationMode.name() + "<->" + idStr;
				
	}

	public static void send(final String destination, final Map<String, ?> payload) {	
		ApplicationContextHelper.getBean(JmsTemplate.class).convertAndSend(destination, payload);
		log.debug("JMS send: " + destination);
	}

	public static String subscribe(final String destination, final WeakReference<Consumer<Map<String, ?>>> wrConsumer) {

		final SimpleJmsListenerEndpoint jmsListenerEndpoint = new SimpleJmsListenerEndpoint();
		jmsListenerEndpoint.setDestination(destination);
		jmsListenerEndpoint.setId(UUID.randomUUID().toString());
		jmsListenerEndpoint.setMessageListener(m -> {

			try {

				final org.apache.activemq.command.ActiveMQMapMessage msg = (org.apache.activemq.command.ActiveMQMapMessage) m;

				final Consumer<Map<String, ?>> consumer = wrConsumer.get();

				if (consumer != null) {
					consumer.accept(msg.getContentMap());
				} else {
					log.warn("JMS msg missing consumer");
				}
	
			} catch (final Exception e) {
				throw new ToolboxGeneralException("JMS error!", e);
			}
		});
		
		log.debug("JMS subscribe: " + destination + ", " + jmsListenerEndpoint.getId());

		jmsListenerEndpointRegistrar.registerEndpoint(jmsListenerEndpoint);

		return jmsListenerEndpoint.getId();

	}

	public static void unsubscribe(final String id) {

		final MessageListenerContainer messageListenerContainer = jmsListenerEndpointRegistrar.getEndpointRegistry().getListenerContainer(id);

		if (messageListenerContainer != null) {
			log.debug("JMS unsubscribe: " + id + ", " + messageListenerContainer.getClass());
			messageListenerContainer.stop();
		}

	}

}
