package com.linknest.linknest.service;

import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;

public interface MediaStorageService {
    String saveFile(MultipartFile file, String userId, String date, String filename) throws IOException;
    void deleteFile(String path) throws IOException;
    // Add more methods as needed for CDN integration
} 