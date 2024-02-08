package org.epos.edmmapping.custommapper;

import org.epos.eposdatamodel.LinkedEntity;
import org.epos.eposdatamodel.Organization;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class CustomMapperPropertyOrganizationOwns extends CustomMapperPropertyAbstract {
	private static final String targetRdfPropertyName = "schema:owns";
	private static final String targetClassName = "Organization";
	private static final Class<?> targetValueClass = String.class;
	private static final Method method;
	static {
		Method setUid = null;
		try {
			setUid = Organization.class.getMethod("addOwnsItem", LinkedEntity.class);
		} catch (NoSuchMethodException ignored) {}
		method = setUid;
	}

	public CustomMapperPropertyOrganizationOwns() {
		super(targetRdfPropertyName, targetValueClass, targetClassName, method);
	}


	@Override
	public void toImplementMap(Object object, Object propertyValue) throws InvocationTargetException, IllegalAccessException {
		LinkedEntity le = new LinkedEntity();
		le.setUid(propertyValue.toString());
		le.setEntityType("equipment");
		method.invoke(object, le);
	}


}
