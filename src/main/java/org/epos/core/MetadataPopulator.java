package org.epos.core;

import abstractapis.AbstractAPI;
import dao.EposDataModelDAO;
import model.*;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.jena.graph.Graph;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.Triple;
import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.util.iterator.ExtendedIterator;
import org.epos.eposdatamodel.EPOSDataModelEntity;
import org.epos.eposdatamodel.LinkedEntity;

import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class MetadataPopulator {

    public static Model retrieveModelMapping(String inputMappingModel){
        EposDataModelDAO eposDataModelDAO = new EposDataModelDAO();

        /**
         * GET ALL ONTOLOGIES FROM DB AND POPULATE MODEL AND MODEL MAPPING
         **/
        List<Ontologies> ontologiesList = eposDataModelDAO.getAllFromDB(Ontologies.class);

        String triples = null;
        for(Ontologies ontologies : ontologiesList){
            if(ontologies.getName().equals(inputMappingModel)){
                triples = new String(Base64.getDecoder().decode(ontologies.getContent()));
            }
        }
        Model modelmapping = null;
        if(triples!=null) {
            modelmapping = ModelFactory.createDefaultModel()
                    .read(IOUtils.toInputStream(triples, StandardCharsets.UTF_8), null, "TURTLE");
        }
        return modelmapping;
    }

    public static Model retrieveMetadataModelFromTTL(String url){

        final Model model = ModelFactory.createDefaultModel();
        model.read(url, null, "TURTLE");

        return model;
    }
    public static void retrievePlainValueFromInnerMethods(Model modelmapping,BeansCreation beansCreation, List<EPOSDataModelEntity> classes, Graph graph, String subject, EPOSDataModelEntity activeClass){
        Map<String, String> prefixes = modelmapping.getNsPrefixMap();

        for (ExtendedIterator<Triple> it = graph.find(); it.hasNext(); ) {
            Map<String, String> itemValue = null;
            Map<String, Object> innerValue = new HashMap<>();
            Triple triple = it.next();
            if(subject.equals(triple.getSubject().toString())) {
                /** Get predicate value of triple and replace long prefix with short one **/
                String value = triple.getPredicate().toString();
                for (String key : prefixes.keySet()) {
                    if (value.contains(prefixes.get(key))) value = value.replaceAll(prefixes.get(key), key + ":");
                }
                if (!value.equals("rdf:type")) {
                    itemValue = SPARQLManager.retrievePropertyValueInEDM(value, activeClass.getClass().getSimpleName(), modelmapping);
                    /** TODO: add recursive gathering of the element **/
                    if(itemValue == null){
                        if(triple.getObject().isBlank()) retrievePlainValueFromInnerMethods(modelmapping, beansCreation, classes, graph, triple.getObject().toString(), activeClass);
                    } else{
                        innerValue.put("itemValue",itemValue);
                        if(triple.getObject().isLiteral()) innerValue.put("plain",triple.getObject().getLiteralValue());
                        if(triple.getObject().isBlank()) innerValue.put("node",triple.getObject().toString());
                        System.out.println("FOUND: "+innerValue);
                        if(innerValue.get("plain")!=null)
                            beansCreation.getEPOSDataModelPropertiesLiteral(activeClass, classes, (Map<String, String>) innerValue.get("itemValue"), innerValue.get("plain").toString());
                        if(innerValue.get("node")!=null)
                            beansCreation.getEPOSDataModelPropertiesNode(activeClass, classes, (Map<String, String>) innerValue.get("itemValue"), innerValue.get("node").toString());

                    }
                }
            }
        }
    }


    public static void exploreGraphAndCreateBeans(Model modelmapping, BeansCreation beansCreation, Graph graph, EPOSDataModelEntity activeClass, List<EPOSDataModelEntity> classes, List<String> uidDone) {
        /** SET PREFIXES **/
        Map<String, String> prefixes = modelmapping.getNsPrefixMap();

        for (ExtendedIterator<Triple> it = graph.find(); it.hasNext(); ) {
            Triple triple = it.next();
            /** Get predicate value of triple and replace long prefix with short one **/
            String value = triple.getPredicate().toString();
            for (String key : prefixes.keySet()) {
                if (value.contains(prefixes.get(key))) value = value.replaceAll(prefixes.get(key), key + ":");
            }

            /** Retrieve Active Class **/
            for(EPOSDataModelEntity entity : classes){
                if(!uidDone.contains(entity.getUid())) {
                    activeClass = entity;
                    if (activeClass != null && activeClass.getUid().equals(triple.getSubject().toString())) {
                        uidDone.add(activeClass.getUid());
                        //exploreGraphAndCreateBeans(modelmapping, beansCreation, graph, activeClass, classes, uidDone);
                        for (ExtendedIterator<Triple> iterator = graph.find(triple.getSubject(), Node.ANY, Node.ANY); iterator.hasNext();) {

                            Triple triple1 = iterator.next();
                            Map<String, String> itemValue = null;

                            /** Get predicate value of triple and replace long prefix with short one **/
                            String predicate = triple1.getPredicate().toString();

                            for (String key : prefixes.keySet()) {
                                if (predicate.contains(prefixes.get(key))) predicate = predicate.replaceAll(prefixes.get(key), key + ":");
                            }

                            /** Manage Properties of Active Class **/
                            if (!predicate.equals("rdf:type")) {
                                itemValue = SPARQLManager.retrievePropertyValueInEDM(predicate, activeClass.getClass().getSimpleName(), modelmapping);
                                if(!triple1.getSubject().isBlank() && triple1.getObject().isBlank() && itemValue==null){
                                    retrievePlainValueFromInnerMethods(modelmapping, beansCreation, classes, graph, triple1.getObject().toString(), activeClass);
                                }
                            }
                            if(itemValue != null) {
                                manageItemValue(activeClass, classes, itemValue, triple1.getObject());
                            }
                        }
                    }
                }
            }
        }
    }

    private static void manageItemValue(EPOSDataModelEntity activeClass, List<EPOSDataModelEntity> classes, Map<String, String> itemValue, Node node) {
        BeansCreation beansCreation = new BeansCreation();
        if (node.isURI()) {
            System.out.println("MANAGE URI");
            beansCreation.getEPOSDataModelPropertiesNode(activeClass, classes, itemValue, node.toString());
        } else if (node.isBlank()) {
            System.out.println("MANAGE BLANK");
            beansCreation.getEPOSDataModelPropertiesNode(activeClass, classes, itemValue, node.toString());
        }else if (node.isLiteral()) {
            System.out.println("MANAGE LITERAL");
            String s = null;
            if(node.toString().contains("\"")){
                s = node.toString();
                s = StringUtils.substringBetween(s, "\"", "\"");
            }
            beansCreation.getEPOSDataModelPropertiesLiteral(activeClass, classes, itemValue, s==null? node.getLiteralValue() : s);
        } else if (node.isConcrete()) {
            System.out.println("MANAGE CONCRETE");
            beansCreation.getEPOSDataModelPropertiesLiteral(activeClass, classes, itemValue, node.getLiteral().getValue());
        } else if (node.isVariable()
                || node.isExt()
                || node.isNodeTriple()
                || node.isNodeGraph()) {
            /** NOT USED ATM **/
            System.out.println("MANAGE OTHER??");
        }
    }

    public static Map<String,Object> startMetadataPopulation(String url, String inputMappingModel){

        /** RETRIEVE MAPPING MODEL AND MODEL FROM TTL **/
        Map<String, Object> returnMap = new HashMap<>();
        Model modelmapping = retrieveModelMapping(inputMappingModel);
        Model model = retrieveMetadataModelFromTTL(url);
        Graph graph = model.getGraph();

        /** DEFINE VARIABLES **/
        List<EPOSDataModelEntity> classes = new ArrayList<>();
        List<String> uidDone = new ArrayList<>();
        BeansCreation beansCreation = new BeansCreation();

        /** PREPARE CLASSES **/
        Map<String,Map<String,String>> classesMap = SPARQLManager.retrieveMainEntities(model);
        for(String uid : classesMap.keySet()){
            String className = SPARQLManager.retrieveEDMMappedClass(classesMap.get(uid).get("class").toString(), modelmapping);
            EPOSDataModelEntity entity = beansCreation.getEPOSDataModelClass(className,uid);
            classes.add(entity);
        }
        classes.removeIf(Objects::isNull);

        /** PREPARE PROPERTIES **/
        exploreGraphAndCreateBeans(modelmapping,beansCreation,graph, null,classes, uidDone);

        /** PRINT CHECK TODO: DELETE **/
        for(EPOSDataModelEntity eposDataModelEntity : classes){
            System.out.println(eposDataModelEntity.toString());
        }

        /** DATABASE POPULATION **/
        for(EPOSDataModelEntity eposDataModelEntity : classes){
            System.out.println("[ADDING TO DATABASE] "+eposDataModelEntity);
            try {
                AbstractAPI api = AbstractAPI.retrieveAPI(eposDataModelEntity.getClass().getSimpleName().toUpperCase());
                LinkedEntity le = api.create(eposDataModelEntity, StatusType.PUBLISHED);
                returnMap.put(le.getUid(), le);
            }catch(Exception apiCreationException){
                apiCreationException.printStackTrace();
                System.err.println("[ERROR] ON: "+ eposDataModelEntity.toString()+"\n[EXCEPTION]: "+apiCreationException.getLocalizedMessage());
            }
        }

        return returnMap;
    }
}
