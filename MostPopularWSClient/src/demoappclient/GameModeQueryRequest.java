package demoappclient;


public class GameModeQueryRequest implements Runnable {
	private String country = null;
	private String gamemode = null;
	public GameModeQueryRequest(String Country, String GameMode){
		country = Country;
		gamemode = GameMode;
	}
	@Override
	public synchronized void run() {
		try {
				SimpleTestClient.fetchMostPopular(country, gamemode);
		} catch (Exception e) {
			System.out.println(Thread.currentThread().getName() + "failed");
		}
	}

}
