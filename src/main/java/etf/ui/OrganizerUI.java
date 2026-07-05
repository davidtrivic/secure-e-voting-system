package etf.ui;

import etf.system.EVotingSystem;
import etf.users.Organizer;
import etf.voting.Voting;
import etf.voting.VotingReport;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

public class OrganizerUI {

	private static final SimpleDateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss");

	public static void createNewVoting(Scanner scanner, EVotingSystem system, Organizer organizer) {
		System.out.println("\n--- KREIRANJE NOVOG GLASANJA ---");

		System.out.print("Naslov glasanja: ");
		String title = scanner.nextLine().trim();

		System.out.print("Opis glasanja: ");
		String description = scanner.nextLine().trim();

		System.out.print("Pocetak glasanja (dd.MM.yyyy HH:mm:ss): ");
		String startTimeStr = scanner.nextLine().trim();

		System.out.print("Kraj glasanja (dd.MM.yyyy HH:mm:ss): ");
		String endTimeStr = scanner.nextLine().trim();
		System.out.print("Broj opcija (2-5): ");

		int numOptions = 0;

		try {
			numOptions = Integer.parseInt(scanner.nextLine().trim());

			if (numOptions < 2 || numOptions > 5) {
				System.out.println("[X] Broj opcija mora biti izmedju 2 i 5!");
				return;
			}

		} catch (NumberFormatException e) {
			System.out.println("[X] Nevalidan broj!");
			return;
		}
		System.out.println("Unesite opcije:");

		java.util.List<String> options = new java.util.ArrayList<>();

		for (int i = 1; i <= numOptions; i++) {
			System.out.print("Opcija " + i + ": ");

			options.add(scanner.nextLine().trim());
		}
		try {
			Date startTime = dateFormat.parse(startTimeStr);

			Date endTime = dateFormat.parse(endTimeStr);

			if (startTime.after(endTime)) {
				System.out.println("[X] Pocetak ne moze biti posle kraja!");

				return;
			}
			if (endTime.getTime() < System.currentTimeMillis()) {
				System.out.println("[X] Vrijeme zatvaranje glasanja je vec proslo!");

				return;
			}
			Voting voting =
					new Voting(organizer.getUsername(), title, description, startTime, endTime, options);

			voting.activate();

			system.addVoting(voting);

			System.out.println("[OK] Glasanje uspesno kreirano!");
			System.out.println("  ID: " + voting.getId());
			System.out.println("  Naslov: " + voting.getTitle());
			System.out.println("  Status: " + voting.getStatus());
		} catch (Exception e) {
			System.out.println("[X] Greska: " + e.getMessage());
		}
	}

	public static void listVotings(EVotingSystem system, Organizer organizer) {
		System.out.println("\n--- MOJA GLASANJA ---");

		List<Voting> votings = system.getVotingsByOrganizer(organizer.getUsername());

		if (votings.isEmpty()) {
			System.out.println("Nema kreiranog glasanja\n");

			return;
		}
		for (int i = 0; i < votings.size(); i++) {
			Voting voting = votings.get(i);

			if (voting.getStatus() != Voting.VotingStatus.RESULTS_CALCULATED) {
				voting.activate();

				if (voting.isVoteExpired() && voting.getStatus() == Voting.VotingStatus.ACTIVE) {
					voting.close();
				}
			}
			System.out.println("\n[" + (i + 1) + "] " + voting.getTitle());

			System.out.println("    ID: " + voting.getId());
			System.out.println("    Status: " + voting.getStatus());
			System.out.println("    Pocetak: " + dateFormat.format(voting.getStartTime()));
			System.out.println("    Kraj: " + dateFormat.format(voting.getEndTime()));
			System.out.println("    Opcije: " + voting.getOptions());
			System.out.println("    Glasova: " + voting.getVotes().size());
		}
	}

	public static void countVotes(Scanner scanner, EVotingSystem system, Organizer organizer) {
		System.out.println("\n--- BROJANJE GLASOVA ---");

		System.out.print("Unesite ID glasanja: ");

		String votingId = scanner.nextLine().trim();

		Voting voting = system.findVoting(votingId);

		if (voting == null) {
			System.out.println("[X] Glasanje nije pronadjeno!");

			return;
		}
		if (!voting.getOrganizerId().equals(organizer.getUsername())) {
			System.out.println("[X] Nemate pristup ovom glasanju!");

			return;
		}
		if (voting.getStatus() == Voting.VotingStatus.ACTIVE && !voting.isVoteExpired()) {
			System.out.println("[X] Glasanje je jos uvek aktivno!");
			System.out.println("    Glasanje se zavrsava: " + dateFormat.format(voting.getEndTime()));

			return;
		}
		try {
			Map<String, Integer> results = system.countVotes(votingId);

			System.out.println("[OK] Glasovi su izbrojani!");
			System.out.println("\nRezultati:");

			for (Map.Entry<String, Integer> entry : results.entrySet()) {
				System.out.println("  " + entry.getKey() + ": " + entry.getValue() + " glasova");
			}
			System.out.println("\nGenerisanje izvjestaja...");

			VotingReport report =
					new VotingReport(
							voting.getId(),
							voting.getTitle(),
							voting.getResults(),
							voting.getVotes().size(),
							organizer.getUsername());

			report.signReport(organizer.getPrivateKey());

			String reportPath = report.saveToFile();

			System.out.println("\n=======================================");
			System.out.println("[OK] IZVJESTAJ JE AUTOMATSKI GENERISAN!");
			System.out.println("=======================================");

			System.out.println("Izvjestaj je digitalno potpisan");

			System.out.println("Sacuvan u: " + reportPath);

			System.out.println();
		} catch (Exception e) {
			System.out.println("[X] Greska: " + e.getMessage());

			e.printStackTrace();
		}
	}
}
