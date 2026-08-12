import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class GtfsData {

    private final Set<String> tripIds;
    private final Set<String> tripsAtStop;
    private final double stopLatitude;
    private final double stopLongitude;


    public GtfsData() throws IOException {
        Path tripsFile = Path.of("gtfs", "trips.txt");

        try (var lines = Files.lines(tripsFile)) {

            tripIds = lines
                    .skip(1)
                    .filter(line -> line.split(",")[1].equals("A101"))
                    .map(line -> line.split(",")[0])
                    .collect(Collectors.toSet());
        }

        Path stopTimesFile = Path.of("gtfs", "stop_times.txt");


        try (var lines = Files.lines(stopTimesFile)) {

            tripsAtStop = lines
                    .skip(1)
                    .map(line -> line.split(","))
                    .filter(fields -> tripIds.contains(fields[0]))
                    .filter(fields -> fields[3].equals("A0964"))
                    .map(fields -> fields[0])
                    .collect(Collectors.toSet());
        }

        Path stopsFile = Path.of("gtfs", "stops.txt");

        double latitude = 0;
        double longitude = 0;

        try (var lines = Files.lines(stopsFile)) {

            String[] stop = lines
                    .skip(1)
                    .map(line -> line.split(","))
                    .filter(fields -> fields[0].equals("A0964"))
                    .findFirst()
                    .orElseThrow();

            latitude = Double.parseDouble(stop[4]);
            longitude = Double.parseDouble(stop[5]);
        }

        stopLatitude = latitude;
        stopLongitude = longitude;
    }

    public Set<String> getTripIds() {
        return tripIds;
    }

    public Set<String> getTripsAtStop() {
        return tripsAtStop;
    }

    public double getStopLatitude() {
        return stopLatitude;
    }

    public double getStopLongitude() {
        return stopLongitude;
    }

    public Set<String> getShapeIds() throws IOException {
        Path tripsFile = Path.of("gtfs", "trips.txt");

        try (var lines = Files.lines(tripsFile)) {
            return lines
                    .skip(1)
                    .map(line -> line.split(","))
                    .filter(fields -> fields[1].equals("A101"))
                    .map(fields -> fields[7])
                    .collect(Collectors.toSet());
        }
    }

    public Set<String> getShapesAtStop() throws IOException {

        Path stopTimesFile = Path.of("gtfs", "stop_times.txt");

        try (var lines = Files.lines(stopTimesFile)) {

            Set<String> trips = lines
                    .skip(1)
                    .map(line -> line.split(","))
                    .filter(fields -> fields[3].equals("A0964"))
                    .map(fields -> fields[0])
                    .collect(Collectors.toSet());

            Path tripsFile = Path.of("gtfs", "trips.txt");

            try (var tripLines = Files.lines(tripsFile)) {

                return tripLines
                        .skip(1)
                        .map(line -> line.split(","))
                        .filter(fields -> trips.contains(fields[0]))
                        .map(fields -> fields[7])
                        .collect(Collectors.toSet());
            }
        }
    }

    public void printShape(String shapeId) throws IOException {

        Path shapesFile = Path.of("gtfs", "shapes.txt");

        try (var lines = Files.lines(shapesFile)) {

            lines
                    .skip(1)
                    .map(line -> line.split(","))
                    .filter(fields -> fields[0].equals(shapeId))
                    .forEach(fields -> System.out.println(
                            "Lat: " + fields[1] +
                                    " | Lon: " + fields[2] +
                                    " | Sequence: " + fields[3]
                    ));
        }
    }

    public List<ShapePoint> getShape(String shapeId) throws IOException {

        Path shapesFile = Path.of("gtfs", "shapes.txt");

        try (var lines = Files.lines(shapesFile)) {

            return lines
                    .skip(1)
                    .map(line -> line.split(","))
                    .filter(fields -> fields[0].equals(shapeId))
                    .map(fields -> new ShapePoint(
                            Double.parseDouble(fields[1]),
                            Double.parseDouble(fields[2]),
                            Integer.parseInt(fields[3])
                    ))
                    .sorted(Comparator.comparingInt(ShapePoint::sequence))
                    .toList();
        }
    }

    public void getTripsForShape(String shapeId) throws IOException {

        Path tripsFile = Path.of("gtfs", "trips.txt");

        try (var lines = Files.lines(tripsFile)) {

            lines
                    .skip(1)
                    .map(line -> line.split(","))
                    .filter(fields -> fields[1].equals("A101"))
                    .filter(fields -> fields[7].equals(shapeId))
                    .forEach(fields -> System.out.println(
                            "Trip: " + fields[0]
                                    + " | Direction: " + fields[5]
                                    + " | Headsign: " + fields[3]
                    ));
        }
    }

    public boolean isBeforeStop(String shapeId,
                                double busLat,
                                double busLon,
                                String stopId) throws IOException {

        ShapePoint nearest = getNearestShapePoint(shapeId, busLat, busLon);
        ShapePoint stopPoint = getShapePointAtStop(shapeId, stopId);

        if (nearest == null || stopPoint == null) {
            return false;
        }

        return nearest.sequence() < stopPoint.sequence();
    }

    public ShapePoint getNearestShapePoint(
            String shapeId,
            double busLat,
            double busLon) throws IOException {

        return getShape(shapeId).stream()
                .min(Comparator.comparingDouble(point ->
                        distance(
                                busLat,
                                busLon,
                                point.latitude(),
                                point.longitude()
                        )
                ))
                .orElse(null);
    }

    public ShapePoint getShapePointAtStop(
            String shapeId,
            String stopId) throws IOException {

        // Засега използваме координатите на спирката
        if (!stopId.equals("A0964")) {
            return null;
        }

        return getShape(shapeId).stream()
                .min(Comparator.comparingDouble(point ->
                        distance(
                                stopLatitude,
                                stopLongitude,
                                point.latitude(),
                                point.longitude()
                        )
                ))
                .orElse(null);
    }

    private double distance(
            double lat1,
            double lon1,
            double lat2,
            double lon2) {

        double dLat = lat2 - lat1;
        double dLon = lon2 - lon1;

        return Math.sqrt(dLat * dLat + dLon * dLon);
    }

    public double distanceToStop(String shapeId, double busLat, double busLon) throws IOException {
        ShapePoint nearest = getNearestShapePoint(shapeId, busLat, busLon);
        ShapePoint stopPoint = getShapePointAtStop(shapeId, "A0964");

        if (nearest == null || stopPoint == null) {
            return -1;
        }

        return distance(
                nearest.latitude(),
                nearest.longitude(),
                stopPoint.latitude(),
                stopPoint.longitude()
        );
    }

    public BusStatus getBusStatus(
            String shapeId,
            double busLat,
            double busLon,
            String stopId) throws IOException {

        ShapePoint nearest =
                getNearestShapePoint(shapeId, busLat, busLon);

        ShapePoint stopPoint =
                getShapePointAtStop(shapeId, stopId);

        if (nearest == null || stopPoint == null) {
            return null;
        }

        if (nearest.sequence() < stopPoint.sequence()) {
            return BusStatus.BEFORE_STOP;
        }

        if (nearest.sequence() > stopPoint.sequence()) {
            return BusStatus.PASSED_STOP;
        }

        return BusStatus.AT_STOP;
    }

    public String getShapeIdForTrip(String tripId) throws IOException {

        Path tripsFile = Path.of("gtfs", "trips.txt");

        try (var lines = Files.lines(tripsFile)) {

            return lines
                    .skip(1)
                    .map(line -> line.split(","))
                    .filter(fields -> fields[0].equals(tripId))
                    .map(fields -> fields[7])
                    .findFirst()
                    .orElse(null);
        }
    }
}
