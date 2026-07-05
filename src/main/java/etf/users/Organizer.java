package etf.users;

import java.io.Serializable;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;

public class Organizer extends User implements Serializable {

	private static final long serialVersionUID = 1L;
	private String organizationName;
	private String organizationId;

	public Organizer(
			String organizationName,
			String organizationId,
			String password,
			X509Certificate certificate,
			PrivateKey privateKey) {
		super(organizationName, password, certificate, privateKey);
		this.organizationName = organizationName;
		this.organizationId = organizationId;
	}

	public String getOrganizationName() {
		return organizationName;
	}

	public String getOrganizationId() {
		return organizationId;
	}

	@Override
	public String getUserType() {
		return "Organizer";
	}

	@Override
	public String toString() {
		return "Organizer{"
				+ "organizationName='"
				+ organizationName
				+ '\''
				+ ", organizationId='"
				+ organizationId
				+ '\''
				+ ", username='"
				+ getUsername()
				+ '\''
				+ '}';
	}
}
