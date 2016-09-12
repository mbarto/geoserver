package org.geoserver.portanalysis;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.geoserver.config.GeoServerDataDirectory;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.platform.resource.Resource;
import org.h2.util.IOUtils;

public class PortAnalysisServlet extends HttpServlet {

    private String path = "portanalysis";

    GeoServerDataDirectory dd;

    @Override
    public void init(ServletConfig config) throws ServletException {
        if (StringUtils.isNotEmpty(config.getInitParameter("path"))) {
            path = config.getInitParameter("path");
        }
        super.init(config);
        dd = GeoServerExtensions.bean(GeoServerDataDirectory.class);
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        if (req.getPathInfo().equals("")) {
            resp.sendRedirect(path + "/");
        } else {
            File indexFileFromDD = dd.findFile("www/portanalysis/index.html");
            String index = "";
            if (indexFileFromDD != null) {
                index = FileUtils.readFileToString(indexFileFromDD);
            } else {
                InputStream indexFile = this.getClass()
                        .getResourceAsStream("/portanalysis/index.html");
                index = IOUtils.readStringAndClose(IOUtils.getReader(indexFile), 0);
            }
            
            resp.getWriter().write(index.replace("##basepath##", req.getContextPath() + "/" + path + "/"));
        }
    }

}
