package de.unistuttgart.iste.meitrex.media_service.service;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.function.Consumer;

@Service
@Slf4j
public class FileConversionService {
    /**
     * Converts a document to pdf using the soffice utility asynchronously. Upon successful conversion, the pdf is
     * written to the provided output stream which can be digested in the outputStreamConsumer function.
     * @param inputStream The input document to convert.
     * @param outputStreamConsumer The consumer function to handle the output pdf.
     */
    @SneakyThrows
    public void convertDocumentToPdf(InputStream inputStream, Consumer<InputStream> outputStreamConsumer) {
        // create a temp file to save the input document to
        File inputFile = File.createTempFile("doc", ".tmp", null);
        inputFile.deleteOnExit();

        // write the input document to the temp file
        Files.copy(inputStream, inputFile.toPath(), StandardCopyOption.REPLACE_EXISTING);

        // create a temp output directory to save the converted pdf to
        // (the soffice utility only accepts directories as output, not a file path directly)
        Path outputDir = Files.createTempDirectory("docout");
        outputDir.toFile().deleteOnExit();

        Thread thread = new Thread(() -> {
            // invoke soffice to convert the input document to pdf
            Process process = null;
            try {
                process = new ProcessBuilder(
                        "soffice",
                        "--headless",
                        "--convert-to", "pdf",
                        "--outdir", outputDir.toString(),
                        inputFile.toString()).start();
                process.waitFor();
                try(InputStream inStream = Files.newInputStream(outputDir.resolve(
                        inputFile.getName().substring(0, inputFile.getName().lastIndexOf(".")) + ".pdf"))) {
                    outputStreamConsumer.accept(inStream);
                }
            } catch (IOException | InterruptedException e) {
                throw new RuntimeException(e);
            } finally {
                if(process != null) {
                    process.destroy();
                }
            }
        });
        thread.start();
    }
}
