package com.keyholesoftware.rag.service;

import org.springframework.ai.document.Document;
import org.springframework.ai.reader.tika.TikaDocumentReader;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.filter.Filter;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;

import static com.keyholesoftware.rag.config.ConfigConstants.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
public class PdfEmbeddingService {

    private final Map<String, VectorStore> vectorStores;

    public PdfEmbeddingService(@Qualifier(VECTOR_STORE_MAP) Map<String, VectorStore> vectorStores) {
        this.vectorStores = vectorStores;
    }

    // method to chunk a pdf document, create embeddings, and store them
    public void processPdfDocumentSimple(Resource pdfResource, String collection) {
        try {
            // Step 1: Read the PDF
            TikaDocumentReader tikaReader = new TikaDocumentReader(pdfResource);
            List<Document> documents = tikaReader.get();

            documents = cleanDocumentWhitespace(documents);

            // Step 2: Split into chunks
            TokenTextSplitter textSplitter = new TokenTextSplitter(300, 50, 5, 10000, true);
            List<Document> chunks = textSplitter.apply(documents);

            // Step 3: Store and let vector store handle embedding generation automatically
            vectorStores.get(collection).add(chunks);

            log.info("Processed and stored " + chunks.size() + " chunks");

        } catch (Exception e) {
            log.error("Error processing PDF", e);
        }
    }

    // Method to search similar content
    public List<Document> searchSimilar(String collection, String query, int topK, int similarityThreshold) {
        SearchRequest searchRequest = SearchRequest.builder()
                .query(query)
                .topK(topK)
                .similarityThreshold(similarityThreshold)
                .build();
        return vectorStores.get(collection).similaritySearch(searchRequest);
    }

    // Method to clear documents from a collection
    public void clearSpecificCollection(String collectionName) {
        try {
            log.info("Clearing collection: " + collectionName);

            var targetStore = vectorStores.get(collectionName);
            if (targetStore != null) {
                Filter.Expression allDocsFilter = new Filter.Expression(
                        Filter.ExpressionType.NE,
                        new Filter.Key("id"),
                        new Filter.Value(""));
                targetStore.delete(allDocsFilter);
            }
            log.info("Cleared collection: " + collectionName);

        } catch (Exception e) {
            log.error("Error clearing collection " + collectionName, e);
            throw new RuntimeException("Failed to clear collection: " + collectionName, e);
        }
    }

    // Method to list available collections
    public List<String> listCollections() {
        try {
            return getAllCollectionNames();
        } catch (Exception e) {
            log.error("Error listing collections", e);
            throw new RuntimeException("Failed to list collections", e);
        }
    }

    private List<Document> cleanDocumentWhitespace(List<Document> documents) {
        return documents.stream()
                .map(this::cleanSingleDocument)
                .filter(doc -> !doc.getFormattedContent().trim().isEmpty()) // Remove empty documents
                .collect(Collectors.toList());
    }

    private Document cleanSingleDocument(Document document) {
        String cleanedContent = cleanText(document.getFormattedContent());

        // Create new document with cleaned content but preserve metadata
        return new Document(cleanedContent, document.getMetadata());
    }

    private String cleanText(String text) {
        if (text == null || text.trim().isEmpty()) {
            return "";
        }

        // Step 1: Normalize line breaks and remove excessive newlines
        text = text.replaceAll("\\r\\n", "\n") // Windows line endings to Unix
                .replaceAll("\\r", "\n") // Mac line endings to Unix
                .replaceAll("\\n{3,}", "\n\n"); // Multiple newlines to double newline

        // Step 2: Remove excessive horizontal whitespace
        text = text.replaceAll("[ \\t]{2,}", " "); // Multiple spaces/tabs to single space

        // Step 3: Clean up lines with excessive spacing
        String[] lines = text.split("\\n");
        StringBuilder cleanedText = new StringBuilder();

        for (String line : lines) {
            // Remove leading/trailing whitespace from each line
            line = line.trim();

            // Skip empty lines (but preserve intentional paragraph breaks)
            if (line.isEmpty()) {
                // Only add empty line if the last character isn't already a newline
                if (cleanedText.length() > 0 &&
                        cleanedText.charAt(cleanedText.length() - 1) != '\n') {
                    cleanedText.append("\n");
                }
                continue;
            }

            // Clean excessive spaces within the line while preserving sentence structure
            line = line.replaceAll("\\s+", " "); // Multiple whitespace to single space

            // Add the cleaned line
            if (cleanedText.length() > 0 &&
                    cleanedText.charAt(cleanedText.length() - 1) != '\n') {
                cleanedText.append(" ");
            }
            cleanedText.append(line);
        }

        // Step 4: Final cleanup
        String result = cleanedText.toString()
                .replaceAll("\\s+", " ") // Ensure no multiple spaces remain
                .replaceAll("\\n\\s+", "\n") // Remove spaces after newlines
                .replaceAll("\\s+\\n", "\n") // Remove spaces before newlines
                .trim(); // Remove leading/trailing whitespace

        return result;
    }

    private List<String> getAllCollectionNames() {
        return new ArrayList<>(vectorStores.keySet());
    }
}