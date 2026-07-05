package etf.voting;

import etf.crypto.EncryptionManager;
import java.io.Serializable;
import java.security.PrivateKey;
import java.util.Date;
import java.util.Map;

public class VotingReport implements Serializable {

	private static final long serialVersionUID = 1L;

	private String votingId;
	private String votingTitle;
	private Map<String, Integer> results;
	private int totalVotes;
	private Date generatedAt;
	private byte[] digitalSignature;
	private String signedBy;

	public VotingReport(
			String votingId,
			String votingTitle,
			Map<String, Integer> results,
			int totalVotes,
			String signedBy) {
		this.votingId = votingId;
		this.votingTitle = votingTitle;
		this.results = results;
		this.totalVotes = totalVotes;
		this.signedBy = signedBy;
		this.generatedAt = new Date();
	}

	public void signReport(PrivateKey organizerPrivateKey) throws Exception {
		String reportData = generateReportString();
		this.digitalSignature =
				EncryptionManager.signData(reportData.getBytes("UTF-8"), organizerPrivateKey);
	}

	private String generateReportString() {
		StringBuilder sb = new StringBuilder();

		sb.append("VOTING REPORT\n");
		sb.append("=============\n");
		sb.append("Voting ID: ").append(votingId).append("\n");
		sb.append("Voting Title: ").append(votingTitle).append("\n");
		sb.append("Generated: ").append(generatedAt).append("\n");
		sb.append("Signed By: ").append(signedBy).append("\n");
		sb.append("\nRESULTS:\n");
		sb.append("---------\n");

		for (Map.Entry<String, Integer> entry : results.entrySet()) {
			sb.append(entry.getKey()).append(": ").append(entry.getValue()).append(" votes\n");
		}
		sb.append("\nTotal Votes: ").append(totalVotes).append("\n");

		return sb.toString();
	}

	public String getReportAsString() {
		return generateReportString() + "\n[SIGNED]: " + (digitalSignature != null ? "YES" : "NO");
	}

	public String saveToFile() throws Exception {
		String votingDir = "./data/votings/" + votingId;

		java.io.File dir = new java.io.File(votingDir);

		if (!dir.exists()) {
			dir.mkdirs();
		}
		String reportFileName = votingDir + "/report.txt";

		try (java.io.FileWriter writer = new java.io.FileWriter(reportFileName)) {
			writer.write(getReportAsString());

			writer.flush();
		}
		return reportFileName;
	}

	public String getVotingId() {
		return votingId;
	}

	public String getVotingTitle() {
		return votingTitle;
	}

	public Map<String, Integer> getResults() {
		return results;
	}

	public int getTotalVotes() {
		return totalVotes;
	}

	public Date getGeneratedAt() {
		return generatedAt;
	}

	public byte[] getDigitalSignature() {
		return digitalSignature;
	}

	public String getSignedBy() {
		return signedBy;
	}

	@Override
	public String toString() {
		return getReportAsString();
	}
}
