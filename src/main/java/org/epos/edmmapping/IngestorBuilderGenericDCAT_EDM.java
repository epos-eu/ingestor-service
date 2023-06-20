package org.epos.edmmapping;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.*;

public abstract class IngestorBuilderGenericDCAT_EDM {


    protected Map<String, Object> getSemanticDCAT(String dcatVersion){

        Map<String, Object> returns = new HashMap<>();

        String queryPropertyMapping = "select epos.class_name as edm_class_name,\n" +
                "       dcat.vocabulary as vocabulary,\n" +
                "       epos.property_name as property_name,\n" +
                "       epos.range_value as range_value\n" +
                "from source_to_target_mapping as mapping\n" +
                "inner join (\n" +
                "    select c.class_name, p.vocabulary, p.id as propertyid\n" +
                "    from ontology o\n" +
                "        inner join ontology_class oc on o.id = oc.ontology_id\n" +
                "        inner join class c on c.id = oc.class_id\n" +
                "        inner join domain d on c.id = d.class_id\n" +
                "        inner join property p on d.property_id = p.id\n" +
                "    where o.name = '" + dcatVersion + "'\n" +
                ") as dcat on mapping.property_target = dcat.propertyid\n" +
                "inner join (\n" +
                "    select p.id as propertyid, p.property_name, c.class_name, c2.class_name as range_value\n" +
                "    from ontology o\n" +
                "        inner join ontology_class oc on o.id = oc.ontology_id\n" +
                "        inner join class c on c.id = oc.class_id\n" +
                "        inner join domain d on c.id = d.class_id\n" +
                "        inner join property p on d.property_id = p.id\n" +
                "        inner join range r on r.property_id = p.id\n" +
                "        inner join class c2 on c2.id = r.range_id\n" +
                "    where o.name = 'epos data model'\n" +
                ") as epos on mapping.property_source = epos.propertyid";

        String queryEntityMapping = "select c2.vocabulary as from, c1.class_name as to\n" +
                "from class_mapping cm\n" +
                "inner join class c1 on c1.id = class1_id\n" +
                "inner join class c2 on c2.id = class2_id";

        HashMap<String, Set<String>> vocabularyMap = new HashMap<>();
        HashMap<String,HashMap<String, AbstractMap.SimpleEntry<String,String>>> proprietyMap = new HashMap<>();
        HashMap<String, String>  classMap = new HashMap<>();


        try (
                Connection c = DBService.getDBConnection();
                Statement stmt = c.createStatement()
        ) {
            try (ResultSet resultSetPropertyMapping = stmt.executeQuery(queryPropertyMapping)) {

                while (resultSetPropertyMapping.next()) {
                    String vocabulary = resultSetPropertyMapping.getString("vocabulary");
                    String labelEdm = resultSetPropertyMapping.getString("property_name");
                    String domainEpos = resultSetPropertyMapping.getString("edm_class_name");
                    String targetEntityType = resultSetPropertyMapping.getString("range_value");
                    if (vocabulary == null || domainEpos == null || labelEdm == null || targetEntityType == null)
                        continue;
                    vocabulary = vocabulary.trim().replace(" ", "");
                    domainEpos = domainEpos.trim().replace(" ", "");
                    labelEdm = labelEdm.trim().replace(" ", "");
                    targetEntityType = targetEntityType.trim().replace(" ", "");
                    if (vocabularyMap.containsKey(vocabulary)) {
                        vocabularyMap.get(vocabulary).add(domainEpos);
                    } else {
                        HashSet<String> tmpList = new HashSet<>();
                        tmpList.add(domainEpos);
                        vocabularyMap.put(vocabulary, tmpList);
                    }
                    if (proprietyMap.containsKey(domainEpos)) {
                        HashMap<String, AbstractMap.SimpleEntry<String, String>> tmpMap = proprietyMap.get(domainEpos);
                        tmpMap.put(vocabulary, new AbstractMap.SimpleEntry<>(labelEdm, targetEntityType));
                    } else {
                        HashMap<String, AbstractMap.SimpleEntry<String, String>> tmpMap = new HashMap<>();
                        tmpMap.put(vocabulary, new AbstractMap.SimpleEntry<>(labelEdm, targetEntityType));
                        proprietyMap.put(domainEpos, tmpMap);
                    }
                }

            }

            try (ResultSet resultSetEntityMapping = stmt.executeQuery(queryEntityMapping)) {
                while (resultSetEntityMapping.next()) {
                    classMap.put(resultSetEntityMapping.getString("from"), resultSetEntityMapping.getString("to"));
                }
            }

            returns.put("vocabulary", vocabularyMap);
            returns.put("propriety", proprietyMap);
            returns.put("class", classMap);
            return returns;
        } catch (SQLException e) {
            System.err.println(e);
            return null;
        }


    }
}
