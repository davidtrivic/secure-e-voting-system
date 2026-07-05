package etf.ui;

import etf.system.EVotingSystem;
import etf.users.Voter;
import etf.voting.Vote;
import etf.voting.VoteProcessor;
import etf.voting.Voting;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Scanner;

public class VoterUI {

	private static final SimpleDateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss");

	public static void listActiveVotings(EVotingSystem system) {
		System.out.println("\n--- AKTIVNA GLASANJA ---");

		List<Voting> votings = system.getActiveVotings();

		if (votings.isEmpty()) {
			System.out.println("Trenutno nema aktivnih glasanja");

			return;
		}
		for (int i = 0; i < votings.size(); i++) {
			Voting voting = votings.get(i);

			System.out.println("\n[" + (i + 1) + "] " + voting.getTitle());

			System.out.println("    ID: " + voting.getId());
			System.out.println("    Opis: " + voting.getDescription());
			System.out.println("    Kraj glasanja: " + dateFormat.format(voting.getEndTime()));
			System.out.println("    Opcije: ");

			for (int j = 0; j < voting.getOptions().size(); j++) {
				System.out.println("      " + (j + 1) + ". " + voting.getOptions().get(j));
			}
		}
	}

	public static void participateInVoting(Scanner scanner, EVotingSystem system, Voter voter) {
		System.out.println("\n--- GLASANJE ---");

		System.out.print("Unesite ID glasanja na kojem zelite da glasate: ");

		String votingId = scanner.nextLine().trim();

		Voting voting = system.findVoting(votingId);

		if (voting == null) {
			System.out.println("[X] Glasanje nije pronadjeno!");
			return;
		}
		if (!voting.isActive()) {
			System.out.println("[X] Glasanje nije aktivno!");
			return;
		}
		for (Vote existingVote : voting.getVotes()) {
			if (existingVote.getVoterUsername().equals(voter.getUsername())) {
				System.out.println("[X] Vec ste glasali na ovom glasanju!");
				return;
			}
		}
		System.out.println("\nGlasanje: " + voting.getTitle());

		System.out.println("Opcije:");

		for (int i = 0; i < voting.getOptions().size(); i++) {
			System.out.println("  " + (i + 1) + ". " + voting.getOptions().get(i));
		}
		System.out.print("\nOdaberite opciju (1-" + voting.getOptions().size() + "): ");

		int choice = 0;

		try {
			choice = Integer.parseInt(scanner.nextLine().trim());

			if (choice < 1 || choice > voting.getOptions().size()) {
				System.out.println("[X] Nevalidna opcija!");

				return;
			}

		} catch (NumberFormatException e) {
			System.out.println("[X] Nevalidan broj!");

			return;
		}
		String selectedOption = voting.getOptions().get(choice - 1);

		try {
			Vote vote = new Vote(votingId, voter.getUsername(), selectedOption);

			String organizerId = voting.getOrganizerId();

			etf.users.User organizerUser = system.findUser(organizerId);

			if (organizerUser == null) {
				System.out.println("[X] Organizator nije pronadjen!");

				return;
			}
			VoteProcessor.encryptVote(
					vote, organizerUser.getCertificate().getPublicKey(), voter.getPrivateKey());

			if (system.addVote(votingId, vote)) {
				System.out.println("\n==========================");

				System.out.println("[OK] GLAS USPJESNO UPISAN!");
				System.out.println("==========================");
				System.out.println("Vas glas je enkriptovan i digitalno potpisan");
				System.out.println("Verifikacioni kod: " + vote.getVotingId().substring(0, 8));
				System.out.println("\n=========================");
			} else {
				System.out.println("[X] Greska pri upisu glasa!");
			}

		} catch (Exception e) {
			System.out.println("[X] Greska pri enkriptovanju: " + e.getMessage());

			e.printStackTrace();
		}
	}

	public static void verifyVote(Scanner scanner, EVotingSystem system, Voter voter) {
		System.out.println("\n--- VERIFIKACIJA GLASA ---");

		System.out.print("Unesite ID glasanja: ");

		String votingId = scanner.nextLine().trim();

		Voting voting = system.findVoting(votingId);

		if (voting == null) {
			System.out.println("[X] Glasanje nije pronadjeno!");

			return;
		}
		Vote vote = null;

		for (Vote v : voting.getVotes()) {
			if (v.getVoterUsername().equals(voter.getUsername())) {
				vote = v;

				break;
			}
		}
		if (vote == null) {
			System.out.println("[X] Niste glasali na ovom glasanju!");

			return;
		}
		try {
			boolean isValid =
					VoteProcessor.verifyVoteSignature(vote, voter.getCertificate().getPublicKey());

			System.out.println("\n=========================");

			if (isValid) {
				System.out.println("[OK] VAS GLAS JE VALIDAN!");
				System.out.println("\n=========================");
				System.out.println("Digitalni potpis je ispravan");
				System.out.println("Glas je enkriptovan i zasticen");

				System.out.println("Vrijeme: " + new java.util.Date(vote.getTimestamp()));
			} else {
				System.out.println("[X] VAS GLAS NIJE VALIDAN!");
				System.out.println("\n=========================");
			}
			System.out.println("\n=========================\n");
		} catch (Exception e) {
			System.out.println("[X] Greska pri verifikaciji: " + e.getMessage());
		}
	}
}
