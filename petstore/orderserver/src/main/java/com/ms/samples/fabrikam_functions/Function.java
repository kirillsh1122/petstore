package com.ms.samples.fabrikam_functions;

import com.microsoft.azure.functions.ExecutionContext;
import com.microsoft.azure.functions.annotation.FunctionName;
import com.microsoft.azure.functions.annotation.ServiceBusQueueTrigger;

import org.json.JSONObject;

public class Function {
    @FunctionName("OrderServerProcessor")
    public void serviceBusProcess(
    	    @ServiceBusQueueTrigger(name = "msg",
    	                             queueName = "orderqueue",
    	                             connection = "AzureConnectionString") String message,
    	   final ExecutionContext context
    	 ) {
    	context.getLogger().info("Original message: " + message);
    	
    	String fixedMessage = message.substring(message.indexOf('{'));
    	context.getLogger().info("Trimmed message: " + fixedMessage);
    	
    	JSONObject jsonObject = new JSONObject(fixedMessage);
    	context.getLogger().info("Session ID: " + jsonObject.getString("id"));
    }
}
