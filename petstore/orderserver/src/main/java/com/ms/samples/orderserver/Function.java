package com.ms.samples.orderserver;

import com.microsoft.azure.functions.ExecutionContext;
import com.microsoft.azure.functions.annotation.FixedDelayRetry;
import com.microsoft.azure.functions.annotation.FunctionName;
import com.microsoft.azure.functions.annotation.ServiceBusQueueTrigger;

import com.azure.storage.blob.*;
import com.azure.storage.blob.specialized.*;
import com.azure.storage.common.*;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import org.json.JSONObject;

public class Function {
	
	private static final String AZURE_STORAGE_ACCOUNT_NAME = System.getenv("AZURE_STORAGE_ACCOUNT_NAME");
    private static final String AZURE_STORAGE_ACCOUNT_KEY = System.getenv("AZURE_STORAGE_ACCOUNT_KEY");
    private static final String AZURE_STORAGE_CONTAINER_NAME = System.getenv("AZURE_STORAGE_CONTAINER_NAME");
	
    @FunctionName("OrderServerProcessor")
    @FixedDelayRetry(maxRetryCount = 3, delayInterval = "00:00:05")
    public void serviceBusProcess(
    		@ServiceBusQueueTrigger(name = "msg", queueName = "orderqueue", connection = "AzureConnectionStringListener") final String message,
    	   final ExecutionContext context
    	 ) throws Exception {
    	
    	context.getLogger().info(
    			String.format("getMaxretrycount: %d; CurrentRetryCount: %d; Problem caused retry: %s",
    					context.getRetryContext().getMaxretrycount(),
    					context.getRetryContext().getRetrycount(),
    					context.getRetryContext().getException().getMessage()));
    	
    	context.getLogger().info("Original message: " + message);
    	
    	String fixedMessage = message.substring(message.indexOf('{'));
    	context.getLogger().info("Trimmed message: " + fixedMessage);
    	
    	JSONObject jsonObject = new JSONObject(fixedMessage);
    	String sessionId = jsonObject.getString("id");
    	context.getLogger().info("Session ID: " + sessionId);
    	
    	String filename = String.format("%s.json", sessionId);
    	
    	BlobServiceClient blobServiceClient = GetBlobServiceClientByAccountKey(AZURE_STORAGE_ACCOUNT_NAME, AZURE_STORAGE_ACCOUNT_KEY);
        BlobContainerClient blobContainerClient = blobServiceClient.getBlobContainerClient(AZURE_STORAGE_CONTAINER_NAME);
        
        context.getLogger().info("Trying to upload file: " + filename);
        
        if (sessionId.equalsIgnoreCase("asd")) {

        	throw new RuntimeException("Retryable failure");
        }
        
        this.uploadBlobFromStream(blobContainerClient, fixedMessage, filename);
        
        context.getLogger().info("File uploaded successfully: " + filename);
        
    }
    
    public static BlobServiceClient GetBlobServiceClientByAccountKey(String accountName, String accountKey) {
    	
        StorageSharedKeyCredential credential = new StorageSharedKeyCredential(accountName, accountKey);
        
        BlobServiceClient blobServiceClient = new BlobServiceClientBuilder()
                .endpoint(String.format("https://%s.blob.core.windows.net/", accountName))
                .credential(credential)
                .buildClient();

        return blobServiceClient;
    }
    
    public static BlobContainerClient GetBlobContainerClient(BlobServiceClient blobServiceClient, String container_name) {
        return blobServiceClient.getBlobContainerClient(container_name);
    }
    
    public void uploadBlobFromStream(BlobContainerClient blobContainerClient, String content, String file_name) {
        BlockBlobClient blockBlobClient = blobContainerClient.getBlobClient(file_name).getBlockBlobClient();
        try (ByteArrayInputStream dataStream = new ByteArrayInputStream(content.getBytes())) {
            blockBlobClient.upload(dataStream, content.length(), true);
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }
}