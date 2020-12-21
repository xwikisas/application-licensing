/*
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package com.xwiki.licensing.internal.upgrades.notifications;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.xwiki.eventstream.Event;
import org.xwiki.eventstream.RecordableEvent;
import org.xwiki.eventstream.RecordableEventConverter;
import org.xwiki.test.mockito.MockitoComponentMockingRule;

/**
 * Unit tests for {@link ExtensionAutoUpgradedEventConverter}.
 *
 * @version $Id$
 * @since 1.17
 */
public class ExtensionAutoUpgradedEventConverterTest
{
    @Rule
    public MockitoComponentMockingRule<ExtensionAutoUpgradedEventConverter> mocker =
        new MockitoComponentMockingRule<>(ExtensionAutoUpgradedEventConverter.class);

    private RecordableEventConverter defaultConverter;


    @Before
    public void configure() throws Exception
    {
        this.defaultConverter = this.mocker.getInstance(RecordableEventConverter.class);
    }

    @Test
    public void convert() throws Exception
    {
        String message = "Extension upgraded";
        RecordableEvent event = mock(RecordableEvent.class);
        Event convertedEvent = mock(Event.class);

        when(this.defaultConverter.convert(event, null, message)).thenReturn(convertedEvent);

        assertEquals(convertedEvent, this.mocker.getComponentUnderTest().convert(event, null, message));

        verify(convertedEvent).setBody(message);
    }
}
