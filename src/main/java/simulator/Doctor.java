package simulator;


public class Doctor extends Thread implements Comparable<Doctor> {
    private int specialty;
    private int priorityDoc; 
    private WaitingRoom waitingRoom;
    private ServiceStation serviceStation; 
    private int materials;

    public Doctor(WaitingRoom waitingRoom, int specialty, int id, int priorityDoc, ServiceStation serviceStation) {
        super("Doctor " + id);
        this.specialty = specialty;
        this.priorityDoc = priorityDoc;
        this.waitingRoom = waitingRoom;
        this.serviceStation = serviceStation;
        this.materials = 3;
    }

    public int getSpecialty() {
        return specialty;
    }
    public int getPriorityDoc() {
        return priorityDoc;
    }

    
    @Override
    public int compareTo(Doctor other) {
        return Integer.compare(this.priorityDoc, other.priorityDoc);
    }

    @Override
    public void run() {
        while (!isInterrupted()) {
            try {
                if (materials == 0) {
                    serviceStation.restockMaterials(this);
                    materials = 3; 
                }
                waitingRoom.attend(this);
                materials--; 
            } catch (InterruptedException e) {
                interrupt();
            }
        }
    }
}
