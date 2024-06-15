package org.epos.core;

import org.apache.jena.query.*;
import org.apache.jena.rdf.model.Model;

import java.util.HashMap;
import java.util.Map;

public class SPARQLRetriever {

    public static Map<String,Map<String,String>> retrieveAllClasses(Model model){

        Map<String,String> prefixes = model.getNsPrefixMap();
        String queryString = "";
        for(String key : prefixes.keySet()){
            queryString+="PREFIX "+key+": <"+prefixes.get(key)+">\n";
        }
        queryString += "SELECT ?class ?uid ?superclass ?relationto WHERE { \n" +
                "  ?uid a ?class .\n" +
                "  OPTIONAL { ?superclass ?relationto ?uid }\n" +
                "}";
        Map<String,Map<String,String>> mapUidItems = new HashMap<>();
        Query query = QueryFactory.create(queryString);
        QueryExecution qexec = QueryExecutionFactory.create(query, model);
        try {
            ResultSet results = qexec.execSelect();
            while(results.hasNext()) {
                Map<String,String> mapItems = new HashMap<>();
                QuerySolution soln = results.nextSolution();
                String className = soln.get("class").toString();
                String classUid = soln.get("uid").toString();
                String superClassName = soln.get("superclass")!=null? soln.get("superclass").toString() : null;
                String relationClassToSuperClass = soln.get("relationto")!=null? soln.get("relationto").toString() : null;
                mapItems.put("class",className);
                mapItems.put("uid",classUid);
                mapItems.put("superclass",superClassName);
                mapItems.put("relationto",relationClassToSuperClass);
                mapUidItems.put(classUid, mapItems);
            }
            return mapUidItems;
        } finally {
            qexec.close();
        }

    }

    public static Map<String, String> executeSPARQLQueryProperty(String value, String className, Model model){

        Map<String, String> returnMapQueryProperty = new HashMap<>();

        Map<String,String> prefixes = model.getNsPrefixMap();
        String queryString = "";
        for(String key : prefixes.keySet()){
            queryString+="PREFIX "+key+": <"+prefixes.get(key)+">\n";
        }
        /*queryString +=
                "SELECT ?class ?property ?range WHERE { \n" +
                        "?class (owl:equivalentClass|^owl:equivalentClass)* edm:"+className+" .\n" +
                        "?property rdfs:domain ?class .\n" +
                        "?property (owl:equivalentProperty|^owl:equivalentProperty)* "+value+" . \n" +
                        "?property rdfs:range  ?range .\n" +
                        "}";*/
        queryString +=
                "SELECT ?property ?range ?prop ?rangeprop \n" +
                        "WHERE {\n" +
                        "  ?property rdfs:domain edm:"+className+" .\n" +
                        "  ?property (owl:equivalentProperty|^owl:equivalentProperty)* "+value+" .\n" +
                        "  ?property rdfs:range  ?range .\n" +
                        "  OPTIONAL { ?range a owl:Class } .\n" +
                        "  OPTIONAL { ?prop rdfs:domain ?range } . \n" +
                        "  OPTIONAL { ?prop rdfs:range ?rangeprop } .\n" +
                        "}";
        Query query = null;
        try {
            query = QueryFactory.create(queryString);
        }catch(Exception e){
            System.out.println(queryString);
            System.out.println(e.getLocalizedMessage());
            System.exit(0);
        }
        QueryExecution qexec = QueryExecutionFactory.create(query, model);
        prefixes.put("edm","http://www.epos-eu.org/epos-data-model#");
        try {
            ResultSet results = qexec.execSelect();
            while(results.hasNext()) {
                QuerySolution soln = results.nextSolution();
                if(soln!=null && soln.get("property").toString().contains("http://www.epos-eu.org/epos-data-model#")) {
                    //String clazz = soln.get("class").toString();
                    String property = soln.get("property").toString();
                    String range = soln.get("range").toString();
                    String subproperty = soln.get("prop")!=null? soln.get("prop").toString() : "";
                    String rangeprop = soln.get("rangeprop")!=null? soln.get("rangeprop").toString() : "";
                    for(String val : prefixes.values()){
                        //if(clazz.contains(val)) clazz = clazz.replaceAll(val,"");
                        if(property.contains(val)) property = property.replaceAll(val,"");
                        if(range.contains(val)) range = range.replaceAll(val,"");
                        if(subproperty.contains(val)) subproperty = subproperty.replaceAll(val,"");
                        if(rangeprop.contains(val)) rangeprop = rangeprop.replaceAll(val,"");
                    }
                    //returnMapQueryProperty.put("class", clazz);
                    returnMapQueryProperty.put("property", property);
                    returnMapQueryProperty.put("range", range);
                    returnMapQueryProperty.put("prop", subproperty);
                    returnMapQueryProperty.put("rangeprop", rangeprop);
                    return returnMapQueryProperty;

                }
            }
        } finally {
            qexec.close();
        }

        return null;
    }

    public static String executeSPARQLQueryClass(Map<String,String> value, Model model){

        Map<String,String> prefixes = model.getNsPrefixMap();
        String queryString = "";
        String itemValue = value.get("class");
        for(String key : prefixes.keySet()){
            queryString+="PREFIX "+key+": <"+prefixes.get(key)+">\n";
            if(itemValue.contains(prefixes.get(key))) itemValue = itemValue.replaceAll(prefixes.get(key),key+":");
        }
        queryString +=
                "SELECT ?x WHERE { ?x (owl:equivalentClass|^owl:equivalentClass)* "+itemValue+" . }";
        Query query = QueryFactory.create(queryString);
        QueryExecution qexec = QueryExecutionFactory.create(query, model);
        try {
            ResultSet results = qexec.execSelect();
            while (results.hasNext()) {
                QuerySolution soln = results.nextSolution();
                if (soln.toString().contains("http://www.epos-eu.org/epos-data-model#"))
                    return soln.get("x").toString().replaceAll("http://www.epos-eu.org/epos-data-model#", "");
            }
        }catch(Exception e){
            System.err.println("Error with query "+queryString+"\n"+e.getLocalizedMessage());
        } finally {
            qexec.close();
        }
        return null;
    }

}
