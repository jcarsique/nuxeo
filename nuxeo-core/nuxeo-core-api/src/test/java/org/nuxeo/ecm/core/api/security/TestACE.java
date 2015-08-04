/*
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Nuxeo - initial API and implementation
 *
 * $Id: JOOoConvertPluginImpl.java 18651 2007-05-13 20:28:53Z sfermigier $
 */

package org.nuxeo.ecm.core.api.security;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Calendar;
import java.util.GregorianCalendar;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class TestACE {
    private ACE ace;

    private ACE acebis;

    @Before
    public void setUp() {
        ace = new ACE("bogdan", "write", false);
        acebis = new ACE("vlad", "write", "pas", new GregorianCalendar(2015, Calendar.JULY, 14, 12, 34, 56),
                new GregorianCalendar(2015, Calendar.AUGUST, 14, 12, 34, 56));
    }

    @After
    public void tearDown() {
        ace = null;
    }

    @Test
    public void testGetType() {
        assertFalse(ace.isGranted());
        assertTrue(ace.isDenied());
    }

    @Test
    public void testGetPrincipals() {
        assertEquals("bogdan", ace.getUsername());
    }

    @Test
    public void testGetPermissions() {
        assertEquals("write", ace.getPermission());
    }

    @SuppressWarnings({ "ObjectEqualsNull" })
    @Test
    public void testEquals() {
        ACE ace2 = new ACE("bogdan", "write", false);
        ACE ace3 = new ACE("raoul", "write", false);
        ACE ace4 = new ACE("bogdan", "read", false);
        ACE ace5 = new ACE("bogdan", "write", true);

        assertEquals(ace, ace);
        assertEquals(ace, ace2);
        assertEquals(ace2, ace);
        assertFalse(ace.equals(null));
        assertFalse(ace.equals(ace3));
        assertFalse(ace.equals(ace4));
        assertFalse(ace.equals(ace5));

        assertEquals(ace.hashCode(), ace2.hashCode());
    }
    
    @Test
    public void testNewConstructors() {
        Calendar cal1 = new GregorianCalendar(2015, Calendar.JULY, 14, 12, 34, 56);
        Calendar cal2 = new GregorianCalendar(2015, Calendar.AUGUST, 14, 12, 34, 56);
        ACE ace1 = new ACE("vlad", "write", "pas", cal1, cal2);
        assertEquals(acebis, ace1);
    }

    @Test
    public void testACEId() {
        Calendar cal1 = new GregorianCalendar(2015, Calendar.JULY, 14, 12, 34, 56);
        Calendar cal2 = new GregorianCalendar(2015, Calendar.AUGUST, 14, 12, 34, 56);
        ACE ace = new ACE("vlad", "write", "pas", cal1, cal2);

        assertEquals("vlad:write:true:pas:" + cal1.getTimeInMillis() + ":" + cal2.getTimeInMillis(), ace.getId());

        String aceId = "bob:write:false:pablo:" + cal1.getTimeInMillis() + ":" + cal2.getTimeInMillis();
        ace = ACE.fromId(aceId);
        assertNotNull(ace);
        assertEquals("bob", ace.getUsername());
        assertEquals("write", ace.getPermission());
        assertFalse(ace.isGranted());
        assertEquals("pablo", ace.getCreator());
        assertEquals(cal1, ace.getBegin());
        assertEquals(cal2, ace.getEnd());

        aceId = "pedro:read:true:::";
        ace = ACE.fromId(aceId);
        assertNotNull(ace);
        assertEquals("pedro", ace.getUsername());
        assertEquals("read", ace.getPermission());
        assertTrue(ace.isGranted());
        assertNull(ace.getCreator());
        assertNull(ace.getBegin());
        assertNull(ace.getEnd());

        aceId = "pedro:read:true::" + cal1.getTimeInMillis() + ":";
        ace = ACE.fromId(aceId);
        assertNotNull(ace);
        assertEquals("pedro", ace.getUsername());
        assertEquals("read", ace.getPermission());
        assertTrue(ace.isGranted());
        assertNull(ace.getCreator());
        assertEquals(cal1, ace.getBegin());
        assertNull(ace.getEnd());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testInvalidACEId() {
        String aceId = "pedro:read:";
        ACE.fromId(aceId);
    }
}
