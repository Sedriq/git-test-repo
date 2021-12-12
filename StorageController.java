package edu.kruhlenko.diploma.storage.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.kruhlenko.diploma.storage.model.FileMetadata;
import edu.kruhlenko.diploma.storage.service.PermissionService;
import edu.kruhlenko.diploma.storage.service.StorageService;
import edu.kruhlenko.diploma.util.TokenUtils;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/storage")
public class StorageController {

    private final ObjectMapper objectMapper;
    private final StorageService storageService;
    private final PermissionService permissionService;

    @GetMapping
    public List<FileMetadata> getFilesMetadataFeature1(@RequestHeader("Authorization") String token) {
        return storageService.getFilesMetadata(TokenUtils.getUser(token));
    }

    @SneakyThrows
    @GetMapping("/{fileId}")
    public FileMetadata getFileMetadata(@RequestHeader("Authorization") String token, @PathVariable String fileId) {
        permissionService.accessToFile(token, fileId);
        return storageService.getMetadata(fileId);
    }

    @PutMapping("/{fileId}")
    public FileMetadata updateFile(@RequestHeader("Authorization") String token, @PathVariable String fileId, @RequestBody FileMetadata metadata) {
        permissionService.isFileOwner(token, fileId);
        return storageService.updateFileMetadata(metadata.setGuid(fileId));
    }

    @DeleteMapping("/{fileId}")
    public void deleteFile(@RequestHeader("Authorization") String token, @PathVariable String fileId) {
        permissionService.isFileOwner(token, fileId);
        storageService.deleteFile(fileId);
    }

    @GetMapping("/{fileId}/download")
    public ResponseEntity<Resource> downloadFile(@RequestHeader("Authorization") String token, @PathVariable String fileId) {
        permissionService.accessToFile(token, fileId);
        var file = storageService.getFile(fileId);
        return ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION,
                "attachment; filename=\"" + file.getFilename() + "\"").body(file);
    }

    @PostMapping
    @SneakyThrows
    public FileMetadata uploadFile(@RequestHeader("Authorization") String token,
                                   @RequestParam("file") MultipartFile file,
                                   @RequestParam("metadata") String metadata) {
        permissionService.hasEnoughSpace(token, file.getSize());
        return storageService.uploadFile(file, objectMapper.readValue(metadata, FileMetadata.class), TokenUtils.getUser(token));
    }

    @PostMapping("/dir")
    public void createDirectory(@RequestHeader("Authorization") String token, @RequestBody FileMetadata metadata) {
        storageService.createDirectory(metadata.setOwner(TokenUtils.getUser(token)));
    }

    @DeleteMapping("/dir")
    public void deleteDirectory(@RequestHeader("Authorization") String token, @RequestBody FileMetadata metadata) {
        storageService.deleteDirectory(metadata.setOwner(TokenUtils.getUser(token)));
    }
}
