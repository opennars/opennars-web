/**
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.opennars.web.httpnar;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.InetSocketAddress;
import java.text.ParseException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.parsers.ParserConfigurationException;
import org.java_websocket.WebSocket;
import org.java_websocket.WebSocketImpl;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;
import org.opennars.interfaces.pub.Reasoner;
import org.opennars.main.Nar;
import org.xml.sax.SAXException;

public class NARServer  {

    private static final int DEFAULT_WEBSOCKET_PORT = 10000;
    static final boolean WEBSOCKET_DEBUG = false;
    
    private static int cycleIntervalMS = 50;
    
    class NARSWebSocketServer extends WebSocketServer  {

        public NARSWebSocketServer(InetSocketAddress addr) {
            super(addr);
        }

        @Override
        public void onStart() {
        }

        public Reasoner nar;
        
        @Override
        public void onOpen(final WebSocket conn, ClientHandshake handshake) {
            //this.sendToAll("new connection: " + handshake.getResourceDescriptor());

            WebSocketImpl.DEBUG = WEBSOCKET_DEBUG;

            if (WEBSOCKET_DEBUG) System.out.println("Connect: " + conn.getRemoteSocketAddress().getAddress().getHostAddress());

            if(nar == null) {
                try {
                    nar = new Nar();
                } catch (IOException ex) {
                    Logger.getLogger(NARServer.class.getName()).log(Level.SEVERE, null, ex);
                } catch (InstantiationException ex) {
                    Logger.getLogger(NARServer.class.getName()).log(Level.SEVERE, null, ex);
                } catch (InvocationTargetException ex) {
                    Logger.getLogger(NARServer.class.getName()).log(Level.SEVERE, null, ex);
                } catch (NoSuchMethodException ex) {
                    Logger.getLogger(NARServer.class.getName()).log(Level.SEVERE, null, ex);
                } catch (ParserConfigurationException ex) {
                    Logger.getLogger(NARServer.class.getName()).log(Level.SEVERE, null, ex);
                } catch (IllegalAccessException ex) {
                    Logger.getLogger(NARServer.class.getName()).log(Level.SEVERE, null, ex);
                } catch (SAXException ex) {
                    Logger.getLogger(NARServer.class.getName()).log(Level.SEVERE, null, ex);
                } catch (ClassNotFoundException ex) {
                    Logger.getLogger(NARServer.class.getName()).log(Level.SEVERE, null, ex);
                } catch (ParseException ex) {
                    Logger.getLogger(NARServer.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
            final NARConnection n = new NARConnection(nar, cycleIntervalMS) {
                @Override public void println(String output) {
                    conn.send(output);
                }
            };
            socketSession.put(conn, n);        

        }

        @Override
        public void onClose(WebSocket conn, int code, String reason, boolean remote) {
            if (WEBSOCKET_DEBUG) System.out.println(conn + " disconnected");

            NARConnection n = socketSession.get(conn);
            if (n!=null) {
                n.stop();
                socketSession.remove(conn);
            }
        }

        @Override
        public void onMessage(WebSocket conn, String message) {

            NARConnection n = socketSession.get(conn);
            if (n!=null) {
                n.read(message);
            }
        }


        @Override
        public void onError(WebSocket conn, Exception ex) {
            ex.printStackTrace();
            if (conn != null) {
                // some errors like port binding failed may not be assignable to a specific websocket
            }
        }
        
    }
    
    final NARSWebSocketServer websockets;
    private final Map<WebSocket, NARConnection> socketSession = new HashMap();

    public NARServer(int httpPort, int webSocketsPort) throws IOException {
        websockets = new NARSWebSocketServer(new InetSocketAddress(webSocketsPort));
        websockets.start();
        
        new HTTPServeFiles(httpPort, new File("nars_web/client"));
        
    }



    public static void main(String[] args) throws Exception {
                
        int httpPort;
        int wsPort = DEFAULT_WEBSOCKET_PORT;
        
        String nlpHost = null;
        int nlpPort = 0;
        
        if (args.length < 1) {
            System.out.println("Usage: NARServer <httpPort> [nlpHost nlpPort] [cycleIntervalMS]");
            
            return;
        }
        else {
            httpPort = Integer.parseInt(args[0]);
            
            if (args.length >= 3) {
                nlpHost = args[1];
                if (!"null".equals(args[2])) {
                    nlpPort = Integer.parseInt(args[2]);
                    //nlp = new NLPInputParser(nlpHost, nlpPort);
                }
            }
            if (args.length >= 4) {
                cycleIntervalMS = Integer.parseInt(args[3]);
            }
        }
                
        NARServer s = new NARServer(httpPort, wsPort);
        
        System.out.println("NARS Web Server ready. port: " + httpPort + ", websockets port: " + wsPort);
        System.out.println("  Cycle interval (ms): " + cycleIntervalMS);
        /*if (nlp!=null) {
            System.out.println("  NLP enabled, using: " + nlpHost + ":" + nlpPort);            
        }*/

    }


    
    /**
     * Sends <var>text</var> to all currently connected WebSocket clients.
     *
     * @param text The String to send across the network.
     * @throws InterruptedException When socket related I/O errors occur.
     */
    /*public void sendToAll(String text) {
        Collection<WebSocket> con = connections();
        synchronized (con) {
            for (WebSocket c : con) {
                c.send(text);
            }
        }
    }*/



}
