import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class InProgressEvents {
    private DroneData drone;
    private FireIncidentEvent fireEvent;
    private Map<Integer, FireIncidentEvent> eventList;

    public InProgressEvents(){
        eventList = new ConcurrentHashMap<>();
    }

    public synchronized void addInProgressEvent(int droneID, FireIncidentEvent event){
        System.out.println("["+Thread.currentThread().getName()+"]: Added an in progress event ... \n");
        eventList.put(droneID, event);
        notifyAll();
    }

    public synchronized FireIncidentEvent removeInProgressEvent(int droneID) throws InterruptedException {
        while(eventList.isEmpty()){
            System.out.println("["+Thread.currentThread().getName()+"]: Can't remove from empty list, going to wait ... \n");
            wait();
        }
        FireIncidentEvent fireEvent = eventList.remove(droneID);
        //notifyAll();
        return fireEvent;
    }

    public synchronized boolean isInProgressEventListEmpty() throws InterruptedException {
        while (eventList.isEmpty()){
            wait();
        }
        notifyAll();
        return false;
    }

    public synchronized FireIncidentEvent getAnEventInProgress(int droneID){
        FireIncidentEvent event = eventList.get(droneID);
        return event;
    }

    public synchronized Collection<Integer> getKeys(){
        return eventList.keySet();
    }

    public synchronized void isInProgressEventsListEmpty() throws InterruptedException {
        while(eventList.isEmpty()){
            System.out.println("["+Thread.currentThread().getName()+"]: going to wait... \n");
            wait();
        }
    }
}
