package org.epos.core;

import metadataapis.EntityNames;
import org.apache.jena.datatypes.xsd.XSDDateTime;
import org.eclipse.persistence.jpa.jpql.parser.DateTime;
import org.epos.eposdatamodel.*;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
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
            object.setInstanceId(UUID.randomUUID().toString());
            object.setMetaId(UUID.randomUUID().toString());
            object.setUid(uid);
            System.out.println(className+" "+uid+" "+object.getUid());
            return object;
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException |
                 InvocationTargetException e) {
            return null;
        }
    }

    public void getEPOSDataModelPropertiesLiteral(EPOSDataModelEntity classObject, Map<String,String> property, Object propertyValue){

        System.out.println("MANAGE LITERAL");

        Class<?> propertyValueClass = propertyValue.getClass();
        String propertyName = property.get("property").substring(0, 1).toUpperCase() + property.get("property").substring(1);
        System.out.println("PRE DEBUG: "+propertyValueClass+" "+propertyValue.getClass()+" "+propertyName);


        if(propertyValueClass.getName().equals("org.apache.jena.datatypes.xsd.XSDDateTime")){
            Calendar calendar = ((XSDDateTime) propertyValue).asCalendar();
            LocalDateTime localDateTime = LocalDateTime.ofInstant(calendar.toInstant(), calendar.getTimeZone().toZoneId());
            propertyValueClass = String.class;
            try {
                DateTimeFormatter formatter = new DateTimeFormatterBuilder()
                        .appendPattern("yyyy-MM-dd'T'HH:mm:ss'Z'")
                        .parseDefaulting(ChronoField.HOUR_OF_DAY, 0)
                        .parseDefaulting(ChronoField.MINUTE_OF_HOUR, 0)
                        .parseDefaulting(ChronoField.SECOND_OF_MINUTE, 0)
                        .toFormatter();
                propertyValue = localDateTime.format(formatter);
            } catch (DateTimeParseException ignored){
                System.err.println(ignored.getLocalizedMessage());
            }
        }
        if(propertyValueClass.getName().equals("java.lang.Boolean")){
            propertyValueClass = java.lang.String.class;
            propertyValue = propertyValue.toString();
        }
        if(propertyValueClass.getName().equals("java.lang.Double")){
            propertyValueClass = java.lang.String.class;
            propertyValue = Double.toString((Double) propertyValue);
        }
        if(propertyValueClass.getName().equals("java.lang.Integer")){
            propertyValueClass = java.lang.String.class;
            propertyValue = Integer.toString((Integer) propertyValue);
        }
        if(propertyValueClass.getName().equals("java.lang.Float")){
            propertyValueClass = java.lang.String.class;
            propertyValue = Float.toString((Float) propertyValue);
        }
        if(propertyValueClass.getName().equals("java.lang.Long")){
            propertyValueClass = java.lang.String.class;
            propertyValue = Long.toString((Long) propertyValue);
        }
        if(propertyValue.getClass().getName().contains("org.apache.jena.datatypes.BaseDatatype")){
            System.err.println("WARNING BASE DATATYPE "+propertyValue+" "+propertyName);
            propertyValueClass = java.lang.String.class;
            propertyValue = propertyValue.toString();
        }
        if(propertyName.equals("LegalName")){
            propertyValueClass = LegalName.class;
            LegalName ln = new LegalName();
            ln.setLegalname(propertyValue.toString());
            propertyValue = ln;
        }

        Method method = null;
        try {
            method = classObject.getClass().getMethod("add"+propertyName, propertyValueClass);
        } catch (NoSuchMethodException e) {
            System.err.println(e.getLocalizedMessage());
        }

        if(method==null) {
            try {
                method = classObject.getClass().getMethod("set" + propertyName, propertyValueClass);
            } catch (NoSuchMethodException e) {
                System.err.println(e.getLocalizedMessage());
            }
        }

        if(method==null){
            if(propertyName.equals("Identifier")) {
                Identifier le = new Identifier();
                le.setType("plain");
                le.setIdentifier(propertyValue.toString());
                propertyValue = le;
                propertyValueClass = Identifier.class;
            }else {
                LinkedEntity le = new LinkedEntity();
                le.setUid(propertyValue.toString());
                propertyValue = le;
                propertyValueClass = LinkedEntity.class;
            }
        }

        if(method==null) {
            try {
                method = classObject.getClass().getMethod("add" + propertyName, propertyValueClass);
            } catch (NoSuchMethodException e) {
                System.err.println(e.getLocalizedMessage());
            }
        }

        if(method==null) {
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

        if(method!=null){
            try {
                System.out.println("Invoking: "+propertyName+" "+propertyValue);
                method.invoke(classObject, propertyValue);
            } catch (IllegalArgumentException |IllegalAccessException | InvocationTargetException e) {
                System.err.println("ERROR Invoking [\nProperty Name: "+propertyName+
                        "\nPropertyValue: "+propertyValue+
                        "\nPropertyClass: "+propertyValueClass+
                        "\nClass Name:"+classObject.getClass().getName()+
                        "\nMethod Name:"+method.getName()+
                        "\nExpected Parameters: "+Arrays.asList(method.getParameterTypes()).toString()+"\n]");
                System.err.println(e.getLocalizedMessage());
                System.exit(0);
            }
        }
    }

    public void getEPOSDataModelPropertiesURI(EPOSDataModelEntity classObject, List<EPOSDataModelEntity> classes, String subject, Map<String,String> property, String propertyValue){

        System.out.println("MANAGE URI");

        System.out.println(subject);
        System.out.println(property.toString());
        Class<?> propertyValueClass = propertyValue.getClass();
        String propertyName = property.get("property").substring(0, 1).toUpperCase() + property.get("property").substring(1);
        System.out.println("PRE DEBUG: "+propertyValueClass+" "+propertyValue.getClass()+" "+propertyName);

        Method method = null;
        LinkedEntity le = null;
        EPOSDataModelEntity entity = null;

        for(EPOSDataModelEntity eposDataModelEntity : classes){
            if(eposDataModelEntity!=null && eposDataModelEntity.getUid().equals(propertyValue)) {
                entity = eposDataModelEntity;
                break;
            }
        }

        if(property.get("range").toLowerCase().equals("string")){
            propertyValueClass = String.class;
        }else {
            if(entity!=null) entity = getEPOSDataModelClass(property.get("range").toString(), propertyValue);
            try {
                le = new LinkedEntity();
                le.setUid(propertyValue.toString());
                le.setEntityType(EntityNames.valueOf(property.get("range").toString().toUpperCase()).toString());
            }catch(Exception skip){
                System.err.println(skip.getLocalizedMessage());
            }
            if(entity!=null) propertyValueClass = entity.getClass();
            else propertyValueClass = LinkedEntity.class;

            System.out.println("CLASS: "+propertyValueClass);
        }

        if(entity!=null) propertyValueClass = entity.getClass();

        try {
            method = classObject.getClass().getMethod("add" + propertyName, propertyValueClass);
        } catch (NoSuchMethodException e) {
            System.err.println(e.getLocalizedMessage());
        }

        if(method==null) {
            try {
                method = classObject.getClass().getMethod("set" + propertyName, propertyValueClass);
            } catch (NoSuchMethodException e) {
                System.err.println(e.getLocalizedMessage());
            }
        }

        if(method==null){
            System.out.println("CHANGE TO LINKED ENTITY");
            propertyValueClass = LinkedEntity.class;
        }

        if(method==null) {
            try {
                method = classObject.getClass().getMethod("add" + propertyName, propertyValueClass);
            } catch (NoSuchMethodException e) {
                System.err.println(e.getLocalizedMessage());
            }
        }

        if(method==null) {
            try {
                method = classObject.getClass().getMethod("set" + propertyName, propertyValueClass);
            } catch (NoSuchMethodException e) {
                System.err.println(e.getLocalizedMessage());
            }
        }


        if(method!=null){
            try {
                if(propertyValueClass.equals(String.class)) method.invoke(classObject, propertyValue);
                else{
                    if(propertyValueClass!=LinkedEntity.class) method.invoke(classObject, entity);
                    else method.invoke(classObject, le);
                }
            } catch (IllegalArgumentException |IllegalAccessException | InvocationTargetException e) {
                System.err.println("ERROR Invoking [\nProperty Name: "+propertyName+
                        "\nPropertyValue: "+propertyValue+
                        "\nPropertyClass: "+propertyValueClass+
                        "\nClass Name:"+classObject.getClass().getName()+
                        "\nMethod Name:"+method.getName()+
                        "\nExpected Parameters: "+Arrays.asList(method.getParameterTypes()).toString()+"\n]");
                System.err.println(e.getLocalizedMessage());
                System.exit(0);
            }
        }

    }

    public void getEPOSDataModelPropertiesBlank(EPOSDataModelEntity classObject, List<EPOSDataModelEntity> classes, String subject, Map<String,String> property, String propertyValue){

        System.out.println("MANAGE BLANK");

        System.out.println(property.toString());
        Class<?> propertyValueClass = propertyValue.getClass();
        String propertyName = property.get("property").substring(0, 1).toUpperCase() + property.get("property").substring(1);
        System.out.println("PRE DEBUG: "+propertyValueClass+" "+propertyValue.getClass()+" "+propertyName);

        Method method = null;
        LinkedEntity le = null;
        EPOSDataModelEntity entity = null;

        for(EPOSDataModelEntity eposDataModelEntity : classes){
            if(eposDataModelEntity!=null) System.out.println(eposDataModelEntity.getUid() +" "+ propertyValue);
            if(eposDataModelEntity!=null && eposDataModelEntity.getUid().equals(propertyValue)) {
                entity = eposDataModelEntity;
                break;
            }
        }

        if(property.get("range").toLowerCase().equals("string")){
            propertyValueClass = String.class;
        }else {
            if(entity!=null) entity = getEPOSDataModelClass(property.get("range").toString(), propertyValue);
            try {
                le = new LinkedEntity();
                le.setUid(propertyValue.toString());
                le.setEntityType(EntityNames.valueOf(property.get("range").toString().toUpperCase()).toString());
            }catch(Exception skip){
                System.err.println(skip.getLocalizedMessage());
            }
            if(entity!=null) propertyValueClass = entity.getClass();
            else propertyValueClass = LinkedEntity.class;

            System.out.println("CLASS: "+propertyValueClass);
        }

        if(entity!=null) propertyValueClass = entity.getClass();

        try {
            method = classObject.getClass().getMethod("add" + propertyName, propertyValueClass);
        } catch (NoSuchMethodException e) {
            System.err.println(e.getLocalizedMessage());
        }

        if(method==null) {
            try {
                method = classObject.getClass().getMethod("set" + propertyName, propertyValueClass);
            } catch (NoSuchMethodException e) {
                System.err.println(e.getLocalizedMessage());
            }
        }

        if(method==null){
            System.out.println("CHANGE TO LINKED ENTITY");
            propertyValueClass = LinkedEntity.class;
        }

        if(method==null) {
            try {
                method = classObject.getClass().getMethod("add" + propertyName, propertyValueClass);
            } catch (NoSuchMethodException e) {
                System.err.println(e.getLocalizedMessage());
            }
        }

        if(method==null) {
            try {
                method = classObject.getClass().getMethod("set" + propertyName, propertyValueClass);
            } catch (NoSuchMethodException e) {
                System.err.println(e.getLocalizedMessage());
            }
        }


        if(method!=null){
            try {
                if(propertyValueClass.equals(String.class)) method.invoke(classObject, propertyValue);
                else{
                    if(propertyValueClass!=LinkedEntity.class) method.invoke(classObject, entity);
                    else method.invoke(classObject, le);
                }
            } catch (IllegalArgumentException |IllegalAccessException | InvocationTargetException e) {
                System.err.println("ERROR Invoking [\nProperty Name: "+propertyName+
                        "\nPropertyValue: "+propertyValue+
                        "\nPropertyClass: "+propertyValueClass+
                        "\nClass Name:"+classObject.getClass().getName()+
                        "\nMethod Name:"+method.getName()+
                        "\nExpected Parameters: "+Arrays.asList(method.getParameterTypes()).toString()+"\n]");
                System.err.println(e.getLocalizedMessage());
                System.exit(0);
            }
        }
    }
}
