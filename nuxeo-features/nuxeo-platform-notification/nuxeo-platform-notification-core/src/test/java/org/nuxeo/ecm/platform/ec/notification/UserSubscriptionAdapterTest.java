/*
 * (C) Copyright 2006-2015 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     dmetzler
 */
package org.nuxeo.ecm.platform.ec.notification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertNotNull;

import java.util.Calendar;
import java.util.concurrent.TimeUnit;

import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

import com.google.inject.Inject;

@RunWith(FeaturesRunner.class)
@Features(CoreFeature.class)
@Deploy({ "org.nuxeo.ecm.platform.notification.core", "org.nuxeo.ecm.platform.notification.api" })
@RepositoryConfig(cleanup = Granularity.METHOD)
public class UserSubscriptionAdapterTest {

    @Inject
    private CoreSession session;

    private DocumentModel doc;

    @Before
    public void doBefore() throws Exception {
        // Given a document
        doc = session.createDocumentModel("/", "testDoc", "Note");
        doc = session.createDocument(doc);

    }

    @Test
    public void aDocumentMayHaveAUserSubscriptionAdapter() throws Exception {

        // I can get a user subscription adapter on it
        SubscriptionAdapter us = doc.getAdapter(SubscriptionAdapter.class);
        assertNotNull("It should be able to get a UserSubscription adapter", us);

        // To set and get subscriptions
        us.addSubscription("Administrator", "timetoeat");
        us.addSubscription("toto", "timetosleep");

        assertThat(us.getNotificationSubscribers("timetoeat")).contains("Administrator");
        assertThat(us.getNotificationSubscribers("timetoslepp")).doesNotContain("Administrator");

    }

    @Test
    public void itCanRetrieveTheSubscriptionsOfAUserOnADocument() throws Exception {

        SubscriptionAdapter us = doc.getAdapter(SubscriptionAdapter.class);
        // To set and get subscriptions
        us.addSubscription("Administrator", "timetoeat");
        // To set and get subscriptions
        us.addSubscription("toto", "timetosleep");

        assertThat(us.getUserSubscriptions("Administrator")).contains("timetoeat");
        assertThat(us.getUserSubscriptions("Administrator")).doesNotContain("timetosleep");

    }

    @Test
    public void itCanUnsubscribeFromANotification() throws Exception {
        SubscriptionAdapter us = doc.getAdapter(SubscriptionAdapter.class);
        // To set and get subscriptions
        us.addSubscription("Administrator", "timetoeat");
        us.addSubscription("Administrator", "timetosleep");
        assertThat(us.getUserSubscriptions("Administrator")).contains("timetoeat", "timetosleep");

        us.removeUserNotificationSubscription("Administrator", "timetoeat");
        assertThat(us.getUserSubscriptions("Administrator")).contains("timetosleep");
        assertThat(us.getUserSubscriptions("Administrator")).doesNotContain("timetoeat");

    }

    @Test
    public void itCanCopySubscriptionsFromADocModelToAnother() throws Exception {
        // Given a second target document
        DocumentModel targetDoc = session.createDocumentModel("/", "testDoc2", "Note");
        targetDoc = session.createDocument(targetDoc);

        SubscriptionAdapter us = doc.getAdapter(SubscriptionAdapter.class);
        SubscriptionAdapter targetUs = targetDoc.getAdapter(SubscriptionAdapter.class);

        us.addSubscription("Administrator", "timetoeat");
        assertThat(us.getUserSubscriptions("Administrator")).contains("timetoeat");
        assertThat(targetUs.getUserSubscriptions("Administrator")).doesNotContain("timetoeat");

        us.copySubscriptionsTo(targetDoc);

        assertThat(targetUs.getUserSubscriptions("Administrator")).contains("timetoeat");

    }

    @Test
    public void itCanSubscriptToAllNotifications() throws Exception {
        SubscriptionAdapter us = doc.getAdapter(SubscriptionAdapter.class);
        us.addSubscriptionsToAll("Administrator");

        assertThat(us.getUserSubscriptions("Administration")).hasSize(0);

        session.createDocument(session.createDocumentModel("/", "workspace", "Workspace"));
        DocumentModel doc = session.createDocument(session.createDocumentModel("/workspace", "subscribablenote",
                "Workspace"));

        us = doc.getAdapter(SubscriptionAdapter.class);
        us.addSubscriptionsToAll("Administrator");
        assertThat(us.getUserSubscriptions("Administrator")).contains("Modification", "Creation");

    }



}
