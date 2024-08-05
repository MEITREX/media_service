package de.unistuttgart.iste.meitrex.media_service.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.unistuttgart.iste.meitrex.media_service.service.MediaService;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * Controller providing endpoints for MinIO webhooks.
 */
@RestController
@RequiredArgsConstructor
public class WebhookController {

    private final MediaService mediaService;

    /**
     * Endpoint for MinIO webhook that is triggered when a new object is created in the bucket.
     * @param payload The webhook http request body
     */
    @PostMapping("/webhook/on-minio-object-create")
    @SneakyThrows
    public void receiveOnMinioObjectCreateWebhook(@RequestBody String payload) {
        // the webhook payload is a JSON object with a "Records" array
        JsonNode root = new ObjectMapper().readTree(payload);

        // from the records array, get the file name of the uploaded object. File name matches the media record id
        String fileName = root.get("Records").get(0).get("s3").get("object").get("key").asText();

        // publish dapr event
        mediaService.publishMediaRecordFileCreatedEvent(UUID.fromString(fileName));
    }
}
