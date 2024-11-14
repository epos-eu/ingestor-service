package org.epos.core;

import abstractapis.AbstractAPI;
import commonapis.LinkedEntityAPI;
import model.StatusType;
import org.apache.jena.datatypes.BaseDatatype;
import org.apache.jena.datatypes.xsd.XSDDateTime;
import org.epos.eposdatamodel.EPOSDataModelEntity;
import org.epos.eposdatamodel.LinkedEntity;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoField;
import java.util.*;

public class BeansCreation <T extends EPOSDataModelEntity> {

    public T getEPOSDataModelClass(String className, String uid){
        try {
            Class clazz = Class.forName("org.epos.eposdatamodel."+className);
            Constructor[] ctor = clazz.getConstructors();
            T object = (T) ctor[0].newInstance();
            //object.setInstanceId(UUID.randomUUID().toString());
            //object.setMetaId(UUID.randomUUID().toString());
            object.setUid(uid);
            object.setEditorId("ingestor");
            object.setFileProvenance("ingestor");
            object.setStatus(StatusType.PUBLISHED);

            return object;
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException |
                 InvocationTargetException e) {
            System.out.println(e.getLocalizedMessage());
            return null;
        }
    }

    public void getEPOSDataModelPropertiesLiteral(EPOSDataModelEntity classObject, List<EPOSDataModelEntity> classes, Map<String, String> property, Object propertyValue) {

        Class<?> propertyValueClass = propertyValue.getClass();
        String propertyName = property.get("property").substring(0, 1).toUpperCase() + property.get("property").substring(1);
        System.out.println("PRE DEBUG: " + classObject.getClass().getName() + " " + propertyValueClass + " " + propertyValue.getClass() + " " + propertyName);

        Method method = null;
        LinkedEntity le = null;
        EPOSDataModelEntity entity = null;

        for (EPOSDataModelEntity eposDataModelEntity : classes) {
            if (eposDataModelEntity != null && eposDataModelEntity.getUid().equals(propertyValue)) {
                entity = eposDataModelEntity;
                break;
            }
        }

        if (entity != null) {
            try {
                le = new LinkedEntity();
                le.setUid(entity.getUid());
                le.setEntityType(entity.getClass().getSimpleName().toUpperCase());
            } catch (Exception skip) {
                System.err.println(skip.getLocalizedMessage());
            }
            propertyValueClass = LinkedEntity.class;
            propertyValue = le;
        } else {
            System.out.println("Is not a linked entity class " + propertyValue);

            if (propertyValueClass.getName().equals("org.apache.jena.datatypes.xsd.XSDDateTime")) {
                propertyValueClass = LocalDateTime.class;
                try {
                    propertyValue = ParseLocalDateTime.parse((String) propertyValue);//LocalDateTime.parse((String)propertyValue,  DateTimeFormatter.ofPattern("yyyy-MM-dd['T'HH:mm:ss'Z']"));
                    System.out.println("DATE: "+propertyValue);

                } catch (DateTimeParseException ignored) {
                    System.err.println(ignored.getLocalizedMessage());
                }
            }
            if (propertyValueClass.getName().equals("java.lang.Boolean")) {
                propertyValueClass = java.lang.String.class;
                propertyValue = Boolean.toString((Boolean)propertyValue);
            }
            if (propertyValueClass.getName().equals("java.lang.Double")) {
                propertyValueClass = java.lang.String.class;
                propertyValue = Double.toString((Double) propertyValue);
            }
            if (propertyValueClass.getName().equals("java.lang.Integer")) {
                propertyValueClass = java.lang.String.class;
                propertyValue = Integer.toString((Integer) propertyValue);
            }
            if (propertyValueClass.getName().equals("java.lang.Float")) {
                propertyValueClass = java.lang.String.class;
                propertyValue = Float.toString((Float) propertyValue);
            }
            if (propertyValueClass.getName().equals("java.lang.Long")) {
                propertyValueClass = java.lang.String.class;
                propertyValue = Long.toString((Long) propertyValue);
            }
            if (propertyValue.getClass().getName().contains("org.apache.jena.datatypes.BaseDatatype")) {
                propertyValueClass = java.lang.String.class;
                propertyValue = ((BaseDatatype.TypedValue) propertyValue).lexicalValue;
            }

        }
        try {
            method = classObject.getClass().getMethod("add" + propertyName, propertyValueClass);
        } catch (NoSuchMethodException e) {
            System.err.println(e.getLocalizedMessage());
        }

        if (method == null) {
            try {
                method = classObject.getClass().getMethod("set" + propertyName, propertyValueClass);
            } catch (NoSuchMethodException e) {
                System.err.println(e.getLocalizedMessage());
            }
        }

        try {
            System.out.println("Method " + method.getName());
        }catch(Exception debug){
            System.out.println("DEBUG: "+propertyName+" "+classObject.getClass().getName()+" "+propertyValueClass+" "+propertyValue.getClass());
        }

        if(method != null && propertyValue != null){
            try {
                System.out.println("Invoking: "+propertyName+" "+propertyValue);
                method.invoke(classObject, propertyValue);
            } catch (IllegalArgumentException |IllegalAccessException | InvocationTargetException e) {
                System.err.println("ERROR Invoking [\nProperty Name: " + propertyName +
                        "\nPropertyValue: " + propertyValue +
                        "\nPropertyClass: " + propertyValueClass +
                        "\nClass Name:" + classObject.getClass().getName() +
                        "\nMethod Name:" + method.getName() +
                        "\nExpected Parameters: " + Arrays.asList(method.getParameterTypes()).toString() + "\n]");
                System.err.println(e.getLocalizedMessage());
                //System.exit(0);
            }
        }
    }

    public void getEPOSDataModelPropertiesNode(EPOSDataModelEntity classObject, List<EPOSDataModelEntity> classes, Map<String, String> property, String propertyValue) {

        Class<?> propertyValueClass = propertyValue.getClass();
        String propertyName = property.get("property").substring(0, 1).toUpperCase() + property.get("property").substring(1);
        System.out.println("PRE DEBUG: " + propertyValueClass + " " + propertyValue.getClass() + " " + propertyName + " "+ propertyValue);

        Method method = null;
        LinkedEntity le = null;
        EPOSDataModelEntity entity = null;

        for (EPOSDataModelEntity eposDataModelEntity : classes) {
            if (eposDataModelEntity != null && eposDataModelEntity.getUid().equals(propertyValue)) {
                entity = eposDataModelEntity;
                break;
            }
        }

        if(entity==null){
            entity = getEPOSDataModelClass(propertyName,propertyValue);
        }

        if (entity != null) {
            try {

                le = new LinkedEntity();
                le.setUid(entity.getUid());
                le.setEntityType(entity.getClass().getSimpleName().toUpperCase());
            } catch (Exception skip) {
                System.err.println(skip.getLocalizedMessage());
            }
            propertyValueClass = LinkedEntity.class;

            try {
                method = classObject.getClass().getMethod("add" + propertyName, propertyValueClass);
            } catch (NoSuchMethodException e) {
                System.err.println(e.getLocalizedMessage());
            }

            if (method == null) {
                try {
                    method = classObject.getClass().getMethod("set" + propertyName, propertyValueClass);
                } catch (NoSuchMethodException e) {
                    System.err.println(e.getLocalizedMessage());
                }
            }

            if (method != null) {
                try {
                    System.out.println("ADDING " + le + " to " + classObject);
                    method.invoke(classObject, le);
                } catch (IllegalArgumentException | IllegalAccessException | InvocationTargetException e) {
                    System.err.println("ERROR Invoking [\nProperty Name: " + propertyName +
                            "\nPropertyValue: " + propertyValue +
                            "\nPropertyClass: " + propertyValueClass +
                            "\nClass Name:" + classObject.getClass().getName() +
                            "\nMethod Name:" + method.getName() +
                            "\nExpected Parameters: " + Arrays.asList(method.getParameterTypes()).toString() + "\n]");
                    System.err.println(e.getLocalizedMessage());
                    //System.exit(0);
                }
            }
        }
    }
}
