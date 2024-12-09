package simulator;

public class Patient extends Thread {

    private WaitingRoom waitingRoom;
    private int specialty;

    public Patient(WaitingRoom waitingRoom, int id) {
        super("Patient " + id);
        this.waitingRoom = waitingRoom;
    }

    public int getSpecialty() {
        return specialty;
    }


    public void setSpecialty(int specialty) {
        this.specialty = specialty;
    }

    @Override
    public void run() {
        try {
            waitingRoom.register(this);
            waitingRoom.waitUntilYourTurn(this);
            waitingRoom.getsAttended(this);
        } catch (InterruptedException e) {
        }
    }

}
