import java.net.DatagramPacket;
import java.util.LinkedList;
import java.util.Queue;

public class DroneRequestBuffer {
    private Queue<DatagramPacket> buffer;

    public DroneRequestBuffer(){
        buffer = new LinkedList<>();
    }

    public synchronized void addRequest(DatagramPacket request){
        buffer.add(request);
        notifyAll();
    }

    public synchronized DatagramPacket removeRequest(){
        DatagramPacket request = buffer.poll();
        return request;
    }

    public synchronized boolean isEmpty(){
        boolean isEmpty = buffer.isEmpty();
        return isEmpty;
    }
}
