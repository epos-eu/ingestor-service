package org.epos.edmmapping.custommapper;

import org.epos.eposdatamodel.Parameter;

import java.util.ArrayList;
import java.util.Map;
import java.util.List;

import static org.epos.edmmapping.IngestorUtil.findKeyByValueObject;

public class CustomMapperEntityPotentialAction implements CustomMapperEntity {
    @Override
    public List map(Map<String, Object> objectMap) {
        if(objectMap.containsKey("schema:potentialAction")){
            Parameter parameterObject = new Parameter();
            Parameter parameterResult = new Parameter();
            for (String property : objectMap.keySet()) {
                if (property.contains("schema:result")) {
                    parameterResult.setEncodingFormat(findKeyByValueObject(objectMap.get(property), "schema:encodingFormat"));
                    parameterResult.setConformsTo(findKeyByValueObject(objectMap.get(property), "dct:conformsTo"));
                    parameterResult.setAction(Parameter.ActionEnum.RESULT);
                }
                if (property.contains("schema:object")) {
                    parameterObject.setEncodingFormat(findKeyByValueObject(objectMap.get(property), "schema:encodingFormat"));
                    parameterObject.setConformsTo(findKeyByValueObject(objectMap.get(property), "dct:conformsTo"));
                    parameterObject.setAction(Parameter.ActionEnum.OBJECT);
                }
            }
            ArrayList<Parameter> parameterList = new ArrayList<>();
            parameterList.add(parameterResult);
            parameterList.add(parameterObject);
            return parameterList;
        } else {
            return null;
        }
    }
}
