package org.datatransferproject.transfer.facebook.events;

import com.restfb.Connection;
import com.restfb.types.Event;
import org.datatransferproject.spi.transfer.types.CopyExceptionWithFailureReason;

import java.util.Optional;

public interface FacebookEventsInterface {
    Connection<Event> getEvents(Optional<String> paginationToken) throws CopyExceptionWithFailureReason;
}
