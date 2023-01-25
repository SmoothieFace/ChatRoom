//package broadcast;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.io.DataInputStream;
import java.io.PrintStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.file.attribute.UserPrincipalNotFoundException;

public class User extends Thread {

	// The user socket
	private static Socket userSocket = null;
	// The output stream
	private static PrintStream output_stream = null;
	// The input stream
	private static BufferedReader input_stream = null;
	private static BufferedReader inputLine = null;
	private static ArrayList<String> friends = new ArrayList<String>();
	private static boolean closed = true;
	public static void main(String[] args) {

		// The default port.
		int portNumber = 58000;
		// The default host.
		String host = "localhost";

		if (args.length < 2) {
			System.out.println("Usage: java User <host> <portNumber>\n"
					+ "Now using host=" + host + ", portNumber=" + portNumber);
		} else {
			host = args[0];
			portNumber = Integer.valueOf(args[1]).intValue();
		}

		/*
		 * Open a socket on a given host and port. Open input and output streams.
		 */
		try {
			userSocket = new Socket(host, portNumber);
			inputLine = new BufferedReader(new InputStreamReader(System.in));
			output_stream = new PrintStream(userSocket.getOutputStream());
			input_stream = new BufferedReader(new InputStreamReader(userSocket.getInputStream()));
		} catch (UnknownHostException e) {
			System.err.println("Don't know about host " + host);
		} catch (IOException e) {
			System.err.println("Couldn't get I/O for the connection to the host "
					+ host);
		}

		/*
		 * If everything has been initialized then we want to write some data to the
		 * socket we have opened a connection to on port portNumber.
		 */
		if (userSocket != null && output_stream != null && input_stream != null) {
			try {                
				// Get user name and join the social net
				System.out.println("Enter your username: ");
                String usernameInput = inputLine.readLine().trim();
				if (usernameInput.length() == 0){
					System.out.println("Please enter a valid username, exiting program");
				}
				else{
					/* ONLY Create a thread to read from the server if the user enters a valid username. */
					closed = false; // When we create a new thread below, it will adopt this static property. Allowing it to to listen on line 110.
					new Thread(new User()).start(); // Notice how we start the server listening thread only after the username is stored by client, but before the username is submitted to server. 
					
					output_stream.println("#join " + usernameInput); //Submit the username
				}
				while (!closed) { //This session can be terminated by the listening thread only, who waits for server #Bye.
					String userInput = inputLine.readLine().trim();
					// Read user input and send protocol message to server
					if(userInput != null && userInput.length() > 0){
						if(userInput.equals("Exit")){
							output_stream.println("#Bye");
							try{
								TimeUnit.MILLISECONDS.sleep(300); // Give time for the server to say #Bye
							} catch(InterruptedException ie){System.err.println(ie);}
						}
						else{
							output_stream.println("#status " + userInput);
						}
					}
					userInput = null;
				}
				/*
				 * Close the output stream, close the input stream, close the socket.
				 */
			} catch (IOException e) {
				System.err.println("IOException:  " + e);
			}
		}
	}
	/*
	 * Create a thread to read from the server.
	 */
	public void run() {
		/*
		 * Keep on reading from the socket till we receive a Bye from the
		 * server. Once we received that then we want to break.
		 */
		String responseLine; //Where I store the servers reponses
		String[] responseLineParsed; //Where I store the servers parsed responses
		try {
			while (!closed) {

				// Display on console based on what protocol message we get from server.
				if(input_stream.ready()){ //If there are bytes to read
					responseLine = input_stream.readLine(); //Let's read the server response
					responseLineParsed = responseLine.split(" "); // Parse it
					switch(responseLineParsed[0]){ // Pattern match the protocol and execute client instructions.
						case "#welcome":
							System.out.println("You have entered the chat (Connection established with server.)");
							break;
						case "#busy":
							System.out.println("Server is busy, try again later.");
							closed = true; // We close the client (terminate the User thread and listening thread)
							break;
						case "#newuser":
							System.out.println(responseLineParsed[1] + " has entered the chat.");
							break;
						case "#newStatus": //Prints the username, then the message after it. 
							System.out.println("["+responseLineParsed[1]+"]: "+String.join(" ",Arrays.copyOfRange(responseLineParsed,2,responseLineParsed.length)));
							break;
						case "#statusPosted":
							System.out.print(" (Message Posted) \n");
							break;
						case "#Bye":
							closed = true; // We close the client (terminate the User thread and listening thread)
							break;
						case "#Leave":
							System.out.println(responseLineParsed[1] + " has left the chat.");
							break;
						default: 
							System.out.println("Server didn't adhere to protocols, closing socket."); 
							closed = true;


					}

				}
			
			}
			//Final step is to close the socket. Once all is done.
			output_stream.close();
			input_stream.close();
			userSocket.close();
		} catch (IOException e) {
			System.err.println("IOException:  " + e);
		}
	}
}



