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
package org.opennars.web.multinar;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.lang.reflect.InvocationTargetException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.parsers.ParserConfigurationException;
import org.opennars.entity.Task;
import org.opennars.io.events.EventEmitter.EventObserver;
import org.opennars.io.events.Events;
import org.opennars.language.CompoundTerm;
import org.opennars.language.Term;
import org.opennars.main.Nar;
import org.xml.sax.SAXException;

public class NarNode extends Nar implements EventObserver  {
    
    /* An extra event for received tasks*/
    public class EventReceivedTask {}
    public class EventReceivedNarsese {}
    
    /* The socket the Nar listens from */
    private DatagramSocket receiveSocket;
    
    @Override
    public long time() {
        return System.currentTimeMillis();
    }
    
    /***
     * Create a Nar node that listens for received tasks from other NarNode instances
     * 
     * @param listenPort
     * @throws SocketException
     * @throws UnknownHostException 
     */
    public NarNode(int listenPort) throws SocketException, UnknownHostException, IOException, InstantiationException, 
            InvocationTargetException, NoSuchMethodException, ParserConfigurationException, IllegalAccessException, SAXException, 
            ClassNotFoundException, ParseException {
        super();
        this.receiveSocket = new DatagramSocket(listenPort, InetAddress.getByName("127.0.0.1"));
        this.event(this, true, Events.TaskAdd.class);
        NarNode THIS = this;
        new Thread() {
            public void run() {
                for(;;) {
                    try {
                        Object ret = THIS.receiveObject();
                        if(ret != null) {
                            if(ret instanceof Task) {
                                THIS.memory.event.emit(EventReceivedTask.class, new Object[]{ret});
                                THIS.addInput((Task) ret, THIS);
                            } else
                            if(ret instanceof String) {
                                THIS.memory.event.emit(EventReceivedNarsese.class, new Object[]{ret});
                                THIS.addInput((String) ret);
                            }
                        }
                    } catch (IOException ex) {
                        Logger.getLogger(NarNode.class.getName()).log(Level.SEVERE, null, ex);
                    } catch (ClassNotFoundException ex) {
                        Logger.getLogger(NarNode.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
            }
        }.start();
    }

    /**
     * Input and derived tasks will be potentially sent
     * 
     * @param event
     * @param args 
     */
    @Override
    public void event(Class event, Object[] args) {
        if(event == Events.TaskAdd.class) {
            Task t = (Task) args[0];
            try {
                sendTask(t);
            } catch (IOException ex) {
                Logger.getLogger(NarNode.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }
    
    /**
     * Send tasks that are above priority threshold and contain the optional mustContainTerm
     * 
     * @param t
     * @throws IOException 
     */
    private void sendTask(Task t) throws IOException {
        ByteArrayOutputStream bStream = new ByteArrayOutputStream();
        ObjectOutput oo = new ObjectOutputStream(bStream); 
        oo.writeObject(t);
        oo.close();
        byte[] serializedMessage = bStream.toByteArray();
        for(TargetNar target : targets) {
            if(t.getPriority() > target.threshold) {
                Term term = t.getTerm();
                boolean isCompound = (term instanceof CompoundTerm);
                boolean searchTerm = target.mustContainTerm != null;
                boolean atomicEqualsSearched =     searchTerm && !isCompound && target.mustContainTerm.equals(term);
                boolean compoundContainsSearched = searchTerm &&  isCompound && ((CompoundTerm) term).containsTermRecursively(target.mustContainTerm);
                if(!searchTerm || atomicEqualsSearched || compoundContainsSearched) {
                    DatagramPacket packet = new DatagramPacket(serializedMessage, serializedMessage.length, target.targetAddress, target.targetPort);
                    target.sendSocket.send(packet);
                    //System.out.println("task sent:" + t);
                }
            }
        }
    }
    
    /**
     * Send Narsese that contains the optional mustContainTerm
     * 
     * @param t
     * @throws IOException 
     */
    private void sendNarsese(String input, TargetNar target) throws IOException {
        ByteArrayOutputStream bStream = new ByteArrayOutputStream();
        ObjectOutput oo = new ObjectOutputStream(bStream); 
        oo.writeObject(input);
        oo.close();
        byte[] serializedMessage = bStream.toByteArray();
        boolean searchTerm = target.mustContainTerm != null;
        boolean containsFound = searchTerm && input.contains(target.mustContainTerm.toString());
        if(!searchTerm || containsFound) {
            DatagramPacket packet = new DatagramPacket(serializedMessage, serializedMessage.length, target.targetAddress, target.targetPort);
            target.sendSocket.send(packet);
            //System.out.println("narsese sent:" + input);
        }
    }

    public class TargetNar {
        
        /**
         * The target Nar node, specifying under which conditions the current Nar node redirects tasks to it.
         * 
         * @param targetIP
         * @param targetPort
         * @param threshold
         * @param mustContainTerm
         * @throws SocketException
         * @throws UnknownHostException 
         */
        public TargetNar(final String targetIP, final int targetPort, final float threshold, Term mustContainTerm) throws SocketException, UnknownHostException {
            this.targetAddress = InetAddress.getByName(targetIP);
            this.sendSocket = new DatagramSocket();
            this.threshold = threshold;
            this.targetPort = targetPort;
            this.mustContainTerm = mustContainTerm;
        }
        final float threshold;
        final DatagramSocket sendSocket;
        final int targetPort;
        final InetAddress targetAddress;
        final Term mustContainTerm;
    }
    
    private List<TargetNar> targets = new ArrayList<>();
    /**
     * Add another target Nar node to redirect tasks to, and under which conditions.
     * 
     * @param targetIP The target Nar node IP
     * @param targetPort The target Nar node port
     * @param taskThreshold The threshold the priority of the task has to have to redirect
     * @param mustContainTerm The optional term that needs to be contained recursively in the task term
     * @throws SocketException
     * @throws UnknownHostException 
     */
    public void addRedirectionTo(final String targetIP, final int targetPort, final float taskThreshold, Term mustContainTerm) throws SocketException, UnknownHostException {
        targets.add(new TargetNar(targetIP, targetPort, taskThreshold, mustContainTerm));
    }
 
    /***
     * NarNode's receiving a task
     * 
     * @return
     * @throws IOException
     * @throws ClassNotFoundException 
     */
    private Object receiveObject() throws IOException, ClassNotFoundException {
        byte[] recBytes = new byte[100000];
        DatagramPacket packet = new DatagramPacket(recBytes, recBytes.length);
        receiveSocket.receive(packet);
        ObjectInputStream iStream = new ObjectInputStream(new ByteArrayInputStream(recBytes));
        Object msg = iStream.readObject();
        iStream.close();
        return msg;
    }
    
    
    /**
     * An example with one NarNode sending a task to another NarNode
     * 
     * @param args
     * @throws SocketException
     * @throws UnknownHostException
     * @throws IOException
     * @throws InterruptedException 
     */
    public static void main(String[] args) throws SocketException, UnknownHostException, IOException, 
            InterruptedException, InstantiationException, InvocationTargetException, ParserConfigurationException, 
            NoSuchMethodException, SAXException, ClassNotFoundException, IllegalAccessException, ParseException {
        int nar1port = 64001;
        int nar2port = 64002;
        String localIP = "127.0.0.1";
        NarNode nar1 = new NarNode(nar1port);
        NarNode nar2 = new NarNode(nar2port);
        nar1.addRedirectionTo(localIP, nar2port, 0.5f, null);
        //nar2.connectTo(localIP, nar1port, 0.5f);
        nar2.event(new EventObserver() {
            @Override
            public void event(Class event, Object[] args) {
                if(event == EventReceivedTask.class) {
                    Task task = (Task) args[0];
                    System.out.println("received task event triggered in nar2: " + task);
                    System.out.println("success");
                }
            }
        }, true, EventReceivedTask.class);
        System.out.println("High priority task occurred in nar1");
        nar1.addInput("<{task1} --> [great]>.");
        Thread.sleep(5000);
        System.exit(0);
    }
    
}
