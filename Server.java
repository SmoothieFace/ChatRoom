//package broadcast;

import java.io.DataInputStream;
import java.io.PrintStream;
import java.io.IOException;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.Socket;
import java.net.ServerSocket;
import java.util.*;

/*
 * A server that delivers status messages to other users.
 */
public class Server {

	// Create a socket for the server 
	private static ServerSocket serverSocket = null;
	// Create a socket for the server 
	private static Socket userSocket = null;
	// Maximum number of users 
	private static int maxUsersCount = 5;
	// An array of threads for users
	private static  userThread[] threads = null;

	public static void main(String args[]) {

		// The default port number.
		int portNumber = 58000;
		if (args.length < 2) {
			System.out.println("Usage: java Server <portNumber>\n"
					+ "Now using port number=" + portNumber + "\n" +
					"Maximum user count=" + maxUsersCount);
		} else {
			portNumber = Integer.valueOf(args[0]).intValue();
			maxUsersCount = Integer.valueOf(args[1]).intValue();
		}

		System.out.println("Server now using port number=" + portNumber + "\n" + "Maximum user count=" + maxUsersCount);
		
		
		userThread[] threads = new userThread[maxUsersCount];


		/*
		 * Open a server socket on the portNumber (default 8000). 
		 */
		try {
			serverSocket = new ServerSocket(portNumber);
		} catch (IOException e) {
			System.out.println(e);
		}

		/*
		 * Create a user socket for each connection and pass it to a new user
		 * thread.
		 */
		while (true) {
			try {
				userSocket = serverSocket.accept();
				int i = 0;
				for (i = 0; i < maxUsersCount; i++) {
					if (threads[i] == null) {
						threads[i] = new userThread(userSocket, threads);
						threads[i].start();
						break;
					}
				}
				if (i == maxUsersCount) {
					PrintStream output_stream = new PrintStream(userSocket.getOutputStream());
					output_stream.println("#busy");
					output_stream.close();
					userSocket.close();
				}
			} catch (IOException e) {
				System.out.println(e);
			}
		}
	}
}

/*
 * Threads
 */
class userThread extends Thread {

	private String userName = null;
	private BufferedReader input_stream = null;
	private PrintStream output_stream = null;
	private Socket userSocket = null;
	private final userThread[] threads;
	private int maxUsersCount;

	public userThread(Socket userSocket, userThread[] threads) {
		this.userSocket = userSocket;
		this.threads = threads;
		maxUsersCount = threads.length;
	}

	public void run() {
		int maxUsersCount = this.maxUsersCount;
		userThread[] threads = this.threads;

		try {
			/*
			 * Create input and output streams for this client.
			 * Read user name.
			 */
			output_stream = new PrintStream(userSocket.getOutputStream());
			input_stream = new BufferedReader(new InputStreamReader(userSocket.getInputStream()));
			
			//Wait for the user to type their username.
			while(!input_stream.ready()){
			}
			//Parse the username, check if it follows protocol.
			String[] usernameOK = (input_stream.readLine()).split(" ");
			if(usernameOK[0].equals("#join")){
				userName = usernameOK[1];
				output_stream.println("#welcome");
				//Broadcast the new user to the existing users.
				synchronized (userThread.class){
					for (int i = 0; i < maxUsersCount; i++) {
						if (threads[i] != null && threads[i] != this) {
							PrintStream temp = new PrintStream(threads[i].userSocket.getOutputStream());
							temp.println("#newuser "+userName);
						}
					}
				}
			}
			/* Start the conversation. */
			//Here I use userName as the condition for the loop, so that people who passed the last step can proceed.
			//Additionally I will use 'userName = null' as a way to terminate the loop in the future. (E.g. line 159/163)
			while (userName != null) {
				if(input_stream.ready()){ // When the client sends something
					String clientRequest = input_stream.readLine();
					String[] clientRequestParsed = clientRequest.split(" "); // Parse it
					switch(clientRequestParsed[0]){ // Determine the protocol
						case "#status": // Broadcast the status to all users
						    synchronized(userThread.class){
								for (int i = 0; i < maxUsersCount; i++) {
									if (threads[i] != null && threads[i] != this) {
										PrintStream temp = new PrintStream(threads[i].userSocket.getOutputStream());
										temp.println("#newStatus "+userName + " "+clientRequest.substring(8)); 
									}
								}
							}
							output_stream.println("#statusPosted");
							break;
						case "#Bye": // Broadcast the exit of the user to all other users
							output_stream.println("#Bye");//Tell the user to close their side.
							synchronized(userThread.class){ // Broadcast to all users the user who left
								for (int i = 0; i < maxUsersCount; i++) {
									if (threads[i] != null && threads[i] != this) {
										PrintStream temp = new PrintStream(threads[i].userSocket.getOutputStream());
										temp.println("#Leave "+userName); 
									}
								}
							}
							userName = null;
							break;
						default: // If the client sends an unknown protocol, we break the connection.
							System.out.println("Client didn't adhere to protocols, closing socket."); 
							userName = null;
							break;

					}

				}	
				
			}
			// conversation ended.

			/*
			 * Clean up. Set the current thread variable to null so that a new user
			 * could be accepted by the server.
			 */
			synchronized (userThread.class) {
				for (int i = 0; i < maxUsersCount; i++) {
					if (threads[i] == this) {
						threads[i] = null;
					}
				}
			}
			/*
			 * Close the output stream, close the input stream, close the socket.
			 */
			input_stream.close();
			output_stream.close();
			userSocket.close();
		} catch (IOException e) {
		}
	}
}




