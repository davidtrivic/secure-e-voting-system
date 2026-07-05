package etf.certificates;

import java.io.*;
import java.security.cert.X509Certificate;
import java.util.HashSet;
import java.util.Set;

public class CRLManager {

	private final Set<String> revokedCertificates = new HashSet<>();
	private final String crlFilePath;

	public CRLManager(String crlFilePath) {
		this.crlFilePath = crlFilePath;

		loadFromFile();
	}

	public void revokeCertificate(X509Certificate certificate) {
		revokedCertificates.add(certificate.getSerialNumber().toString());
		saveToFile();
	}

	public boolean isCertificateRevoked(X509Certificate certificate) {
		return revokedCertificates.contains(certificate.getSerialNumber().toString());
	}

	public Set<String> getRevokedCertificates() {
		return new HashSet<>(revokedCertificates);
	}

	public void clear() {
		revokedCertificates.clear();
		saveToFile();
	}

	private void loadFromFile() {
		File file = new File(crlFilePath);

		if (!file.exists()) {
			return;
		}
		try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
			String line;

			while ((line = reader.readLine()) != null) {
				line = line.trim();

				if (!line.isEmpty()) {
					revokedCertificates.add(line);
				}
			}
		} catch (IOException e) {
			System.err.println(
					"Greska pri ucitavanju CRL liste iz " + crlFilePath + ": " + e.getMessage());
		}
	}

	private void saveToFile() {
		File file = new File(crlFilePath);

		file.getParentFile().mkdirs();

		try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
			for (String serialNumber : revokedCertificates) {
				writer.write(serialNumber);

				writer.newLine();
			}

		} catch (IOException e) {
			System.err.println("Greska pri cuvanju CRL liste u " + crlFilePath + ": " + e.getMessage());
		}
	}
}
