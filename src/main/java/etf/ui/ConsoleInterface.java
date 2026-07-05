package etf.ui;

import etf.auth.AuthenticationManager;
import etf.auth.AuthenticationManager.AuthenticationResult;
import etf.auth.RegistrationManager;
import etf.auth.RegistrationManager.ValidationResult;
import etf.system.EVotingSystem;
import etf.users.Organizer;
import etf.users.User;
import etf.users.Voter;
import java.util.Scanner;

public class ConsoleInterface {

	private EVotingSystem system;
	private AuthenticationManager authManager;
	private RegistrationManager regManager;
	private Scanner scanner;
	private User currentUser;
	private boolean running;

	public ConsoleInterface(
			EVotingSystem system, AuthenticationManager authManager, RegistrationManager regManager) {
		this.system = system;
		this.authManager = authManager;
		this.regManager = regManager;
		this.scanner = new Scanner(System.in);
		this.running = true;
		this.currentUser = null;
	}

	public void start() {
		System.out.println("\n=== E-VOTING SISTEM ===");
		System.out.println("Sigurno Elektronsko Glasanje\n");

		while (running) {
			if (currentUser == null) {
				showMainMenu();
			} else {
				if ("Organizer".equals(currentUser.getUserType())) {
					showOrganizerMenu();
				} else {
					showVoterMenu();
				}
			}
		}
		scanner.close();

		System.out.println("\nHvala sto ste koristili E-voting sistem!");
	}

	private void showMainMenu() {
		System.out.println("\n--- GLAVNI MENI ---");
		System.out.println("1. Registracija novog korisnika");
		System.out.println("2. Prijava");
		System.out.println("3. Izlaz");
		System.out.print("Odaberite opciju: ");

		String choice = scanner.nextLine().trim();

		switch (choice) {
			case "1":
				showRegistrationMenu();
				break;

			case "2":
				showLoginMenu();
				break;

			case "3":
				running = false;
				break;

			default:
				System.out.println("Nevalidna opcija!");
		}
	}

	private void showRegistrationMenu() {
		System.out.println("\n--- REGISTRACIJA ---");
		System.out.println("1. Registruj se kao ORGANIZATOR");
		System.out.println("2. Registruj se kao GLASAC");
		System.out.println("3. Nazad");

		System.out.print("Odaberite tip naloga: ");
		String choice = scanner.nextLine().trim();

		switch (choice) {
			case "1":
				registerOrganizer();

				break;

			case "2":
				registerVoter();

				break;

			case "3":
				break;

			default:
				System.out.println("Nevalidna opcija!");
		}
	}

	private void registerOrganizer() {
		System.out.println("\n--- REGISTRACIJA ORGANIZATORA ---");

		System.out.print("Naziv organizacije: ");
		String organizationName = scanner.nextLine().trim();

		System.out.print("ID organizacije: ");
		String organizationId = scanner.nextLine().trim();

		System.out.print("Lozinka (min 8 karaktera): ");
		String password = scanner.nextLine();

		System.out.print("Ponovi lozinku: ");
		String password2 = scanner.nextLine();

		if (!password.equals(password2)) {
			System.out.println("[X] Lozinke se ne poklapaju!");
			return;
		}
		if (system.organizerExists(organizationId)) {
			System.out.println("[X] Organizator sa ID-em '" + organizationId + "' vec postoji!");
			return;
		}
		if (system.userExists(organizationName)) {
			System.out.println("[X] Korisnik sa nazivom '" + organizationName + "' vec postoji!");
			return;
		}
		ValidationResult result =
				regManager.validateOrganizerRegistration(organizationName, organizationId, password);

		if (!result.valid) {
			System.out.println("[X] " + result.message);
			return;
		}
		try {
			Organizer org = regManager.registerOrganizer(organizationName, organizationId, password);

			system.addUser(org);

			System.out.println("[OK] Organizer uspesno registrovan!");
			System.out.println("  Sertifikat je generisan: " + org.getCertificate().getSubjectDN());
		} catch (Exception e) {
			System.out.println("[X] Greska pri registraciji: " + e.getMessage());
		}
	}

	private void registerVoter() {
		System.out.println("\n--- REGISTRACIJA GLASACA ---");

		System.out.print("Ime: ");
		String firstName = scanner.nextLine().trim();

		System.out.print("Prezime: ");
		String lastName = scanner.nextLine().trim();

		System.out.print("Korisnicko ime: ");
		String username = scanner.nextLine().trim();

		System.out.print("Lozinka (min 8 karaktera): ");
		String password = scanner.nextLine();

		System.out.print("Ponovi lozinku: ");
		String password2 = scanner.nextLine();

		if (!password.equals(password2)) {
			System.out.println("[X] Lozinke se ne poklapaju!");
			return;
		}
		if (system.userExists(username)) {
			System.out.println("[X] Korisnik sa korisnickim imenom '" + username + "' vec postoji!");
			return;
		}
		ValidationResult result =
				regManager.validateVoterRegistration(firstName, lastName, username, password);

		if (!result.valid) {
			System.out.println("[X] " + result.message);
			return;
		}
		try {
			Voter voter = regManager.registerVoter(firstName, lastName, username, password);

			system.addUser(voter);

			System.out.println("[OK] Glasac uspesno registrovan!");
			System.out.println("  Sertifikat je generisan: " + voter.getCertificate().getSubjectDN());
		} catch (Exception e) {
			System.out.println("[X] Greska pri registraciji: " + e.getMessage());
		}
	}

	private void showLoginMenu() {
		System.out.println("--- PRIJAVA ---");
		System.out.println("======================================");
		System.out.println();

		System.out.println("[KORAK 1/2] Validacija digitalnog sertifikata");
		System.out.println("--------------------------------------------");

		System.out.println("Unesite putanju do vaseg sertifikata.");
		System.out.println("Primjeri:");
		System.out.println("  - Za glasaca: ./data/voters/{username}/certificate.pem");
		System.out.println("  - Za organizatora: ./data/organizers/{orgId}/certificate.pem");

		System.out.println();

		System.out.print("Putanja sertifikata: ");

		String certificatePath = scanner.nextLine().trim();

		AuthenticationManager.CertificateValidationResult certResult =
				authManager.validateCertificateFromFile(certificatePath);

		if (!certResult.valid) {
			System.out.println("\n[X] GRESKA - " + certResult.message);
			System.out.println("Prijava prekinuta.\n");

			return;
		}
		System.out.println("[OK] " + certResult.message);

		System.out.println(
				"     Sertifikat DN: " + certResult.certificate.getSubjectX500Principal().getName());
		System.out.println("     Serial Number: " + certResult.certificate.getSerialNumber());
		System.out.println("     Validan od: " + certResult.certificate.getNotBefore());
		System.out.println("     Validan do: " + certResult.certificate.getNotAfter());
		System.out.println();

		System.out.println("[KORAK 2/2] Unos korisnickih podataka");
		System.out.println("--------------------------------------------");

		System.out.print("Korisnicko ime: ");
		String username = scanner.nextLine().trim();

		System.out.print("Lozinka: ");
		String password = scanner.nextLine();

		User user = system.findUser(username);

		if (user == null) {
			System.out.println("\n[X] Korisnik nije pronadjen!");
			return;
		}
		try {
			AuthenticationResult authResult =
					authManager.authenticateWithCredentials(certResult.certificate, username, password, user);

			if (authResult.success) {
				currentUser = user;

				system.refreshVotingStatuses();

				System.out.println("\n=====================================");
				System.out.println("[OK] USPJESNA PRIJAVA");
				System.out.println("=====================================");

				System.out.println("Dobrodosli, " + username + "!");
				System.out.println("Tip naloga: " + user.getUserType());
				System.out.println();
			} else {
				System.out.println("\n[X] " + authResult.message);
			}

		} catch (Exception e) {
			System.out.println("\n[X] Greska pri prijavi: " + e.getMessage());
		}
	}

	private void showOrganizerMenu() {
		System.out.println("\n--- MENI ORGANIZATORA ---");

		System.out.println("Korisnik: " + currentUser.getUsername());
		System.out.println("---------------------------");

		System.out.println("1. Kreiraj novo glasanje");
		System.out.println("2. Pregled mojih glasanja");
		System.out.println("3. Broji glasove");
		System.out.println("4. Odjava");
		System.out.println("5. Izlaz");

		System.out.print("Odaberite opciju: ");
		String choice = scanner.nextLine().trim();

		switch (choice) {
			case "1":
				OrganizerUI.createNewVoting(scanner, system, (Organizer) currentUser);
				break;

			case "2":
				OrganizerUI.listVotings(system, (Organizer) currentUser);
				break;

			case "3":
				OrganizerUI.countVotes(scanner, system, (Organizer) currentUser);
				break;

			case "4":
				authManager.logout(currentUser.getUsername());
				currentUser = null;
				System.out.println("[OK] Odlogovani ste");
				break;

			case "5":
				running = false;
				break;

			default:
				System.out.println("Nevalidna opcija!");
		}
	}

	private void showVoterMenu() {
		System.out.println("\n--- MENI GLASACA ---");
		System.out.println("Korisnik: " + currentUser.getUsername());
		System.out.println("----------------------");
		System.out.println("1. Pregled aktivnih glasanja");
		System.out.println("2. Glasaj");
		System.out.println("3. Verifikuj svoj glas");
		System.out.println("4. Odjava");
		System.out.println("5. Izlaz");

		System.out.print("Odaberite opciju: ");

		String choice = scanner.nextLine().trim();

		switch (choice) {
			case "1":
				VoterUI.listActiveVotings(system);
				break;

			case "2":
				VoterUI.participateInVoting(scanner, system, (Voter) currentUser);
				break;

			case "3":
				VoterUI.verifyVote(scanner, system, (Voter) currentUser);
				break;

			case "4":
				authManager.logout(currentUser.getUsername());
				currentUser = null;
				System.out.println("Odlogovani ste");
				break;

			case "5":
				running = false;
				break;

			default:
				System.out.println("Nevalidna opcija!");
		}
	}
}
