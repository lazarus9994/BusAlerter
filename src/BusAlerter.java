public class BusAlerter {

    public void check(BusStatus status) {

        if (status == BusStatus.BEFORE_STOP) {
            System.out.println("Автобусът наближава!");
        }
    }
}