/*
 * (C) Copyright 2006-2012 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Nuxeo - initial API and implementation
 *
 * $Id$
 */

package org.nuxeo.connect.packages;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.connect.NuxeoConnectClient;
import org.nuxeo.connect.connector.ConnectServerError;
import org.nuxeo.connect.data.DownloadablePackage;
import org.nuxeo.connect.registration.ConnectRegistrationService;
import org.nuxeo.connect.update.Package;
import org.nuxeo.connect.update.PackageType;

/**
 * Implements {@link PackageSource} for remote {@link Package} hosted on Nuxeo
 * Connect Server.
 * 
 * @author <a href="mailto:td@nuxeo.com">Thierry Delprat</a>
 */
public class RemotePackageSource implements PackageSource {

    protected static final Log log = LogFactory.getLog(RemotePackageSource.class);

    public static final String DEFAULT_REMOTE_SOURCE_ID = "registred";

    protected String id;

    protected PackageListCache cache;

    public String getName() {
        return "Connect Server";
    }

    public String getId() {
        return id;
    }

    public RemotePackageSource() {
        this(DEFAULT_REMOTE_SOURCE_ID);
    }

    public RemotePackageSource(String id) {
        cache = new PackageListCache();
        this.id = id;
    }

    public List<DownloadablePackage> listPackages() {
        List<DownloadablePackage> all = new ArrayList<DownloadablePackage>();
        for (PackageType type : PackageType.values()) {
            all.addAll(listPackages(type));
        }
        return all;
    }

    public List<DownloadablePackage> listPackages(PackageType type) {
        List<DownloadablePackage> result = new ArrayList<DownloadablePackage>();
        ConnectRegistrationService crs = NuxeoConnectClient.getConnectRegistrationService();

        if (crs.isInstanceRegistred()) {
            try {
                List<DownloadablePackage> pkgs = cache.getFromCache(type.toString());
                if (pkgs == null) {
                    pkgs = crs.getConnector().getDownloads(id, type);
                    cache.add(pkgs, type.toString());
                }
                for (DownloadablePackage pkg : pkgs) {
                    result.add(pkg);
                }
            } catch (ConnectServerError e) {
                log.error("Unable to fetch remote packages list", e);
                // store an empty list to avoid calling back the server
                // since anyway we probably have no connection ...
                cache.add(new ArrayList<DownloadablePackage>(), type.toString());
            }
        }
        return result;
    }

    public void flushCache() {
        // memory cache
        cache = new PackageListCache();
        // disk cache
        ConnectRegistrationService crs = NuxeoConnectClient.getConnectRegistrationService();
        crs.getConnector().flushCache();
    }

}
