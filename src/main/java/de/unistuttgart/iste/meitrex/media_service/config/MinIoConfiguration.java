package de.unistuttgart.iste.meitrex.media_service.config;


import io.minio.MinioClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MinIoConfiguration {

    @Value("${minio.access.key}")
    private String accessKey;

    @Value("${minio.access.secret}")
    private String secretKey;

    @Value("${minio.url}")
    private String minioUrl;

    @Value("${minio.port}")
    private int minioPort;

    @Value("${minio.external.url}")
    private String minioExternalUrl;

    @Value("${minio.external.port}")
    private int minioExternalPort;

    @Bean
    public MinioClient minioInternalClient() {
        return new MinioClient.Builder()
                .credentials(accessKey, secretKey)
                .endpoint(minioUrl, minioPort, minioUrl.startsWith("https://"))
                .build();
    }

    @Bean
    public MinioClient minioExternalClient() {
        return new MinioClient.Builder()
                .credentials(accessKey, secretKey)
                .endpoint(minioExternalUrl, minioExternalPort, minioExternalUrl.startsWith("https://"))
                .region("eu-central-1")
                .build();
    }

}
