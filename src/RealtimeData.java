import com.google.transit.realtime.GtfsRealtime;

import javax.net.ssl.HttpsURLConnection;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.List;

public class RealtimeData {

    private static final String URL =
            "https://gtfs.sofiatraffic.bg/api/v1/vehicle-positions";


    public GtfsRealtime.FeedMessage readFeed() throws IOException {

        URL url = new URL(URL);

        HttpsURLConnection connection =
                (HttpsURLConnection) url.openConnection();

        try {
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(10_000);
            connection.setReadTimeout(10_000);

            connection.setRequestProperty(
                    "User-Agent", "BusAlerter/1.0"
            );

            connection.setRequestProperty(
                    "Accept", "*/*"
            );

            try (InputStream inputStream = connection.getInputStream();
                 ByteArrayOutputStream outputStream =
                         new ByteArrayOutputStream()) {

                byte[] buffer = new byte[4096];
                int bytesRead;

                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                }

                byte[] data = outputStream.toByteArray();

                return GtfsRealtime.FeedMessage.parseFrom(data);
            }

        } finally {
            connection.disconnect();
        }
    }

    public List<GtfsRealtime.FeedEntity> getEntities()
            throws IOException {

        return readFeed().getEntityList();
    }
}