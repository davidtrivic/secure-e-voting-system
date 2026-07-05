package etf.voting;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public class Voting implements Serializable {

	private static final long serialVersionUID = 1L;

	private String id;

	private String organizerId;

	private String title;

	private String description;

	private Date startTime;

	private Date endTime;

	private List<String> options;

	private List<Vote> votes;

	private VotingStatus status;

	private Map<String, Integer> results;

	private byte[] hmacMetadata;

	public enum VotingStatus {
		CREATED,
		ACTIVE,
		CLOSED,
		RESULTS_CALCULATED
	}

	public Voting(
			String organizerId,
			String title,
			String description,
			Date startTime,
			Date endTime,
			List<String> options) {
		this.id = UUID.randomUUID().toString();

		this.organizerId = organizerId;
		this.title = title;
		this.description = description;
		this.startTime = startTime;
		this.endTime = endTime;
		this.options = new ArrayList<>(options);
		this.votes = new ArrayList<>();
		this.status = VotingStatus.CREATED;
		this.results = new HashMap<>();

		try {
			byte[] hmacKey = new byte[32];

			new java.security.SecureRandom().nextBytes(hmacKey);

			String metadata =
					id
							+ "|"
							+ organizerId
							+ "|"
							+ title
							+ "|"
							+ startTime.getTime()
							+ "|"
							+ endTime.getTime();

			this.hmacMetadata = etf.crypto.EncryptionManager.generateHMAC(metadata.getBytes("UTF-8"), hmacKey);
		} catch (Exception e) {
			this.hmacMetadata = null;
		}
		try {
			saveHMACToFile();
		} catch (Exception e) {
			System.err.println("Greska pri cuvanju HMAC: " + e.getMessage());
		}
	}

	public boolean addVote(Vote vote) {
		if (status != VotingStatus.ACTIVE) {
			return false;
		}
		if (isVoteExpired()) {
			status = VotingStatus.CLOSED;

			return false;
		}
		for (Vote existingVote : votes) {
			if (existingVote.getVoterUsername().equals(vote.getVoterUsername())) {
				return false;
			}
		}
		votes.add(vote);

		try {
			saveVoteToFile(vote);
		} catch (Exception e) {
			System.err.println("Greska pri cuvanju glasa: " + e.getMessage());
		}
		return true;
	}

	private void saveVoteToFile(Vote vote) throws Exception {
		String votingDir = "./data/votings/" + this.id;

		java.io.File dir = new java.io.File(votingDir);

		if (!dir.exists()) {
			dir.mkdirs();
		}
		String voteFileName = votingDir + "/vote_" + vote.getVoterUsername() + ".dat";

		try (java.io.FileOutputStream fos = new java.io.FileOutputStream(voteFileName)) {
			java.io.ObjectOutputStream oos = new java.io.ObjectOutputStream(fos);

			oos.writeObject(vote);

			oos.flush();
		}
	}

	public void saveHMACToFile() throws Exception {
		if (hmacMetadata == null) {
			return;
		}
		String votingDir = "./data/votings/" + this.id;

		java.io.File dir = new java.io.File(votingDir);

		if (!dir.exists()) {
			dir.mkdirs();
		}
		String hmacFileName = votingDir + "/hmac_metadata.dat";

		try (java.io.FileOutputStream fos = new java.io.FileOutputStream(hmacFileName)) {
			fos.write(hmacMetadata);
		}
	}

	public void activate() {
		if (status == VotingStatus.CREATED && System.currentTimeMillis() >= startTime.getTime()) {
			status = VotingStatus.ACTIVE;
		}
	}

	public boolean isVoteExpired() {
		return System.currentTimeMillis() > endTime.getTime();
	}

	public boolean isActive() {
		return status == VotingStatus.ACTIVE && !isVoteExpired();
	}

	public void close() {
		status = VotingStatus.CLOSED;
	}

	public void closeIfExpired() {
		if (isVoteExpired() && status == VotingStatus.ACTIVE) {
			status = VotingStatus.CLOSED;
		}
	}

	public void setResults(Map<String, Integer> results) {
		this.results = new HashMap<>(results);

		this.status = VotingStatus.RESULTS_CALCULATED;
	}

	public String getId() {
		return id;
	}

	public String getOrganizerId() {
		return organizerId;
	}

	public String getTitle() {
		return title;
	}

	public String getDescription() {
		return description;
	}

	public Date getStartTime() {
		return startTime;
	}

	public Date getEndTime() {
		return endTime;
	}

	public List<String> getOptions() {
		return Collections.unmodifiableList(options);
	}

	public List<Vote> getVotes() {
		return Collections.unmodifiableList(votes);
	}

	public VotingStatus getStatus() {
		return status;
	}

	public Map<String, Integer> getResults() {
		return Collections.unmodifiableMap(results);
	}

	public byte[] getHmacMetadata() {
		return hmacMetadata;
	}

	public void setHmacMetadata(byte[] hmacMetadata) {
		this.hmacMetadata = hmacMetadata;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;

		if (o == null || getClass() != o.getClass()) return false;

		Voting voting = (Voting) o;

		return Objects.equals(id, voting.id);
	}

	@Override
	public int hashCode() {
		return Objects.hash(id);
	}

	@Override
	public String toString() {
		return "Voting{"
				+ "id='"
				+ id
				+ '\''
				+ ", title='"
				+ title
				+ '\''
				+ ", status="
				+ status
				+ ", votes="
				+ votes.size()
				+ '}';
	}
}
