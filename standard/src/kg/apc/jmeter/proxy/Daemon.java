package kg.apc.jmeter.proxy;

/**
 * Created by dbollaer on 5/21/15.
 */

import org.apache.jorphan.logging.LoggingManager;
import org.apache.jorphan.util.JOrphanUtils;
import org.apache.log.Logger;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;


/**
 * Created by dbollaer on 5/21/15.
 */
import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.apache.jmeter.gui.Stoppable;
import org.apache.jmeter.protocol.http.proxy.ProxyControl;
import org.apache.jorphan.logging.LoggingManager;
import org.apache.jorphan.util.JOrphanUtils;
import org.apache.log.Logger;

/**
 * Web daemon thread. Creates main socket on port 8080 and listens on it
 * forever. For each client request, creates a Proxy thread to handle the
 * request.
 *
 */
public class Daemon extends Thread implements Stoppable {

    private static final Logger log = LoggingManager.getLoggerForClass();

    /**
     * The time (in milliseconds) to wait when accepting a client connection.
     * The accept will be retried until the Daemon is told to stop. So this
     * interval is the longest time that the Daemon will have to wait after
     * being told to stop.
     */
    private static final int ACCEPT_TIMEOUT = 1000;

    /** The port to listen on. */
    private final int daemonPort;

    private final ServerSocket mainSocket;

    /** True if the Daemon is currently running. */
    private volatile boolean running;

    /** The target which will receive the generated JMeter test components. */
    private final ProxyControlDummy target;

    /**
     * The proxy class which will be used to handle individual requests. This
     * class must be the {@link Proxy} class or a subclass.
     */
    private final Class<? extends Proxy> proxyClass;

    /**
     * Create a new Daemon with the specified port and target.
     *
     * @param port
     *            the port to listen on.
     * @param target
     *            the target which will receive the generated JMeter test
     *            components.
     * @throws IOException if an I/O error occurs opening the socket
     * @throws IllegalArgumentException if <code>port</code> is outside the allowed range from <code>0</code> to <code>65535</code>
     * @throws SocketException when something is wrong on the underlying protocol layer
     */
    public Daemon(int port, ProxyControlDummy target) throws IOException {
        this(port, target, Proxy.class);
    }

    /**
     * Create a new Daemon with the specified port and target, using the
     * specified class to handle individual requests.
     *
     * @param port
     *            the port to listen on.
     * @param target
     *            the target which will receive the generated JMeter test
     *            components.
     * @param proxyClass
     *            the proxy class to use to handle individual requests. This
     *            class must be the {@link Proxy} class or a subclass.
     * @throws IOException if an I/O error occurs opening the socket
     * @throws IllegalArgumentException if <code>port</code> is outside the allowed range from <code>0</code> to <code>65535</code>
     * @throws SocketException when something is wrong on the underlying protocol layer
     */
    public Daemon(int port, ProxyControlDummy target, Class<? extends Proxy> proxyClass) throws IOException {
        super("HTTP PROXY");
        this.target = target;
        this.daemonPort = port;
        this.proxyClass = proxyClass;
        log.info("Creating Daemon Socket on port: " + daemonPort);
        mainSocket = new ServerSocket(daemonPort);
        mainSocket.setSoTimeout(ACCEPT_TIMEOUT);
    }

    /**
     * Listen on the daemon port and handle incoming requests. This method will
     * not exit until {@link #stopServer()} is called or an error occurs.
     */
    @Override
    public void run() {
        running = true;
        log.info("Test Script Recorder up and running!");

        // Maps to contain page and form encodings
        // TODO - do these really need to be shared between all Proxy instances?
        Map<String, String> pageEncodings = Collections.synchronizedMap(new HashMap<String, String>());
        Map<String, String> formEncodings = Collections.synchronizedMap(new HashMap<String, String>());

        try {
            while (running) {
                try {
                    // Listen on main socket
                    Socket clientSocket = mainSocket.accept();
                    if (running) {
                        // Pass request to new proxy thread
                        Proxy thd = proxyClass.newInstance();
                        thd.configure(clientSocket, target, pageEncodings, formEncodings);
                        thd.start();
                    }
                } catch (InterruptedIOException e) {
                    continue;
                    // Timeout occurred. Ignore, and keep looping until we're
                    // told to stop running.
                }
            }
            log.info("HTTP(S) Test Script Recorder stopped");
        } catch (Exception e) {
            log.warn("HTTP(S) Test Script Recorder stopped", e);
        } finally {
            JOrphanUtils.closeQuietly(mainSocket);
        }

        // Clear maps
        pageEncodings = null;
        formEncodings = null;
    }

    /**
     * Stop the proxy daemon. The daemon may not stop immediately.
     *
     * see #ACCEPT_TIMEOUT
     */
    @Override
    public void stopServer() {
        running = false;
    }
}
