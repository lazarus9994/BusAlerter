public class BusAlerter {

    private static final long ALERT_TIME_SECONDS = 10 * 60;

    public void check(BusStatus status, long estimatedArrivalSeconds) {

        if (status == null) {
            return;
        }

        switch (status) {

            case BEFORE_STOP:

                // Алармираме само ако автобусът е
                // на максимум 10 минути от спирката.
                if (estimatedArrivalSeconds >= 0
                        && estimatedArrivalSeconds <= ALERT_TIME_SECONDS) {

                    System.out.println(
                            "Автобусът наближава! " +
                                    "Остава приблизително " +
                                    estimatedArrivalSeconds +
                                    " секунди."
                    );
                }

                break;

            case AT_STOP:

                System.out.println("Автобусът е на спирката!");

                break;

            case PASSED_STOP:

                // Нищо не правим.
                break;
        }
    }
}