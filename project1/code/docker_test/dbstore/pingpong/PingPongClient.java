import rmi.*;

public class PingPongClient {
	public String pingServer(String ip, int port ) throws RMIException {
		StringMethodInterface server = PingServerFactory.makePingServer(ip, port);
        return server.ping(291);
	}

	public static void main(String[] args) {
		String ip = args[0];
		int port = Integer.parseInt( args[1] );
		PingPongClient client = new PingPongClient();

		int failed = 0;
		for (int i=0;i<4;i++) {
			try {
				String rntValue = client.pingServer(ip, port);
			} catch (RMIException e){
				failed++;
			}
		}
		System.out.println( 4-failed + " Tests Completed, "+ failed + " Tests Failed.");
	}
	
}