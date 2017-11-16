/* (c) 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps.gs;

import java.io.InputStream;
import java.io.OutputStream;

import org.geoserver.wps.ppio.CDataPPIO;
import org.geoserver.wps.ppio.ComplexPPIO;
import org.geoserver.wps.ppio.GeoJSONPPIO;
import org.geoserver.wps.ppio.ProcessParameterIO;
import org.geoserver.wps.ppio.WFSPPIO;
import org.geotools.feature.DefaultFeatureCollection;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.process.vector.AggregateProcess;
import org.geotools.process.vector.AggregateProcess.AggregationFunction;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import com.vividsolutions.jts.geom.Geometry;

/**
 * 
 */
public abstract class AggregateProcessFeatureCollectionPPIO extends CDataPPIO {

    
    
    protected AggregateProcessFeatureCollectionPPIO(String mimeType) {
        super(AggregateProcess.Results.class, AggregateProcess.Results.class, mimeType);
    }

    @Override
    public void encode(Object value, OutputStream output) throws Exception {
        AggregateProcess.Results processResult = (AggregateProcess.Results) value;
        SimpleFeatureTypeBuilder typeBuilder = new SimpleFeatureTypeBuilder();
        typeBuilder.setName("aggregate");
        Object[] sample = processResult.getGroupByResult().get(0);
        int index = 0;
        for (String attrName : processResult.getGroupByAttributes()) {
            Class<?> binding = sample[index].getClass();
            if(Geometry.class.isAssignableFrom(binding)) {
                Geometry geom = (Geometry)sample[index];
                CoordinateReferenceSystem crs = (CoordinateReferenceSystem)geom.getUserData();
                typeBuilder.add(attrName, binding, crs);
            } else {
                typeBuilder.add(attrName, binding);
            }
            index++;
        }
        for (AggregationFunction func : processResult.getFunctions()) {
            Class<?> binding = sample[index].getClass();
            typeBuilder.add(func.name(), binding);
            index++;
        }
        SimpleFeatureBuilder builder = new SimpleFeatureBuilder(typeBuilder.buildFeatureType());
        DefaultFeatureCollection result = new DefaultFeatureCollection();
        index = 0;
        for(Object[] feature : processResult.getGroupByResult()) {
            builder.reset();
            for(Object attribute : feature) {
                builder.add(attribute);
            }
            result.add(builder.buildFeature("aggregate-" + index));
            index++;
        }
        ComplexPPIO delegate = getDelegatePPIO();
        delegate.encode(result, output);
    }

    
    protected abstract ComplexPPIO getDelegatePPIO();

    @Override
    public Object decode(InputStream input) throws Exception {
        throw new UnsupportedOperationException("JSON parsing is not supported");
    }

    @Override
    public Object decode(String input) throws Exception {
        throw new UnsupportedOperationException("JSON parsing is not supported");
    }

    @Override
    public final String getFileExtension() {
        return "xml";
    }
    
    public static class WFS10 extends AggregateProcessFeatureCollectionPPIO {
        
        public WFS10() {
            super("text/xml; subtype=wfs-collection/1.0");
        }

        @Override
        protected ComplexPPIO getDelegatePPIO() {
            // TODO Auto-generated method stub
            return new WFSPPIO.WFS10();
        }
        
    }
    
    public static class GeoJSON extends AggregateProcessFeatureCollectionPPIO {
        
        public GeoJSON() {
            super("application/json; subtype=GeoJSON");
        }
        
        @Override
        protected ComplexPPIO getDelegatePPIO() {
            // TODO Auto-generated method stub
            return new GeoJSONPPIO.FeatureCollections();
        }
        
    }
}
