package simulator;

import java.util.Random;

public class Patient extends Thread {

    private WaitingRoom waitingRoom;
    private int specialty;
    private int turnsWaited;
    private Random rand;
    private double waitingTime;

    public Patient(WaitingRoom waitingRoom, int id) {
        super("Patient " + id);
        this.waitingRoom = waitingRoom;
        turnsWaited = 0;
        rand = new Random();
        waitingTime = 0;
    }

    public int getSpecialty() {
        return specialty;
    }

    public void setSpecialty(int specialty) {
        this.specialty = specialty;
    }

    public int getTurnsWaited() {
        return turnsWaited;
    }

    public void setTurnsWaited(int turnsWaited) {
        this.turnsWaited = turnsWaited;
    }

    public double getWaitingTime() {
        return waitingTime;
    }

    public void setWaitingTime(double waitingTime) {
        this.waitingTime = waitingTime;
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
