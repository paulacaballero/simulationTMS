package simulator;

import java.util.Random;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

public class WaitingRoom {
    final static int NUMSPECIALTIES = 3;
    final static int NUMPATIENTS = 20;
    private Semaphore mutex;
    private Semaphore patientReady;
    private Semaphore doctorDone;
    private Semaphore patientDone;
    private Semaphore patientRegistered;
    private Semaphore triageValidated;
    private Semaphore newCase;
    private Semaphore[] queueSemaphores;
    private Random rand;
    private PriorityBlockingQueue<Patient>[] queues;
    private long[] nextPatientId;
    private int[] numPatientsWaiting;

    public WaitingRoom(PriorityBlockingQueue<Patient>[] queues) {
        mutex = new Semaphore(1);
        patientReady = new Semaphore(0);
        doctorDone = new Semaphore(0);
        patientDone = new Semaphore(0);
        patientRegistered = new Semaphore(0);
        triageValidated = new Semaphore(0);
        newCase = new Semaphore(0);
        queueSemaphores = new Semaphore[NUMSPECIALTIES];
        for(int i = 0; i < NUMSPECIALTIES; i++){
            queueSemaphores[i] = new Semaphore(0);
        }
        rand = new Random();
        this.queues = queues;
        nextPatientId = new long[NUMSPECIALTIES];
        numPatientsWaiting = new int[NUMPATIENTS];
    }

    public void register(Patient patient) throws InterruptedException{
        
        mutex.acquire();
            // The patient registers their symptomps
            Thread.sleep(rand.nextInt(1000));
            print(patient,0, ": registers");
            
            // The model assigns a specialty and a priority value
            patient.setSpecialty(rand.nextInt(NUMSPECIALTIES));
            patient.setPriority(rand.nextInt(5));
            
        mutex.release();

            // The triage has to validate the results of the model
            patientRegistered.release();
            
            triageValidated.acquire();
            // Once the triage staff validates the results the patient enters a queue
            entersQueue(patient);
        
    }
    public void validate(Triage triage) throws InterruptedException{

        // The triage staff checks if the values of the model are correct
        patientRegistered.acquire();
        Thread.sleep(rand.nextInt(1000));
        print(triage,2, ": The prediction has been validated");
        triageValidated.release();
        
    }

    public void attend(Doctor doctor) throws InterruptedException {
        
        // The doctor signals their patients that is ready
        print(doctor,1, ": ready");
        takePatientFromQueue(doctor);
        // And waits for the patient to awake
        patientReady.acquire();
        print(doctor,1, ": treating");
        Thread.sleep(rand.nextInt(1000) + 1000);
        print(doctor,1, ": treating done");
        // The treatment is done
        doctorDone.release();
        patientDone.acquire();
        print(doctor,1, ": done\n");
        
    }

    public void getsAttended(Patient patient) throws InterruptedException {
        mutex.acquire();
            // The patient is the first in the queue
            print(patient,0, ": is the first in the queue");
            patientReady.release();
            doctorDone.acquire();
            // Gets treated
            print(patient,0, ": treating");
            Thread.sleep(rand.nextInt(1000) + 100);
            print(patient,0, ": treatment done");
            // Signals that they're done
            patientDone.release();
        mutex.release();
        
    }
    public void waitUntilYourTurn(Patient patient) throws InterruptedException{
        mutex.acquire();
        
            // While the patient isnt the one that comes out of the queue, wait
            while(patient.getId() != nextPatientId[patient.getSpecialty()]){
                print(patient, 0, ": waits.");
                numPatientsWaiting[patient.getSpecialty()]++;
                mutex.release();
                queueSemaphores[patient.getSpecialty()].acquire();
            }

        mutex.acquire();
        // Since the thread exited the queue one less is waiting
            numPatientsWaiting[patient.getSpecialty()]--;
            print(patient,0, ": turn arrived.");
        
        mutex.release();
        
    }

    private void takePatientFromQueue(Doctor doctor) throws InterruptedException{
        
        print(doctor,1, ": about to take a pacient from the queue.");
        Patient patient = null;
        // The doctor will remain in the loop until there is a patient in their queue
        do{
            mutex.acquire();
            // We take the patient with the highest priority from the queue of the doctor, with a timeout of 1 second
            patient = queues[doctor.getSpecialty()].poll(1, TimeUnit.SECONDS);
            if (patient != null) {
                // If there is a patient taken from the queue its id is saved in a variable to sort it from the other patients in the queue
                nextPatientId[doctor.getSpecialty()] = patient.getId();
                print(doctor,1, ": signal all patients that one is out.");
                // All the threads in the doctor's queue are signaled
                queueSemaphores[doctor.getSpecialty()].release(numPatientsWaiting[doctor.getSpecialty()]);
                mutex.release();
            } else {
                print(doctor,1, ": has no patients in their queue.");
                mutex.release();
                // If there is no patients in the queue the doctor waits until there is a new case
                newCase.acquire();
            }
        } while(patient == null);
            
        // print(doctor, 1, " out of the loop.");
    }
    public void entersQueue(Patient patient) throws InterruptedException{
        
        mutex.acquire();
            // The patient is added to the queue of their assigned specialty
            print(patient,0, ": has been assigned to queue " + patient.getSpecialty() + " with priority " + patient.getPriority());
            queues[patient.getSpecialty()].add(patient);
            // Signals that a new case has been added to a queue
            newCase.release();

        mutex.release();
    }

    private void print(Thread thread, int id, String msg) {
        
        String gap = new String(new char[id + 1]).replace('\0', '\t');
        System.out.println(gap + "🖥️" + id + " " + thread.getName() + ": " + msg);
    }

}
