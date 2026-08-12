import java.io.IOException;
import java.net.InetAddress;

void main() throws IOException {

    System.setProperty("java.net.preferIPv4Stack", "true");

    System.out.println("Java DNS:");
    for (InetAddress address : InetAddress.getAllByName("gtfs.sofiatraffic.bg")) {
        System.out.println(address);
    }

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