/*
 * Copyright 2020 The Data Transfer Project Authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.datatransferproject.transfer.facebook.events;

import com.restfb.Connection;
import com.restfb.types.Event;
import com.restfb.util.StringUtils;
import org.datatransferproject.api.launcher.Monitor;
import org.datatransferproject.spi.transfer.provider.ExportResult;
import org.datatransferproject.spi.transfer.types.CopyExceptionWithFailureReason;
import org.datatransferproject.types.common.ExportInformation;
import org.datatransferproject.types.common.models.calendar.CalendarContainerResource;
import org.datatransferproject.types.common.models.calendar.CalendarEventModel;
import org.datatransferproject.types.transfer.auth.AppCredentials;
import org.datatransferproject.types.transfer.auth.TokensAndUrlAuthData;
import org.junit.Before;
import org.junit.Test;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.Optional;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class FacebookEventsExporterTest {

  private static final String EVENT_ID = "937644721";
  private static final String EVENT_NAME = "Test Event";
  private static final String EVENT_LOCATION = "Menlo Park, CA";
  private static final Date EVENT_START = new Date();

  private FacebookEventsExporter facebookEventsExporter;
  private UUID uuid = UUID.randomUUID();

  private static CalendarEventModel.CalendarEventTime getEventTime(Date date, String timezone) {
    if (date == null) {
      return null;
    }

    if (StringUtils.isBlank(timezone)) {
      timezone = "UTC";
    }

    OffsetDateTime offsetDateTime =
        OffsetDateTime.ofInstant(Instant.ofEpochMilli(date.getTime()), ZoneId.of(timezone));
    return new CalendarEventModel.CalendarEventTime(offsetDateTime, false);
  }

  @Before
  public void setUp() throws CopyExceptionWithFailureReason {
    FacebookEventsInterface eventsInterface = mock(FacebookEventsInterface.class);

    // Set up example event
    Event event = new Event();
    event.setId(EVENT_ID);
    event.setName(EVENT_NAME);
    event.setStartTime(EVENT_START);

    ArrayList<Event> events = new ArrayList<>();
    events.add(event);

    @SuppressWarnings("unchecked")
    Connection<Event> eventConnection = mock(Connection.class);

    Monitor monitor = mock(Monitor.class);

    when(eventsInterface.getEvents(Optional.empty())).thenReturn(eventConnection);
    when(eventConnection.getData()).thenReturn(events);

    facebookEventsExporter =
        new FacebookEventsExporter(new AppCredentials("key", "secret"), eventsInterface, monitor);
  }

  @Test
  public void testExportEvents() throws CopyExceptionWithFailureReason {
    ExportResult<CalendarContainerResource> result =
        facebookEventsExporter.export(
            uuid,
            new TokensAndUrlAuthData("accessToken", null, null),
            Optional.of(new ExportInformation(null, null)));

    assertEquals(ExportResult.ResultType.END, result.getType());
    CalendarContainerResource exportedData = result.getExportedData();
    assertEquals(1, exportedData.getEvents().size());
    CalendarEventModel exportEvent = (CalendarEventModel) exportedData.getEvents().toArray()[0];
    assertEquals(EVENT_NAME, exportEvent.getTitle());
  }
}
