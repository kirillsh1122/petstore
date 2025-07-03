package com.ms.samples.fabrikam_functions;

import com.microsoft.azure.functions.ExecutionContext;
import com.microsoft.azure.functions.HttpMethod;
import com.microsoft.azure.functions.HttpRequestMessage;
import com.microsoft.azure.functions.HttpResponseMessage;
import com.microsoft.azure.functions.HttpStatus;
import com.microsoft.azure.functions.annotation.AuthorizationLevel;
import com.microsoft.azure.functions.annotation.FunctionName;
import com.microsoft.azure.functions.annotation.HttpTrigger;

import com.azure.storage.blob.*;
import com.azure.storage.blob.specialized.*;
import com.azure.storage.common.*;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import org.json.JSONObject;

import java.util.Optional;

/**
 * Azure Functions with HTTP Trigger.
 */
public class Function {

	private static final String AZURE_STORAGE_ACCOUNT_NAME = System.getenv("AZURE_STORAGE_ACCOUNT_NAME");
    private static final String AZURE_STORAGE_ACCOUNT_KEY = System.getenv("AZURE_STORAGE_ACCOUNT_KEY");
    private static final String AZURE_STORAGE_CONTAINER_NAME = System.getenv("AZURE_STORAGE_CONTAINER_NAME");
	
    @FunctionName("HttpExample")
    public HttpResponseMessage run(
            @HttpTrigger(
                name = "req",
                methods = {HttpMethod.GET, HttpMethod.POST},
                authLevel = AuthorizationLevel.ANONYMOUS)
                HttpRequestMessage<Optional<String>> request,
            final ExecutionContext context) {
        context.getLogger().info("Java HTTP trigger processed a request.");

//        request.getQueryParameters().entrySet().forEach(e -> context.getLogger().info(e.getKey() + ": " + e.getValue()));
        final String content = request.getBody().orElse("");        
        JSONObject jsonObject = new JSONObject(content);
        final String sessionId = jsonObject.getString("id");
        
        String filename = String.format("%s.json", sessionId);
        
        BlobServiceClient blobServiceClient = GetBlobServiceClientAccountKey(AZURE_STORAGE_ACCOUNT_NAME, AZURE_STORAGE_ACCOUNT_KEY);
        BlobContainerClient blobContainerClient = blobServiceClient.getBlobContainerClient(AZURE_STORAGE_CONTAINER_NAME);
        
        this.uploadBlobFromStream(blobContainerClient, content, filename);
        
        context.getLogger().info(content);

        if (content == "") {
            return request.createResponseBuilder(HttpStatus.BAD_REQUEST).body("Please provide a request body").build();
        } else {
            return request.createResponseBuilder(HttpStatus.OK).body(content).build();
        }
    }
    
    public static BlobServiceClient GetBlobServiceClientAccountKey(String accountName, String accountKey) {
        StorageSharedKeyCredential credential = new StorageSharedKeyCredential(accountName, accountKey);

        // Azure SDK client builders accept the credential as a parameter
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
