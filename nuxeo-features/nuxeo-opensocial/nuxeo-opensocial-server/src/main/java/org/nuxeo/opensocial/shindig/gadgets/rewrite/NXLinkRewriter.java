/*
 * (C) Copyright 2010 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 */

package org.nuxeo.opensocial.shindig.gadgets.rewrite;

import org.apache.shindig.common.uri.Uri;
import org.apache.shindig.gadgets.rewrite.ContentRewriterFeature;
import org.apache.shindig.gadgets.rewrite.ContentRewriterUris;
import org.apache.shindig.gadgets.rewrite.ProxyingLinkRewriter;
import org.nuxeo.ecm.platform.web.common.vh.VirtualHostHelper;

/**
 * @author <a href="mailto:troger@nuxeo.com">Thomas Roger</a>
 */
public class NXLinkRewriter extends ProxyingLinkRewriter {

    public NXLinkRewriter(ContentRewriterUris rewriterUris, Uri gadgetUri,
            ContentRewriterFeature rewriterFeature, String container,
            boolean debug, boolean ignoreCache) {
        super(rewriterUris, gadgetUri, rewriterFeature, container, debug,
                ignoreCache);
    }

    @Override
    public String rewrite(String link, Uri context) {
        return !link.startsWith(VirtualHostHelper.getContextPathProperty()) ? super.rewrite(
                link, context) : link;
    }

}
