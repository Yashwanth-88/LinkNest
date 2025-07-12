package com.linknest.linknest.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import com.linknest.linknest.repository.MessageRepository;
import com.linknest.linknest.repository.GroupMessageRepository;
import java.io.File;
import java.nio.file.Files;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.HashSet;
import java.util.Set;

@Service
public class FileCleanupService {
    @Value("${upload.dir:uploads/media/}")
    private String uploadDir;

    @Autowired
    private MessageRepository messageRepository;
    @Autowired
    private GroupMessageRepository groupMessageRepository;

    // Run daily at 2am
    @Scheduled(cron = "0 0 2 * * *")
    public void cleanupOldFiles() {
        Set<String> referenced = new HashSet<>();
        // Collect all referenced media URLs from messages
        messageRepository.findAll().forEach(m -> { if (m.getMediaUrl() != null) referenced.add(m.getMediaUrl()); });
        groupMessageRepository.findAll().forEach(m -> { if (m.getMediaUrl() != null) referenced.add(m.getMediaUrl()); });
        File root = new File(uploadDir);
        if (!root.exists()) return;
        long cutoff = System.currentTimeMillis() - 30L * 24 * 60 * 60 * 1000;
        deleteOldFiles(root, referenced, cutoff);
    }

    private void deleteOldFiles(File dir, Set<String> referenced, long cutoff) {
        for (File file : dir.listFiles()) {
            if (file.isDirectory()) {
                deleteOldFiles(file, referenced, cutoff);
            } else {
                if (file.lastModified() < cutoff) {
                    String relPath = file.getPath().replace("\\", "/").replaceFirst("^.*uploads/media/", "/uploads/media/");
                    boolean isReferenced = referenced.stream().anyMatch(url -> url.endsWith(relPath));
                    if (!isReferenced) {
                        try { Files.deleteIfExists(file.toPath()); } catch (Exception ignored) {}
                    }
                }
            }
        }
    }
} 