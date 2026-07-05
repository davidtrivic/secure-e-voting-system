package etf.auth;

import etf.certificates.CRLManager;
import etf.users.User;
import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.Map;

public class AuthenticationManager {

	private CRLManager voterCRL;
	private CRLManager organizerCRL;
	private CRLManager rootCRL;
	private Map<String, User> authenticatedUsers;
	private X509Certificate organizerCACert;
	private X509Certificate voterCACert;

	public AuthenticationManager(X509Certificate organizerCACert, X509Certificate voterCACert) {
		this.voterCRL = new CRLManager("./data/ca/voter_crl.dat");
		this.organizerCRL = new CRLManager("./data/ca/organizer_crl.dat");
		this.rootCRL = new CRLManager("./data/ca/root_crl.dat");
		this.authenticatedUsers = new HashMap<>();
		this.organizerCACert = organizerCACert;
		this.voterCACert = voterCACert;
	}

	public CertificateValidationResult validateCertificateFromFile(String certificatePath) {
		try {
			X509Certificate certificate = etf.crypto.KeyManager.loadCertificateFromFile(certificatePath);

			certificate.checkValidity();

			boolean isValidVoter = false;

			boolean isValidOrganizer = false;

			try {
				certificate.verify(voterCACert.getPublicKey());

				isValidVoter = true;
			} catch (Exception e) {

			}

			try {
				certificate.verify(organizerCACert.getPublicKey());

				isValidOrganizer = true;
			} catch (Exception e) {

			}

			if (!isValidVoter && !isValidOrganizer) {
				return new CertificateValidationResult(
						false, null, "Sertifikat nije izdat od validnog CA tijela");
			}

			if (isValidVoter) {
				if (voterCRL.isCertificateRevoked(certificate)) {
					return new CertificateValidationResult(false, null, "Sertifikat je povucen (Voter CRL)");
				}

			} else if (isValidOrganizer) {
				if (organizerCRL.isCertificateRevoked(certificate)) {
					return new CertificateValidationResult(
							false, null, "Sertifikat je povucen (Organizer CRL)");
				}
			}

			return new CertificateValidationResult(true, certificate, "Sertifikat je validan");
		} catch (java.io.FileNotFoundException e) {
			return new CertificateValidationResult(
					false, null, "Fajl sertifikata nije pronadjen: " + certificatePath);
		} catch (java.security.cert.CertificateExpiredException e) {
			return new CertificateValidationResult(false, null, "Sertifikat je istekao");
		} catch (java.security.cert.CertificateNotYetValidException e) {
			return new CertificateValidationResult(false, null, "Sertifikat jos nije validan");
		} catch (Exception e) {
			return new CertificateValidationResult(
					false, null, "Greska pri ucitavanju sertifikata: " + e.getMessage());
		}
	}

	public AuthenticationResult authenticateWithCredentials(
			X509Certificate validatedCertificate, String username, String password, User user)
			throws Exception {

		if (!user.getCertificate().equals(validatedCertificate)) {
			return new AuthenticationResult(false, "Sertifikat ne pripada ovom korisniku");
		}

		if (!user.verifyPassword(password)) {
			user.incrementFailedAttempts();

			if (user.isLocked()) {
				revokeCertificate(user);
				return new AuthenticationResult(
						false, "Korisnicki racun je zakljucan - sertifikat je automatski povucen");
			}

			int remainingAttempts = 3 - user.getFailedLoginAttempts();

			return new AuthenticationResult(
					false, "Lozinka je pogresna. Preostalo pokusaja: " + remainingAttempts);
		}

		user.resetFailedAttempts();

		authenticatedUsers.put(user.getUsername(), user);

		return new AuthenticationResult(true, "Uspesna prijava");
	}

	private boolean validateCertificate(User user) {
		try {
			X509Certificate certificate = user.getCertificate();
			if (certificate == null) {
				return false;
			}

			certificate.checkValidity();

			return true;

		} catch (Exception e) {
			return false;
		}
	}

	private boolean isCertificateRevoked(User user) {
		String userType = user.getUserType();

		X509Certificate certificate = user.getCertificate();

		if ("Voter".equals(userType)) {
			return voterCRL.isCertificateRevoked(certificate);
		} else if ("Organizer".equals(userType)) {
			return organizerCRL.isCertificateRevoked(certificate);
		}
		return false;
	}

	public void revokeCertificate(User user) {
		String userType = user.getUserType();

		X509Certificate certificate = user.getCertificate();

		if ("Voter".equals(userType)) {
			voterCRL.revokeCertificate(certificate);
		} else if ("Organizer".equals(userType)) {
			organizerCRL.revokeCertificate(certificate);
		}

		user.lock();
	}

	public void logout(String username) {
		authenticatedUsers.remove(username);
	}

	public boolean isAuthenticated(String username) {
		return authenticatedUsers.containsKey(username);
	}

	public User getAuthenticatedUser(String username) {
		return authenticatedUsers.get(username);
	}

	public CRLManager getVoterCRL() {
		return voterCRL;
	}

	public CRLManager getOrganizerCRL() {
		return organizerCRL;
	}

	public CRLManager getRootCRL() {
		return rootCRL;
	}

	public static class CertificateValidationResult {
		public final boolean valid;
		public final X509Certificate certificate;
		public final String message;

		public CertificateValidationResult(boolean valid, X509Certificate certificate, String message) {
			this.valid = valid;
			this.certificate = certificate;
			this.message = message;
		}
	}

	public static class AuthenticationResult {

		public final boolean success;
		public final String message;

		public AuthenticationResult(boolean success, String message) {
			this.success = success;
			this.message = message;
		}
	}
}
