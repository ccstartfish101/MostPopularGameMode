package demoappclient;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Random;


public class SimpleTestClient {
	private static final String DEMOAPP_URL = "http://localhost:8888/mostpopularws";
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		correctnessTest();
		stressTest();
	}
	public static void stressTest()
	{
		int numberOfRequests = 100;
		int round = 5;
		double requestsPerSecond = 0;
		for (int i=0; i < round; i++){
			requestsPerSecond =
					(double)numberOfRequests/dispatchRequests(numberOfRequests);
			numberOfRequests *=2;
		}
		System.out.println("Average number of requests per second : " + requestsPerSecond);
	}
	public static double dispatchRequests(int numberofRequests){
		ArrayList<Thread> threads = new ArrayList<Thread>();
		String[] gameModes = {"Single", "Multi", "Dual", "Group"};
		Random rn = new Random();
		
		long startTime = System.currentTimeMillis();
        
		for (int i = 0; i < numberofRequests; i++) {
        	char[] c = {(char)(rn.nextInt(26) + 'A'), (char)(rn.nextInt(26) + 'A')};
        	String countryCode = String.valueOf(c);
            Runnable task = new GameModeQueryRequest(countryCode, gameModes[rn.nextInt(4)]);
            Thread worker = new Thread(task);
            // We can set the name of the thread
            worker.setName("Thread - " + String.valueOf(i));
            // Start the thread, never call method run() direct
            worker.start();
            // Remember the thread for later usage
            threads.add(worker);
        }
        for (int i = 0; i < numberofRequests; i++){
        	try {
				threads.get(i).join();
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
        }
		long endTime   = System.currentTimeMillis();
		long totalTime = endTime - startTime;
		System.out.println("Handling "+ numberofRequests +" requests at one time costs :" + (double)totalTime/1000.0 + "seconds");
		return (double)totalTime/1000.0;
	}
	public static void correctnessTest()
	{
		try {
			assert("MostPopular : Single".equals( fetchMostPopular("US","Single")));
			assert("MostPopular : Single".equals( fetchMostPopular("US","Single")));
			assert("MostPopular : Single".equals( fetchMostPopular("US","Multi")));
			assert("MostPopular : Multi".equals( fetchMostPopular("US","Multi")));
			assert("MostPopular : Dual".equals( fetchMostPopular("UK","Dual")));
			assert("MostPopular : Single".equals( fetchMostPopular("UK","Single")));
			assert("MostPopular : Multi".equals( fetchMostPopular("FR","Multi")));
			assert(fetchMostPopular("JP", null) == null);
			assert("MostPopular : Single".equals( fetchMostPopular("JP","Single")));
			System.out.println("Test done");
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}		
	}
    public static String fetchMostPopular(String Country, String GameMode) throws Exception {

        URL url = new URL(DEMOAPP_URL);
        HttpURLConnection conn = (HttpURLConnection)url.openConnection();
        conn.setRequestMethod("GET");
        conn.setRequestProperty("Cookie", "Country=US;GameMode=Single");
        conn.connect();

        BufferedReader in = new BufferedReader(
                                    new InputStreamReader(
                                    conn.getInputStream()));
        String respString = null;
        try {
        	respString = in.readLine();
        } catch (Throwable t){
        	in.close();
        }
        in.close();
        return respString;
    }
}
