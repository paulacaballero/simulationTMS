package simulator;

import java.util.concurrent.PriorityBlockingQueue;

/**
 * TMS simulator
 * Author: Team 4
 *
 */
public class App {
    static final  int NUMDOCTOR = 4;
    static final  int NUMSPECIALTIES = 3;
    static final  int NUMPATIENTS = 30;
    static final  int NUMTRIAGE = 2;

    private Doctor doctors[];
    private Patient patients[];
    private Triage triage[];
    private PriorityBlockingQueue<Patient>[] queues;
    private WaitingRoom waitingRoom;
    private ServiceStation serviceStation;

    @SuppressWarnings("unchecked")
    public App() {

        // Initialize the queues
        queues = new PriorityBlockingQueue[NUMSPECIALTIES];
        for (int i = 0; i < NUMSPECIALTIES; i++) {
            queues[i] = new PriorityBlockingQueue<>(
                10, (t1, t2) -> Integer.compare(t2.getPriority(), t1.getPriority()) // Descending order by priority
            );
        }

        // Initialize the waiting room
        waitingRoom = new WaitingRoom(queues, NUMPATIENTS, NUMSPECIALTIES);
        serviceStation = new ServiceStation();

        // Initialize the doctors
        doctors = new Doctor[NUMDOCTOR];
        for (int i = 0; i < NUMDOCTOR; i++) {
            int priority = (i % 3) + 1; // Asignar prioridad (1, 2, o 3)
            int specialty=(i % 3); // Specialty (0,1,2)
            doctors[i] = new Doctor(waitingRoom, specialty, i, priority, serviceStation);
        }

        // Initialize the patients
        patients = new Patient[NUMPATIENTS];
        for (int i = 0; i < NUMPATIENTS; i++) {
            patients[i] = new Patient(waitingRoom, i);
        }

        // Initialize the triage
        triage = new Triage[NUMTRIAGE];
        for(int i = 0; i < NUMTRIAGE; i++){
            triage[i] = new Triage(waitingRoom, i);
        }
    }

    public void startThreads() {
        // Start patient threads
        for (int i = 0; i < NUMPATIENTS; i++) {
            patients[i].start();
        }

        // Start triage threads
        for(int i = 0; i < NUMTRIAGE; i++){
            triage[i].start();
        }

        // Start doctor threads
        for (int i = 0; i < NUMDOCTOR; i++) {
            doctors[i].start();
        }
        
    }

    public void waitEndOfThreads() {
        
        try {
            // Stop patient threads
            for (int i = 0; i < NUMPATIENTS; i++) {
                patients[i].join();
            }

            // Stop doctor threads
            for (int i = 0; i < NUMDOCTOR; i++) {
                doctors[i].interrupt();
            }
            for (int i = 0; i < NUMDOCTOR; i++) {
                doctors[i].join();
            }

            // Stop triage threads
            for (int i = 0; i < NUMTRIAGE; i++) {
                triage[i].interrupt();
            }
            for (int i = 0; i < NUMTRIAGE; i++) {
                triage[i].join();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
    }
    public void calculateAvgWaitingTime(){
        int waitingMinutes = waitingRoom.getTotalWaitingTime();
        waitingMinutes = waitingMinutes / NUMPATIENTS;
        System.out.println("The average waiting time has been "+ waitingMinutes+" minutes.");
    }
    
        public static void main(String[] args) {
    
            App app = new App();
    
            app.startThreads();
            app.waitEndOfThreads();
            app.calculateAvgWaitingTime();
    }
}
