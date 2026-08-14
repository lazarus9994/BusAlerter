import java.io.IOException;

void main() throws IOException {

    GtfsData gtfs = new GtfsData();

    RealtimeData realtime =
            new RealtimeData();

    BusAlerter alerter =
            new BusAlerter();

    BusMonitor monitor =
            new BusMonitor(
                    gtfs,
                    realtime,
                    alerter
            );

    monitor.run();
}