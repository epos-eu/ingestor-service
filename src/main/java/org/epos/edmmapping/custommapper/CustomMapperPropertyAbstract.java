package org.epos.edmmapping.custommapper;

import org.epos.edmmapping.EPOSDataModelMainEntity;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public abstract class CustomMapperPropertyAbstract implements CustomMapperProperty{
    protected String targetRdfPropertyName;
    protected Class<?> targetValueClass;
    protected String targetClassName;
    protected Method method;

    public CustomMapperPropertyAbstract(String targetRdfPropertyName, Class<?> targetValueClass, String targetClassName, Method method) {
        this.targetRdfPropertyName = targetRdfPropertyName;
        this.targetValueClass = targetValueClass;
        this.targetClassName = targetClassName;
        this.method = method;
    }

    protected abstract void toImplementMap(Object object, Object propertyValue) throws InvocationTargetException, IllegalAccessException;

    @Override
    public void map(Object object, Object propertyValue) {
        try {
            toImplementMap(object,propertyValue);
        } catch (InvocationTargetException | IllegalAccessException ignored) {}
    }

    @Override
    public boolean isToBeMapped(String className, String rdfPropertyName, Class<?> valueClass) {
        return className.equals(targetClassName) && targetValueClass.equals(valueClass) && rdfPropertyName.equals(targetRdfPropertyName);
    }
}
