package org.epos.edmmapping.custommapper;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class CustomMapperEntityHasQualityAnnotation implements CustomMapperEntity {
    @Override
    public String map(Map<String, Object> objectMap) {
        if(objectMap.containsKey("dqv:hasQualityAnnotation")){
            for (String property : objectMap.keySet()) {
                Object o = objectMap.get(property);
                if (o.getClass().equals(ArrayList.class) && ((List) o).get(0).equals("oa:hasBody"))
                    return property;

            }
        }
        return null;

    }
}
