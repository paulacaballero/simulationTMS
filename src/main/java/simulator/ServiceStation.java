package simulator;

import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.Semaphore;

public class ServiceStation {
    private PriorityBlockingQueue<Doctor> doctorQueue; 
    private Semaphore availableSpot;

    public ServiceStation() {
        doctorQueue = new PriorityBlockingQueue<>();
        availableSpot = new Semaphore(1); //Only one doctor can be taking materials at the same time
    }

    public void restockMaterials(Doctor doctor) throws InterruptedException {
        doctorQueue.add(doctor); //Doctor waiting in the queue

        System.out.println(doctor.getName() + " is waiting to replenish materials.");

        synchronized (this) {
            while (doctorQueue.peek() != doctor) {
                wait();
            }
        }

        availableSpot.acquire();

        System.out.println(doctor.getName() + " is replenishing materials.");
        Thread.sleep(1000);

        System.out.println(doctor.getName() + " has finished replenishing materials.");
        availableSpot.release();

        synchronized (this) {
            doctorQueue.poll();
            notifyAll();
        }
    }
}
