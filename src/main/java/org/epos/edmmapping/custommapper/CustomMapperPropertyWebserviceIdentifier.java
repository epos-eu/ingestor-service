package org.epos.edmmapping.custommapper;

import org.epos.edmmapping.EPOSDataModelMainEntity;
import org.epos.eposdatamodel.WebService;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class CustomMapperPropertyWebserviceIdentifier extends CustomMapperPropertyAbstract {
    private static final String targetRdfPropertyName = "schema:identifier";
    private static final String targetClassName = "WebService";
    private static final Class<?> targetValueClass = String.class;
    private static final Method method;
    static {
        Method setSchemaIdentifier = null;
        try {
            setSchemaIdentifier = WebService.class.getMethod("setSchemaIdentifier", targetValueClass);
        } catch (NoSuchMethodException ignored) {}
        method = setSchemaIdentifier;
    }

    public CustomMapperPropertyWebserviceIdentifier() {
        super(targetRdfPropertyName, targetValueClass, targetClassName, method);
    }


    @Override
    public void toImplementMap(Object object, Object propertyValue) throws InvocationTargetException, IllegalAccessException {
            method.invoke(object, propertyValue.toString());
    }


}
