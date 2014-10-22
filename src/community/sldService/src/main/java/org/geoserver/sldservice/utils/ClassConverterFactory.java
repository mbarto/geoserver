package org.geoserver.sldservice.utils;

import org.geotools.factory.Hints;
import org.geotools.util.Converter;
import org.geotools.util.ConverterFactory;

public class ClassConverterFactory implements ConverterFactory {

	
	public Converter createConverter(Class source, Class target, Hints hints) {
		if ( target.equals( Class.class ) && source.equals(String.class)) {
			
			
			return new Converter() {

				public Object convert(Object source, Class target) throws Exception {
					try {
						return Class.forName(source.toString());
					} catch(Exception e) {
						return null;
					}
				}
				
			};
			
		}
		return null;
	}
    
		


}
