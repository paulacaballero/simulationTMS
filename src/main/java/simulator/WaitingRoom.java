package simulator;

import java.util.Random;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

public class WaitingRoom {
    final static String RESET = "\u001B[0m";
    final static String RED = "\u001B[31m";
    final static String GREEN = "\u001B[32m";
    final static String YELLOW = "\u001B[33m";
    private int NUMSPECIALTIES;
    private int NUMPATIENTS;
    private Semaphore mutex;
    private Semaphore patientReady;
    private Semaphore doctorDone;
    private Semaphore patientDone;
    private Semaphore patientRegistered;
    private Semaphore triageValidated;
    private Semaphore[] caseInQueue;
    private Semaphore[] queueSemaphores;
    private Random rand;
    private PriorityBlockingQueue<Patient>[] queues;
    private long[] nextPatientId;
    private int[] numPatientsWaiting;
    public int totalWaitingTime;

    public WaitingRoom(PriorityBlockingQueue<Patient>[] queues, int numPatients, int numSpecialties) {
        mutex = new Semaphore(1);
        patientReady = new Semaphore(0);
        doctorDone = new Semaphore(0);
        patientDone = new Semaphore(0);
        patientRegistered = new Semaphore(0);
        triageValidated = new Semaphore(0);
        NUMPATIENTS =  numPatients;
        NUMSPECIALTIES = numSpecialties;
        queueSemaphores = new Semaphore[NUMSPECIALTIES];
        caseInQueue = new Semaphore[NUMSPECIALTIES];
        for(int i = 0; i < NUMSPECIALTIES; i++){
            queueSemaphores[i] = new Semaphore(0);
            caseInQueue[i] = new Semaphore(0);
        }
        rand = new Random();
        totalWaitingTime = 0;
        this.queues = queues;
        nextPatientId = new long[NUMSPECIALTIES];
        numPatientsWaiting = new int[NUMPATIENTS];
    }

    public void register(Patient patient) throws InterruptedException{
        
        mutex.acquire();
            // The patient registers their symptomps
            Thread.sleep(rand.nextInt(1000));
            print(patient,0, ": registers", RED);
            
            // The model assigns a specialty and a priority value
            patient.setSpecialty(rand.nextInt(NUMSPECIALTIES));
            patient.setPriority(rand.nextInt(1,5));
            
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
        print(triage,2, ": The prediction has been validated", YELLOW);
        triageValidated.release();
        
    }

    public void attend(Doctor doctor) throws InterruptedException {
        
        // The doctor signals their patients that is ready
        print(doctor,5, ": ready", GREEN);
        takePatientFromQueue(doctor);
        // And waits for the patient to awake
        patientReady.acquire();
        print(doctor,5, ": treating", GREEN);
        Thread.sleep(rand.nextInt(1000) + 1000);
        // The treatment is done
        doctorDone.release();
        print(doctor,5, ": treating done", GREEN);
    
        patientDone.acquire();
        print(doctor,5, ": done\n", GREEN);
        
    }

    public void getsAttended(Patient patient) throws InterruptedException {
        
            // The patient is the first in the queue
            patientReady.release();
            
            // Gets treated
            print(patient,5, ": treating", RED);
            doctorDone.acquire();
            // Signals that they're done
            patientDone.release();
            print(patient,5, ": treatment done", RED);
        mutex.acquire();
            calculateWaitingTime(patient);
        mutex.release();
        
    }
    private void calculateWaitingTime(Patient patient){
        double waitingTime = 0;
        waitingTime = patient.getTurnsWaited() * 30;
        totalWaitingTime += waitingTime;
        print(patient, 7, "Waited " + waitingTime + " minutes", RESET);
    }
    
    public void waitUntilYourTurn(Patient patient) throws InterruptedException{
        mutex.acquire();
        
            // While the patient isnt the one that comes out of the queue, wait
            numPatientsWaiting[patient.getSpecialty()]++;
            caseInQueue[patient.getSpecialty()].release();
            while(patient.getId() != nextPatientId[patient.getSpecialty()]){
                int turns = patient.getTurnsWaited();
                if(turns % 10 == 0 && turns != 0){
                    patient.setPriority(patient.getPriority()+1);
                }
                patient.setTurnsWaited(turns + 1);
                print(patient, 2, ": waits.", RED);
                mutex.release();
                queueSemaphores[patient.getSpecialty()].acquire();
            }

        mutex.acquire();
        // Since the thread exited the queue one less is waiting
            numPatientsWaiting[patient.getSpecialty()]--;
            print(patient,3, ": turn arrived.", RED);
            printWaitingRoom();    
        mutex.release();
        
    }

    private void takePatientFromQueue(Doctor doctor) throws InterruptedException{
        
        print(doctor,2, ": about to take a pacient from the queue.", GREEN);
        Patient patient = null;
        // The doctor will remain in the loop until there is a patient in their queue
        do{
            mutex.acquire();
            printWaitingRoom();
            // We take the patient with the highest priority from the queue of the doctor, with a timeout of 1 second
            patient = queues[doctor.getSpecialty()].poll(1, TimeUnit.SECONDS);
            if (patient != null) {
                // If there is a patient taken from the queue its id is saved in a variable to sort it from the other patients in the queue
                nextPatientId[doctor.getSpecialty()] = patient.getId();
                // All the threads in the doctor's queue are signaled
                queueSemaphores[doctor.getSpecialty()].release(numPatientsWaiting[doctor.getSpecialty()]);
                mutex.release();
                // caseInQueue[doctor.getSpecialty()].acquire();
            } else {
                print(doctor,2, ": has no patients in their queue.", GREEN);
                mutex.release();
                // If there is no patients in the queue the doctor waits until there is a new case
                
                // Thread.sleep(rand.nextInt(1000));
            }
            caseInQueue[doctor.getSpecialty()].acquire();
        } while(patient == null);
    }
    public void entersQueue(Patient patient) throws InterruptedException{
        
        mutex.acquire();
            // The patient is added to the queue of their assigned specialty
            print(patient,0, ": has been assigned to queue " + patient.getSpecialty() + " with priority " + patient.getPriority(), RED);
            queues[patient.getSpecialty()].add(patient);
            // Signals that a new case has been added to a queue
            caseInQueue[patient.getSpecialty()].release();

        mutex.release();
    }

    private void print(Thread thread, int id, String msg, String color) {
        
        String gap = new String(new char[id + 1]).replace('\0', '\t');
        System.out.println(gap + color + "🖥️" + id + " " + thread.getName() + ": " + msg + RESET);
    }
    private void printWaitingRoom(){
        
        String out = new String();
        for (PriorityBlockingQueue<Patient> queue : queues){
            if(!queue.isEmpty())
            for(Patient patient : queue){
                out += " " + patient.getName() + " |";
            }   
            out += "\n";
        }
        System.out.println(out);
    }

    public int getTotalWaitingTime() {
        return totalWaitingTime;
    }
    
}
