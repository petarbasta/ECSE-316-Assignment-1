import java.nio.ByteBuffer;

public class DnsRequest {

	private String domainName;
	private QueryType queryType;
	private byte[] randomID;

	public DnsRequest(String domain, QueryType queryType, byte[] randomID) {
		this.domainName = domain;
		this.queryType = queryType;
		this.randomID = randomID;
	}

	public byte[] createHeader() {
		ByteBuffer header = ByteBuffer.allocate(12);

		// Random 16 bit ID for each request
		header.put(this.randomID);

		// QR + OPCODE + AA + TC + RD
		header.put((byte) 0x10000001);

		// RA + Z + RCODE
		header.put((byte) 0);

		// Both Bytes QDCount
		header.put((byte) 0);
		header.put((byte) 1);

		// Both Bytes ANCOUNT
		header.put((byte) 0);
		header.put((byte) 0);

		// Both bytes NSCOUNT
		header.put((byte) 0);
		header.put((byte) 0);

		// Both bytes ARCOUNT
		header.put((byte) 0);
		header.put((byte) 0);

		return header.array();
	}

	public byte[] createQuestions(int nameLength) {
		// Size = QNAME + QTYPE + QCLASS
		ByteBuffer question = ByteBuffer.allocate(nameLength + 5);

		// QNAME
		// Split domain into labels
		String[] labels = domainName.split("\\.");
		// For each label
		for (String label : labels) {
			// Put label length
			question.put((byte) label.length());
			for (int j = 0; j < label.length(); j++) {
				// Put label
				question.put((byte) ((int) label.charAt(j)));
			}
		}
		// Finish with Null label of root
		question.put((byte) 0x00);

		// First byte of QTYPE
		question.put((byte) 0x00);

		// Second byte of QTYPE
		switch (queryType) {
		case A:
			question.put((byte) 1);
			break;
		case NS:
			question.put((byte) 2);
			break;
		case MX:
			question.put((byte) 0x00f);
			break;
		default:
			break;
		}

		// First byte of QCLASS
		question.put((byte) 0x00);

		// Second byte of QCLASS
		question.put((byte) 0x0001);

		return question.array();
	}

	public int getDomainNameLength() {
		int byteLength = 0;
		String[] labels = domainName.split("\\.");
		for (String label : labels) {
			byteLength += label.length() + 1;
		}
		return byteLength;
	}

	public byte[] getRequest() {
		int nameLength = getDomainNameLength();
		// Allocate space for header + questions + query name
		ByteBuffer request = ByteBuffer.allocate(17 + nameLength);
		request.put(createHeader());
		request.put(createQuestions(nameLength));
		return request.array();
	}
}
