package org.epos.edmmapping;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class IngestorUtil {
    public static String findKeyByValueObject(Object objectMap, String value){
        if (objectMap.getClass().equals(HashMap.class))
            return findKeyByValue((Map<String, Object>) objectMap, value);
        return null;
    }

    public static String findKeyByValue(Map<String, Object> objectMap, String value){
        for (String propertyValue : objectMap.keySet()) {
            Object propertyPredicate = objectMap.get(propertyValue);
            if (( propertyPredicate.getClass().equals(ArrayList.class) && ((ArrayList<?>) propertyPredicate).contains(value) )
                    || ( propertyPredicate.getClass().equals(String.class) && propertyPredicate.equals(value) )) {
                return propertyValue;
            }
        }
        return null;
    }

    // create beans from class
    public static Object createObjectFromClass(Class<?> clazz) throws NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException {
        Constructor<?> ctor = clazz.getConstructor();
        return  ctor.newInstance();
    }

    // identify the name of one entity (the argument) in the EDM
    public static String findEposDataModelClass(Map<String, Object> objectMap, HashMap<String, String> classMap){
        for (String propertyValue : objectMap.keySet()) {
            Object propertyPredicate = objectMap.get(propertyValue);
            if (propertyPredicate.getClass().equals(ArrayList.class)) {
                if (((ArrayList<?>) propertyPredicate).contains("rdf:type") || ((ArrayList<?>) propertyPredicate).contains("http://www.w3.org/1999/02/22-rdf-syntax-ns#type")) {
                    if(classMap.get(propertyValue) != null)
                        return classMap.get(propertyValue).replace(" ", "");
                }
            }
        }
        return null;
    }

}
