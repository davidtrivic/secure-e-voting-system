package etf.certificates;

import etf.crypto.KeyManager;
import java.math.BigInteger;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.asn1.DEROctetString;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.BasicConstraints;
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.asn1.x509.KeyUsage;

public class CAManager {

	private static final long CA_VALIDITY_DAYS = 3650;
	private static final long USER_CERT_VALIDITY_DAYS = 365;

	public static CAInfo generateRootCA(String organizationName) throws Exception {
		KeyPair keyPair = KeyManager.generateRSAKeyPair();

		X500Name x500Name = new X500Name("C=RS,O=" + organizationName + ",CN=Root CA");

		X509Certificate certificate =
				KeyManager.generateSelfSignedCertificate(
						keyPair, x500Name, BigInteger.ONE, (int) CA_VALIDITY_DAYS);

		return new CAInfo(keyPair.getPrivate(), certificate, x500Name);
	}

	public static CAInfo generateOrganizationCA(
			PrivateKey rootPrivateKey, X509Certificate rootCertificate, String organizationName)
			throws Exception {
		KeyPair keyPair = KeyManager.generateRSAKeyPair();

		X500Name x500Name = new X500Name("C=RS,O=" + organizationName + ",CN=Organization CA");

		X500Name issuer = new X500Name(rootCertificate.getSubjectX500Principal().getName());

		Extension[] extensions =
				new Extension[] {
					new Extension(Extension.basicConstraints, true, new BasicConstraints(0).getEncoded()),
					new Extension(
							Extension.keyUsage,
							true,
							new KeyUsage(KeyUsage.keyCertSign | KeyUsage.cRLSign).getEncoded())
				};

		X509Certificate certificate =
				KeyManager.generateCertificate(
						keyPair,
						x500Name,
						BigInteger.valueOf(2),
						issuer,
						rootPrivateKey,
						(int) CA_VALIDITY_DAYS,
						extensions);

		return new CAInfo(keyPair.getPrivate(), certificate, x500Name);
	}

	public static CAInfo generateVoterCA(
			PrivateKey rootPrivateKey, X509Certificate rootCertificate, String organizationName)
			throws Exception {
		KeyPair keyPair = KeyManager.generateRSAKeyPair();

		X500Name x500Name = new X500Name("C=RS,O=" + organizationName + ",CN=Voter CA");

		X500Name issuer = new X500Name(rootCertificate.getSubjectX500Principal().getName());

		Extension[] extensions =
				new Extension[] {
					new Extension(Extension.basicConstraints, true, new BasicConstraints(0).getEncoded()),
					new Extension(
							Extension.keyUsage,
							true,
							new KeyUsage(KeyUsage.keyCertSign | KeyUsage.cRLSign).getEncoded())
				};

		X509Certificate certificate =
				KeyManager.generateCertificate(
						keyPair,
						x500Name,
						BigInteger.valueOf(3),
						issuer,
						rootPrivateKey,
						(int) CA_VALIDITY_DAYS,
						extensions);

		return new CAInfo(keyPair.getPrivate(), certificate, x500Name);
	}

	public static X509Certificate generateOrganizerCertificate(
			KeyPair organizerKeyPair,
			String organizationName,
			String organizationId,
			BigInteger serialNumber,
			PrivateKey caPrivateKey,
			X509Certificate caCertificate)
			throws Exception {
		X500Name subjectName =
				new X500Name("C=RS,O=" + organizationName + ",UID=" + organizationId + ",CN=Organizer");

		X500Name issuerName = new X500Name(caCertificate.getSubjectX500Principal().getName());

		Extension[] extensions =
				new Extension[] {
					new Extension(
							Extension.keyUsage,
							true,
							new KeyUsage(KeyUsage.digitalSignature | KeyUsage.keyEncipherment).getEncoded()),
					new Extension(
							new ASN1ObjectIdentifier("2.16.840.1.113730.1.13"),
							false,
							new DEROctetString("Organizer".getBytes()))
				};

		return KeyManager.generateCertificate(
				organizerKeyPair,
				subjectName,
				serialNumber,
				issuerName,
				caPrivateKey,
				(int) USER_CERT_VALIDITY_DAYS,
				extensions);
	}

	public static X509Certificate generateVoterCertificate(
			KeyPair voterKeyPair,
			String firstName,
			String lastName,
			String username,
			BigInteger serialNumber,
			PrivateKey caPrivateKey,
			X509Certificate caCertificate)
			throws Exception {
		X500Name subjectName =
				new X500Name("C=RS,CN=" + firstName + " " + lastName + ",UID=" + username);

		X500Name issuerName = new X500Name(caCertificate.getSubjectX500Principal().getName());

		Extension[] extensions =
				new Extension[] {
					new Extension(
							Extension.keyUsage,
							true,
							new KeyUsage(KeyUsage.digitalSignature | KeyUsage.keyEncipherment).getEncoded()),
					new Extension(
							new ASN1ObjectIdentifier("2.16.840.1.113730.1.13"),
							false,
							new DEROctetString("Voter".getBytes()))
				};

		return KeyManager.generateCertificate(
				voterKeyPair,
				subjectName,
				serialNumber,
				issuerName,
				caPrivateKey,
				(int) USER_CERT_VALIDITY_DAYS,
				extensions);
	}

	public static boolean validateCertificate(
			X509Certificate certificate, X509Certificate issuerCertificate) throws Exception {
		try {
			certificate.checkValidity();

			certificate.verify(issuerCertificate.getPublicKey());

			return true;
		} catch (Exception e) {
			return false;
		}
	}

	public static class CAInfo {

		public final PrivateKey privateKey;
		public final X509Certificate certificate;
		public final X500Name x500Name;

		public CAInfo(PrivateKey privateKey, X509Certificate certificate, X500Name x500Name) {
			this.privateKey = privateKey;

			this.certificate = certificate;

			this.x500Name = x500Name;
		}
	}
}
