package simulator;

import java.util.Random;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;
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
    private BlockingQueue<Patient> entranceQueue;
    private long[] nextPatientId;
    private int[] numPatientsOutofQueue;
    public int totalWaitingTime;

    public WaitingRoom(PriorityBlockingQueue<Patient>[] queues, int numPatients, int numSpecialties) {
        mutex = new Semaphore(1);
        patientReady = new Semaphore(0);
        doctorDone = new Semaphore(0);
        patientDone = new Semaphore(0);
        patientRegistered = new Semaphore(0);
        triageValidated = new Semaphore(0);
        NUMPATIENTS = numPatients;
        NUMSPECIALTIES = numSpecialties;
        queueSemaphores = new Semaphore[NUMSPECIALTIES];
        caseInQueue = new Semaphore[NUMSPECIALTIES];
        for (int i = 0; i < NUMSPECIALTIES; i++) {
            queueSemaphores[i] = new Semaphore(0);
            caseInQueue[i] = new Semaphore(0);
        }
        rand = new Random();
        entranceQueue = new LinkedBlockingDeque<>();
        totalWaitingTime = 0;
        this.queues = queues;
        nextPatientId = new long[NUMSPECIALTIES];
        numPatientsOutofQueue = new int[NUMPATIENTS];
    }

    public void register(Patient patient) throws InterruptedException {

        // The patient registers their symptomps
        print(patient, 0, ": registers", RED);
        Thread.sleep(rand.nextInt(1000));
        mutex.acquire();
        // The model assigns a specialty and a priority value
        patient.setSpecialty(rand.nextInt(NUMSPECIALTIES));
        patient.setPriority(rand.nextInt(1, 5));

        mutex.release();

        // The triage has to validate the results of the model
        entranceQueue.add(patient);
        patientRegistered.release();

        triageValidated.acquire();
        // Once the triage staff validates the results the patient enters a queue
        entersQueue(patient);

    }

    public void validate(Triage triage) throws InterruptedException {

        // The triage staff checks if the values of the model are correct
        patientRegistered.acquire();
        Patient patient = entranceQueue.poll(1, TimeUnit.SECONDS);
        Thread.sleep(rand.nextInt(1000));
        print(triage, 2, ": The prediction of " + patient.getName() + " has been validated", YELLOW);
        triageValidated.release();

    }

    public void attend(Doctor doctor) throws InterruptedException {

        // The doctor signals their patients that is ready
        print(doctor, 5, ": ready", GREEN);
        takePatientFromQueue(doctor);
        // And waits for the patient to awake
        patientReady.acquire();
        print(doctor, 5, ": treating", GREEN);
        Thread.sleep(rand.nextInt(1000) + 1000L);
        // The treatment is done
        doctorDone.release();
        print(doctor, 5, ": treating done", GREEN);

        patientDone.acquire();
        print(doctor, 5, ": done\n", GREEN);

    }

    public void getsAttended(Patient patient) throws InterruptedException {

        // The patient is the first in the queue
        patientReady.release();

        // Gets treated
        print(patient, 5, ": treating", RED);
        doctorDone.acquire();
        // Signals that they're done
        patientDone.release();
        print(patient, 5, ": treatment done", RED);
        mutex.acquire();
        calculateWaitingTime(patient);
        mutex.release();

    }

    private void calculateWaitingTime(Patient patient) {
        double waitingTime = 0;
        waitingTime = patient.getWaitedTime();
        totalWaitingTime += waitingTime;
        print(patient, 7, "Waited " + waitingTime + " minutes", RESET);
    }

    public void waitUntilYourTurn(Patient patient) throws InterruptedException {

        print(patient, 2, ": waits.", RED);

        // The patient waits until a doctor wakes them up
        patient.getPatientSemaphore().acquire();

        mutex.acquire();
        // The waiting time of the patient is calculated based on the amount of patients
        // that have already been treated
        int waitedTime = patient.getWaitedTime();
        patient.setWaitedTime(waitedTime + 30 * numPatientsOutofQueue[patient.getSpecialty()]);

        // Since the thread exited the queue one more patient is added
        numPatientsOutofQueue[patient.getSpecialty()]++;
        print(patient, 3, ": turn arrived.", RED);
        printWaitingRoom();
        mutex.release();

    }

    private void takePatientFromQueue(Doctor doctor) throws InterruptedException {
        print(doctor, 2, ": about to take a patient from the queue.", GREEN);

        Patient patient = null;
        // Waits until there is a patient in the queue
        caseInQueue[doctor.getSpecialty()].acquire();

        mutex.acquire();
        // Take the patient with the highest priority in the queue
        patient = queues[doctor.getSpecialty()].poll();
        if (patient != null) {
            // If there is a patient, the doctor signals them
            patient.getPatientSemaphore().release();
            print(doctor, 2, ": took patient " + patient.getName() + " from queue.", GREEN);
        } else {
            print(doctor, 2, ": no patients available in the queue.", GREEN);
        }
        mutex.release();
    }

    public void entersQueue(Patient patient) throws InterruptedException {
        mutex.acquire();

        // The patient is added to their assigned specialty's queue
        print(patient, 0, ": has been assigned to queue " + patient.getSpecialty() + " with priority "
                + patient.getPriority(), RED);
        queues[patient.getSpecialty()].add(patient);

        // Signal the doctor that there is a patient in their queue
        caseInQueue[patient.getSpecialty()].release();

        mutex.release();

    }

    private void print(Thread thread, int id, String msg, String color) {

        String gap = new String(new char[id + 1]).replace('\0', '\t');
        System.out.println(gap + color + "🖥️" + id + " " + thread.getName() + ": " + msg + RESET);
    }

    private void printWaitingRoom() {

        String out = new String();
        for (PriorityBlockingQueue<Patient> queue : queues) {
            if (!queue.isEmpty())
                for (Patient patient : queue) {
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
