package etf.voting;

import etf.crypto.EncryptionManager;
import etf.crypto.KeyManager;
import java.security.PrivateKey;
import java.security.PublicKey;
import javax.crypto.SecretKey;

public class VoteProcessor {

	public static void encryptVote(
			Vote vote, PublicKey organizerPublicKey, PrivateKey voterPrivateKey) throws Exception {
		SecretKey symmetricKey = KeyManager.generateSymmetricKey();

		byte[] voteData = vote.getSelectedOption().getBytes("UTF-8");
		byte[] encryptedVote = EncryptionManager.encryptSymmetric(voteData, symmetricKey);

		byte[] encryptedSymmetricKey =
				EncryptionManager.encryptSymmetricKeyWithPublic(symmetricKey, organizerPublicKey);

		byte[] digitalSignature = EncryptionManager.signData(encryptedVote, voterPrivateKey);

		vote.setEncryptedVote(encryptedVote);
		vote.setEncryptedSymmetricKey(encryptedSymmetricKey);
		vote.setDigitalSignature(digitalSignature);
		vote.setIv(new byte[12]);

		vote.setSelectedOption(null);
	}

	public static String decryptVote(Vote vote, PrivateKey organizerPrivateKey) throws Exception {
		byte[] symmetricKeyData =
				EncryptionManager.decryptSymmetricKeyWithPrivate(
						vote.getEncryptedSymmetricKey(), organizerPrivateKey);

		javax.crypto.spec.SecretKeySpec symmetricKey =
				new javax.crypto.spec.SecretKeySpec(symmetricKeyData, 0, symmetricKeyData.length, "AES");

		byte[] decryptedVote =
				EncryptionManager.decryptSymmetric(vote.getEncryptedVote(), symmetricKey, vote.getIv());

		return new String(decryptedVote, "UTF-8");
	}

	public static boolean verifyVoteSignature(Vote vote, PublicKey voterPublicKey) throws Exception {
		return EncryptionManager.verifySignature(
				vote.getEncryptedVote(), vote.getDigitalSignature(), voterPublicKey);
	}

	public static byte[] generateVotingMetadataHMAC(String votingId, byte[] key) throws Exception {
		byte[] metadata = votingId.getBytes("UTF-8");

		return EncryptionManager.generateHMAC(metadata, key);
	}

	public static boolean verifyVotingMetadataHMAC(String votingId, byte[] hmac, byte[] key)
			throws Exception {
		byte[] metadata = votingId.getBytes("UTF-8");

		return EncryptionManager.verifyHMAC(metadata, hmac, key);
	}
}
