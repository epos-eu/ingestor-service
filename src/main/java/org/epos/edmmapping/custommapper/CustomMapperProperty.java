package org.epos.edmmapping.custommapper;

import org.epos.edmmapping.EPOSDataModelMainEntity;

import java.util.Map;

public interface CustomMapperProperty {

    void map(Object object, Object propertyValue);

    boolean isToBeMapped(String className, String rdfPropertyName, Class<?> valueClass);

}
