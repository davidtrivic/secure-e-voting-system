package etf.voting;

import java.io.Serializable;
import java.util.Objects;

public class Vote implements Serializable {

	private static final long serialVersionUID = 1L;

	private String votingId;
	private String voterUsername;
	private String selectedOption;
	private byte[] encryptedVote;
	private byte[] encryptedSymmetricKey;
	private byte[] digitalSignature;
	private byte[] iv;
	private long timestamp;

	public Vote(String votingId, String voterUsername, String selectedOption) {
		this.votingId = votingId;
		this.voterUsername = voterUsername;
		this.selectedOption = selectedOption;
		this.timestamp = System.currentTimeMillis();
	}

	public String getVotingId() {
		return votingId;
	}

	public String getVoterUsername() {
		return voterUsername;
	}

	public String getSelectedOption() {
		return selectedOption;
	}

	public void setSelectedOption(String selectedOption) {
		this.selectedOption = selectedOption;
	}

	public byte[] getEncryptedVote() {
		return encryptedVote;
	}

	public void setEncryptedVote(byte[] encryptedVote) {
		this.encryptedVote = encryptedVote;
	}

	public byte[] getEncryptedSymmetricKey() {
		return encryptedSymmetricKey;
	}

	public void setEncryptedSymmetricKey(byte[] encryptedSymmetricKey) {
		this.encryptedSymmetricKey = encryptedSymmetricKey;
	}

	public byte[] getDigitalSignature() {
		return digitalSignature;
	}

	public void setDigitalSignature(byte[] digitalSignature) {
		this.digitalSignature = digitalSignature;
	}

	public byte[] getIv() {
		return iv;
	}

	public void setIv(byte[] iv) {
		this.iv = iv;
	}

	public long getTimestamp() {
		return timestamp;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;

		if (o == null || getClass() != o.getClass()) return false;

		Vote vote = (Vote) o;

		return Objects.equals(votingId, vote.votingId)
				&& Objects.equals(voterUsername, vote.voterUsername);
	}

	@Override
	public int hashCode() {
		return Objects.hash(votingId, voterUsername);
	}

	@Override
	public String toString() {
		return "Vote{"
				+ "votingId='"
				+ votingId
				+ '\''
				+ ", voterUsername='"
				+ voterUsername
				+ '\''
				+ ", selectedOption='"
				+ selectedOption
				+ '\''
				+ ", timestamp="
				+ timestamp
				+ '}';
	}
}
