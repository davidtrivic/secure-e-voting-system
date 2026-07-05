# Secure E-Voting System

A console-based **secure electronic voting system** built for the Cryptography and Computer Security course. It demonstrates a full applied-cryptography stack - a two-tier PKI, certificate-based two-factor authentication, hybrid (asymmetric + symmetric) vote encryption, digital signatures, and HMAC-protected metadata - rather than relying on any single library's "batteries-included" security.

## Overview

The system supports two account types, **organizers** and **voters**, each issued a certificate by a dedicated Certificate Authority. Organizers create and manage votings; voters cast encrypted, digitally signed votes and can verify their vote was recorded without revealing its content. After a voting closes, the organizer decrypts and tallies the votes and a signed report is produced.

## Security Design

### Two-tier PKI

```
Root CA
├── Organization CA  →  issues certificates to organizers only
└── Voter CA         →  issues certificates to voters only
```

The Root CA only ever signs the two subordinate CAs - it never issues end-user certificates directly. Each issued certificate carries **key usage extensions** appropriate to its purpose (e.g. `keyCertSign`/`cRLSign` on the CA certificates, `digitalSignature`/`keyEncipherment` on end-user certificates) plus a custom extension identifying the holder's role (`Organizer` / `Voter`).

### Two-factor login

1. **Certificate check** - the user supplies their certificate, which is validated for:
   - time validity (not expired / not yet valid)
   - issuer (must chain to either the Organizer CA or the Voter CA)
   - revocation status (checked against the relevant CA's own CRL)
2. **Credentials check** - username + password, verified against the account bound to that certificate

Three consecutive failed login attempts automatically **revokes the user's certificate** (added to the appropriate CRL) and locks the account.

### Vote confidentiality & integrity

Each vote uses a **hybrid encryption** scheme:
- A fresh, random **AES symmetric key** is generated per vote and used to encrypt the selected option (AES/GCM)
- That symmetric key is itself encrypted with the **organizer's RSA public key** (RSA-OAEP), so only the organizer can recover it
- The encrypted vote is **digitally signed** with the voter's private key (SHA256withRSA), so tampering or forgery is detectable
- Voting metadata is stored separately from the votes themselves and integrity-protected with **HMAC-SHA256**

At tally time, the organizer's private key decrypts each vote's symmetric key, which in turn decrypts the vote - the organizer never needs the voter's private key, and votes stay confidential until counting.

### Private key protection

Private keys are never stored in plaintext: they're encrypted with a key derived from the owner's password via **PBKDF2WithHmacSHA256** (65,536 iterations, 256-bit key) and AES/GCM, with a random salt and IV per key.

## Features

- User registration (organizer or voter) with automatic certificate + key pair generation
- Two-step, certificate-based login with CRL and expiry checks
- Organizer: create votings (title, description, time window, 2–5 options), view voting status, trigger vote counting, retrieve signed results
- Voter: view active votings, cast a vote, receive confirmation, verify their vote was recorded without revealing its content
- Automatic certificate revocation after 3 failed login attempts
- Signed final report generated after counting

## Tech Stack

- **Java 21**
- **Bouncy Castle** (`bcprov-jdk18on`, `bcpkix-jdk18on`) - X.509 certificate generation/validation, RSA/AES/HMAC primitives
- **Gson** - JSON persistence
- **SLF4J** - logging
- **JUnit 4** - testing
- **Maven** - build

## Project Structure

```
src/main/java/etf/
├── auth/            # AuthenticationManager, RegistrationManager
├── certificates/    # CAManager (2-tier CA hierarchy), CRLManager
├── crypto/          # KeyManager, EncryptionManager (AES/RSA/HMAC/signatures)
├── system/          # EVotingSystem - top-level orchestration
├── ui/              # ConsoleInterface, OrganizerUI, VoterUI
├── users/           # User, Organizer, Voter
└── voting/          # Voting, Vote, VoteProcessor, VotingCounter, VotingReport
```

## Getting Started

### Prerequisites
- JDK 21+
- Maven 3.9+

### Build & Run

```bash
mvn clean package
java -jar target/evoting.jar
```

On first run, the app bootstraps the Root CA and the two subordinate CAs under `./data/ca/`. All generated keys, certificates, and voting data are persisted under `./data/` at runtime (not included in this repository - see note below).

## Note on Persisted Data

The `./data/` directory (CA keys/certificates, registered users, votings, cast votes) is generated at runtime and is intentionally excluded from version control - it contains encrypted private keys and personal registration data that shouldn't live in a public repository. The application recreates this directory structure automatically on first launch.
