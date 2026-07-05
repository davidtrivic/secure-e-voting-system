package etf.voting;

import java.io.Serializable;
import java.security.PrivateKey;
import java.util.HashMap;
import java.util.Map;

public class VotingCounter implements Serializable {

	private static final long serialVersionUID = 1L;

	private String votingId;
	private Map<String, Integer> voteCount;
	private int totalVotes;

	public VotingCounter(String votingId) {
		this.votingId = votingId;
		this.voteCount = new HashMap<>();
		this.totalVotes = 0;
	}

	public Map<String, Integer> countVotes(Voting voting, PrivateKey organizerPrivateKey)
			throws Exception {
		voteCount.clear();

		totalVotes = 0;

		for (String option : voting.getOptions()) {
			voteCount.put(option, 0);
		}
		for (Vote vote : voting.getVotes()) {
			String selectedOption = VoteProcessor.decryptVote(vote, organizerPrivateKey);

			if (voteCount.containsKey(selectedOption)) {
				voteCount.put(selectedOption, voteCount.get(selectedOption) + 1);

				totalVotes++;
			} else {
				System.err.println("UPOZORENJE: Nevalidna opcija u glasu: " + selectedOption);
			}
		}
		return voteCount;
	}

	public static boolean verifyVote(Vote vote) {
		return vote.getEncryptedVote() != null
				&& vote.getEncryptedSymmetricKey() != null
				&& vote.getDigitalSignature() != null
				&& vote.getIv() != null;
	}

	public String getVotingId() {
		return votingId;
	}

	public Map<String, Integer> getVoteCount() {
		return new HashMap<>(voteCount);
	}

	public int getTotalVotes() {
		return totalVotes;
	}

	@Override
	public String toString() {
		return "VotingCounter{"
				+ "votingId='"
				+ votingId
				+ '\''
				+ ", totalVotes="
				+ totalVotes
				+ ", voteCount="
				+ voteCount
				+ '}';
	}
}
