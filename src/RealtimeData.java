import com.google.transit.realtime.GtfsRealtime;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;

public class RealtimeData {

    private static final String URL =
            "https://gtfs.sofiatraffic.bg/api/v1/vehicle-positions";

    public GtfsRealtime.FeedMessage readFeed() throws IOException {

        URL url = new URL(URL);

        HttpURLConnection connection =
                (HttpURLConnection) url.openConnection();

        connection.setRequestMethod("GET");
        connection.setConnectTimeout(10_000);
        connection.setReadTimeout(10_000);

        try (InputStream inputStream = connection.getInputStream()) {
            return GtfsRealtime.FeedMessage.parseFrom(inputStream);
        } finally {
            connection.disconnect();
        }
    }

    public List<GtfsRealtime.FeedEntity> getEntities()
            throws IOException {

        return readFeed().getEntityList();
    }
}