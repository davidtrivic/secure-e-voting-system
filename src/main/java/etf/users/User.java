package etf.users;

import java.io.Serializable;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;

public abstract class User implements Serializable {

	private static final long serialVersionUID = 1L;

	private String username;
	private String passwordHash;
	private X509Certificate certificate;
	private PrivateKey privateKey;
	private int failedLoginAttempts;
	private boolean locked;

	public User(
			String username, String password, X509Certificate certificate, PrivateKey privateKey) {
		this.username = username;
		this.passwordHash = hashPassword(password);
		this.certificate = certificate;
		this.privateKey = privateKey;
		this.failedLoginAttempts = 0;
		this.locked = false;
	}

	private String hashPassword(String password) {
		return Integer.toHexString(password.hashCode());
	}

	public boolean verifyPassword(String password) {
		return passwordHash.equals(hashPassword(password));
	}

	public void incrementFailedAttempts() {
		failedLoginAttempts++;

		if (failedLoginAttempts >= 3) {
			locked = true;
		}
	}

	public void resetFailedAttempts() {
		failedLoginAttempts = 0;
	}

	public void lock() {
		locked = true;
	}

	public void unlock() {
		locked = false;

		failedLoginAttempts = 0;
	}

	public String getUsername() {
		return username;
	}

	public X509Certificate getCertificate() {
		return certificate;
	}

	public PrivateKey getPrivateKey() {
		return privateKey;
	}

	public int getFailedLoginAttempts() {
		return failedLoginAttempts;
	}

	public boolean isLocked() {
		return locked;
	}

	public abstract String getUserType();
}
