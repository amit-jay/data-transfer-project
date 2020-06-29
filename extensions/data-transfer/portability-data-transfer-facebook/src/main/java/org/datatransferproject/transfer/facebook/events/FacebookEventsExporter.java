package org.datatransferproject.transfer.facebook.events;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.restfb.Connection;
import com.restfb.types.Event;
import com.restfb.types.Location;
import com.restfb.types.Place;
import com.restfb.util.StringUtils;
import org.datatransferproject.api.launcher.Monitor;
import org.datatransferproject.spi.transfer.provider.ExportResult;
import org.datatransferproject.spi.transfer.provider.Exporter;
import org.datatransferproject.spi.transfer.types.ContinuationData;
import org.datatransferproject.spi.transfer.types.CopyExceptionWithFailureReason;
import org.datatransferproject.types.common.ExportInformation;
import org.datatransferproject.types.common.PaginationData;
import org.datatransferproject.types.common.StringPaginationToken;
import org.datatransferproject.types.common.models.calendar.CalendarContainerResource;
import org.datatransferproject.types.common.models.calendar.CalendarEventModel;
import org.datatransferproject.types.common.models.calendar.CalendarModel;
import org.datatransferproject.types.transfer.auth.AppCredentials;
import org.datatransferproject.types.transfer.auth.TokensAndUrlAuthData;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.*;

public class FacebookEventsExporter
    implements Exporter<TokensAndUrlAuthData, CalendarContainerResource> {

  private final Monitor monitor;
  private List<CalendarModel> calendarModels = new ArrayList<>();
  private AppCredentials appCredentials;
  private FacebookEventsInterface facebookEventsInterface;

  public FacebookEventsExporter(AppCredentials appCredentials, Monitor monitor) {
    this.appCredentials = appCredentials;
    this.monitor = monitor;
  }

  @VisibleForTesting
  FacebookEventsExporter(
      AppCredentials appCredentials,
      FacebookEventsInterface facebookEventsInterface,
      Monitor monitor) {
    this.appCredentials = appCredentials;
    this.facebookEventsInterface = facebookEventsInterface;
    this.monitor = monitor;
  }

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

  @Override
  public ExportResult<CalendarContainerResource> export(
      UUID jobId, TokensAndUrlAuthData authData, Optional<ExportInformation> exportInformation)
      throws CopyExceptionWithFailureReason {

    Preconditions.checkNotNull(authData);
    return exportEvents(
        authData, exportInformation.map(e -> (StringPaginationToken) e.getPaginationData()));
  }

  private ExportResult<CalendarContainerResource> exportEvents(
      TokensAndUrlAuthData authData, Optional<StringPaginationToken> paginationData)
      throws CopyExceptionWithFailureReason {

    Optional<String> paginationToken = paginationData.map(StringPaginationToken::getToken);

    Connection<Event> eventConnection =
        getOrCreateEventsInterface(authData).getEvents(paginationToken);

    List<Event> events = eventConnection.getData();

    if (events.isEmpty()) {
      monitor.info(()->"No Facebook events to export");
      return new ExportResult<>(ExportResult.ResultType.END, null);
    }

    if (calendarModels.size() == 0) {
      CalendarModel calendarModel =
          new CalendarModel(
              UUID.randomUUID().toString(),
              "Facebook Events Export",
              "Events exported from Facebook.");
      calendarModels.add(calendarModel);
    }

    ArrayList<CalendarEventModel> exportEvents = new ArrayList<>(events.size());
    for (Event event : events) {
      exportEvents.add(
          new CalendarEventModel(
              calendarModels.get(0).getId(),
              event.getName(),
              event.getDescription(),
              null,
              getEventLocation(event.getPlace()),
              getEventTime(event.getStartTime(), event.getTimezone()),
              getEventTime(event.getEndTime(), event.getTimezone()),
              null));
    }

    String token = eventConnection.getAfterCursor();
    if (Strings.isNullOrEmpty(token)) {
      monitor.info(()->"No more Facebook events to export");
      return new ExportResult<>(
          ExportResult.ResultType.END, new CalendarContainerResource(calendarModels, exportEvents));
    } else {
      monitor.info(()->"Continuing to fetch Facebook events to export");
      PaginationData nextPageData = new StringPaginationToken(token);
      ContinuationData continuationData = new ContinuationData(nextPageData);
      return new ExportResult<>(
          ExportResult.ResultType.CONTINUE,
          new CalendarContainerResource(calendarModels, exportEvents),
          continuationData);
    }
  }

  private String getEventLocation(Place eventPlace) {
    String exportEventLocation = "";

    if (eventPlace != null) {
      exportEventLocation = eventPlace.getName() != null? eventPlace.getName() : "";
      Location eventLocation = eventPlace.getLocation();
      if (eventLocation != null) {
        String city = eventLocation.getCity() != null ? eventLocation.getCity() : "";
        String state = eventLocation.getState() != null ? eventLocation.getState() : "";
        String country = eventLocation.getCountry() != null ? eventLocation.getCountry() : "";

        exportEventLocation = exportEventLocation + "\n" + city + "," + state + "\n" + country;
      }
    }
    return exportEventLocation;
  }

  private synchronized FacebookEventsInterface getOrCreateEventsInterface(
      TokensAndUrlAuthData authData) {
    return facebookEventsInterface == null
        ? makeEventsInterface(authData)
        : facebookEventsInterface;
  }

  private synchronized FacebookEventsInterface makeEventsInterface(TokensAndUrlAuthData authData) {
    facebookEventsInterface = new RestFbFacebookEvents(authData, appCredentials);
    return facebookEventsInterface;
  }
}
