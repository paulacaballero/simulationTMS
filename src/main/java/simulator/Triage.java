package simulator;

public class Triage extends Thread {

    private WaitingRoom waitingRoom;

    public Triage(WaitingRoom waitingRoom, int id) {
        super("Triage " + id);
        this.waitingRoom = waitingRoom;
    }

    @Override
    public void run() {
        while (!isInterrupted()) {
            try {
                waitingRoom.validate(this);
            } catch (InterruptedException e) {
                interrupt();
            }
        }
    }
}
