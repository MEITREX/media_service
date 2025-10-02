package de.unistuttgart.iste.meitrex.media_service.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.unistuttgart.iste.meitrex.media_service.service.MediaService;
import de.unistuttgart.iste.meitrex.media_service.service.SubmissionService;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * Controller providing endpoints for MinIO webhooks.
 */
@RestController
@RequiredArgsConstructor
@Slf4j
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
        log.info(payload);
        JsonNode root = new ObjectMapper().readTree(payload);

        // from the records array, get the file name of the uploaded object. File name matches the media record id
        String fileName = root.get("Records").get(0).get("s3").get("object").get("key").asText();

        String bucketName = root.get("Records").get(0).get("s3").get("bucket").get("name").asText();

        if(fileName.endsWith("_standardized")) {
            // ignore standardized files, they are a conversion of an already uploaded file
            return;
        }

        if(bucketName.equals(SubmissionService.BUCKET_ID)) {
            //ignore files for submissions
            log.info("skipped publish topic for submission");
            return;
        }

        UUID mediaRecordId = UUID.fromString(fileName);

        // check if this file needs to be converted to a standardized format and do so if it needs to be
        mediaService.convertToStandardizedFileIfPossibleAndNecessary(mediaRecordId);

        // publish dapr event
        mediaService.publishMediaRecordFileCreatedEvent(mediaRecordId);
    }
}
