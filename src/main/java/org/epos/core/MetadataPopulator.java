package org.epos.core;

import abstractapis.AbstractAPI;
import commonapis.*;
import dao.EposDataModelDAO;
import metadataapis.*;
import model.*;
import org.apache.commons.io.IOUtils;
import org.apache.jena.graph.Graph;
import org.apache.jena.graph.Triple;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.util.iterator.ExtendedIterator;
import org.epos.eposdatamodel.EPOSDataModelEntity;
import org.epos.eposdatamodel.LinkedEntity;

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



    public static Map<String, Object> retrievePlainValueFromInnerMethods(Model modelmapping,Graph graph, String subject, EPOSDataModelEntity activeClass){
        Map<String, String> prefixes = modelmapping.getNsPrefixMap();
        Map<String, String> itemValue = null;
        Map<String, Object> innerValue = new HashMap<>();
        for (ExtendedIterator<Triple> it = graph.find(); it.hasNext(); ) {
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
                    if ((triple.getObject().isBlank()) && itemValue == null) {
                        innerValue = retrievePlainValueFromInnerMethods(modelmapping,graph, triple.getObject().toString(), activeClass);
                    }else{
                        innerValue.put("itemValue",itemValue);
                        innerValue.put("plain",triple.getObject().toString());
                    }
                }
            }
        }
        return innerValue;
    }


    public static void innerMethodToTest(Model modelmapping, BeansCreation beansCreation, Graph graph, EPOSDataModelEntity activeClass, List<EPOSDataModelEntity> classes,List<EPOSDataModelEntity> missingClasses) {
        /** SET PREFIXES **/
        Map<String, String> prefixes = modelmapping.getNsPrefixMap();

        if(activeClass==null) {
            for (ExtendedIterator<Triple> it = graph.find(); it.hasNext(); ) {
                Triple triple = it.next();
                /** Get predicate value of triple and replace long prefix with short one **/
                String value = triple.getPredicate().toString();
                for (String key : prefixes.keySet()) {
                    if (value.contains(prefixes.get(key))) value = value.replaceAll(prefixes.get(key), key + ":");
                }

                /** Retrieve Active Class **/
                ListIterator<EPOSDataModelEntity> iter = missingClasses.listIterator();
                while(iter.hasNext()){
                    activeClass = iter.next();
                    if (activeClass != null && activeClass.getUid().equals(triple.getSubject().toString())) {
                        iter.remove();
                        innerMethodToTest(modelmapping, beansCreation, graph, activeClass, classes, missingClasses);
                    }
                }
            }
        } else {
            for (ExtendedIterator<Triple> it = graph.find(); it.hasNext(); ) {
                Triple triple = it.next();
                if(triple.getSubject().toString().equals(activeClass.getUid())) {
                    System.out.println("Active class: "+activeClass.getUid());
                    /** Get predicate value of triple and replace long prefix with short one **/
                    String value = triple.getPredicate().toString();
                    for (String key : prefixes.keySet()) {
                        if (value.contains(prefixes.get(key))) value = value.replaceAll(prefixes.get(key), key + ":");
                    }
                    /** Manage Properties of Active Class **/
                    Map<String, String> itemValue = null;
                    Map<String,Object> innerValue = null;
                    if (!value.equals("rdf:type")) {
                        itemValue = SPARQLManager.retrievePropertyValueInEDM(value, activeClass.getClass().getSimpleName(), modelmapping);
                        /*System.out.println(activeClass.getClass().getSimpleName() + " " + value);
                        System.out.println(itemValue);
                        System.out.println(triple.getObject().toString());*/
                        /** TODO: add recursive gathering of the element **/
                        if((triple.getObject().isBlank()) && itemValue==null){
                            innerValue = retrievePlainValueFromInnerMethods(modelmapping,graph, triple.getObject().toString(), activeClass);
                            System.out.println("FOUND: "+innerValue);
                        }
                    }
                    if(itemValue == null && innerValue!=null){
                        System.out.println("MANAGE RECURSIVE ITEM");
                        beansCreation.getEPOSDataModelPropertiesLiteral(activeClass, classes, triple.getSubject().toString(), (Map<String, String>) innerValue.get("itemValue"), innerValue.get("plain").toString());

                    }
                    if(itemValue != null){
                        if (triple.getObject().isURI()) {
                            System.out.println("MANAGE URI");
                            beansCreation.getEPOSDataModelPropertiesNode(activeClass, classes, triple.getSubject().toString(), itemValue, triple.getObject().toString());
                        } else if (triple.getObject().isBlank()) {
                            System.out.println("MANAGE BLANK");
                            beansCreation.getEPOSDataModelPropertiesNode(activeClass, classes, triple.getSubject().toString(), itemValue, triple.getObject().toString());
                        }else if (triple.getObject().isLiteral()) {
                            System.out.println("MANAGE LITERAL");
                            beansCreation.getEPOSDataModelPropertiesLiteral(activeClass, classes, triple.getSubject().toString(), itemValue, triple.getObject().getLiteralValue());
                        } else if (triple.getObject().isConcrete()) {
                            System.out.println("MANAGE CONCRETE");
                            beansCreation.getEPOSDataModelPropertiesLiteral(activeClass, classes, triple.getSubject().toString(), itemValue, triple.getObject().getLiteral().getValue());
                        } else if (triple.getObject().isVariable()
                                || triple.getObject().isExt()
                                || triple.getObject().isNodeTriple()
                                || triple.getObject().isNodeGraph()) {
                            /** NOT USED ATM **/
                            System.out.println("MANAGE OTHER??");
                        }
                    }
                }
            }
        }
    }

    public static Map<String,Object> startMetadataPopulation(String url, String inputMappingModel){

        Map<String, Object> returnMap = new HashMap<>();

        Model modelmapping = retrieveModelMapping(inputMappingModel);

        Model model = retrieveMetadataModelFromTTL(url);
        Graph graph = model.getGraph();


        /** PREPARE CLASSES **/

        List<EPOSDataModelEntity> classes = new ArrayList<>();
        List<EPOSDataModelEntity> missingClasses = new ArrayList<>();
        EPOSDataModelEntity activeClass = null;
        BeansCreation beansCreation = new BeansCreation();

        Map<String,Map<String,String>> classesMap = SPARQLManager.retrieveMainEntities(model);
        for(String uid : classesMap.keySet()){
            String className = SPARQLManager.retrieveEDMMappedClass(classesMap.get(uid).get("class").toString(), modelmapping);
            EPOSDataModelEntity entity = beansCreation.getEPOSDataModelClass(className,uid);
            classes.add(entity);
            missingClasses.add(entity);
        }


        innerMethodToTest(modelmapping,beansCreation,graph,activeClass,classes, missingClasses);
        System.out.println("ADDING TO DATABASE "+classes.size());

        for(EPOSDataModelEntity eposDataModelEntity : classes){
            System.out.println("ADDING TO DATABASE "+eposDataModelEntity);
            if(eposDataModelEntity!=null) {
                try {
                    AbstractAPI api = AbstractAPI.retrieveAPI(eposDataModelEntity.getClass().getSimpleName().toUpperCase());
                    System.out.println(eposDataModelEntity.getClass().getSimpleName().toUpperCase());
                    System.out.println(eposDataModelEntity);
                    System.out.println(api);
                    LinkedEntity le = api.create(eposDataModelEntity);
                    returnMap.put(le.getUid(), le);
                }catch(Exception apiCreationException){
                    apiCreationException.printStackTrace();
                    System.err.println("ERROR ON: "+ eposDataModelEntity.toString()+" "+apiCreationException.getLocalizedMessage());
                }
            }
        }

        return returnMap;
    }

}
