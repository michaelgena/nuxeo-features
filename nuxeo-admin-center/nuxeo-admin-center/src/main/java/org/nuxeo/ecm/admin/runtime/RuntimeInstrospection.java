/*
 * (C) Copyright 2006-2014 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Nuxeo - initial API and implementation
 *     tdelprat, jcarsique
 */

package org.nuxeo.ecm.admin.runtime;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.PropertyResourceBundle;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.connect.update.Version;
import org.nuxeo.osgi.BundleFile;
import org.nuxeo.osgi.BundleImpl;
import org.nuxeo.osgi.JarBundleFile;
import org.nuxeo.runtime.RuntimeService;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.model.RegistrationInfo;
import org.osgi.framework.Bundle;

/**
 * Extracts information about the Bundles currently deployed in Nuxeo Runtime
 *
 * @author tiry
 */
public class RuntimeInstrospection {

    protected static final Log log = LogFactory.getLog(RuntimeInstrospection.class);

    protected static SimplifiedServerInfo info;

    protected static List<String> bundleIds;

    public static synchronized SimplifiedServerInfo getInfo() {
        if (info == null) {
            RuntimeService runtime = Framework.getRuntime();
            Collection<RegistrationInfo> registrations = runtime.getComponentManager().getRegistrations();
            bundleIds = new ArrayList<>();
            List<SimplifiedBundleInfo> bundles = new ArrayList<>();
            for (RegistrationInfo ri : registrations) {
                Bundle bundle = ri.getContext().getBundle();
                if (bundle != null
                        && !bundleIds.contains(bundle.getSymbolicName())) {
                    SimplifiedBundleInfo bi = getBundleSimplifiedInfo(bundle);
                    bundleIds.add(bundle.getSymbolicName());
                    if (bi != null) {
                        bundles.add(bi);
                    }
                }
            }
            Collections.sort(bundles);
            info = new SimplifiedServerInfo();
            info.setBundleInfos(bundles);
            info.setRuntimeVersion(runtime.getVersion().toString());
            info.setWarnings(runtime.getWarnings());
        }
        return info;
    }

    public static List<String> getBundleIds() {
        if (bundleIds == null) {
            getInfo();
        }
        return bundleIds;
    }

    protected static SimplifiedBundleInfo getBundleSimplifiedInfo(Bundle bundle) {
        if (!(bundle instanceof BundleImpl)) {
            return null;
        }
        BundleImpl nxBundle = (BundleImpl) bundle;
        BundleFile file = nxBundle.getBundleFile();
        File jarFile = null;
        if (file instanceof JarBundleFile) {
            JarBundleFile jar = (JarBundleFile) file;
            jarFile = jar.getFile();
        }
        if (jarFile == null || jarFile.isDirectory()) {
            return null;
        }
        SimplifiedBundleInfo result = null;
        try (ZipFile zFile = new ZipFile(jarFile)) {
            // Look for a pom.properties to extract its Maven version
            Enumeration<ZipEntry> entries = (Enumeration<ZipEntry>) zFile.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                if (entry.getName().endsWith("pom.properties")) {
                    try (InputStream pomStream = zFile.getInputStream(entry)) {
                        PropertyResourceBundle prb = new PropertyResourceBundle(
                                pomStream);
                        String version = prb.getString("version");
                        result = new SimplifiedBundleInfo(
                                bundle.getSymbolicName(), version);
                    }
                    break;
                }
            }
        } catch (IOException e) {
            log.debug(e.getMessage(), e);
        }
        if (result == null) {
            // Fall back on the filename to extract a version
            try {
                Version version = new Version(jarFile.getName());
                result = new SimplifiedBundleInfo(bundle.getSymbolicName(),
                        version.toString());
            } catch (NumberFormatException e) {
                log.debug(e.getMessage());
            }
        }
        if (result == null) {
            // Fall back on the MANIFEST Bundle-Version
            try {
                org.osgi.framework.Version version = bundle.getVersion();
                result = new SimplifiedBundleInfo(bundle.getSymbolicName(),
                        version.toString());
            } catch (RuntimeException e) {
                log.debug(e.getMessage());
                result = new SimplifiedBundleInfo(bundle.getSymbolicName(),
                        "unknown");
            }
        }
        return result;
    }
}
