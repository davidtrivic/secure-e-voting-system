package etf.system;

import etf.users.Organizer;
import etf.users.User;
import etf.users.Voter;
import etf.voting.Vote;
import etf.voting.Voting;
import etf.voting.VotingCounter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EVotingSystem {

	private Map<String, User> users;
	private Map<String, Voting> votings;
	private List<String> revokedCertificates;

	public EVotingSystem() {
		this.users = new HashMap<>();
		this.votings = new HashMap<>();
		this.revokedCertificates = new ArrayList<>();
	}

	public boolean addUser(User user) {
		if (users.containsKey(user.getUsername())) {
			return false;
		}
		users.put(user.getUsername(), user);

		return true;
	}

	public User findUser(String username) {
		return users.get(username);
	}

	public boolean organizerExists(String organizationId) {
		for (User user : users.values()) {
			if (user instanceof Organizer) {
				Organizer org = (Organizer) user;

				if (org.getOrganizationId().equals(organizationId)) {
					return true;
				}
			}
		}
		return false;
	}

	public boolean voterExists(String username) {
		User user = users.get(username);

		return user != null && user instanceof Voter;
	}

	public boolean userExists(String username) {
		return users.containsKey(username);
	}

	public boolean addVoting(Voting voting) {
		if (votings.containsKey(voting.getId())) {
			return false;
		}
		votings.put(voting.getId(), voting);

		try {
			saveVotingToFile(voting);
		} catch (Exception e) {
			System.err.println("Greška pri čuvanju glasanja: " + e.getMessage());
		}
		return true;
	}

	public Voting findVoting(String votingId) {
		return votings.get(votingId);
	}

	public List<Voting> getVotingsByOrganizer(String organizerId) {
		List<Voting> result = new ArrayList<>();

		for (Voting voting : votings.values()) {
			if (voting.getOrganizerId().equals(organizerId)) {
				result.add(voting);
			}
		}
		return result;
	}

	public List<Voting> getActiveVotings() {
		List<Voting> result = new ArrayList<>();

		for (Voting voting : votings.values()) {
			if (voting.isActive()) {
				result.add(voting);
			}
		}
		return result;
	}

	public boolean addVote(String votingId, Vote vote) {
		Voting voting = votings.get(votingId);

		if (voting == null) {
			return false;
		}
		boolean success = voting.addVote(vote);

		if (success) {
			try {
				saveVotingToFile(voting);
			} catch (Exception e) {
				System.err.println("Greška pri ažuriranju glasanja: " + e.getMessage());
			}
		}
		return success;
	}

	public Map<String, Integer> countVotes(String votingId) throws Exception {
		Voting voting = votings.get(votingId);

		if (voting == null) {
			return null;
		}
		if (voting.isVoteExpired() && voting.getStatus() == Voting.VotingStatus.ACTIVE) {
			voting.close();
		}
		User organizerUser = findUser(voting.getOrganizerId());

		if (organizerUser == null || !(organizerUser instanceof etf.users.Organizer)) {
			throw new Exception("Organizator nije pronađen!");
		}
		etf.users.Organizer organizer = (etf.users.Organizer) organizerUser;

		VotingCounter counter = new VotingCounter(votingId);

		Map<String, Integer> results = counter.countVotes(voting, organizer.getPrivateKey());

		voting.setResults(results);

		try {
			saveVotingToFile(voting);
		} catch (Exception e) {
			System.err.println("Greška pri ažuriranju glasanja: " + e.getMessage());
		}
		return results;
	}

	public void revokeCertificate(String username) {
		User user = users.get(username);

		if (user != null) {
			revokedCertificates.add(user.getCertificate().getSerialNumber().toString());

			user.lock();
		}
	}

	public boolean isCertificateRevoked(String serialNumber) {
		return revokedCertificates.contains(serialNumber);
	}

	public Map<String, User> getUsers() {
		return new HashMap<>(users);
	}

	public Map<String, Voting> getVotings() {
		return new HashMap<>(votings);
	}

	public int getTotalUsers() {
		return users.size();
	}

	public int getTotalVotings() {
		return votings.size();
	}

	public void refreshVotingStatuses() {
		for (Voting voting : votings.values()) {
			voting.activate();
		}
	}

	private void saveVotingToFile(Voting voting) throws Exception {
		String votingDir = "./data/votings/" + voting.getId();

		java.io.File dir = new java.io.File(votingDir);

		dir.mkdirs();

		String votingFile = votingDir + "/voting.dat";

		try (java.io.FileOutputStream fos = new java.io.FileOutputStream(votingFile);
				java.io.ObjectOutputStream oos = new java.io.ObjectOutputStream(fos)) {
			oos.writeObject(voting);
		}
	}

	public void loadAllVotings() {
		java.io.File votingsDir = new java.io.File("./data/votings");

		if (!votingsDir.exists() || !votingsDir.isDirectory()) {
			return;
		}
		java.io.File[] votingDirs = votingsDir.listFiles();

		if (votingDirs == null) {
			return;
		}
		int loadedCount = 0;

		for (java.io.File votingDir : votingDirs) {
			if (votingDir.isDirectory()) {
				java.io.File votingFile = new java.io.File(votingDir, "voting.dat");

				if (votingFile.exists()) {
					try (java.io.FileInputStream fis = new java.io.FileInputStream(votingFile);
							java.io.ObjectInputStream ois = new java.io.ObjectInputStream(fis)) {
						Object obj = ois.readObject();

						if (obj instanceof Voting) {
							Voting voting = (Voting) obj;

							votings.put(voting.getId(), voting);

							loadedCount++;
						}

					} catch (Exception e) {
						System.err.println(
								"Greska pri ucitavanju glasanja iz "
										+ votingFile.getPath()
										+ ": "
										+ e.getMessage());
					}
				}
			}
		}
		if (loadedCount > 0) {
			System.out.println("[OK] Ucitano " + loadedCount + " glasanja iz fajlova");
		}
	}
}
