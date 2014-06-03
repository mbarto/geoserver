/* Copyright (c) 2014 OpenPlans - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps.gs;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;

import org.geoserver.wps.ppio.CDataPPIO;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

/**
 * A PPIO to generate good looking JSON for the UniqueValues process results
 * 
 * @author Sandro Salari - GeoSolutions
 */
public class UniqueValuesProcessPPIO extends CDataPPIO {

    static final private JSONParser parser = new JSONParser();
    
    protected UniqueValuesProcessPPIO() {
        super(UniqueValuesProcess.Results.class, UniqueValuesProcess.Results.class,
                "application/json");
    
    }
    
    @Override
    public Object decode(String input) throws Exception {
        return parser.parse(input);
    }
    
    @Override
    public Object decode(InputStream input) throws Exception {
        Reader reader = new InputStreamReader(input);
        return parser.parse(reader);
    }
    
    @Override
    public void encode(Object value, OutputStream os) throws Exception {
        UniqueValuesProcess.Results result = (UniqueValuesProcess.Results) value;
        JSONObject obj = new JSONObject();
        obj.putAll(result.getData());
        Writer writer = new OutputStreamWriter(os);
        obj.writeJSONString(writer);
        writer.flush();
    }
    
    @Override
    public String getFileExtension() {
        return "json";
    }

}