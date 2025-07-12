package com.linknest.linknest.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;
import java.time.LocalDate;
import net.coobird.thumbnailator.Thumbnails;
import java.io.OutputStream;
import java.io.InputStream;
import java.net.Socket;

@RestController
@RequestMapping("/api/media")
public class MediaController {
    @Value("${upload.dir:uploads/media/}")
    private String uploadDir;

    @Value("${media.base-url:http://localhost:8080}")
    private String mediaBaseUrl;

    @PostMapping("/upload")
    public ResponseEntity<?> uploadMedia(@RequestParam("file") MultipartFile file) throws IOException {
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body("No file uploaded");
        }
        // File size limit: 10MB
        long maxSize = 10 * 1024 * 1024;
        if (file.getSize() > maxSize) {
            return ResponseEntity.badRequest().body("File too large. Max size is 10MB.");
        }
        // Allowed file types
        String[] allowedExtensions = {".jpg", ".jpeg", ".png", ".gif", ".mp4", ".pdf"};
        String extension = "";
        String originalName = file.getOriginalFilename();
        if (originalName != null && originalName.contains(".")) {
            extension = originalName.substring(originalName.lastIndexOf('.')).toLowerCase();
        }
        boolean allowed = false;
        for (String ext : allowedExtensions) {
            if (extension.equals(ext)) {
                allowed = true;
                break;
            }
        }
        if (!allowed) {
            return ResponseEntity.badRequest().body("File type not allowed. Allowed: jpg, jpeg, png, gif, mp4, pdf");
        }
        // Organize by user and date
        String username = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication().getName();
        String userId = username != null ? username : "anonymous";
        String date = LocalDate.now().toString();
        String filename = UUID.randomUUID() + extension;
        Path uploadPath = Paths.get(uploadDir, userId, date);
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }
        Path filePath = uploadPath.resolve(filename);
        file.transferTo(filePath);
        String thumbUrl = null;
        // Generate thumbnail for images
        String[] imageExtensions = {".jpg", ".jpeg", ".png", ".gif"};
        boolean isImage = false;
        for (String ext : imageExtensions) {
            if (extension.equals(ext)) {
                isImage = true;
                break;
            }
        }
        if (isImage) {
            String thumbName = "thumb_" + filename;
            Path thumbPath = uploadPath.resolve(thumbName);
            if (extension.equals(".jpg") || extension.equals(".jpeg")) {
                Thumbnails.of(filePath.toFile()).size(200, 200).outputFormat("jpg").outputQuality(0.8).toFile(thumbPath.toFile());
                // Also compress original
                Thumbnails.of(filePath.toFile()).scale(1.0).outputFormat("jpg").outputQuality(0.8).toFile(filePath.toFile());
            } else if (extension.equals(".png")) {
                Thumbnails.of(filePath.toFile()).size(200, 200).outputFormat("png").toFile(thumbPath.toFile());
                // Also optimize original (lossless)
                Thumbnails.of(filePath.toFile()).scale(1.0).outputFormat("png").toFile(filePath.toFile());
            } else {
                Thumbnails.of(filePath.toFile()).size(200, 200).toFile(thumbPath.toFile());
            }
            String thumbRelativeUrl = "/" + uploadDir.replace("\\", "/").replaceAll("^/+|/+$", "") + "/" + userId + "/" + date + "/" + thumbName;
            thumbUrl = mediaBaseUrl.replaceAll("/+$", "") + thumbRelativeUrl;
        }
        // Generate thumbnail for videos (.mp4)
        if (extension.equals(".mp4")) {
            String thumbName = "thumb_" + filename.replace(".mp4", ".jpg");
            Path thumbPath = uploadPath.resolve(thumbName);
            // Use FFmpeg to extract a frame at 1 second
            ProcessBuilder pb = new ProcessBuilder(
                "ffmpeg", "-i", filePath.toString(), "-ss", "00:00:01.000", "-vframes", "1", thumbPath.toString()
            );
            pb.redirectErrorStream(true);
            try {
                Process process = pb.start();
                process.waitFor();
                if (Files.exists(thumbPath)) {
                    String thumbRelativeUrl = "/" + uploadDir.replace("\\", "/").replaceAll("^/+|/+$", "") + "/" + userId + "/" + date + "/" + thumbName;
                    thumbUrl = mediaBaseUrl.replaceAll("/+$", "") + thumbRelativeUrl;
                }
            } catch (Exception e) {
                // Log error, but don't fail upload
                System.err.println("FFmpeg thumbnail generation failed: " + e.getMessage());
            }
        }
        // Virus scan with ClamAV (clamd must be running on localhost:3310)
        try (Socket clamav = new Socket("localhost", 3310);
             OutputStream out = clamav.getOutputStream();
             InputStream in = clamav.getInputStream()) {
            out.write("zINSTREAM\0".getBytes());
            try (InputStream fileIn = Files.newInputStream(filePath)) {
                byte[] buf = new byte[2048];
                int read;
                while ((read = fileIn.read(buf)) != -1) {
                    int size = Integer.reverseBytes(read);
                    out.write(new byte[] {
                        (byte)(size >> 24), (byte)(size >> 16), (byte)(size >> 8), (byte)size
                    });
                    out.write(buf, 0, read);
                }
            }
            out.write(new byte[] {0,0,0,0}); // End of stream
            out.flush();
            StringBuilder response = new StringBuilder();
            int b;
            while ((b = in.read()) != -1) {
                response.append((char)b);
                if (response.toString().endsWith("\n")) break;
            }
            if (response.toString().contains("FOUND")) {
                Files.deleteIfExists(filePath);
                if (thumbUrl != null && !thumbUrl.isEmpty()) {
                    Path thumbPathToDelete = Paths.get(uploadPath.toString(), new java.io.File(thumbUrl).getName());
                    Files.deleteIfExists(thumbPathToDelete);
                }
                return ResponseEntity.status(400).body("File rejected: virus detected");
            }
        } catch (Exception e) {
            System.err.println("ClamAV scan failed: " + e.getMessage());
            // Optionally, reject upload if scan fails
            // return ResponseEntity.status(500).body("Virus scan failed");
        }
        String relativeUrl = "/" + uploadDir.replace("\\", "/").replaceAll("^/+|/+$", "") + "/" + userId + "/" + date + "/" + filename;
        String fileUrl = mediaBaseUrl.replaceAll("/+$", "") + relativeUrl;
        return ResponseEntity.ok().body((isImage || extension.equals(".mp4")) ? java.util.Map.of("url", fileUrl, "thumbnail", thumbUrl) : java.util.Map.of("url", fileUrl));
    }
} 