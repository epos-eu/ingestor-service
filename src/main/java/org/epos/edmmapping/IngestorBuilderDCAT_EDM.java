package org.epos.edmmapping;

import org.epos.edmmapping.custommapper.*;

import java.util.*;

public class IngestorBuilderDCAT_EDM extends IngestorBuilderGenericDCAT_EDM implements IngestorBuilder{

    @Override
    public Ingestor build(){

        Map<String, Object> returns = this.getSemanticDCAT("epos-dcat-ap");

        HashMap<String, Set<String>> vocabularyMap = (HashMap<String, Set<String>>) returns.get("vocabulary");
        HashMap<String,HashMap<String, AbstractMap.SimpleEntry<String,String>>> proprietyMap = (HashMap<String, HashMap<String, AbstractMap.SimpleEntry<String, String>>>) returns.get("propriety");
        HashMap<String, String>  classMap = (HashMap<String, String>) returns.get("class");
        


        //------------
        proprietyMap.get("ContactPoint").put("schema:contactPoint", new AbstractMap.SimpleEntry<>("Person", "Person"));
        proprietyMap.get("WebService").put("dct:conformsTo", new AbstractMap.SimpleEntry<>("Documentation", "Documentation"));
        proprietyMap.get("WebService").put("hydra:entrypoint", new AbstractMap.SimpleEntry<>("EntryPoint", "Literal"));
        proprietyMap.get("WebService").put("dcat:keyword", new AbstractMap.SimpleEntry<>("Keywords", "Literal"));
        proprietyMap.get("WebService").put("schema:identifier", new AbstractMap.SimpleEntry<>("Identifier", "Identifier"));
        proprietyMap.get("WebService").put("dct:relation", new AbstractMap.SimpleEntry<>("Relation", "WebService"));
        proprietyMap.get("DataProduct").put("dct:identifier", new AbstractMap.SimpleEntry<>("DctIdentifier", "Literal"));
        proprietyMap.get("DataProduct").put("dcat:version", new AbstractMap.SimpleEntry<>("VersionInfo", "Literal"));
        proprietyMap.get("DataProduct").put("dqv:hasQualityAnnotation", new AbstractMap.SimpleEntry<>("HasQualityAnnotation", "Literal"));
        proprietyMap.get("Mapping").put("schema:multipleValues", new AbstractMap.SimpleEntry<>("MultipleValues", "Literal"));
        proprietyMap.get("Mapping").put("schema:readonlyValue", new AbstractMap.SimpleEntry<>("ReadOnlyValue", "Literal"));
        classMap.put("schema:PropertyValue", "Identifier");
        classMap.put("skos:Concept", "Category");
        proprietyMap.get("Category").remove("skos:inScheme");
        proprietyMap.get("Category").remove("skos:definition");
        proprietyMap.get("Category").remove("skos:prefLabel");
        proprietyMap.get("CategoryScheme").put("skos:prefLabel", new AbstractMap.SimpleEntry<>("Code", "Literal"));
        proprietyMap.get("CategoryScheme").put("foaf:logo", new AbstractMap.SimpleEntry<>("Logo", "Literal"));
        proprietyMap.get("CategoryScheme").put("foaf:homepage", new AbstractMap.SimpleEntry<>("Homepage", "Literal"));
        proprietyMap.get("CategoryScheme").put("schema:color", new AbstractMap.SimpleEntry<>("Color", "Literal"));
        proprietyMap.get("CategoryScheme").put("schema:orderItemNumber", new AbstractMap.SimpleEntry<>("Orderitemnumber", "Literal"));
        proprietyMap.get("CategoryScheme").put("skos:hasTopConcept", new AbstractMap.SimpleEntry<>("TopConcepts", "Literal"));
        proprietyMap.get("Category").put("skos:definition", new AbstractMap.SimpleEntry<>("Description", "Literal"));
        proprietyMap.get("Category").put("skos:prefLabel", new AbstractMap.SimpleEntry<>("Name", "Literal"));
        proprietyMap.get("Category").put("skos:inScheme", new AbstractMap.SimpleEntry<>("InScheme", "Literal"));
        proprietyMap.get("Category").put("skos:broader", new AbstractMap.SimpleEntry<>("Broader", "Literal"));
        proprietyMap.get("Category").put("skos:narrower", new AbstractMap.SimpleEntry<>("Narrower", "Literal"));
        proprietyMap.get("Organization").remove("schema:owns");
        proprietyMap.get("Organization").put("schema:owns", new AbstractMap.SimpleEntry<>("Owns", "Literal"));
        proprietyMap.get("Facility").put("dcat:keyword", new AbstractMap.SimpleEntry<>("Keywords", "Literal"));
        proprietyMap.get("Facility").put("dct:identifier", new AbstractMap.SimpleEntry<>("Identifier", "Literal"));
        proprietyMap.get("Equipment").put("dcat:keyword", new AbstractMap.SimpleEntry<>("Keywords", "Literal"));
        proprietyMap.get("Equipment").put("dct:identifier", new AbstractMap.SimpleEntry<>("Identifier", "Literal"));
        proprietyMap.get("Equipment").put("dct:title", new AbstractMap.SimpleEntry<>("Name", "Literal"));
        proprietyMap.get("Equipment").put("dct:description", new AbstractMap.SimpleEntry<>("Description", "Literal"));
        proprietyMap.get("Equipment").put("dcat:landingPage", new AbstractMap.SimpleEntry<>("PageURL", "Literal"));
        //------------

        System.out.println(proprietyMap.get("Equipment").toString());
        return new StandardIngestor()
                .vocabularyMap(vocabularyMap)
                .proprietyMap(proprietyMap)
                .classMap(classMap)
                .addCustomMapperEntities(new CustomMapperEntityLocation())
                .addCustomMapperEntities(new CustomMapperEntityDocumentation())
                .addCustomMapperEntities(new CustomMapperEntityPotentialAction())
                .addCustomMapperEntities(new CustomMapperEntityHasQualityAnnotation())
                .addCustomMapperProperties(new CustomMapperPropertyWebserviceIdentifier());
    }
}
