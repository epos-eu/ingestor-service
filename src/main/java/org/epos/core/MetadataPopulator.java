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
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;

public class MetadataPopulator {


    public static Map<String,String> startMetadataPopulation(String url, String inputMappingModel){

        EposDataModelDAO eposDataModelDAO = new EposDataModelDAO();

        /**
         * GET ALL ONTOLOGIES FROM DB AND POPULATE MODEL AND MODEL MAPPING
         **/
        List<Ontologies> ontologiesList = eposDataModelDAO.getAllFromDB(Ontologies.class);

        String triples = null;
        for(Ontologies ontologies : ontologiesList){
            if(ontologies.getOntologyname().equals(inputMappingModel)){
                triples = new String(Base64.getDecoder().decode(ontologies.getOntologybase64()));
            }
        }
        Model modelmapping = null;
        if(triples!=null) {
            modelmapping = ModelFactory.createDefaultModel()
                    .read(IOUtils.toInputStream(triples, StandardCharsets.UTF_8), null, "TURTLE");
        }

        /** READ TTL FILE FROM URL **/

        final Model model = ModelFactory.createDefaultModel();
        model.read(url, null, "TURTLE");
        Graph graph = model.getGraph();

        /** PREPARE CLASSES **/

        List<EPOSDataModelEntity> classes = new ArrayList<>();
        EPOSDataModelEntity activeClass = null;
        BeansCreation beansCreation = new BeansCreation();

        Map<String,Map<String,String>> classesMap = SPARQLRetriever.retrieveAllClasses(model);
        for(String uid : classesMap.keySet()){
            String className = SPARQLRetriever.executeSPARQLQueryClass(classesMap.get(uid), modelmapping);
            classes.add(beansCreation.getEPOSDataModelClass(className,uid));
        }

        for(EPOSDataModelEntity clazz : classes){
            if(clazz!=null)
                System.out.println(clazz.getClass().getName()+" -----> "+clazz.getUid());
        }

        for (ExtendedIterator<Triple> it = graph.find(); it.hasNext(); ) {
            Triple triple = it.next();
            if(modelmapping!=null){
                Map<String,String> prefixes = modelmapping.getNsPrefixMap();
                String value = triple.getPredicate().toString();
                for(String key : prefixes.keySet()){
                    if(value.contains(prefixes.get(key))) value = value.replaceAll(prefixes.get(key),key+":");
                }
                Map<String,String> itemValue = null;
                boolean isClass = false;
                for(EPOSDataModelEntity eposDataModelEntity : classes){
                    if(eposDataModelEntity!=null && eposDataModelEntity.getUid().equals(triple.getSubject().toString())){
                        activeClass = eposDataModelEntity;
                    }
                }

                if(!value.equals("rdf:type") && activeClass!=null) {
                    itemValue = SPARQLRetriever.executeSPARQLQueryProperty(value, activeClass.getClass().getSimpleName(), modelmapping);
                }

                if(itemValue!=null) {
                    if(!isClass) {
                        if(activeClass!=null) {
                            if(triple.getObject().isURI()){
                                System.out.println("MANAGE URI");
                                beansCreation.getEPOSDataModelPropertiesURI(activeClass, classes, triple.getSubject().toString(), itemValue, triple.getObject().toString());
                            }
                            else if(triple.getObject().isBlank()){
                                System.out.println("MANAGE BLANK");
                                beansCreation.getEPOSDataModelPropertiesBlank(activeClass, classes, triple.getSubject().toString(), itemValue, triple.getObject().toString());
                            }
                            else if(triple.getObject().isLiteral()){
                                System.out.println("MANAGE LITERAL");
                                beansCreation.getEPOSDataModelPropertiesLiteral(activeClass, classes, triple.getSubject().toString(), itemValue, triple.getObject().getLiteralValue());
                            }
                            else if(triple.getObject().isNodeGraph()){
                                System.out.println("IS NODEGRAPH TODO "+triple.getObject().toString());
                            }
                            else if(triple.getObject().isConcrete()){
                                System.out.println("MANAGE CONCRETE");
                                beansCreation.getEPOSDataModelPropertiesLiteral(activeClass, classes, triple.getSubject().toString(), itemValue, triple.getObject().getLiteral().getValue());
                            }
                            else if(triple.getObject().isNodeTriple()){
                                System.out.println("IS NODETRIPLE TODO "+triple.getObject().toString());
                            }
                            else if(triple.getObject().isExt()){
                                System.out.println("IS EXT TODO "+triple.getObject().toString());
                            }
                            else if(triple.getObject().isVariable()){
                                System.out.println("IS VARIABLE TODO "+triple.getObject().toString());
                            }
                        }
                    }
                    else {
                        activeClass = beansCreation.getEPOSDataModelClass(itemValue.get("property"), triple.getSubject().toString());
                        if(activeClass!=null) classes.add(activeClass);
                    }
                }
            }
        }
        for(EPOSDataModelEntity eposDataModelEntity : classes){
            if(eposDataModelEntity!=null) {
                try {
                    AbstractAPI api = retrieveAPI(eposDataModelEntity.getClass().getSimpleName().toUpperCase(), eposDataModelEntity.getClass());
                    LinkedEntity le = api.create(eposDataModelEntity);
                }catch(Exception apiCreationException){
                    apiCreationException.printStackTrace();
                    System.err.println("ERROR ON: "+ eposDataModelEntity.toString()+" "+apiCreationException.getLocalizedMessage());
                }
            }
        }

        return null;
    }

    private static AbstractAPI retrieveAPI(String entityType, Class<?> edmClass){
        AbstractAPI api = null;

        switch(EntityNames.valueOf(entityType)){
            case PERSON:
                edmClass = Person.class;
                api = new PersonAPI(entityType, edmClass);
                break;
            case MAPPING:
                edmClass = Mapping.class;
                api = new MappingAPI(entityType, edmClass);
                break;
            case CATEGORY:
                edmClass = Category.class;
                api = new CategoryAPI(entityType, edmClass);
                break;
            case FACILITY:
                edmClass = Facility.class;
                api = new FacilityAPI(entityType, edmClass);
                break;
            case EQUIPMENT:
                edmClass = Equipment.class;
                api = new EquipmentAPI(entityType, edmClass);
                break;
            case OPERATION:
                edmClass = Operation.class;
                api = new OperationAPI(entityType, edmClass);
                break;
            case WEBSERVICE:
                edmClass = Webservice.class;
                api = new WebServiceAPI(entityType, edmClass);
                break;
            case DATAPRODUCT:
                edmClass = Dataproduct.class;
                api = new DataProductAPI(entityType, edmClass);
                break;
            case CONTACTPOINT:
                edmClass = Contactpoint.class;
                api = new ContactPointAPI(entityType, edmClass);
                break;
            case DISTRIBUTION:
                edmClass = Distribution.class;
                api = new DistributionAPI(entityType, edmClass);
                break;
            case ORGANIZATION:
                edmClass = Organization.class;
                api = new OrganizationAPI(entityType, edmClass);
                break;
            case CATEGORYSCHEME:
                edmClass = CategoryScheme.class;
                api = new CategorySchemeAPI(entityType, edmClass);
                break;
            case SOFTWARESOURCECODE:
                edmClass = SoftwareSourceCode.class;
                api = new SoftwareSourceCodeAPI(entityType, edmClass);
                break;
            case SOFTWAREAPPLICATION:
                edmClass = SoftwareApplication.class;
                api = new SoftwareApplicationAPI(entityType, edmClass);
                break;
            case ADDRESS:
                edmClass = Address.class;
                api = new AddressAPI(entityType, edmClass);
                break;
            case ELEMENT:
                edmClass = Element.class;
                api = new ElementAPI(entityType, edmClass);
                break;
            case LOCATION:
                edmClass = Spatial.class;
                api = new SpatialAPI(entityType, edmClass);
                break;
            case PERIODOFTIME:
                edmClass = Temporal.class;
                api = new TemporalAPI(entityType, edmClass);
                break;
            case IDENTIFIER:
                edmClass = Identifier.class;
                api = new IdentifierAPI(entityType, edmClass);
                break;
            case QUANTITATIVEVALUE:
                edmClass = QuantitativeValue.class;
                api = new QuantitativeValueAPI(entityType, edmClass);
                break;
            case DOCUMENTATION:
                edmClass = Element.class;
                api = new DocumentationAPI(entityType, edmClass);
                break;
        }
        return api;
    }


}
