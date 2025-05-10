// src/main/java/com/application/Backend/FileUploader.java
package com.application.Backend;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Path;
import java.time.Duration;
import java.util.UUID; // To generate unique remote filenames

public class FileUploader {

    private static final String TRANSFER_SH_BASE_URL = "https://transfer.sh/";

    /**
     * Uploads a file to transfer.sh.
     *
     * @param fileToUpload The File object representing the file to upload. This should be the ENCRYPTED file.
     * @param originalFilename The original name of the file (used to suggest a remote name).
     * @return The direct download URL from transfer.sh if successful.
     * @throws IOException If an I/O error occurs during upload.
     * @throws InterruptedException If the upload operation is interrupted.
     * @throws RuntimeException If the upload fails with a non-200 status code.
     */
    public String uploadFile(File fileToUpload, String originalFilename) throws IOException, InterruptedException {
        if (fileToUpload == null || !fileToUpload.exists() || !fileToUpload.isFile()) {
            throw new IOException("Invalid file provided for upload.");
        }

        // Create a somewhat unique remote filename to avoid clashes on transfer.sh
        // Using original filename's extension if available.
        String extension = "";
        int lastDot = originalFilename.lastIndexOf('.');
        if (lastDot > 0 && lastDot < originalFilename.length() - 1) {
            extension = originalFilename.substring(lastDot); // Includes the dot
        }
        String remoteFilename = UUID.randomUUID().toString() + extension; // e.g., "uuid.pdf"

        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(15)) // Connection timeout
                .build();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(TRANSFER_SH_BASE_URL + remoteFilename))
                .timeout(Duration.ofMinutes(5)) // Request timeout (generous for uploads)
                .PUT(HttpRequest.BodyPublishers.ofFile(fileToUpload.toPath()))
                .header("User-Agent", "AnonChatE2EE/1.0 FileUploader") // Good practice
                .build();

        System.out.println("[FileUploader] Uploading '" + originalFilename + "' (as " + remoteFilename + ") to " + request.uri());
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() == 200) {
            String downloadUrl = response.body().trim(); // transfer.sh returns the URL as the body
            System.out.println("[FileUploader] Upload successful for '" + originalFilename + "'. Download URL: " + downloadUrl);
            return downloadUrl;
        } else {
            String errorMsg = "Upload to transfer.sh failed for '" + originalFilename + "'. Status: " +
                    response.statusCode() + " - Body: " + response.body();
            System.err.println("[FileUploader] " + errorMsg);
            throw new IOException(errorMsg);
        }
    }

    // Optional: A method to upload from byte array if you encrypt in memory
    // public String uploadBytes(byte[] fileBytes, String remoteFilenameSuggestion) throws IOException, InterruptedException {
    //     String remoteFilename = UUID.randomUUID().toString() + getExtension(remoteFilenameSuggestion);
    //     HttpClient client = ...
    //     HttpRequest request = HttpRequest.newBuilder()
    //             .uri(URI.create(TRANSFER_SH_BASE_URL + remoteFilename))
    //             .PUT(HttpRequest.BodyPublishers.ofByteArray(fileBytes))
    //             ...
    //     HttpResponse<String> response = ...
    //     // Handle response
    // }
    // private String getExtension(String filename) { ... }
}