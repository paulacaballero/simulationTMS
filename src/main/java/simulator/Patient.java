package simulator;

import java.util.Random;
import java.util.concurrent.Semaphore;

public class Patient extends Thread {

    private WaitingRoom waitingRoom;
    private Semaphore patientSemaphore;
    private int specialty;
    private int waitedTime;
    private Random rand;

    public Patient(WaitingRoom waitingRoom, int id) {
        super("Patient " + id);
        this.waitingRoom = waitingRoom;
        waitedTime = 0;
        rand = new Random();
        patientSemaphore = new Semaphore(0);
    }
    

    public Semaphore getPatientSemaphore() {
        return patientSemaphore;
    }


    public int getSpecialty() {
        return specialty;
    }

    public void setSpecialty(int specialty) {
        this.specialty = specialty;
    }


    public int getWaitedTime() {
        return waitedTime;
    }

    public void setWaitedTime(int turnsWaited) {
        this.waitedTime = turnsWaited;
    }

    @Override
    public void run() {
        try {
            if (this.getId() % 2 == 0) {
                Thread.sleep(rand.nextInt(10000));
            } else {
                Thread.sleep(rand.nextInt(10000) + 2000L);
            }
            waitingRoom.register(this);
            waitingRoom.waitUntilYourTurn(this);
            waitingRoom.getsAttended(this);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

}
