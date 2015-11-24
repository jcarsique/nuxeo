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

package org.nuxeo.opensocial.services;

import java.util.List;

import org.apache.shindig.gadgets.render.OpenSocialI18NGadgetRewriter;
import org.apache.shindig.gadgets.render.RenderingGadgetRewriter;
import org.apache.shindig.gadgets.render.SanitizingGadgetRewriter;
import org.apache.shindig.gadgets.render.SanitizingRequestRewriter;
import org.apache.shindig.gadgets.rewrite.CssRequestRewriter;
import org.apache.shindig.gadgets.rewrite.GadgetRewriter;
import org.apache.shindig.gadgets.rewrite.HTMLContentRewriter;
import org.apache.shindig.gadgets.rewrite.PipelineDataGadgetRewriter;
import org.apache.shindig.gadgets.rewrite.ProxyingLinkRewriterFactory;
import org.apache.shindig.gadgets.rewrite.RequestRewriter;
import org.apache.shindig.gadgets.rewrite.TemplateRewriter;
import org.apache.shindig.gadgets.servlet.CajaContentRewriter;
import org.nuxeo.opensocial.shindig.gadgets.rewrite.NXHTMLContentRewriter;
import org.nuxeo.opensocial.shindig.gadgets.rewrite.NXProxyingLinkRewriterFactory;

import com.google.common.collect.Lists;
import com.google.inject.AbstractModule;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.TypeLiteral;

/**
 * This replaces the default RewriteModule by using our own
 * NXHTMLContentRewriter
 *
 * @author dmetzler
 *
 */
public class NXRewriteModule extends AbstractModule {

    @Override
    protected void configure() {
        bind(new TypeLiteral<List<GadgetRewriter>>() {
        }).toProvider(GadgetRewritersProvider.class);
        bind(new TypeLiteral<List<RequestRewriter>>() {
        }).toProvider(RequestRewritersProvider.class);

        bind(ProxyingLinkRewriterFactory.class).to(
                NXProxyingLinkRewriterFactory.class);
    }

    private static class GadgetRewritersProvider implements
            Provider<List<GadgetRewriter>> {
        private final List<GadgetRewriter> rewriters;

        @Inject
        public GadgetRewritersProvider(
                PipelineDataGadgetRewriter pipelineRewriter,
                TemplateRewriter templateRewriter,
                NXHTMLContentRewriter optimizingRewriter,
                CssRequestRewriter cssRewriter,
                CajaContentRewriter cajaRewriter,
                SanitizingGadgetRewriter sanitizedRewriter,
                RenderingGadgetRewriter renderingRewriter,
                OpenSocialI18NGadgetRewriter i18nRewriter) {
            rewriters = Lists.newArrayList();
            rewriters.add(pipelineRewriter);
            rewriters.add(templateRewriter);
            rewriters.add(optimizingRewriter);
            rewriters.add(cajaRewriter);
            rewriters.add(sanitizedRewriter);
            rewriters.add(renderingRewriter);
            rewriters.add(i18nRewriter);
        }

        public List<GadgetRewriter> get() {
            return rewriters;
        }
    }

    private static class RequestRewritersProvider implements
            Provider<List<RequestRewriter>> {
        private final List<RequestRewriter> rewriters;

        @Inject
        public RequestRewritersProvider(HTMLContentRewriter optimizingRewriter,
                CssRequestRewriter cssRewriter,
                SanitizingRequestRewriter sanitizedRewriter) {
            rewriters = Lists.newArrayList();
            rewriters.add(optimizingRewriter);
            rewriters.add(cssRewriter);
            rewriters.add(sanitizedRewriter);
        }

        public List<RequestRewriter> get() {
            return rewriters;
        }
    }
}
