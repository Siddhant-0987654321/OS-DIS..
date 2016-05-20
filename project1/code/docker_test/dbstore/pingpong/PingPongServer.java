import rmi.*;

import java.net.*;


public class PingPongServer implements StringMethodInterface{
    @Override
    public String ping(int idNumber) throws RMIException {
        return "Pong " + idNumber;
    }

    public static void main(String[] args) throws Exception {
        int port = Integer.parseInt( args[0] );

        StringMethodInterface server = new PingPongServer();
        Skeleton<StringMethodInterface> skeleton = new Skeleton<StringMethodInterface>(StringMethodInterface.class, server,
                new InetSocketAddress(InetAddress.getLocalHost().getHostAddress(), port));
        try {
            skeleton.start();
        } catch (RMIException e) {
            e.printStackTrace();
        }
        System.out.println("Server starts");
    }
}
