/* Copyright (c) 2014 OpenPlans - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps.gs;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.namespace.QName;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import net.sf.json.JSONSerializer;

import org.geoserver.catalog.AttributeTypeInfo;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.ResourcePool;
import org.geoserver.catalog.impl.AttributeTypeInfoImpl;
import org.geoserver.catalog.impl.FeatureTypeInfoImpl;
import org.geoserver.catalog.impl.LayerInfoImpl;
import org.geoserver.data.test.MockData;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.wps.WPSTestSupport;
import org.geoserver.wps.gs.UniqueValuesProcess.Results;
import org.geotools.data.DataStoreFinder;
import org.geotools.data.FeatureSource;
import org.geotools.factory.Hints;
import org.geotools.filter.text.cql2.CQLException;
import org.geotools.jdbc.JDBCDataStore;
import org.junit.Test;
import org.mockito.Mockito;

import com.google.common.collect.Ordering;

public class UniqueValuesProcessTest extends WPSTestSupport {

    private static final String FIELD_NAME = "state_name";
    
    private static final int TOTAL_DISTINCT = 4;
    
    public UniqueValuesProcessTest() {
        super();
    }
    
    @Override
    protected void onSetUp(SystemTestData testData) throws Exception {
        // do the default by calling super
        super.onSetUp(testData);
        Catalog catalog = getCatalog();
        testData.addVectorLayer(new QName(MockData.SF_URI, "states",
                MockData.SF_PREFIX), Collections.EMPTY_MAP, "states.properties",
                UniqueValuesProcessTest.class, catalog);
    }
    
    
    @Test
    public void testJDBC() throws CQLException, IOException {
        
        Map<String, Serializable> datastoreParams = new HashMap<String, Serializable>();
        datastoreParams.put("port", 5432);
        datastoreParams.put("schema", "siig_p");
        datastoreParams.put("passwd", "siig_p");
        datastoreParams.put("dbtype", "postgis");
        datastoreParams.put("host", "192.168.1.31");
        datastoreParams.put("Expose primary keys", "true");
        datastoreParams.put("user", "siig_p");
        datastoreParams.put("database", "destination_staging");
        
        
        
        JDBCDataStore dataStore = (JDBCDataStore)DataStoreFinder.getDataStore(datastoreParams);
        FeatureSource featureSource = dataStore.getFeatureSource("siig_gate_geo_gate");
        
        Catalog mockCatalog = Mockito.mock(Catalog.class);
        ResourcePool resourcePool = Mockito.mock(ResourcePool.class);
        LayerInfo layerInfo = new LayerInfoImpl();
        FeatureTypeInfoImpl resourceInfo = new FeatureTypeInfoImpl(mockCatalog);
        List<AttributeTypeInfo> attributes = new ArrayList<AttributeTypeInfo>();
        AttributeTypeInfoImpl attributeInfo = new AttributeTypeInfoImpl();
        attributeInfo.setName("descrizione");
        attributes.add(attributeInfo);
        resourceInfo.setAttributes(attributes);
        layerInfo.setResource(resourceInfo);
        
        Mockito.when(mockCatalog.getLayerByName(Mockito.anyString())).thenReturn(layerInfo);
        Mockito.when(mockCatalog.getResourcePool()).thenReturn(resourcePool);
        Mockito.when(resourcePool.getAttributes(Mockito.any(FeatureTypeInfo.class))).thenReturn(attributes);
        Mockito.when(resourcePool.getFeatureSource(Mockito.any(FeatureTypeInfo.class), Mockito.any(Hints.class))).thenReturn(featureSource);
        
        UniqueValuesProcess process = new UniqueValuesProcess(mockCatalog);
        
        Results result = process.execute("destination:siig_gate_geo_gate", "descrizione", "*v*", 1, 2, "DESC");
        System.out.println("ciao");
    }
    
    @Test
    public void testAll() throws Exception {
        String xml = buildInputXml(FIELD_NAME, null, null, null, null);
        String jsonString = string(post(root(), xml));
        JSONObject json = (JSONObject) JSONSerializer.toJSON(jsonString);
        JSONArray values = json.getJSONArray("values");
        int size = json.getInt("size");
        assertEquals(TOTAL_DISTINCT, size);
        assertEquals(size, values.size());
    }
    
    @Test
    public void testASCPagination1() throws Exception {
            String xml = buildInputXml(FIELD_NAME,null,0,1,"ASC");
            String jsonString = string(post( root(), xml ));
            JSONObject json = (JSONObject) JSONSerializer.toJSON(jsonString);
            JSONArray values = json.getJSONArray("values");
            int size = json.getInt("size");
            assertEquals(TOTAL_DISTINCT,size);
            assertEquals(1,values.size());
            assertEquals("Delaware",values.get(0)); 
    }
    
    @Test
    public void testASCPagination2() throws Exception {
            String xml = buildInputXml(FIELD_NAME,null,1,1,"ASC");
            String jsonString = string(post( root(), xml ));
            JSONObject json = (JSONObject) JSONSerializer.toJSON(jsonString);
            JSONArray values = json.getJSONArray("values");
            int size = json.getInt("size");
            assertEquals(TOTAL_DISTINCT,size);      
            assertEquals("District of Columbia",values.get(0));
    }
    
    @Test
    public void testASCPagination3() throws Exception {
            String xml = buildInputXml(FIELD_NAME,null,2,1,"ASC");
            String jsonString = string(post( root(), xml ));
            JSONObject json = (JSONObject) JSONSerializer.toJSON(jsonString);
            JSONArray values = json.getJSONArray("values");
            int size = json.getInt("size");
            assertEquals(TOTAL_DISTINCT,size);      
            assertEquals("Illinois",values.get(0));
    }
    
    @Test
    public void testDESCPagination1() throws Exception {
            String xml = buildInputXml(FIELD_NAME,null,0,1,"DESC");
            String jsonString = string(post( root(), xml ));
            JSONObject json = (JSONObject) JSONSerializer.toJSON(jsonString);
            JSONArray values = json.getJSONArray("values");
            int size = json.getInt("size");
            assertEquals(TOTAL_DISTINCT,size);      
            assertEquals("West Virginia",values.get(0));    
    }
    
    @Test
    public void testDESCPagination2() throws Exception {
            String xml = buildInputXml(FIELD_NAME,null,1,1,"DESC");
            String jsonString = string(post( root(), xml ));
            JSONObject json = (JSONObject) JSONSerializer.toJSON(jsonString);
            JSONArray values = json.getJSONArray("values");
            int size = json.getInt("size");
            assertEquals(TOTAL_DISTINCT,size);      
            assertEquals("Illinois",values.get(0)); 
    }
    
    @Test
    public void testDESCPagination3() throws Exception {
            String xml = buildInputXml(FIELD_NAME,null,2,1,"DESC");
            String jsonString = string(post( root(), xml ));
            JSONObject json = (JSONObject) JSONSerializer.toJSON(jsonString);
            JSONArray values = json.getJSONArray("values");
            int size = json.getInt("size");
            assertEquals(TOTAL_DISTINCT,size);      
            assertEquals("District of Columbia",values.get(0));     
    }
    
    @Test
    public void testLimits() throws Exception {
            String xml = buildInputXml(FIELD_NAME,null,2,2,null);
            String jsonString = string(post( root(), xml ));
            JSONObject json = (JSONObject) JSONSerializer.toJSON(jsonString);
            JSONArray values = json.getJSONArray("values");
            int size = json.getInt("size");
            assertEquals(TOTAL_DISTINCT,size);
            assertEquals(2,values.size());
    }
    
    
    /*
     * MaxFeature overflow is not an error: return all result from startIndex to end
     */
    @Test
    public void testMaxFeaturesOverflow() throws Exception {
            String xml = buildInputXml(FIELD_NAME,null,2,20,null);
            String jsonString = string(post( root(), xml ));
            JSONObject json = (JSONObject) JSONSerializer.toJSON(jsonString);
            JSONArray values = json.getJSONArray("values");
            int size = json.getInt("size");
            assertEquals(TOTAL_DISTINCT,size);
            assertEquals(TOTAL_DISTINCT-2,values.size());
    }
    
    @Test
    public void testAllParameters() throws Exception {
            String xml = buildInputXml(FIELD_NAME,"*a*",1,2,"DESC");
            String jsonString = string(post( root(), xml ));
            JSONObject json = (JSONObject) JSONSerializer.toJSON(jsonString);
            JSONArray values = json.getJSONArray("values");
            int size = json.getInt("size");
            assertEquals(3,size);
            assertEquals(2,values.size());
            assertEquals(true,Ordering.natural().reverse().isOrdered(values));
            for(int count = 0; count<values.size(); count++){
                    assertEquals(true, ((String)values.get(count)).matches(".*(?i:a)?.*"));
            }
    }
    
    @Test
    public void testFilteredStarts() throws Exception {
            String xml = buildInputXml(FIELD_NAME,"d*",null,null,null);
            String jsonString = string(post( root(), xml ));
            JSONObject json = (JSONObject) JSONSerializer.toJSON(jsonString);
            JSONArray values = json.getJSONArray("values");
            int size = json.getInt("size");
            assertEquals(size,values.size());               
            for(int count = 0; count<values.size(); count++){
                    assertEquals(true, ((String)values.get(count)).matches("^(?i:d).*"));
            }
    }
    
    @Test
    public void testFilteredContains() throws Exception {
            String xml = buildInputXml(FIELD_NAME,"*A*",null,null,null);
            String jsonString = string(post( root(), xml ));
            JSONObject json = (JSONObject) JSONSerializer.toJSON(jsonString);
            JSONArray values = json.getJSONArray("values");
            int size = json.getInt("size");
            assertEquals(size,values.size());               
            for(int count = 0; count<values.size(); count++){
                    assertEquals(true, ((String)values.get(count)).matches(".*(?i:a)?.*"));
            }
    }
    
    @Test
    public void testFilteredEnds() throws Exception {
            String xml = buildInputXml(FIELD_NAME,"*A",null,null,null);
            String jsonString = string(post( root(), xml ));
            JSONObject json = (JSONObject) JSONSerializer.toJSON(jsonString);
            JSONArray values = json.getJSONArray("values");
            int size = json.getInt("size");
            assertEquals(size,values.size());               
            for(int count = 0; count<values.size(); count++){
                    assertEquals(true, ((String)values.get(count)).matches(".*(?i:a)$"));
            }
    }
    
    @Test
    /*
     * StartIndex overflow is an error: return no result
     */
    public void testStartIndexOverflow() throws Exception {
            String xml = buildInputXml(FIELD_NAME,null,6,4,null);
            String jsonString = string(post( root(), xml ));
            JSONObject json = (JSONObject) JSONSerializer.toJSON(jsonString);
            JSONArray values = json.getJSONArray("values");
            int size = json.getInt("size");
            assertEquals(TOTAL_DISTINCT,size);
            assertEquals(0,values.size());
    }
    
    @Test
    public void testAscOrder() throws Exception {
            String xml = buildInputXml(FIELD_NAME,null,null,null,"ASC");
            String jsonString = string(post( root(), xml ));
            JSONObject json = (JSONObject) JSONSerializer.toJSON(jsonString);
            JSONArray values = json.getJSONArray("values");
            int size = json.getInt("size");
            assertEquals(TOTAL_DISTINCT,size);
            assertEquals(true,Ordering.natural().isOrdered(values));
    }
    
    @Test
    public void testDescOrder() throws Exception {
            String xml = buildInputXml(FIELD_NAME,null,null,null,"DESC");
            String jsonString = string(post( root(), xml ));
            JSONObject json = (JSONObject) JSONSerializer.toJSON(jsonString);
            JSONArray values = json.getJSONArray("values");
            int size = json.getInt("size");
            assertEquals(TOTAL_DISTINCT,size);
            assertEquals(true,Ordering.natural().reverse().isOrdered(values));
    }
    
    private String buildInputXml(String fieldName, String fieldFilter,
            Integer startIndex, Integer maxFeatures, String sort) {
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                + "<wps:Execute version=\"1.0.0\" service=\"WPS\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns=\"http://www.opengis.net/wps/1.0.0\" xmlns:wfs=\"http://www.opengis.net/wfs\" xmlns:wps=\"http://www.opengis.net/wps/1.0.0\" xmlns:ows=\"http://www.opengis.net/ows/1.1\" xmlns:gml=\"http://www.opengis.net/gml\" xmlns:ogc=\"http://www.opengis.net/ogc\" xmlns:wcs=\"http://www.opengis.net/wcs/1.1.1\" xmlns:xlink=\"http://www.w3.org/1999/xlink\" xsi:schemaLocation=\"http://www.opengis.net/wps/1.0.0 http://schemas.opengis.net/wps/1.0.0/wpsAll.xsd\">\n"
                + "  <ows:Identifier>gs:UniqueValues</ows:Identifier>\n"
                + "  <wps:DataInputs>\n" + "    <wps:Input>\n"
                + "      <ows:Identifier>layerName</ows:Identifier>\n"
                + "      <wps:Data>\n"
                + "        <wps:LiteralData>states</wps:LiteralData>\n"
                + "      </wps:Data>\n" + "    </wps:Input>\n";
        if (fieldName != null) {
            xml = xml + "    <wps:Input>\n"
                    + "      <ows:Identifier>fieldName</ows:Identifier>\n"
                    + "      <wps:Data>\n" + "        <wps:LiteralData>"
                    + fieldName + "</wps:LiteralData>\n" + "      </wps:Data>\n"
                    + "    </wps:Input>\n";
        }
        if (fieldFilter != null) {
            xml = xml + "    <wps:Input>\n"
                    + "      <ows:Identifier>fieldFilter</ows:Identifier>\n"
                    + "      <wps:Data>\n" + "        <wps:LiteralData>"
                    + fieldFilter + "</wps:LiteralData>\n" + "      </wps:Data>\n"
                    + "    </wps:Input>\n";
        }
        if (startIndex != null) {
            xml = xml + "    <wps:Input>\n"
                    + "      <ows:Identifier>startIndex</ows:Identifier>\n"
                    + "      <wps:Data>\n" + "        <wps:LiteralData>"
                    + startIndex + "</wps:LiteralData>\n" + "      </wps:Data>\n"
                    + "    </wps:Input>\n";
        }
        if (maxFeatures != null) {
            xml = xml + "    <wps:Input>\n"
                    + "      <ows:Identifier>maxFeatures</ows:Identifier>\n"
                    + "      <wps:Data>\n" + "        <wps:LiteralData>"
                    + maxFeatures + "</wps:LiteralData>\n" + "      </wps:Data>\n"
                    + "    </wps:Input>\n";
        }
        ;
        if (sort != null) {
            xml = xml + "    <wps:Input>\n"
                    + "      <ows:Identifier>sort</ows:Identifier>\n"
                    + "      <wps:Data>\n" + "        <wps:LiteralData>" + sort
                    + "</wps:LiteralData>\n" + "      </wps:Data>\n"
                    + "    </wps:Input>\n";
        }
        xml = xml + "  </wps:DataInputs>\n" + "  <wps:ResponseForm>\n"
                + "    <wps:RawDataOutput mimeType=\"application/json\">\n"
                + "      <ows:Identifier>result</ows:Identifier>\n"
                + "    </wps:RawDataOutput>\n" + "  </wps:ResponseForm>\n"
                + "</wps:Execute>";
        return xml;
    }

}
