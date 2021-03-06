/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.syncope.console.wicket.markup.html.form.preview;

import org.apache.syncope.console.preview.BinaryPreview;
import java.io.ByteArrayInputStream;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import javax.naming.ldap.LdapName;
import javax.naming.ldap.Rdn;
import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.Component;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.Model;
import org.apache.wicket.util.io.IOUtils;

@BinaryPreview(mimeTypes = { "application/x-x509-ca-cert", "application/x-x509-user-cert", "application/pkix-cert" })
public class BinaryCertPreviewer extends AbstractBinaryPreviewer {

    private static final long serialVersionUID = -5843835939538055110L;

    public BinaryCertPreviewer(final String id, final String mimeType, final byte[] uploadedBytes) {
        super(id, mimeType, uploadedBytes);
    }

    @Override
    public Component preview() {
        final Label commonNameLabel = new Label("certCommonName", new Model<String>());
        final ByteArrayInputStream certificateStream = new ByteArrayInputStream(uploadedBytes);
        try {
            final X509Certificate certificate = (X509Certificate) CertificateFactory.getInstance("X.509").
                    generateCertificate(certificateStream);

            final StringBuilder commonNameBuilder = new StringBuilder("cn=");

            final LdapName ldapName = new LdapName(certificate.getIssuerDN().getName());

            for (Rdn rdn : ldapName.getRdns()) {
                if ("CN".equalsIgnoreCase(rdn.getType())) {
                    commonNameBuilder.append(rdn.getValue() == null
                            ? StringUtils.EMPTY
                            : rdn.getValue().toString());
                }
            }
            commonNameLabel.setDefaultModelObject(commonNameBuilder.toString());
        } catch (Exception e) {
            LOG.error("Error evaluating certificate file", e);
            throw new IllegalArgumentException("Error evaluating certificate file", e);
        } finally {
            IOUtils.closeQuietly(certificateStream);
        }
        return this.add(commonNameLabel);
    }
}
