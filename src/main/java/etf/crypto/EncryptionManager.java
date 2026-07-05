package etf.crypto;

import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

public class EncryptionManager {

	public static byte[] encryptSymmetric(byte[] plaintext, SecretKey key) throws Exception {
		Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding", "BC");
		javax.crypto.spec.GCMParameterSpec gcmSpec = new javax.crypto.spec.GCMParameterSpec(128, new byte[12]);
		cipher.init(Cipher.ENCRYPT_MODE, key, gcmSpec);

		return cipher.doFinal(plaintext);
	}

	public static byte[] decryptSymmetric(byte[] ciphertext, SecretKey key, byte[] iv)
			throws Exception {
		Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding", "BC");
		javax.crypto.spec.GCMParameterSpec gcmSpec = new javax.crypto.spec.GCMParameterSpec(128, iv);
		cipher.init(Cipher.DECRYPT_MODE, key, gcmSpec);

		return cipher.doFinal(ciphertext);
	}

	public static byte[] encryptSymmetricKeyWithPublic(SecretKey symmetricKey, PublicKey publicKey)
			throws Exception {
		Cipher cipher = Cipher.getInstance("RSA/ECB/OAEPWithSHA256AndMGF1Padding", "BC");
		cipher.init(Cipher.ENCRYPT_MODE, publicKey);
		return cipher.doFinal(symmetricKey.getEncoded());
	}

	public static byte[] decryptSymmetricKeyWithPrivate(byte[] encryptedKey, PrivateKey privateKey)
			throws Exception {
		Cipher cipher = Cipher.getInstance("RSA/ECB/OAEPWithSHA256AndMGF1Padding", "BC");
		cipher.init(Cipher.DECRYPT_MODE, privateKey);
		return cipher.doFinal(encryptedKey);
	}

	public static byte[] signData(byte[] data, PrivateKey privateKey) throws Exception {
		Signature signature = Signature.getInstance("SHA256withRSA", "BC");
		signature.initSign(privateKey);
		signature.update(data);

		return signature.sign();
	}

	public static boolean verifySignature(byte[] data, byte[] signature, PublicKey publicKey)
			throws Exception {
		Signature sig = Signature.getInstance("SHA256withRSA", "BC");
		sig.initVerify(publicKey);
		sig.update(data);

		return sig.verify(signature);
	}

	public static byte[] generateHMAC(byte[] data, byte[] key) throws Exception {
		Mac mac = Mac.getInstance("HmacSHA256", "BC");
		SecretKeySpec keySpec = new SecretKeySpec(key, 0, key.length, "HmacSHA256");
		mac.init(keySpec);

		return mac.doFinal(data);
	}

	public static boolean verifyHMAC(byte[] data, byte[] hmac, byte[] key) throws Exception {
		byte[] computedHmac = generateHMAC(data, key);

		return java.util.Arrays.equals(computedHmac, hmac);
	}

	public static byte[] encryptPrivateKeyWithPassword(PrivateKey privateKey, String password) throws Exception {
		byte[] salt = new byte[16];

		java.security.SecureRandom random = new java.security.SecureRandom();

		random.nextBytes(salt);

		javax.crypto.SecretKeyFactory factory = javax.crypto.SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");

		javax.crypto.spec.PBEKeySpec spec = new javax.crypto.spec.PBEKeySpec(password.toCharArray(), salt, 65536, 256);

		SecretKey tmp = factory.generateSecret(spec);
		SecretKey aesKey = new SecretKeySpec(tmp.getEncoded(), "AES");
		byte[] iv = new byte[12];
		random.nextBytes(iv);

		Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding", "BC");
		javax.crypto.spec.GCMParameterSpec gcmSpec = new javax.crypto.spec.GCMParameterSpec(128, iv);
		cipher.init(Cipher.ENCRYPT_MODE, aesKey, gcmSpec);
		byte[] encryptedKey = cipher.doFinal(privateKey.getEncoded());

		byte[] result = new byte[salt.length + iv.length + encryptedKey.length];

		System.arraycopy(salt, 0, result, 0, salt.length);
		System.arraycopy(iv, 0, result, salt.length, iv.length);
		System.arraycopy(encryptedKey, 0, result, salt.length + iv.length, encryptedKey.length);

		return result;
	}

	public static PrivateKey decryptPrivateKeyWithPassword(byte[] encryptedData, String password, String algorithm) throws Exception {
        byte[] salt = new byte[16];
		byte[] iv = new byte[12];
		byte[] encryptedKey = new byte[encryptedData.length - 28];

		System.arraycopy(encryptedData, 0, salt, 0, 16);
		System.arraycopy(encryptedData, 16, iv, 0, 12);
		System.arraycopy(encryptedData, 28, encryptedKey, 0, encryptedKey.length);

		javax.crypto.SecretKeyFactory factory = javax.crypto.SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");

		javax.crypto.spec.PBEKeySpec spec = new javax.crypto.spec.PBEKeySpec(password.toCharArray(), salt, 65536, 256);

		SecretKey tmp = factory.generateSecret(spec);

		SecretKey aesKey = new SecretKeySpec(tmp.getEncoded(), "AES");

		Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding", "BC");

		javax.crypto.spec.GCMParameterSpec gcmSpec = new javax.crypto.spec.GCMParameterSpec(128, iv);

		cipher.init(Cipher.DECRYPT_MODE, aesKey, gcmSpec);

		byte[] decryptedKey = cipher.doFinal(encryptedKey);

		java.security.spec.PKCS8EncodedKeySpec keySpec =
				new java.security.spec.PKCS8EncodedKeySpec(decryptedKey);

		java.security.KeyFactory keyFactory = java.security.KeyFactory.getInstance(algorithm, "BC");

		return keyFactory.generatePrivate(keySpec);
	}
}
