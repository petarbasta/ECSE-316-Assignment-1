import java.io.IOException;
import java.net.*;
import java.util.Arrays;
import java.util.ListIterator;
import java.util.NoSuchElementException;
import java.util.Random;

public class DnsClient {

	static int port = 53;
	static QueryType queryType = QueryType.A;
	static int timeout = 5000;
	static int maxRetries = 3;
	static byte[] serverIP = new byte[4];
	static String address;
	static String domainName;
	static int attemptNumber = 0;

	public static void main(String args[]) {
		// Validate input
		try {
			checkInput(args);
		} catch (Exception e) {
			throw new IllegalArgumentException("ERROR \t Incorrect input syntax: Check your arguments");
		}

		// Print Request info
		System.out.println("DnsClient sending request for " + domainName);
		System.out.println("Server: " + address);
		System.out.println("Request type: " + queryType);

		makeRequest();
	}

	public static void makeRequest() {
		if (attemptNumber >= maxRetries) {
			System.out.println("ERROR \t Maximum number of retries " + maxRetries + " exceeded");
			return;
		}

		try {
			DatagramSocket socket = new DatagramSocket();

			// Set timeout
			socket.setSoTimeout(timeout);

			// Get address from IP
			InetAddress inetaddress = InetAddress.getByAddress(serverIP);
			
			// Create random ID
			byte[] randomID = new byte[2];
			new Random().nextBytes(randomID);
			
			DnsRequest request = new DnsRequest(domainName, queryType, randomID);

			byte[] requestData = request.getRequest();
			byte[] responseData = new byte[1024];

			// Create Datagrams
			DatagramPacket requestPacket = new DatagramPacket(requestData, requestData.length, inetaddress, port);
			DatagramPacket responsePacket = new DatagramPacket(responseData, responseData.length);

			// Log response time
			long startTime = System.currentTimeMillis();
			socket.send(requestPacket);
			socket.receive(responsePacket);
			long endTime = System.currentTimeMillis();
			socket.close();

			System.out.println("Response received after " + (endTime - startTime) / 1000.0 + " seconds " + "("
					+ attemptNumber + " retries)");
			System.out.println();

			// Unpack response
			DnsResponse response = new DnsResponse(responsePacket.getData(), requestData.length, queryType, randomID);
			response.printRecords();

		} catch (SocketTimeoutException e) {
			System.out.println("ERROR \t Socket timed out. Trying again...");
			attemptNumber++;
			makeRequest();
		} catch (IOException e) {
			System.out.println("ERROR \t IOException occured");
		}
	}

	public static void checkInput(String args[]) {
		ListIterator<String> iter = Arrays.asList(args).listIterator();

		while (iter.hasNext()) {
			String argument = iter.next();
			switch (argument) {
			case "-t":
				timeout = Integer.parseInt(iter.next()) * 1000;
				break;
			case "-r":
				maxRetries = Integer.parseInt(iter.next());
				break;
			case "-p":
				port = Integer.parseInt(iter.next());
				break;
			case "-mx":
				queryType = QueryType.MX;
				break;
			case "-ns":
				queryType = QueryType.NS;
				break;
			default:

				if (argument.charAt(0) == '@') {
					address = argument.substring(1);
					String[] labels = address.split("\\.");

					for (int i = 0; i < labels.length; i++) {
						int ipPart = Integer.parseUnsignedInt(labels[i]);
						serverIP[i] = (byte) ipPart;
					}

					try {
						domainName = iter.next();
					} catch (NoSuchElementException e) {
						System.out.println("No domain name given");
						System.exit(1);
					}
				}
				break;
			}
		}

		if (serverIP == null || domainName == null) {
			throw new IllegalArgumentException("ERROR \t Missing server IP or domain name");
		}
	}

}
