package com.qmetry.qaf.automation.step;

import static com.qmetry.qaf.automation.core.ConfigurationManager.getBundle;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.ArrayList;

import javax.inject.Inject;

import com.qmetry.qaf.automation.ui.api.TestPage;
import com.qmetry.qaf.automation.ui.webdriver.QAFWebElement;

/**
 * Default {@link ObjectFactory} used by step factory to create object of step
 * provider class. To share instance between step and scenario set
 * `step.provider.sharedinstance` property to `true`
 * 
 * @author chirag.jayswal
 *
 */
public class DefaultObjectFactory implements ObjectFactory {
	public static final String OBJ_STORE_PREFIX = "shared.object.store";

	@SuppressWarnings("unchecked")
	@Override
	public <T> T getObject(Class<T> cls) throws Exception {
		if (getBundle().getBoolean("step.provider.sharedinstance", false) && isSharableInstance(cls)) {
			// allow class variable sharing among steps
			String key = OBJ_STORE_PREFIX + "." + cls.getName();
			Object obj = getBundle().getObject(key);
			if (null == obj) {
				obj = createInstance(cls);
				inject(obj);
				getBundle().setProperty(key, obj);
			}
			return (T) obj;
		}
		Object obj = createInstance(cls);
		inject(obj);
		return (T) obj;
	}

	private void inject(Object obj) {
		try {
			// new ElementFactory().initFields(obj);
			Field[] flds = obj.getClass().getDeclaredFields();
			for (Field fld : flds) {
				if (fld.isAnnotationPresent(Inject.class)) {
					fld.setAccessible(true);
					Object value = getObject(fld.getType());
					fld.set(obj, value);
				}
			}
		} catch (Exception e) {

		}
	}

	private Object createInstance(Class<?> cls) throws Exception {
		try {
			return cls.newInstance();
		} catch (Exception e) {
			// only public constructors with or without parameter(s) to be
			// considered!...
			Constructor<?> con = cls.getConstructors()[0];
			con.setAccessible(true);
			ArrayList<Object> args = new ArrayList<Object>();
			for (Class<?> param : con.getParameterTypes()) {
				args.add(getObject(param));
			}
			return con.newInstance(args.toArray(new Object[args.size()]));

		}
	}

	private boolean isSharableInstance(Class<?> cls) {

		if (TestPage.class.isAssignableFrom(cls) || QAFWebElement.class.isAssignableFrom(cls)) {
			return false;
		}
		return true;
	}
}
