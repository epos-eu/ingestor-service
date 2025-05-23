package org.epos.core;

import abstractapis.AbstractAPI;
import dao.EposDataModelDAO;
import model.*;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.jena.graph.Graph;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.Triple;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.util.iterator.ExtendedIterator;
import org.epos.eposdatamodel.EPOSDataModelEntity;
import org.epos.eposdatamodel.Group;
import org.epos.eposdatamodel.LinkedEntity;
import org.epos.handler.dbapi.util.CacheInitializationUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import usermanagementapis.UserGroupManagementAPI;

import java.nio.charset.StandardCharsets;
import java.util.*;

public class MetadataPopulator {

    private static final Logger LOGGER = LoggerFactory.getLogger(MetadataPopulator.class);

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
                        LOGGER.info("Cache system initialized for MetadataPopulator");
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
            synchronized (MetadataPopulator.class) {
                if (eposDataModelDAO == null) {
                    try {
                        // Assicura inizializzazione del sistema
                        ensureSystemInitialized();

                        eposDataModelDAO = new EposDataModelDAO();
                        LOGGER.debug("EposDataModelDAO initialized for MetadataPopulator");
                    } catch (Exception e) {
                        LOGGER.error("Failed to create EposDataModelDAO: {}", e.getMessage());
                        throw new RuntimeException("Failed to initialize DAO for MetadataPopulator", e);
                    }
                }
            }
        }
        return eposDataModelDAO;
    }

    public static Model retrieveModelMapping(String inputMappingModel) {
        if (inputMappingModel == null || inputMappingModel.trim().isEmpty()) {
            throw new IllegalArgumentException("Input mapping model name cannot be null or empty");
        }

        LOGGER.debug("Retrieving model mapping for: {}", inputMappingModel);

        try {
            EposDataModelDAO dao = getEposDataModelDAO();

            // ✅ MIGLIORATO: Ricerca più efficiente dell'ontologia
            List<Ontology> ontologiesList = dao.getOneFromDBBySpecificKeySimple("name", inputMappingModel, Ontology.class);

            if (ontologiesList.isEmpty()) {
                LOGGER.warn("No ontology found with name: {}", inputMappingModel);
                return null;
            }

            if (ontologiesList.size() > 1) {
                LOGGER.warn("Multiple ontologies found with name: {} (count: {}), using first one",
                        inputMappingModel, ontologiesList.size());
            }

            Ontology ontology = ontologiesList.get(0);
            String triples = new String(Base64.getDecoder().decode(ontology.getContent()), StandardCharsets.UTF_8);

            if (triples.trim().isEmpty()) {
                LOGGER.warn("Ontology content is empty for: {}", inputMappingModel);
                return null;
            }

            Model modelMapping = ModelFactory.createDefaultModel()
                    .read(IOUtils.toInputStream(triples, StandardCharsets.UTF_8), null, "TURTLE");

            LOGGER.debug("Successfully loaded model mapping for: {} ({} triples)",
                    inputMappingModel, modelMapping.size());
            return modelMapping;

        } catch (Exception e) {
            LOGGER.error("Error retrieving model mapping for '{}': {}", inputMappingModel, e.getMessage(), e);
            throw new RuntimeException("Failed to retrieve model mapping: " + inputMappingModel, e);
        }
    }

    public static Model retrieveMetadataModelFromTTL(String url) {
        if (url == null || url.trim().isEmpty()) {
            throw new IllegalArgumentException("URL cannot be null or empty");
        }

        LOGGER.debug("Retrieving metadata model from TTL URL: {}", url);

        try {
            final Model model = ModelFactory.createDefaultModel();
            model.read(url, null, "TURTLE");

            LOGGER.debug("Successfully loaded TTL model from URL: {} ({} triples)", url, model.size());
            return model;

        } catch (Exception e) {
            LOGGER.error("Error retrieving metadata model from TTL URL '{}': {}", url, e.getMessage(), e);
            throw new RuntimeException("Failed to retrieve metadata model from TTL: " + url, e);
        }
    }

    public static void retrievePlainValueFromInnerMethods(Model modelmapping, BeansCreation beansCreation,
                                                          List<EPOSDataModelEntity> classes, Graph graph,
                                                          String subject, EPOSDataModelEntity activeClass,
                                                          Group selectedGroup) {
        if (modelmapping == null || beansCreation == null || graph == null || activeClass == null) {
            LOGGER.warn("Null parameters passed to retrievePlainValueFromInnerMethods");
            return;
        }

        try {
            Map<String, String> prefixes = modelmapping.getNsPrefixMap();

            for (ExtendedIterator<Triple> it = graph.find(); it.hasNext(); ) {
                Map<String, String> itemValue = null;
                Map<String, Object> innerValue = new HashMap<>();
                Triple triple = it.next();

                if (subject.equals(triple.getSubject().toString())) {
                    /** Get predicate value of triple and replace long prefix with short one **/
                    String value = triple.getPredicate().toString();
                    for (String key : prefixes.keySet()) {
                        if (value.contains(prefixes.get(key))) {
                            value = value.replaceAll(prefixes.get(key), key + ":");
                        }
                    }

                    if (!"rdf:type".equals(value)) {
                        itemValue = SPARQLManager.retrievePropertyValueInEDM(value, activeClass.getClass().getSimpleName(), modelmapping);

                        /** TODO: add recursive gathering of the element **/
                        if (itemValue == null) {
                            if (triple.getObject().isBlank()) {
                                retrievePlainValueFromInnerMethods(modelmapping, beansCreation, classes, graph,
                                        triple.getObject().toString(), activeClass, selectedGroup);
                            }
                        } else {
                            innerValue.put("itemValue", itemValue);
                            if (triple.getObject().isLiteral()) {
                                innerValue.put("plain", triple.getObject().getLiteralValue());
                            }
                            if (triple.getObject().isBlank()) {
                                innerValue.put("node", triple.getObject().toString());
                            }

                            if (innerValue.get("plain") != null) {
                                beansCreation.getEPOSDataModelPropertiesLiteral(activeClass, classes,
                                        (Map<String, String>) innerValue.get("itemValue"),
                                        innerValue.get("plain").toString());
                            }
                            if (innerValue.get("node") != null) {
                                beansCreation.getEPOSDataModelPropertiesNode(activeClass, classes,
                                        (Map<String, String>) innerValue.get("itemValue"),
                                        innerValue.get("node").toString(), selectedGroup);
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.error("Error in retrievePlainValueFromInnerMethods: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to retrieve plain values from inner methods", e);
        }
    }

    public static void exploreGraphAndCreateBeans(Model modelmapping, BeansCreation beansCreation, Graph graph,
                                                  EPOSDataModelEntity activeClass, List<EPOSDataModelEntity> classes,
                                                  List<String> uidDone, Group selectedGroup) {
        if (modelmapping == null || beansCreation == null || graph == null || classes == null || uidDone == null) {
            LOGGER.warn("Null parameters passed to exploreGraphAndCreateBeans");
            return;
        }

        try {
            /** SET PREFIXES **/
            Map<String, String> prefixes = modelmapping.getNsPrefixMap();

            for (ExtendedIterator<Triple> it = graph.find(); it.hasNext(); ) {
                Triple triple = it.next();
                /** Get predicate value of triple and replace long prefix with short one **/
                String value = triple.getPredicate().toString();
                for (String key : prefixes.keySet()) {
                    if (value.contains(prefixes.get(key))) {
                        value = value.replaceAll(prefixes.get(key), key + ":");
                    }
                }

                /** Retrieve Active Class **/
                for (EPOSDataModelEntity entity : classes) {
                    if (!uidDone.contains(entity.getUid())) {
                        activeClass = entity;
                        if (activeClass != null && activeClass.getUid().equals(triple.getSubject().toString())) {
                            uidDone.add(activeClass.getUid());

                            for (ExtendedIterator<Triple> iterator = graph.find(triple.getSubject(), Node.ANY, Node.ANY); iterator.hasNext();) {
                                Triple triple1 = iterator.next();
                                Map<String, String> itemValue = null;

                                /** Get predicate value of triple and replace long prefix with short one **/
                                String predicate = triple1.getPredicate().toString();

                                for (String key : prefixes.keySet()) {
                                    if (predicate.contains(prefixes.get(key))) {
                                        predicate = predicate.replaceAll(prefixes.get(key), key + ":");
                                    }
                                }

                                /** Manage Properties of Active Class **/
                                if (!"rdf:type".equals(predicate)) {
                                    itemValue = SPARQLManager.retrievePropertyValueInEDM(predicate, activeClass.getClass().getSimpleName(), modelmapping);
                                    if (!triple1.getSubject().isBlank() && triple1.getObject().isBlank() && itemValue == null) {
                                        retrievePlainValueFromInnerMethods(modelmapping, beansCreation, classes, graph,
                                                triple1.getObject().toString(), activeClass, selectedGroup);
                                    }
                                }
                                if (itemValue != null) {
                                    manageItemValue(activeClass, classes, itemValue, triple1.getObject(), selectedGroup);
                                }
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.error("Error in exploreGraphAndCreateBeans: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to explore graph and create beans", e);
        }
    }

    private static void manageItemValue(EPOSDataModelEntity activeClass, List<EPOSDataModelEntity> classes,
                                        Map<String, String> itemValue, Node node, Group selectedGroup) {
        if (activeClass == null || itemValue == null || node == null) {
            LOGGER.warn("Null parameters passed to manageItemValue");
            return;
        }

        try {
            BeansCreation beansCreation = new BeansCreation();

            if (node.isURI()) {
                beansCreation.getEPOSDataModelPropertiesNode(activeClass, classes, itemValue, node.toString(), selectedGroup);
            } else if (node.isBlank()) {
                beansCreation.getEPOSDataModelPropertiesNode(activeClass, classes, itemValue, node.toString(), selectedGroup);
            } else if (node.isLiteral()) {
                String s = null;
                if (node.toString().contains("\"")) {
                    s = node.toString();
                    s = StringUtils.substringBetween(s, "\"", "\"");
                }
                beansCreation.getEPOSDataModelPropertiesLiteral(activeClass, classes, itemValue,
                        s == null ? node.getLiteralValue() : s);
            } else if (node.isConcrete()) {
                beansCreation.getEPOSDataModelPropertiesLiteral(activeClass, classes, itemValue,
                        node.getLiteral().getValue());
            } else if (node.isVariable() || node.isExt() || node.isNodeTriple() || node.isNodeGraph()) {
                /** NOT USED ATM **/
                LOGGER.debug("Skipping node type: {}", node.getClass().getSimpleName());
            }
        } catch (Exception e) {
            LOGGER.error("Error managing item value: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to manage item value", e);
        }
    }


    public static Map<String, LinkedEntity> startMetadataPopulation(String url, String inputMappingModel, Group selectedGroup) {
        if (url == null || url.trim().isEmpty()) {
            throw new IllegalArgumentException("URL cannot be null or empty");
        }
        if (inputMappingModel == null || inputMappingModel.trim().isEmpty()) {
            throw new IllegalArgumentException("Input mapping model cannot be null or empty");
        }

        LOGGER.info("Starting metadata population from URL: {} using mapping: {}", url, inputMappingModel);

        try {
            /** RETRIEVE MAPPING MODEL AND MODEL FROM TTL **/
            Map<String, LinkedEntity> returnMap = new HashMap<>();

            Model modelmapping = retrieveModelMapping(inputMappingModel);
            if (modelmapping == null) {
                throw new RuntimeException("Failed to retrieve model mapping: " + inputMappingModel);
            }

            Model model = retrieveMetadataModelFromTTL(url);
            if (model == null) {
                throw new RuntimeException("Failed to retrieve metadata model from URL: " + url);
            }

            Graph graph = model.getGraph();

            /** DEFINE VARIABLES **/
            List<EPOSDataModelEntity> classes = new ArrayList<>();
            List<String> uidDone = new ArrayList<>();
            BeansCreation beansCreation = new BeansCreation();

            /** PREPARE CLASSES **/
            Map<String, Map<String, String>> classesMap = SPARQLManager.retrieveMainEntities(model);
            LOGGER.debug("Found {} main entities to process", classesMap.size());

            for (String uid : classesMap.keySet()) {
                try {
                    String className = SPARQLManager.retrieveEDMMappedClass(classesMap.get(uid).get("class").toString(), modelmapping);
                    EPOSDataModelEntity entity = beansCreation.getEPOSDataModelClass(className, uid, selectedGroup);
                    if (entity != null) {
                        classes.add(entity);
                        LOGGER.debug("Created entity: {} with UID: {}", className, uid);
                    }
                } catch (Exception e) {
                    LOGGER.error("Failed to create entity for UID: {} - {}", uid, e.getMessage());
                }
            }
            classes.removeIf(Objects::isNull);
            LOGGER.info("Successfully created {} EPOS entities", classes.size());

            /** PREPARE PROPERTIES **/
            exploreGraphAndCreateBeans(modelmapping, beansCreation, graph, null, classes, uidDone, selectedGroup);

            /** DATABASE POPULATION **/
            int successCount = 0;
            int errorCount = 0;

            for (EPOSDataModelEntity eposDataModelEntity : classes) {
                try {
                    AbstractAPI api = AbstractAPI.retrieveAPI(eposDataModelEntity.getClass().getSimpleName().toUpperCase());
                    if (api == null) {
                        LOGGER.error("No API found for entity type: {}", eposDataModelEntity.getClass().getSimpleName());
                        errorCount++;
                        continue;
                    }

                    LOGGER.debug("Ingesting -> {}", eposDataModelEntity);
                    LinkedEntity le = api.create(eposDataModelEntity, StatusType.PUBLISHED, null, null);
                    if (le != null) {
                        returnMap.put(le.getUid(), le);
                        successCount++;
                        LOGGER.debug("Successfully ingested entity: {} -> {}", eposDataModelEntity.getClass().getSimpleName(), le.getUid());
                    } else {
                        LOGGER.error("API returned null for entity: {}", eposDataModelEntity);
                        errorCount++;
                    }
                } catch (Exception apiCreationException) {
                    errorCount++;
                    LOGGER.error("Error ingesting entity: {} - {}", eposDataModelEntity.toString(), apiCreationException.getLocalizedMessage(), apiCreationException);
                }
            }

            LOGGER.info("Database population completed: {} success, {} errors", successCount, errorCount);

            /** ADD TO GROUP IF SPECIFIED **/
            if (selectedGroup != null) {
                int groupAddCount = 0;
                for (Map.Entry<String, LinkedEntity> entry : returnMap.entrySet()) {
                    try {
                        boolean added = UserGroupManagementAPI.addMetadataElementToGroup(entry.getValue().getMetaId(), selectedGroup.getId());
                        if (added) {
                            groupAddCount++;
                        }
                    } catch (Exception e) {
                        LOGGER.error("Failed to add metadata element to group: {} - {}", entry.getValue().getMetaId(), e.getMessage());
                    }
                }
                LOGGER.info("Added {} metadata elements to group: {}", groupAddCount, selectedGroup.getId());
            }

            LOGGER.info("Metadata population completed successfully. Created {} linked entities", returnMap.size());
            return returnMap;

        } catch (Exception e) {
            LOGGER.error("Error during metadata population: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to populate metadata", e);
        }
    }

    public static String getSystemInfo() {
        StringBuilder info = new StringBuilder();
        info.append("MetadataPopulator System Info:\n");
        info.append("- System Initialized: ").append(systemInitialized).append("\n");
        info.append("- DAO Initialized: ").append(eposDataModelDAO != null).append("\n");

        if (eposDataModelDAO != null) {
            try {
                info.append("- Cache Enabled: ").append(eposDataModelDAO.isCacheEnabled()).append("\n");
                info.append("- DAO Healthy: ").append(eposDataModelDAO.isHealthy()).append("\n");
            } catch (Exception e) {
                info.append("- Error getting DAO info: ").append(e.getMessage()).append("\n");
            }
        }

        return info.toString();
    }

    public static void resetForTesting() {
        synchronized (initLock) {
            systemInitialized = false;
            eposDataModelDAO = null;
            LOGGER.info("MetadataPopulator reset for testing");
        }
    }
}