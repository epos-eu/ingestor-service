package org.epos.core;

import dao.EposDataModelDAO;
import model.Ontology;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

public class OntologiesManager {

    public static void createOntology(String name, String type, String ontologyURL) throws IOException {
        EposDataModelDAO eposDataModelDAO = new EposDataModelDAO();

        List<Ontology> ontologiesList = eposDataModelDAO.getAllFromDB(Ontology.class);


        Ontology existingOntology = null;

        for(Ontology ontologies : ontologiesList){
            if(ontologies.getName().equals(name)) existingOntology = ontologies;
        }

        URL url = new URL(ontologyURL);
        StringBuilder resultStringBuilder = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(url.openStream()))) {
            String line;
            while ((line = br.readLine()) != null) {
                resultStringBuilder.append(line).append("\n");
            }
        }
        String encoded = Base64.getEncoder().encodeToString(resultStringBuilder.toString().getBytes(StandardCharsets.UTF_8));

        Ontology ont = new Ontology();
        ont.setId(existingOntology!=null? existingOntology.getId() :UUID.randomUUID().toString());
        ont.setName(name);
        ont.setType(type);
        ont.setContent(encoded);

        eposDataModelDAO.updateObject(ont);
    }

    public static List retrieveOntologies() {
        EposDataModelDAO eposDataModelDAO = new EposDataModelDAO();

        return eposDataModelDAO.getAllFromDB(Ontology.class);
    }
}
