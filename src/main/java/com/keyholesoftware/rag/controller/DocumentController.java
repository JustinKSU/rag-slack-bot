package com.keyholesoftware.rag.controller;

import java.util.List;

import org.springframework.ai.document.Document;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.keyholesoftware.rag.service.PdfEmbeddingService;

@RestController
@RequestMapping("/api/documents")
public class DocumentController {

    private final PdfEmbeddingService pdfEmbeddingService;

    public DocumentController(PdfEmbeddingService pdfEmbeddingService) {
        this.pdfEmbeddingService = pdfEmbeddingService;
    }

    @GetMapping("/collections")
    public ResponseEntity<List<String>> listCollections() {
        try {
            List<String> collections = pdfEmbeddingService.listCollections();
            return ResponseEntity.ok(collections);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(List.of("Error listing collections: " + e.getMessage()));
        }
    }

    @PostMapping("/upload")
    public ResponseEntity<String> uploadPdf(@RequestParam("file") MultipartFile file,
            @RequestParam String collection) {
        try {
            // Convert MultipartFile to Resource
            Resource resource = new InputStreamResource(file.getInputStream()) {
                @Override
                public String getFilename() {
                    return file.getOriginalFilename();
                }
            };

            pdfEmbeddingService.processPdfDocumentSimple(resource, collection);
            return ResponseEntity.ok("PDF processed successfully");

        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error processing PDF: " + e.getMessage());
        }
    }

    @GetMapping("/search")
    public ResponseEntity<List<Document>> search(
            @RequestParam String collection,
            @RequestParam String query,
            @RequestParam(defaultValue = "6") int topK,
            @RequestParam(defaultValue = "0") int similarityThreshold) {

        List<Document> results = pdfEmbeddingService.searchSimilar(collection, query, topK, similarityThreshold);
        return ResponseEntity.ok(results);
    }

    @DeleteMapping("/clear/{collectionName}")
    public ResponseEntity<String> clearCollection(@PathVariable String collectionName) {
        try {
            pdfEmbeddingService.clearSpecificCollection(collectionName);
            return ResponseEntity.ok("Collection '" + collectionName + "' cleared successfully");
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body("Error clearing collection '" + collectionName + "': " + e.getMessage());
        }
    }
}
