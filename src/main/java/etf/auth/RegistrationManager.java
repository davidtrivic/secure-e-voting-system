package etf.auth;

import etf.certificates.CAManager;
import etf.crypto.KeyManager;
import etf.users.Organizer;
import etf.users.User;
import etf.users.Voter;
import java.math.BigInteger;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.concurrent.atomic.AtomicInteger;

public class RegistrationManager {
	private CAManager.CAInfo rootCA;
	private CAManager.CAInfo organizerCA;
	private CAManager.CAInfo voterCA;
	private AtomicInteger certificateSerialNumber;

	public RegistrationManager(
			CAManager.CAInfo rootCA, CAManager.CAInfo organizerCA, CAManager.CAInfo voterCA) {
		this.rootCA = rootCA;
		this.organizerCA = organizerCA;
		this.voterCA = voterCA;
		this.certificateSerialNumber = new AtomicInteger(100);
	}

	public Organizer registerOrganizer(
			String organizationName, String organizationId, String password) throws Exception {
		KeyPair keyPair = KeyManager.generateRSAKeyPair();
		X509Certificate certificate =
				CAManager.generateOrganizerCertificate(
						keyPair,
						organizationName,
						organizationId,
						BigInteger.valueOf(certificateSerialNumber.getAndIncrement()),
						organizerCA.privateKey,
						organizerCA.certificate);

		Organizer organizer =
				new Organizer(
						organizationName, organizationId, password, certificate, keyPair.getPrivate());

		String sanitizedId = organizationId.replaceAll("[^a-zA-Z0-9-_]", "_");
		String certPath = "./data/organizers/" + sanitizedId + "/certificate.pem";
		String keyPath = "./data/organizers/" + sanitizedId + "/private_key.enc";

		KeyManager.saveCertificateToFile(certificate, certPath);
		KeyManager.saveEncryptedPrivateKeyToFile(keyPair.getPrivate(), password, keyPath);

		String userPath = "./data/organizers/" + sanitizedId + "/user.dat";
		saveUserToFile(organizer, userPath);

		System.out.println("Organizator uspesno registrovan");
		System.out.println("  - Sertifikat sacuvan: " + certPath);
		System.out.println("  - Privatni kljuc (enkriptovan) sacuvan: " + keyPath);
		System.out.println("  - Korisnicki podaci sacuvani: " + userPath);

		return organizer;
	}

	public Voter registerVoter(String firstName, String lastName, String username, String password)
			throws Exception {
		KeyPair keyPair = KeyManager.generateRSAKeyPair();
		X509Certificate certificate =
				CAManager.generateVoterCertificate(
						keyPair,
						firstName,
						lastName,
						username,
						BigInteger.valueOf(certificateSerialNumber.getAndIncrement()),
						voterCA.privateKey,
						voterCA.certificate);

		Voter voter =
				new Voter(firstName, lastName, username, password, certificate, keyPair.getPrivate());

		String sanitizedUsername = username.replaceAll("[^a-zA-Z0-9-_]", "_");
		String certPath = "./data/voters/" + sanitizedUsername + "/certificate.pem";
		String keyPath = "./data/voters/" + sanitizedUsername + "/private_key.enc";

		KeyManager.saveCertificateToFile(certificate, certPath);
		KeyManager.saveEncryptedPrivateKeyToFile(keyPair.getPrivate(), password, keyPath);

		String userPath = "./data/voters/" + sanitizedUsername + "/user.dat";
		saveUserToFile(voter, userPath);

		System.out.println("Glasac uspesno registrovan");
		System.out.println("  - Sertifikat sacuvan: " + certPath);
		System.out.println("  - Privatni kljuc (enkriptovan) sacuvan: " + keyPath);
		System.out.println("  - Korisnicki podaci sacuvani: " + userPath);

		return voter;
	}

	public ValidationResult validateOrganizerRegistration(
			String organizationName, String organizationId, String password) {
		if (organizationName == null || organizationName.trim().isEmpty()) {
			return new ValidationResult(false, "Naziv organizacije ne sme biti prazan");
		}

		if (organizationId == null || organizationId.trim().isEmpty()) {
			return new ValidationResult(false, "ID organizacije ne sme biti prazan");
		}

		if (password == null || password.length() < 8) {
			return new ValidationResult(false, "Lozinka mora biti najmanje 8 karaktera");
		}

		return new ValidationResult(true, "Validacija uspešna");
	}

	public ValidationResult validateVoterRegistration(
			String firstName, String lastName, String username, String password) {
		if (firstName == null || firstName.trim().isEmpty()) {
			return new ValidationResult(false, "Ime ne sme biti prazno");
		}

		if (lastName == null || lastName.trim().isEmpty()) {
			return new ValidationResult(false, "Prezime ne sme biti prazno");
		}

		if (username == null || username.length() < 3) {
			return new ValidationResult(false, "Korisničko ime mora biti najmanje 3 karaktera");
		}

		if (password == null || password.length() < 8) {
			return new ValidationResult(false, "Lozinka mora biti najmanje 8 karaktera");
		}

		return new ValidationResult(true, "Validacija uspešna");
	}

	public Organizer loadOrganizer(String organizationId, String password) throws Exception {
		String sanitizedId = organizationId.replaceAll("[^a-zA-Z0-9-_]", "_");
		String certPath = "./data/organizers/" + sanitizedId + "/certificate.pem";
		String keyPath = "./data/organizers/" + sanitizedId + "/private_key.enc";

		X509Certificate certificate = KeyManager.loadCertificateFromFile(certPath);
		PrivateKey privateKey = KeyManager.loadEncryptedPrivateKeyFromFile(keyPath, password, "RSA");

		String subjectDN = certificate.getSubjectX500Principal().getName();
		String organizationName = extractFromDN(subjectDN, "O");
		String uidFromCert = extractFromDN(subjectDN, "UID");

		return new Organizer(organizationName, organizationId, password, certificate, privateKey);
	}

	public Voter loadVoter(String username, String password) throws Exception {
		String sanitizedUsername = username.replaceAll("[^a-zA-Z0-9-_]", "_");
		String certPath = "./data/voters/" + sanitizedUsername + "/certificate.pem";
		String keyPath = "./data/voters/" + sanitizedUsername + "/private_key.enc";

		X509Certificate certificate = KeyManager.loadCertificateFromFile(certPath);
		PrivateKey privateKey = KeyManager.loadEncryptedPrivateKeyFromFile(keyPath, password, "RSA");

		String subjectDN = certificate.getSubjectX500Principal().getName();
		String fullName = extractFromDN(subjectDN, "CN");
		String uidFromCert = extractFromDN(subjectDN, "UID");
		String[] nameParts = fullName.split(" ", 2);
		String firstName = nameParts.length > 0 ? nameParts[0] : "";
		String lastName = nameParts.length > 1 ? nameParts[1] : "";

		return new Voter(firstName, lastName, username, password, certificate, privateKey);
	}

	private String extractFromDN(String dn, String attribute) {
		String[] parts = dn.split(",");
		for (String part : parts) {
			part = part.trim();
			if (part.startsWith(attribute + "=")) {
				return part.substring(attribute.length() + 1);
			}
		}

		return "";
	}

	private void saveUserToFile(User user, String filePath) throws Exception {
		java.io.File file = new java.io.File(filePath);
		file.getParentFile().mkdirs();

		try (java.io.FileOutputStream fos = new java.io.FileOutputStream(file);
				java.io.ObjectOutputStream oos = new java.io.ObjectOutputStream(fos)) {
			oos.writeObject(user);
		}
	}

	public java.util.List<Organizer> loadAllOrganizers() {
		java.util.List<Organizer> organizers = new java.util.ArrayList<>();
		java.io.File organizersDir = new java.io.File("./data/organizers");

		if (!organizersDir.exists() || !organizersDir.isDirectory()) {
			return organizers;
		}

		java.io.File[] userDirs = organizersDir.listFiles();
		if (userDirs == null) {
			return organizers;
		}

		for (java.io.File userDir : userDirs) {
			if (userDir.isDirectory()) {
				java.io.File userFile = new java.io.File(userDir, "user.dat");

				if (userFile.exists()) {
					try (java.io.FileInputStream fis = new java.io.FileInputStream(userFile);
							java.io.ObjectInputStream ois = new java.io.ObjectInputStream(fis)) {
						Object obj = ois.readObject();
						if (obj instanceof Organizer) {
							organizers.add((Organizer) obj);
						}
					} catch (Exception e) {
						System.err.println(
								"Greska pri ucitavanju organizatora iz "
										+ userFile.getPath()
										+ ": "
										+ e.getMessage());
					}
				}
			}
		}

		return organizers;
	}

	public java.util.List<Voter> loadAllVoters() {
		java.util.List<Voter> voters = new java.util.ArrayList<>();
		java.io.File votersDir = new java.io.File("./data/voters");

		if (!votersDir.exists() || !votersDir.isDirectory()) {
			return voters;
		}

		java.io.File[] userDirs = votersDir.listFiles();
		if (userDirs == null) {
			return voters;
		}

		for (java.io.File userDir : userDirs) {
			if (userDir.isDirectory()) {
				java.io.File userFile = new java.io.File(userDir, "user.dat");

				if (userFile.exists()) {
					try (java.io.FileInputStream fis = new java.io.FileInputStream(userFile);
							java.io.ObjectInputStream ois = new java.io.ObjectInputStream(fis)) {
						Object obj = ois.readObject();
						if (obj instanceof Voter) {
							voters.add((Voter) obj);
						}
					} catch (Exception e) {
						System.err.println(
								"Greska pri ucitavanju glasaca iz " + userFile.getPath() + ": " + e.getMessage());
					}
				}
			}
		}

		return voters;
	}

	public static class ValidationResult {
		public final boolean valid;
		public final String message;

		public ValidationResult(boolean valid, String message) {
			this.valid = valid;
			this.message = message;
		}
	}
}
