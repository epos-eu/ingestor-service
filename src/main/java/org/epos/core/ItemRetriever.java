package org.epos.core;

import org.apache.jena.query.*;
import org.apache.jena.rdf.model.Model;

import java.util.HashMap;
import java.util.Map;

public class ItemRetriever {

    public static Map<String,String> retrieveAllClasses(Model model){

        Map<String,String> prefixes = model.getNsPrefixMap();
        String queryString = "";
        for(String key : prefixes.keySet()){
            queryString+="PREFIX "+key+": <"+prefixes.get(key)+">\n";
        }
        queryString += "SELECT ?uid ?class { ?uid a ?class . }";
        Map<String,String> mapUidClassName = new HashMap<>();
        Query query = QueryFactory.create(queryString);
        QueryExecution qexec = QueryExecutionFactory.create(query, model);
        try {
            ResultSet results = qexec.execSelect();
            while(results.hasNext()) {
                QuerySolution soln = results.nextSolution();
                String classUid = soln.get("uid").toString();
                String className = soln.get("class").toString();
                mapUidClassName.put(classUid,className);
            }
            return mapUidClassName;
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
                "SELECT ?property ?range ?subproperty ?rangeprop \n" +
                        "WHERE {\n" +
                        "  ?property rdfs:domain edm:"+className+" .\n" +
                        "  ?property (owl:equivalentProperty|^owl:equivalentProperty)* "+value+" .\n" +
                        "  ?property rdfs:range  ?range .\n" +
                        "  OPTIONAL { ?prop rdfs:domain ?range } .\n" +
                        "  OPTIONAL { ?prop (owl:equivalentProperty|^owl:equivalentProperty)* ?subproperty } . \n" +
                        "  OPTIONAL { ?prop rdfs:range ?rangeprop } .\n" +
                        "}";
        System.out.println(queryString);
        Query query = QueryFactory.create(queryString);
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
                    String subproperty = soln.get("subproperty").toString();
                    String rangeprop = soln.get("rangeprop").toString();
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
                    returnMapQueryProperty.put("subproperty", subproperty);
                    returnMapQueryProperty.put("rangeprop", rangeprop);
                    System.out.println(returnMapQueryProperty);
                    return returnMapQueryProperty;

                }
            }
        } finally {
            qexec.close();
        }

        return null;
    }

    public static String executeSPARQLQueryClass(String value, Model model){

        Map<String,String> prefixes = model.getNsPrefixMap();
        String queryString = "";
        for(String key : prefixes.keySet()){
            queryString+="PREFIX "+key+": <"+prefixes.get(key)+">\n";
            if(value.contains(prefixes.get(key))) value = value.replaceAll(prefixes.get(key),key+":");
        }
        queryString +=
                "SELECT ?x WHERE { ?x (owl:equivalentClass|^owl:equivalentClass)* "+value+" . }";
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
