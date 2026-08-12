import com.google.transit.realtime.GtfsRealtime;

import java.io.IOException;
import java.util.List;

public class BusMonitor {

    private final GtfsData gtfs;
    private final RealtimeData realtime;
    private final BusAlerter alerter;

    public BusMonitor(
            GtfsData gtfs,
            RealtimeData realtime,
            BusAlerter alerter) {

        this.gtfs = gtfs;
        this.realtime = realtime;
        this.alerter = alerter;
    }

    public void runOnce() throws IOException {

        System.out.println("Checking realtime...");

        List<GtfsRealtime.FeedEntity> entities =
                realtime.getEntities();

        for (GtfsRealtime.FeedEntity entity : entities) {

            if (!entity.hasVehicle()) {
                continue;
            }

            GtfsRealtime.VehiclePosition vehicle =
                    entity.getVehicle();

            if (!vehicle.hasTrip()) {
                continue;
            }

            String tripId =
                    vehicle.getTrip().getTripId();

            if (!tripId.startsWith("A101-")) {
                continue;
            }

            double latitude =
                    vehicle.getPosition().getLatitude();

            double longitude =
                    vehicle.getPosition().getLongitude();

            String shapeId =
                    gtfs.getShapeIdForTrip(tripId);

            if (shapeId == null) {
                continue;
            }

            BusStatus status =
                    gtfs.getBusStatus(
                            shapeId,
                            latitude,
                            longitude,
                            "A0964"
                    );

            System.out.println(
                    "Bus: " +
                            vehicle.getVehicle().getId() +
                            " | Trip: " +
                            tripId +
                            " | Status: " +
                            status
            );

            alerter.check(status);
        }
    }

    public void run() {

        while (true) {
            try {
                runOnce();
            }catch (IOException e) {
                System.out.println("Connection error: " + e.getMessage());
                e.printStackTrace();
            }

            try {
                Thread.sleep(30_000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }
}