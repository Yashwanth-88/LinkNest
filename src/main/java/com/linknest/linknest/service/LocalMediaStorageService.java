package com.linknest.linknest.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Service
public class LocalMediaStorageService implements MediaStorageService {
    @Value("${upload.dir:uploads/media/}")
    private String uploadDir;

    @Override
    public String saveFile(MultipartFile file, String userId, String date, String filename) throws IOException {
        Path uploadPath = Paths.get(uploadDir, userId, date);
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }
        Path filePath = uploadPath.resolve(filename);
        file.transferTo(filePath);
        return filePath.toString();
    }

    @Override
    public void deleteFile(String path) throws IOException {
        Files.deleteIfExists(Paths.get(path));
    }
} 