public class Record {
	private boolean auth;
	private QueryType queryType;
	private int lengthBytes;
	private int ttl;
	private String domain;
	private String pref = "";

	public Record(boolean auth) {
		this.auth = auth;
	}

	public void printRecord() {
		switch (this.queryType) {
		case A:
			System.out.print("IP \t");
			break;
		case NS:
			System.out.print("NS \t");
			break;
		case MX:
			System.out.print("MX \t");
			break;
		case CNAME:
			System.out.print("CNAME \t");
			break;
		default:
			break;
		}
		System.out.println(this.domain + "\t" + this.pref + "\t" + this.ttl  + "\t" + (this.auth ? "auth" : "nonauth"));
	}

	public void setQueryType(QueryType queryType) {
		this.queryType = queryType;
	}

	public int getByteLength() {
		return lengthBytes;
	}

	public void setLengthBytes(int lengthBytes) {
		this.lengthBytes = lengthBytes;
	}

	public void setTTL(int ttl) {
		this.ttl = ttl;
	}

	public void setDomain(String domain) {
		this.domain = domain;
	}
	
	public void setPref(String pref) {
		this.pref = pref;
	}
}
