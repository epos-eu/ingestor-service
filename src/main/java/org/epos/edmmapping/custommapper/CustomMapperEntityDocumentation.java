package org.epos.edmmapping.custommapper;

import org.epos.eposdatamodel.Documentation;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class CustomMapperEntityDocumentation implements CustomMapperEntity {
    @Override
    public List map(Map<String, Object> objectMap) {
        if(objectMap.containsKey("hydra:ApiDocumentation")){
            ArrayList<Documentation> documentationList = new ArrayList<>();
            Documentation d = new Documentation();

            for (String property : objectMap.keySet()) {
                Object o = objectMap.get(property);
                if (objectMap.get(property).getClass().equals(ArrayList.class) && ((List) objectMap.get(property)).get(0).equals("hydra:description")) {
                    d.setDescription(property);
                }
                if (objectMap.get(property).getClass().equals(ArrayList.class) && ((List) objectMap.get(property)).get(0).equals("hydra:entrypoint")) {
                    d.setURI(property);
                }
                if (objectMap.get(property).getClass().equals(ArrayList.class) && ((List) objectMap.get(property)).get(0).equals("hydra:title")) {
                    d.setTitle(property);
                }
            }

            documentationList.add(d);
            return documentationList;
        } else {
            return null;
        }
    }
}
