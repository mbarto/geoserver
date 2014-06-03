package org.geoserver.wps.gs;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.geoserver.catalog.AttributeTypeInfo;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.ResourceInfo;
import org.geotools.data.Query;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.factory.GeoTools;
import org.geotools.feature.visitor.UniqueVisitor;
import org.geotools.filter.text.cql2.CQLException;
import org.geotools.process.factory.DescribeParameter;
import org.geotools.process.factory.DescribeProcess;
import org.geotools.process.factory.DescribeResult;
import org.geotools.util.logging.Logging;
import org.opengis.filter.Filter;
import org.opengis.filter.FilterFactory;
import org.opengis.filter.sort.SortBy;
import org.opengis.filter.sort.SortOrder;

/**
 * A WPS process to retrieve unique field values from a layer on Geoserver
 * catalog. Requires a valid layer name and a field name to extract the unique
 * values. It accepts sorting and paging parameters.
 * 
 * @author Cesar Martinez Izquierdo
 * @author Sandro Salari
 */
@DescribeProcess(title = "UniqueValues", description = "Gets the list of unique values that match the text filter for the given layer and field")
public class UniqueValuesProcess implements GeoServerProcess {

    /** The LOGGER. */
    private static final Logger LOGGER = Logging
            .getLogger(UniqueValuesProcess.class);
    
    private final FilterFactory FF = CommonFactoryFinder.getFilterFactory2();
    
    private final Catalog catalog;
    
    public UniqueValuesProcess(Catalog catalog) {
        this.catalog = catalog;
    }
    
    public static final class Results {
    Map<String, Object> data = new HashMap<String, Object>();
    
    public Map<String, Object> getData() {
        return data;
    }
    
    public void setData(Map<String, Object> data) {
        this.data = data;
    }
    
    }
    
    @DescribeResult(name = "result", type = Results.class, description = "List of values")
    public Results execute(
            @DescribeParameter(name = "layerName", min = 1, max = 1, description = "Layer from which field values should be retrieved") String layerName,
            @DescribeParameter(name = "fieldName", min = 1, max = 1, description = "Field from which the values should be retrieved") String fieldName,
            @DescribeParameter(name = "fieldFilter", min = 0, max = 1, description = "Filter to apply to field values ('*','?' wildcard is allowed; escape with '\\', case sensitive)") String fieldFilter,
            @DescribeParameter(name = "startIndex", min = 0, max = 1, description = "The index of the first feature to retrieve") Integer startIndex,
            @DescribeParameter(name = "maxFeatures", min = 0, max = 1, description = "The maximum numbers of features to fetch") Integer maxFeatures,
            @DescribeParameter(name = "sort", min = 0, max = 1, description = "The sort order (ASC, DESC or NONE)") String sort)
            throws IOException, CQLException {
    
        // initial checks on mandatory params
        if (layerName == null || layerName.length() <= 0) {
            throw new IllegalArgumentException("Empty or null layerName provided!");
        }
        if (fieldName == null || fieldName.length() <= 0) {
            throw new IllegalArgumentException("Empty or null fieldName provided!");
        }
        LOGGER.fine("Download process called on resource: " + layerName
                + " - field: " + fieldName);
    
        //
        // Move on with the real code
        //
        // checking for the resources on the GeoServer
        LayerInfo layerInfo = catalog.getLayerByName(layerName);
        if (layerInfo == null) {
            // could not find any layer ... abruptly interrupt the process
            throw new IllegalArgumentException("Unable to locate layer: "
                    + layerName);
    
        }
        ResourceInfo resourceInfo = layerInfo.getResource();
        if (resourceInfo == null) {
            // could not find any data store associated to the specified layer ...
            // abruptly interrupt the process
            throw new IllegalArgumentException(
                    "Unable to locate ResourceInfo for layer:" + layerName);
    
        }
        LOGGER.log(Level.FINE,
                "The resource to work on is " + resourceInfo.getName());
    
        // CORE CODE
    
        if (resourceInfo instanceof FeatureTypeInfo) {
            LOGGER.log(Level.FINE, "The resource to work on is a vector layer");
            // get the feature collection
            FeatureTypeInfo featureType = (FeatureTypeInfo) resourceInfo;
            List<AttributeTypeInfo> attributes = featureType.attributes();
            checkField(fieldName, attributes);
            SimpleFeatureSource featureSource = (SimpleFeatureSource) featureType
                    .getFeatureSource(null, GeoTools.getDefaultHints());
            UniqueVisitor visitor = new UniqueVisitor(FF.property(fieldName));
    
            // Get result with pagination filters
            Integer listSize = 0;
            List<String> list = new ArrayList<String>();
    
            try {
                // Count without pagination filters
                // Reset visitor
                visitor.reset();
                Query queryCount = queryCountBuilder(featureType.getName(),
                        fieldName, fieldFilter);
                SimpleFeatureCollection featuresCount = featureSource
                        .getFeatures(queryCount);
                featuresCount.accepts(visitor, null);
                listSize = visitor.getResult().toList().size();
    
                // Reset visitor
                visitor.reset();
                if(startIndex != null) {
                    visitor.setStartIndex(startIndex);
                }
                if(maxFeatures != null) {
                    visitor.setMaxFeatures(maxFeatures);
                }
                // If source supports unique visitor, delegate to it
                Query query = queryBuilder(featureType.getName(), fieldName,
                        fieldFilter, null, null, sort, listSize);
                SimpleFeatureCollection features = featureSource.getFeatures(query);
                features.accepts(visitor, null);
                list = visitor.getResult().toList();
                // TODO : if source not supports unique visitor (like SHP), do
                // offset after unique visitor and other filters
    
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, e.getMessage(), e);
                
            }
            if(list == null) {
                list = new ArrayList<String>(0);
            }
            Results result = new Results();
            result.getData().put("layerName", layerName);
            result.getData().put("fieldName", fieldName);
            result.getData().put("size", listSize);
            result.getData().put("values", list);
    
            return result;
        } else {
            // wrong type
            throw new IllegalArgumentException(
                    "Could not complete the process, requested layer is not vector layer");
    
        }
    }
    
    private void checkField(String fieldName, List<AttributeTypeInfo> attributes) {
        for (AttributeTypeInfo field : attributes) {
            if (fieldName.equals(field.getName())) {
                return;
            }
        }
        throw new IllegalArgumentException(
                "Could not complete the Process, requested field does not exist");
    }
    
    private Filter buildFilterCondition(String fieldName, String fieldFilter)
            throws CQLException {
        // Filter filter = FF.equals(FF.property(fieldName), FF.literal( fieldFilter
        // ));
        Filter filter = Filter.INCLUDE;
        // wildcards filter condition
        if (fieldFilter != null && !fieldFilter.isEmpty()) {
            filter = FF.like(FF.property(fieldName), fieldFilter, "*", "?", "\\",
                    false);
        }
        return filter;
    }
    
    private Query queryCountBuilder(String typeName, String fieldName,
            String fieldFilter) throws CQLException {
    
        Query query = new Query(typeName, buildFilterCondition(fieldName,
                fieldFilter), new String[] { fieldName });
    
        return query;
    
    }
    
    private Query queryBuilder(String typeName, String fieldName,
            String fieldFilter, Integer startIndex, Integer maxFeatures,
            String sort, Integer listSize) throws CQLException, StartIndexOverflow {
    
        Query query = new Query(typeName, buildFilterCondition(fieldName,
                fieldFilter), new String[] { fieldName });
    
        if (sort != null) {
            if (sort.equals("ASC")) {
                query.setSortBy(new SortBy[] { FF.sort(fieldName,
                        SortOrder.ASCENDING) });
            }
            if (sort.equals("DESC")) {
                query.setSortBy(new SortBy[] { FF.sort(fieldName,
                        SortOrder.DESCENDING) });
            }
        }
        if (startIndex != null) {
            if (startIndex >= 0 && startIndex < listSize) {
                query.setStartIndex(startIndex);
            } else {
                throw new StartIndexOverflow();
            }
        }
        if (maxFeatures != null && maxFeatures > 0
                && maxFeatures < (listSize - startIndex)) {
            query.setMaxFeatures(maxFeatures);
        }
    
        return query;
    
    }
    
    private class StartIndexOverflow extends Exception {

}

}