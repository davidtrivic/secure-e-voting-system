package etf;

import etf.auth.AuthenticationManager;
import etf.auth.RegistrationManager;
import etf.certificates.CAManager;
import etf.system.EVotingSystem;
import etf.ui.ConsoleInterface;
import etf.users.Organizer;
import etf.users.Voter;
import java.security.Security;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

public class App {

	private static EVotingSystem system;
	private static AuthenticationManager authManager;
	private static RegistrationManager regManager;
	private static ConsoleInterface ui;

	public static void main(String[] args) {
		try {
			Security.addProvider(new BouncyCastleProvider());
			initializeSystem();
			ui = new ConsoleInterface(system, authManager, regManager);
			ui.start();
		} catch (Exception e) {
			System.err.println("Greska pri pokretanju sistema: " + e.getMessage());
			e.printStackTrace();
		}
	}

	private static void initializeSystem() throws Exception {
		System.out.println("\nInicijalizacija e-voting sistema...");

		CAManager.CAInfo rootCA;
		CAManager.CAInfo organizerCA;
		CAManager.CAInfo voterCA;

		java.io.File rootCertFile = new java.io.File("./data/ca/root_ca_cert.pem");
		java.io.File rootKeyFile = new java.io.File("./data/ca/root_ca_key.enc");

		if (rootCertFile.exists() && rootKeyFile.exists()) {
			System.out.println("Ucitavanje postojecih CA tijela...");

			java.security.cert.X509Certificate rootCert =
					etf.crypto.KeyManager.loadCertificateFromFile("./data/ca/root_ca_cert.pem");

			java.security.PrivateKey rootKey =
					etf.crypto.KeyManager.loadEncryptedPrivateKeyFromFile(
							"./data/ca/root_ca_key.enc", "root_ca_password", "RSA");

			org.bouncycastle.asn1.x500.X500Name rootX500 =
					new org.bouncycastle.asn1.x500.X500Name(rootCert.getSubjectX500Principal().getName());

			rootCA = new CAManager.CAInfo(rootKey, rootCert, rootX500);

			System.out.println("[OK] Root CA tijelo ucitano");

			java.security.cert.X509Certificate orgCert =
					etf.crypto.KeyManager.loadCertificateFromFile("./data/ca/organizer_ca_cert.pem");

			java.security.PrivateKey orgKey =
					etf.crypto.KeyManager.loadEncryptedPrivateKeyFromFile(
							"./data/ca/organizer_ca_key.enc", "organizer_ca_password", "RSA");

			org.bouncycastle.asn1.x500.X500Name orgX500 =
					new org.bouncycastle.asn1.x500.X500Name(orgCert.getSubjectX500Principal().getName());

			organizerCA = new CAManager.CAInfo(orgKey, orgCert, orgX500);

			System.out.println("[OK] Organizaciono CA tijelo ucitano");

			java.security.cert.X509Certificate voterCert =
					etf.crypto.KeyManager.loadCertificateFromFile("./data/ca/voter_ca_cert.pem");

			java.security.PrivateKey voterKey =
					etf.crypto.KeyManager.loadEncryptedPrivateKeyFromFile(
							"./data/ca/voter_ca_key.enc", "voter_ca_password", "RSA");

			org.bouncycastle.asn1.x500.X500Name voterX500 =
					new org.bouncycastle.asn1.x500.X500Name(voterCert.getSubjectX500Principal().getName());

			voterCA = new CAManager.CAInfo(voterKey, voterCert, voterX500);

			System.out.println("[OK] Glasacko CA tijelo ucitano");
		} else {
			System.out.println("Generisanje novih CA tijela...");

			rootCA = CAManager.generateRootCA("E-Voting System");

			System.out.println("[OK] Root CA tijelo kreirano");

			organizerCA =
					CAManager.generateOrganizationCA(
							rootCA.privateKey, rootCA.certificate, "E-Voting System");

			System.out.println("[OK] Organizaciono CA tijelo kreirano");

			voterCA = CAManager.generateVoterCA(rootCA.privateKey, rootCA.certificate, "E-Voting System");

			System.out.println("[OK] Glasacko CA tijelo kreirano");

			System.out.println("\nCuvanje CA tijela u fajlove...");

			etf.crypto.KeyManager.saveCertificateToFile(rootCA.certificate, "./data/ca/root_ca_cert.pem");

			etf.crypto.KeyManager.saveEncryptedPrivateKeyToFile(
					rootCA.privateKey, "root_ca_password", "./data/ca/root_ca_key.enc");

			System.out.println("[OK] Root CA tijelo sacuvano");

			etf.crypto.KeyManager.saveCertificateToFile(
					organizerCA.certificate, "./data/ca/organizer_ca_cert.pem");

			etf.crypto.KeyManager.saveEncryptedPrivateKeyToFile(
					organizerCA.privateKey, "organizer_ca_password", "./data/ca/organizer_ca_key.enc");

			System.out.println("[OK] Organizaciono CA tijelo sacuvano");

			etf.crypto.KeyManager.saveCertificateToFile(
					voterCA.certificate, "./data/ca/voter_ca_cert.pem");

			etf.crypto.KeyManager.saveEncryptedPrivateKeyToFile(
					voterCA.privateKey, "voter_ca_password", "./data/ca/voter_ca_key.enc");

			System.out.println("[OK] Glasacko CA tijelo sacuvano");
		}
		system = new EVotingSystem();
		authManager = new AuthenticationManager(organizerCA.certificate, voterCA.certificate);
		regManager = new RegistrationManager(rootCA, organizerCA, voterCA);

		System.out.println("\nUcitavanje postojecih korisnika...");

		java.util.List<Organizer> organizers = regManager.loadAllOrganizers();

		for (Organizer org : organizers) {
			system.addUser(org);
		}
		if (organizers.size() > 0) {
			System.out.println("[OK] Ucitano " + organizers.size() + " organizatora");
		}
		java.util.List<Voter> voters = regManager.loadAllVoters();

		for (Voter voter : voters) {
			system.addUser(voter);
		}
		if (voters.size() > 0) {
			System.out.println("[OK] Ucitano " + voters.size() + " glasaca");
		}
		System.out.println("\nUcitavanje postojecih glasanja...");

		system.loadAllVotings();

		System.out.println("\n[OK] Sistem je spreman za rad\n");
	}
}
