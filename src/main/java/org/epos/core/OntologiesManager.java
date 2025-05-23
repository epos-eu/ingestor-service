package org.epos.core;

import dao.EposDataModelDAO;
import model.Ontology;
import org.epos.handler.dbapi.util.CacheInitializationUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

public class OntologiesManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(OntologiesManager.class);

    private static EposDataModelDAO eposDataModelDAO;

    private static volatile boolean systemInitialized = false;
    private static final Object initLock = new Object();

    private static void ensureSystemInitialized() {
        if (!systemInitialized) {
            synchronized (initLock) {
                if (!systemInitialized) {
                    try {
                        if (!CacheInitializationUtil.isInitialized()) {
                            String environment = System.getenv("ENVIRONMENT");
                            if ("test".equals(environment) || "testing".equals(environment)) {
                                CacheInitializationUtil.initializeMinimal();
                            } else {
                                CacheInitializationUtil.initializeForProduction();
                            }
                        }
                        systemInitialized = true;
                        LOGGER.info("Cache system initialized for OntologiesManager");
                    } catch (Exception e) {
                        LOGGER.warn("Failed to initialize cache system: {} - continuing without cache", e.getMessage());
                        systemInitialized = true; // Evita loop infiniti
                    }
                }
            }
        }
    }

    private static EposDataModelDAO getEposDataModelDAO() {
        if (eposDataModelDAO == null) {
            synchronized (OntologiesManager.class) {
                if (eposDataModelDAO == null) {
                    try {
                        // Assicura inizializzazione del sistema
                        ensureSystemInitialized();

                        eposDataModelDAO = new EposDataModelDAO();
                        LOGGER.debug("EposDataModelDAO initialized for OntologiesManager");
                    } catch (Exception e) {
                        LOGGER.error("Failed to create EposDataModelDAO: {}", e.getMessage());
                        throw new RuntimeException("Failed to initialize DAO for OntologiesManager", e);
                    }
                }
            }
        }
        return eposDataModelDAO;
    }

    public static void createOntology(String name, String type, String ontologyURL) throws IOException {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Ontology name cannot be null or empty");
        }
        if (type == null || type.trim().isEmpty()) {
            throw new IllegalArgumentException("Ontology type cannot be null or empty");
        }
        if (ontologyURL == null || ontologyURL.trim().isEmpty()) {
            throw new IllegalArgumentException("Ontology URL cannot be null or empty");
        }

        LOGGER.info("Creating/updating ontology: name={}, type={}, url={}", name, type, ontologyURL);

        try {
            EposDataModelDAO dao = getEposDataModelDAO();

            List<Ontology> existingOntologies = dao.getOneFromDBBySpecificKeySimple("name", name, Ontology.class);
            Ontology existingOntology = existingOntologies.isEmpty() ? null : existingOntologies.get(0);

            if (existingOntology != null) {
                LOGGER.info("Found existing ontology with name: {}, ID: {}", name, existingOntology.getId());
            } else {
                LOGGER.info("No existing ontology found with name: {}, creating new one", name);
            }

            String content = downloadOntologyContent(ontologyURL);
            String encoded = Base64.getEncoder().encodeToString(content.getBytes(StandardCharsets.UTF_8));

            Ontology ontology = new Ontology();
            ontology.setId(existingOntology != null ? existingOntology.getId() : UUID.randomUUID().toString());
            ontology.setName(name);
            ontology.setType(type);
            ontology.setContent(encoded);

            boolean result;
            if (existingOntology != null) {
                result = dao.updateObject(ontology);
                LOGGER.info("Updated existing ontology: {}", result ? "SUCCESS" : "FAILED");
            } else {
                result = dao.createObject(ontology);
                LOGGER.info("Created new ontology: {}", result ? "SUCCESS" : "FAILED");
            }

            if (!result) {
                throw new RuntimeException("Failed to save ontology to database");
            }

            LOGGER.info("Ontology '{}' processed successfully with ID: {}", name, ontology.getId());

        } catch (IOException e) {
            LOGGER.error("Failed to download ontology content from URL: {}", ontologyURL, e);
            throw e;
        } catch (Exception e) {
            LOGGER.error("Error creating/updating ontology '{}': {}", name, e.getMessage(), e);
            throw new RuntimeException("Failed to create/update ontology: " + name, e);
        }
    }

    private static String downloadOntologyContent(String ontologyURL) throws IOException {
        LOGGER.debug("Downloading ontology content from: {}", ontologyURL);

        URL url = new URL(ontologyURL);
        StringBuilder resultStringBuilder = new StringBuilder();

        try (BufferedReader br = new BufferedReader(new InputStreamReader(url.openStream(), StandardCharsets.UTF_8))) {
            String line;
            int lineCount = 0;
            while ((line = br.readLine()) != null) {
                resultStringBuilder.append(line).append("\n");
                lineCount++;
            }
            LOGGER.debug("Downloaded {} lines from ontology URL", lineCount);
        } catch (IOException e) {
            LOGGER.error("Failed to download content from URL: {}", ontologyURL, e);
            throw new IOException("Failed to download ontology from URL: " + ontologyURL, e);
        }

        String content = resultStringBuilder.toString();
        if (content.trim().isEmpty()) {
            throw new IOException("Downloaded ontology content is empty from URL: " + ontologyURL);
        }

        LOGGER.debug("Successfully downloaded ontology content ({} characters)", content.length());
        return content;
    }

    public static List<Ontology> retrieveOntologies() {
        LOGGER.debug("Retrieving all ontologies");

        try {
            EposDataModelDAO dao = getEposDataModelDAO();
            List<Ontology> ontologies = dao.getAllFromDB(Ontology.class);

            LOGGER.info("Retrieved {} ontologies from database", ontologies.size());
            return ontologies;

        } catch (Exception e) {
            LOGGER.error("Error retrieving ontologies: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to retrieve ontologies", e);
        }
    }

    public static Ontology retrieveOntologyByName(String name) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Ontology name cannot be null or empty");
        }

        LOGGER.debug("Retrieving ontology by name: {}", name);

        try {
            EposDataModelDAO dao = getEposDataModelDAO();
            List<Ontology> ontologies = dao.getOneFromDBBySpecificKeySimple("name", name, Ontology.class);

            if (ontologies.isEmpty()) {
                LOGGER.info("No ontology found with name: {}", name);
                return null;
            }

            if (ontologies.size() > 1) {
                LOGGER.warn("Multiple ontologies found with name: {} (count: {}), returning first one", name, ontologies.size());
            }

            Ontology result = ontologies.get(0);
            LOGGER.debug("Found ontology: {} (ID: {})", result.getName(), result.getId());
            return result;

        } catch (Exception e) {
            LOGGER.error("Error retrieving ontology by name '{}': {}", name, e.getMessage(), e);
            throw new RuntimeException("Failed to retrieve ontology by name: " + name, e);
        }
    }

    public static boolean deleteOntology(String name) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Ontology name cannot be null or empty");
        }

        LOGGER.info("Deleting ontology: {}", name);

        try {
            Ontology ontology = retrieveOntologyByName(name);
            if (ontology == null) {
                LOGGER.warn("Cannot delete ontology '{}' - not found", name);
                return false;
            }

            EposDataModelDAO dao = getEposDataModelDAO();
            boolean result = dao.deleteObject(ontology);

            if (result) {
                LOGGER.info("Successfully deleted ontology: {}", name);
            } else {
                LOGGER.error("Failed to delete ontology: {}", name);
            }

            return result;

        } catch (Exception e) {
            LOGGER.error("Error deleting ontology '{}': {}", name, e.getMessage(), e);
            throw new RuntimeException("Failed to delete ontology: " + name, e);
        }
    }

    public static String getDecodedOntologyContent(String name) {
        Ontology ontology = retrieveOntologyByName(name);
        if (ontology == null) {
            return null;
        }

        try {
            byte[] decodedBytes = Base64.getDecoder().decode(ontology.getContent());
            return new String(decodedBytes, StandardCharsets.UTF_8);
        } catch (Exception e) {
            LOGGER.error("Failed to decode ontology content for '{}': {}", name, e.getMessage(), e);
            throw new RuntimeException("Failed to decode ontology content: " + name, e);
        }
    }

    public static String getSystemInfo() {
        StringBuilder info = new StringBuilder();
        info.append("OntologiesManager System Info:\n");
        info.append("- System Initialized: ").append(systemInitialized).append("\n");
        info.append("- DAO Initialized: ").append(eposDataModelDAO != null).append("\n");

        if (eposDataModelDAO != null) {
            try {
                info.append("- Cache Enabled: ").append(eposDataModelDAO.isCacheEnabled()).append("\n");
                info.append("- DAO Healthy: ").append(eposDataModelDAO.isHealthy()).append("\n");

                List<Ontology> ontologies = retrieveOntologies();
                info.append("- Total Ontologies: ").append(ontologies.size()).append("\n");

            } catch (Exception e) {
                info.append("- Error getting DAO info: ").append(e.getMessage()).append("\n");
            }
        }

        return info.toString();
    }
}