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

package org.nuxeo.opensocial.shindig.crypto;

import java.io.FileReader;

import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.shindig.auth.BlobCrypterSecurityTokenDecoder;
import org.apache.shindig.common.crypto.BasicBlobCrypter;
import org.apache.shindig.common.crypto.BlobCrypter;
import org.apache.shindig.config.ContainerConfig;
import org.apache.shindig.gadgets.oauth.BasicOAuthStore;
import org.apache.shindig.gadgets.oauth.OAuthStore;
import org.nuxeo.ecm.platform.oauth.providers.OAuthServiceProviderRegistry;
import org.nuxeo.opensocial.service.api.OpenSocialService;
import org.nuxeo.runtime.api.Framework;

import com.google.inject.Inject;

public class NXBlobCrypterSecurityTokenDecoder extends
        BlobCrypterSecurityTokenDecoder {

    private static final Log log = LogFactory.getLog(NXBlobCrypterSecurityTokenDecoder.class);

    @Inject
    public NXBlobCrypterSecurityTokenDecoder(ContainerConfig config,
            OAuthStore store) {
        super(config);
        try {
            OpenSocialService os = Framework.getService(OpenSocialService.class);
            for (String container : config.getContainers()) {
                String key = IOUtils.toString(new FileReader(
                        os.getSigningStateKeyFile()));
                if (key != null) {
                    BlobCrypter crypter = new BasicBlobCrypter(key.getBytes());
                    crypters.put(container, crypter);
                } else {
                    log.error("Should not be able to run any opensocial instance "
                            + "without a signing state key!");
                }

                /*
                 * It's unclear that this is really the right place to do this
                 */
                if (!(store instanceof BasicOAuthStore)) {
                    log.warn("We expected to be able to use a BasicOAuthStore "
                            + "to configure OAuth services!");
                } else {
                    OAuthServiceProviderRegistry spr = Framework.getLocalService(OAuthServiceProviderRegistry.class);

                    for (OAuthServiceDescriptor descriptor : os.getOAuthServices()) {

                        spr.addReadOnlyProvider(descriptor.gadgetUrl,
                                descriptor.serviceName, descriptor.consumerKey,
                                descriptor.consumerSecret, null);

                        /**
                         * BasicOAuthStore oauthStore = (BasicOAuthStore) store;
                         * BasicOAuthStoreConsumerIndex index = new
                         * BasicOAuthStoreConsumerIndex();
                         * index.setGadgetUri(descriptor.getGadgetUrl());
                         * index.setServiceName(descriptor.getServiceName());
                         * String oauthKey = IOUtils.toString(new FileReader(
                         * os.getOAuthPrivateKeyFile())); if
                         * (!StringUtils.isEmpty
                         * (descriptor.getConsumerSecret())) { oauthKey =
                         * descriptor.getConsumerSecret(); }
                         * BasicOAuthStoreConsumerKeyAndSecret keyAndSecret =
                         * new BasicOAuthStoreConsumerKeyAndSecret(
                         * descriptor.getConsumerKey(), oauthKey,
                         * KeyType.RSA_PRIVATE, os.getOAuthPrivateKeyName(),
                         * os.getOAuthCallbackUrl());
                         * oauthStore.setConsumerKeyAndSecret(index,
                         * keyAndSecret);
                         **/
                    }
                }
            }
        } catch (Exception e) {
            // Someone specified securityTokenKeyFile, but we couldn't load the
            // key. That merits killing
            // the server.
            throw new RuntimeException(e);
        }
    }

    // @Override
    // public SecurityToken createToken(Map<String, String> tokenParameters)
    // throws SecurityTokenException {
    // SecurityToken anon = super.createToken(tokenParameters);
    // if (anon.isAnonymous()) {
    // if (tokenParameters.get(NXAuthenticationHandler.NX_COOKIE) != null) {
    // return new NxSecurityToken(anon.getViewerId(),
    // anon.getOwnerId(), null,
    // tokenParameters.get(NXAuthenticationHandler.NX_COOKIE));
    // } else {
    // return anon;
    // }
    // }
    // BlobCrypterSecurityToken token = (BlobCrypterSecurityToken) anon;
    // // convert to nuxeo token
    // NxSecurityToken tokenResult = new NxSecurityToken(token.getViewerId(),
    // token.getOwnerId(), null,
    // tokenParameters.get(NXAuthenticationHandler.NX_COOKIE));
    // return tokenResult;
    // }
}
