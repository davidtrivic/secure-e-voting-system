package etf.crypto;

import java.io.*;
import java.math.BigInteger;
import java.security.*;
import java.security.cert.X509Certificate;
import java.security.spec.RSAKeyGenParameterSpec;
import java.util.Date;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;

public class KeyManager {

	private static final int RSA_KEY_SIZE = 2048;
	private static final int SYMMETRIC_KEY_SIZE = 256;
	private static final long CERTIFICATE_VALIDITY_DAYS = 365;

	public static KeyPair generateRSAKeyPair() throws Exception {
		KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA", "BC");

		kpg.initialize(new RSAKeyGenParameterSpec(RSA_KEY_SIZE, RSAKeyGenParameterSpec.F4));

		return kpg.generateKeyPair();
	}

	public static SecretKey generateSymmetricKey() throws Exception {
		KeyGenerator keyGen = KeyGenerator.getInstance("AES", "BC");

		keyGen.init(SYMMETRIC_KEY_SIZE);

		return keyGen.generateKey();
	}

	public static X509Certificate generateSelfSignedCertificate(
			KeyPair keyPair, X500Name subjectName, BigInteger serialNumber, int validityDays)
			throws Exception {

		PublicKey publicKey = keyPair.getPublic();

		PrivateKey privateKey = keyPair.getPrivate();

		X509v3CertificateBuilder builder =
				new JcaX509v3CertificateBuilder(
						subjectName,
						serialNumber,
						new Date(),
						new Date(System.currentTimeMillis() + (long) validityDays * 24 * 60 * 60 * 1000),
						subjectName,
						publicKey);

		ContentSigner signer =
				new JcaContentSignerBuilder("SHA256WithRSAEncryption").setProvider("BC").build(privateKey);

		X509CertificateHolder holder = builder.build(signer);

		return new JcaX509CertificateConverter().setProvider("BC").getCertificate(holder);
	}

	public static X509Certificate generateCertificate(
			KeyPair subjectKeyPair,
			X500Name subjectName,
			BigInteger serialNumber,
			X500Name issuerName,
			PrivateKey issuerPrivateKey,
			int validityDays,
			Extension... extensions)
			throws Exception {

		PublicKey subjectPublicKey = subjectKeyPair.getPublic();

		X509v3CertificateBuilder builder =
				new JcaX509v3CertificateBuilder(
						issuerName,
						serialNumber,
						new Date(),
						new Date(System.currentTimeMillis() + (long) validityDays * 24 * 60 * 60 * 1000),
						subjectName,
						subjectPublicKey);

		if (extensions != null && extensions.length > 0) {
			for (Extension ext : extensions) {
				builder.addExtension(ext);
			}
		}
		ContentSigner signer =
				new JcaContentSignerBuilder("SHA256WithRSAEncryption")
						.setProvider("BC")
						.build(issuerPrivateKey);

		X509CertificateHolder holder = builder.build(signer);

		return new JcaX509CertificateConverter().setProvider("BC").getCertificate(holder);
	}

	public static void savePrivateKeyToFile(PrivateKey privateKey, String filePath) throws Exception {
		byte[] encodedKey = privateKey.getEncoded();

		try (FileOutputStream fos = new FileOutputStream(filePath)) {
			fos.write(encodedKey);
		}
	}

	public static void saveSymmetricKeyToFile(SecretKey secretKey, String filePath) throws Exception {
		byte[] encodedKey = secretKey.getEncoded();

		try (FileOutputStream fos = new FileOutputStream(filePath)) {
			fos.write(encodedKey);
		}
	}

	public static void saveCertificateToFile(X509Certificate certificate, String filePath)
			throws Exception {
		File file = new File(filePath);

		File parentDir = file.getParentFile();

		if (parentDir != null && !parentDir.exists()) {
			parentDir.mkdirs();
		}
		try (FileOutputStream fos = new FileOutputStream(filePath)) {
			fos.write("-----BEGIN CERTIFICATE-----\n".getBytes());

			String base64 = java.util.Base64.getEncoder().encodeToString(certificate.getEncoded());

			for (int i = 0; i < base64.length(); i += 64) {
				fos.write(base64.substring(i, Math.min(i + 64, base64.length())).getBytes());

				fos.write("\n".getBytes());
			}
			fos.write("-----END CERTIFICATE-----\n".getBytes());
		}
	}

	public static void saveEncryptedPrivateKeyToFile(
			PrivateKey privateKey, String password, String filePath) throws Exception {
		File file = new File(filePath);

		File parentDir = file.getParentFile();

		if (parentDir != null && !parentDir.exists()) {
			parentDir.mkdirs();
		}
		byte[] encryptedKey = EncryptionManager.encryptPrivateKeyWithPassword(privateKey, password);

		try (FileOutputStream fos = new FileOutputStream(filePath)) {
			fos.write(encryptedKey);
		}
	}

	public static PrivateKey loadEncryptedPrivateKeyFromFile(
			String filePath, String password, String algorithm) throws Exception {
		File file = new File(filePath);

		if (!file.exists()) {
			throw new FileNotFoundException("Fajl privatnog ključa ne postoji: " + filePath);
		}
		byte[] encryptedData;

		try (FileInputStream fis = new FileInputStream(filePath)) {
			encryptedData = fis.readAllBytes();
		}
		return EncryptionManager.decryptPrivateKeyWithPassword(encryptedData, password, algorithm);
	}

	public static X509Certificate loadCertificateFromFile(String filePath) throws Exception {
		File file = new File(filePath);

		if (!file.exists()) {
			throw new FileNotFoundException("Fajl sertifikata ne postoji: " + filePath);
		}
		StringBuilder pemContent = new StringBuilder();

		try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
			String line;

			boolean inCert = false;

			while ((line = reader.readLine()) != null) {
				if (line.contains("BEGIN CERTIFICATE")) {
					inCert = true;

					continue;
				}
				if (line.contains("END CERTIFICATE")) {
					break;
				}
				if (inCert) {
					pemContent.append(line);
				}
			}
		}
		byte[] certBytes = java.util.Base64.getDecoder().decode(pemContent.toString());

		java.security.cert.CertificateFactory cf =
				java.security.cert.CertificateFactory.getInstance("X.509", "BC");

		return (X509Certificate) cf.generateCertificate(new java.io.ByteArrayInputStream(certBytes));
	}
}
