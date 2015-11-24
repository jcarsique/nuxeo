/*
 * (C) Copyright 2011 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 * Contributors:
 *     Thomas Roger <troger@nuxeo.com>
 */

package org.nuxeo.opensocial.shindig.crypto;

import static org.junit.Assert.assertEquals;
import static org.nuxeo.opensocial.shindig.crypto.OpenSocialDescriptor.NUXEO_BIND_ADDRESS_PROPERTY;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

/**
 * @author <a href="mailto:troger@nuxeo.com">Thomas Roger</a>
 */
@RunWith(FeaturesRunner.class)
@Features(CoreFeature.class)
public class TestOpenSocialDescriptor {

    @Test
    public void trustedHostShouldBeLocalhostWhenListeningOnAllIPs() {
        // IPv5
        Framework.getProperties().put(NUXEO_BIND_ADDRESS_PROPERTY, "0.0.0.0");
        OpenSocialDescriptor openSocialDescriptor = new OpenSocialDescriptor();
        String trustedHost = openSocialDescriptor.getTrustedHostForNuxeoBindAddress();
        assertEquals("localhost", trustedHost);

        // IPv6
        Framework.getProperties().put(NUXEO_BIND_ADDRESS_PROPERTY, "000:0000::0:0000:00");
        trustedHost = openSocialDescriptor.getTrustedHostForNuxeoBindAddress();
        assertEquals("localhost", trustedHost);
    }

    @Test
    public void trustedHostShouldBeEqualsToNuxeoBindAddress() {
        Framework.getProperties().put(NUXEO_BIND_ADDRESS_PROPERTY, "127.0.0.45");
        OpenSocialDescriptor openSocialDescriptor = new OpenSocialDescriptor();
        String trustedHost = openSocialDescriptor.getTrustedHostForNuxeoBindAddress();
        assertEquals("127.0.0.45", trustedHost);
    }

    @Test
    public void nuxeoBindAddressShouldBeAddedTotheConfiguredListOfTrustedHosts() {
        Framework.getProperties().put(NUXEO_BIND_ADDRESS_PROPERTY, "127.0.0.45");
        OpenSocialDescriptor openSocialDescriptor = new OpenSocialDescriptor();
        String trustedHost = openSocialDescriptor.getTrustedHostForNuxeoBindAddress();
        assertEquals("127.0.0.45", trustedHost);

        openSocialDescriptor.setTrustedHosts("host1,host2,10.0.40.40");
        String[] trustedHosts = openSocialDescriptor.getTrustedHosts();
        assertEquals(4, trustedHosts.length);
        assertEquals("127.0.0.45", trustedHosts[0]);
        assertEquals("host1", trustedHosts[1]);
        assertEquals("host2", trustedHosts[2]);
        assertEquals("10.0.40.40", trustedHosts[3]);
    }

}
