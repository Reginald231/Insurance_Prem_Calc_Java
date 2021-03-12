import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.*;
import java.util.Scanner;

public class scraper {

    public static void start() {
        homeowner h = new homeowner();
        System.out.println("Enter thread pool limit. (Warning, a driver will be instantiated for each thread.): ");

        Scanner scan = new Scanner(System.in);

        final int THREAD_LIM = scan.nextInt();

        ArrayList<Task> tasks = new ArrayList<Task>();
        ExecutorService pool =  Executors.newFixedThreadPool(THREAD_LIM);
            for (int i = 0; i < h.zipCodes.size(); i++){
                tasks.add(new Task(h, h.zipCodes.get(i)));
            }

        for (Task task : tasks) {
            pool.execute(task);
//            System.out.println("Created new execution");
        }
        pool.shutdown();
        try {
            pool.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
        } catch (InterruptedException e) {
            System.out.println("Thread interrupted.");
        }

            System.out.println("Thread pool shut down.");
        h.buildSpreadsheet();
        System.out.println("Done.");
    }
    public static void main(String [] args){
        scraper.start();
    }
}
class Task implements Runnable{
    homeowner h;
    String zip;
    public Task(homeowner h, String zip){
        this.h = h;
        this.zip = zip;
    }
    @Override
    public void run() {
            this.zip = this.zip.replace("\"", "");
//            System.out.println("Running...");
            this.h.getAnnualPremiums(this.zip);
    }
}
