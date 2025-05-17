import java.awt.*;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.concurrent.*;

import static java.util.concurrent.TimeUnit.SECONDS;

public class TimeStamp {
    private  boolean lost = false;
    private  final ScheduledExecutorService delayedScheduler = Executors.newScheduledThreadPool(1);

    public  void scheduleDroneCheckUp(int delay, DroneData drone) {
        System.out.println("hey");
        final Runnable beeper = new Runnable() {
            TimeStamp timer = new TimeStamp();
            @Override
            public void run() {
                timer.markDroneLost(drone);
                if(lost){
                    checkToAddEventAgain();
                }
            }
        };

        final ScheduledFuture<?> timer = delayedScheduler.schedule(beeper, delay, SECONDS);
        delayedScheduler.schedule(new Runnable() {
            public void run() { delayedScheduler.shutdown(); }
        }, delay+1, SECONDS);



    }

    private  void checkToAddEventAgain() {

    }

    public  boolean markDroneLost(DroneData drone){
        if(!drone.isCompletedJob()){
            drone.setCompletedJob(false);
        }
        lost = true;
        drone.setLost(true);
        return lost;
    }

    public boolean isDroneLost(){
        return lost;
    }

}