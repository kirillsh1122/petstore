package com.chtrembl.petstoreapp.service;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.stereotype.Service;


@Service
public class JmsOrderMessagingService implements OrderMessagingService {
	
	private JmsTemplate jms;
	
	public JmsOrderMessagingService(JmsTemplate jms) {
		this.jms = jms;
	}

	@Override
	public void sendOrder(String order) {
		jms.send(session -> session.createObjectMessage(order));
	}

}
