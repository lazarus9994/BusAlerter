import com.google.transit.realtime.GtfsRealtime;

import java.io.IOException;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BusMonitor {

    private final Map<String, ArrayList<PositionSample>> history = new HashMap<>();
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

        long timestamp = System.currentTimeMillis();

        System.out.println(
                "Realtime check: " +
                        LocalTime.now()
        );

        System.out.println("Checking realtime...");

        List<GtfsRealtime.FeedEntity> entities =
                realtime.getEntities();

        for (GtfsRealtime.FeedEntity entity : entities) {

            if (!entity.hasVehicle()) {
                continue;
            }

            GtfsRealtime.VehiclePosition vehicle =
                    entity.getVehicle();

            if (!vehicle.hasPosition()) {
                continue;
            }

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

            ShapePoint currentPoint =
                    gtfs.getNearestShapePoint(
                            shapeId,
                            latitude,
                            longitude
                    );

            ShapePoint stopPoint =
                    gtfs.getShapePointAtStop(
                            shapeId,
                            "A0964"
                    );

            if (currentPoint == null || stopPoint == null) {
                continue;
            }

            System.out.println(
                    "Sequence: " +
                            currentPoint.sequence() +
                            " | Stop sequence: " +
                            stopPoint.sequence()
            );

            double distance = gtfs.distanceAlongShape(
                    shapeId,
                    currentPoint.sequence(),
                    stopPoint.sequence()
            );

            String vehicleId = vehicle.getVehicle().getId();

            history
                    .computeIfAbsent(vehicleId, k -> new ArrayList<>())
                    .add(new PositionSample(timestamp, distance));


            System.out.println(
                    "Distance along shape to stop: " +
                            distance + " m"
            );

            double speed = calculateSpeed(vehicleId);


            long seconds =
                    estimateArrivalSeconds(distance, speed);

            if (seconds >= 0) {
                System.out.println(
                        "Estimated arrival: " +
                                seconds +
                                " seconds"
                );
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
                            vehicleId +
                            " | Trip: " +
                            tripId +
                            " | Status: " +
                            status
            );

            /*
             * Scheduled arrival е само справочна информация.
             * Не използваме миналия час, за да определяме
             * дали автобусът още е в движение.
             */
            String arrivalTime =
                    gtfs.getArrivalTime(tripId, "A0964");

            if (arrivalTime != null &&
                    !arrivalTime.equals("null")) {

                LocalTime scheduledArrival =
                        LocalTime.parse(arrivalTime);

                if (scheduledArrival.isAfter(LocalTime.now())) {
                    System.out.println(
                            "Scheduled arrival at A0964: " +
                                    arrivalTime
                    );
                }
            }

            alerter.check(status, seconds);
        }
    }

    private long estimateArrivalSeconds(
            double distanceMeters,
            double speedMetersPerSecond) {

        if (speedMetersPerSecond <= 0) {
            return -1;
        }

        return Math.round(
                distanceMeters / speedMetersPerSecond
        );
    }

    private double calculateSpeed(String vehicleId) {

        ArrayList<PositionSample> samples =
                history.get(vehicleId);

        if (samples == null || samples.size() < 2) {
            return 0;
        }

        PositionSample previous =
                samples.get(samples.size() - 2);

        PositionSample current =
                samples.get(samples.size() - 1);

        double distanceTravelled =
                previous.distance() - current.distance();

        double timeSeconds =
                (current.timestamp() -
                        previous.timestamp()) / 1000.0;

        if (timeSeconds <= 0) {
            return 0;
        }

        return distanceTravelled / timeSeconds;
    }

    public void run() {

        while (true) {

            try {
                runOnce();

            } catch (IOException e) {

                System.out.println(
                        "Connection error: " +
                                e.getMessage()
                );

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