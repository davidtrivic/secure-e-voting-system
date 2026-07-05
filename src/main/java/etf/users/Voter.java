package etf.users;

import java.io.Serializable;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;

public class Voter extends User implements Serializable {

	private static final long serialVersionUID = 1L;

	private String firstName;
	private String lastName;

	public Voter(
			String firstName,
			String lastName,
			String username,
			String password,
			X509Certificate certificate,
			PrivateKey privateKey) {
		super(username, password, certificate, privateKey);
		this.firstName = firstName;
		this.lastName = lastName;
	}

	public String getFirstName() {
		return firstName;
	}

	public String getLastName() {
		return lastName;
	}

	public String getFullName() {
		return firstName + " " + lastName;
	}

	@Override
	public String getUserType() {
		return "Voter";
	}

	@Override
	public String toString() {
		return "Voter{"
				+ "firstName='"
				+ firstName
				+ '\''
				+ ", lastName='"
				+ lastName
				+ '\''
				+ ", username='"
				+ getUsername()
				+ '\''
				+ '}';
	}
}
