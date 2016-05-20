import rmi.*;

import java.net.*;

public class PingServerFactory {
    public static StringMethodInterface makePingServer(String ip, int port){
    	InetSocketAddress address = new InetSocketAddress(ip, port);
        return Stub.create(StringMethodInterface.class, address );
    }

}