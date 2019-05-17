/* (c) 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.saml;

import org.geoserver.security.config.PreAuthenticatedUserNameFilterConfig;

/**
 * Configuration for SAML SSO authentication
 *
 * @author Xandros
 */
public class SAMLAuthenticationFilterConfig extends PreAuthenticatedUserNameFilterConfig {

    private static final long serialVersionUID = 1199751476823173800L;

    private String entityId;

    private String metadataURL;

    private String metadata;

    private Boolean wantAssertionSigned = false;

    public String getEntityId() {
        return entityId;
    }

    public void setEntityId(String entityId) {
        this.entityId = entityId;
    }

    public String getMetadataURL() {
        return metadataURL;
    }

    public void setMetadataURL(String metadataURL) {
        this.metadataURL = metadataURL;
    }

    public String getMetadata() {
        return metadata;
    }

    public void setMetadata(String metadata) {
        this.metadata = metadata;
    }

    public Boolean getWantAssertionSigned() {
        return wantAssertionSigned;
    }

    public void setWantAssertionSigned(Boolean wantAssertionSigned) {
        this.wantAssertionSigned = wantAssertionSigned;
    }
}
