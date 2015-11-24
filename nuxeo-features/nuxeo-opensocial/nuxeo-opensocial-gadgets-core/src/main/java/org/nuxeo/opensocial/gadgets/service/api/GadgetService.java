/*
 * (C) Copyright 2006-2009 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Leroy Merlin (http://www.leroymerlin.fr/) - initial implementation
 */

package org.nuxeo.opensocial.gadgets.service.api;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.shindig.gadgets.spec.GadgetSpec;

public interface GadgetService {

    /**
     * Return a gadget declaration
     *
     * @param gadgetSymbolicName the symbolic name of the gadget
     */
    GadgetDeclaration getGadget(String gadgetSymbolicName);

    /**
     * Returns the list of all registered gadgets declarations
     */
    List<GadgetDeclaration> getGadgetList();

    /**
     * Returns the list of gadget categories
     */
    List<String> getGadgetCategory();

    /**
     * Returns the list of the registered gadgets for a given category
     *
     * @param category the category
     */
    List<GadgetDeclaration> getGadgetList(String category);

    /**
     * Return a hashed map of all registered gadgets by category TODO: check if
     * this is necessary in the API (perhaps a GWT tweak)
     */
    Map<String, ArrayList<String>> getGadgetNameByCategory();

    /**
     * Returns a stream on a resource of a gadget
     *
     * @param gadgetName the symbolic name of the gadget
     * @param resourcePath the relative path to the resources
     */
    InputStream getGadgetResource(String gadgetName, String resourcePath)
            throws IOException;

    /**
     * Returns a URL to the gadget definition
     *
     * @param gadgetName the gadget symbolic name
     */
    URL getGadgetDefinition(String gadgetName);

    /**
     * Returns a relative URL to the gadget icon
     *
     * @param gadgetName the gadget symbolic name
     */
    String getIconUrl(String gadgetName);

    /**
     * Return the parsed Gadget Spec (may require an http fetch)
     *
     * @param gadgetName the gadget symbolic name
     * @return
     * @throws Exception
     */
    GadgetSpec getGadgetSpec(String gadgetName) throws Exception;

    /**
     * Return the parsed Gadget Spec (may require an http fetch)
     *
     * @param declaration
     * @return
     * @throws Exception
     */
    GadgetSpec getGadgetSpec(GadgetDeclaration declaration) throws Exception;

}
