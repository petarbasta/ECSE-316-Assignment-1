import java.net.InetAddress;
import java.nio.ByteBuffer;

public class DnsResponse {
	private byte[] response;
	private Record[] answers;
	private Record[] additionalRecords;
	private boolean QR, AA;
	private int RCODE, ANCOUNT, NSCOUNT, ARCOUNT;

	public DnsResponse(byte[] response, int length, QueryType queryType) {
		this.response = response;

		// Make sure response matches request type
		checkQueryTypeMatches(queryType);

		// Read packet header
		this.getHeader();

		// Make sure no errors in response
		try {
			this.validateRCODE();
		} catch (RuntimeException e) {
			return;
		}

		// Populate answers
		answers = new Record[ANCOUNT];
		int index = length;
		for (int i = 0; i < ANCOUNT; i++) {
			answers[i] = this.getRecord(index);
			index += answers[i].getByteLength();
		}

		// Ignore NSCOUNT
		for (int i = 0; i < NSCOUNT; i++) {
			index += getRecord(index).getByteLength();
		}

		// Populate additional records
		additionalRecords = new Record[ARCOUNT];
		for (int i = 0; i < ARCOUNT; i++) {
			additionalRecords[i] = this.getRecord(index);
			index += additionalRecords[i].getByteLength();
		}
	}

	private void checkQueryTypeMatches(QueryType queryType) {
		// Check that response matches query sent
		int counter = 12;
		while (this.response[counter] != 0) {
			counter++;
		}
		byte[] queryTypeBytes = { response[counter + 1], response[counter + 2] };
		if (this.getQueryType(queryTypeBytes) != queryType) {
			throw new RuntimeException("ERROR \t Response does not match request sent");
		}
	}

	public void printRecords() {
		if (this.ANCOUNT == 0) {
			System.out.println("NOTFOUND");
		} else {
			System.out.println("***Answer Section (" + this.ANCOUNT + " records)***");

			for (Record record : answers) {
				record.outputRecord();
			}
			System.out.println();

			if (this.ARCOUNT > 0) {

				System.out.println("***Additional Section (" + this.ARCOUNT + " records)***");

				for (Record record : additionalRecords) {
					record.outputRecord();
				}
			}
		}
	}

	public void getHeader() {
		// QR
		this.QR = ((response[2] >> 7) & 1) == 1;

		if (!this.QR) {
			throw new RuntimeException("ERROR \t Packet is not a response");
		}

		// AA
		this.AA = ((response[2] >> 2) & 1) == 1;

		// RCODE
		this.RCODE = response[3] & 0x0F;

		// ANCount
		byte[] ANCount = { response[6], response[7] };
		this.ANCOUNT = ByteBuffer.wrap(ANCount).getShort();

		// NSCount
		byte[] NSCount = { response[8], response[9] };
		this.NSCOUNT = ByteBuffer.wrap(NSCount).getShort();

		// ARCount
		byte[] ARCount = { response[10], response[11] };
		this.ARCOUNT = ByteBuffer.wrap(ARCount).getShort();
	}

	public Record getRecord(int index) {
		Record answer = new Record(this.AA);
		int counter = index + getBytesByIndex(index);

		// QTYPE
		byte[] bytesQTYPE = new byte[2];
		bytesQTYPE[0] = response[counter];
		bytesQTYPE[1] = response[counter + 1];
		answer.setQueryType(getQueryType(bytesQTYPE));
		counter += 2;

		// CLASS
		byte[] bytesCLASS = new byte[2];
		bytesCLASS[0] = response[counter];
		bytesCLASS[1] = response[counter + 1];

		if (bytesCLASS[0] != 0 && bytesCLASS[1] != 1) {
			throw new RuntimeException(("ERROR \t Class field is not 1"));
		}
		counter += 2;

		// TTL
		byte[] bytesTTL = { response[counter], response[counter + 1], response[counter + 2], response[counter + 3] };
		answer.setTTL(ByteBuffer.wrap(bytesTTL).getInt());
		counter += 4;

		// RDLENGTH
		byte[] bytesRDLength = { response[counter], response[counter + 1] };
		int RDLENGTH = ByteBuffer.wrap(bytesRDLength).getShort();
		counter += 2;

		switch (getQueryType(bytesQTYPE)) {
		case A:
			answer.setDomain(getDomainAType(counter));
			break;
		case NS:
			answer.setDomain(getDomainByIndex(counter));
			break;
		case MX:
			answer.setDomain(getDomainByIndex(counter + 2));
			break;
		case CNAME:
			answer.setDomain(getDomainByIndex(counter));
			break;
		}
		answer.setLengthBytes(counter - index + RDLENGTH);
		return answer;
	}

	public String getDomainAType(int counter) {
		String address = "";
		byte[] byteAddress = { response[counter], response[counter + 1], response[counter + 2], response[counter + 3] };
		try {
			InetAddress inetaddress = InetAddress.getByAddress(byteAddress);
			address = inetaddress.toString().substring(1);
		} catch (Exception e) {
			System.out.println("Error getting domain");
		}
		return address;
	}

	public String getDomainByIndex(int index) {
		String domain = "";
		int size = response[index];

		// Do not add period to first label
		if ((size & 0xC0) == (int) 0xC0) {
			byte[] offset = { (byte) (response[index] & 0x3F), response[index + 1] };
			domain += getDomainByIndex(ByteBuffer.wrap(offset).getShort());
			index += 2;
			size = 0;
		} else {
			domain += getDomainLabelByIndex(index);
			index += size + 1;
			size = response[index];
		}

		while (size != 0) {
			// For every successive label put a period
			domain += ".";
			if ((size & 0xC0) == (int) 0xC0) {
				byte[] offset = { (byte) (response[index] & 0x3F), response[index + 1] };
				domain += getDomainByIndex(ByteBuffer.wrap(offset).getShort());
				index += 2;
				break;
			} else {
				domain += getDomainLabelByIndex(index);
				index += size + 1;
				size = response[index];
			}
		}
		return domain;
	}

	public int getBytesByIndex(int index) {
		int size = response[index];
		int count = 0;
		while (size != 0) {
			if ((size & 0xC0) == (int) 0xC0) {
				index += 2;
				count += 2;
				break;
			} else {
				index += size + 1;
				count += size + 1;
				size = response[index];
			}
		}
		return count;
	}

	public QueryType getQueryType(byte[] queryType) {
		if (queryType[1] == 1) {
			return QueryType.A;
		} else if (queryType[1] == 2) {
			return QueryType.NS;
		} else if (queryType[1] == 15) {
			return QueryType.MX;
		} else {
			return QueryType.CNAME;
		}
	}

	public String getDomainLabelByIndex(int index) {
		String label = "";
		int size = response[index];
		for (int i = 1; i <= size; i++) {
			label += (char) response[index + i];
		}
		return label;
	}

	public void validateRCODE() {
		switch (this.RCODE) {
		case 0:
			break;
		case 1:
			throw new RuntimeException("Query has incorrect format");
		case 2:
			throw new RuntimeException("Issue with name server");
		case 3:
			throw new RuntimeException("Domain missing");
		case 4:
			throw new RuntimeException("Query not supported");
		case 5:
			throw new RuntimeException("Name server refused query");
		}
	}
}
