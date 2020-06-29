package org.datatransferproject.transfer.facebook.events;

import com.restfb.Connection;
import com.restfb.DefaultFacebookClient;
import com.restfb.Parameter;
import com.restfb.Version;
import com.restfb.exception.FacebookOAuthException;
import com.restfb.types.Event;
import org.datatransferproject.spi.transfer.types.CopyExceptionWithFailureReason;
import org.datatransferproject.transfer.facebook.utils.FacebookTransferUtils;
import org.datatransferproject.types.transfer.auth.AppCredentials;
import org.datatransferproject.types.transfer.auth.TokensAndUrlAuthData;

import java.util.ArrayList;
import java.util.Optional;

public class RestFbFacebookEvents implements FacebookEventsInterface {

    private DefaultFacebookClient client;

    RestFbFacebookEvents(TokensAndUrlAuthData authData, AppCredentials appCredentials) {
        client =
                new DefaultFacebookClient(
                        authData.getAccessToken(), appCredentials.getSecret(), Version.VERSION_3_2);
    }

    @Override
    public Connection<Event> getEvents(Optional<String> paginationToken) throws CopyExceptionWithFailureReason  {
        ArrayList<Parameter> parameters = new ArrayList<>();
        parameters.add(Parameter.with("fields", "name,description,category,start_time,end_time,timezone,place"));
        paginationToken.ifPresent(token -> parameters.add(Parameter.with("after", token)));
        try {
            return client.fetchConnection(
                    "me/events", Event.class, parameters.toArray(new Parameter[0]));
        } catch (FacebookOAuthException e) {
            throw FacebookTransferUtils.handleFacebookOAuthException(e);
        }
    }
}
