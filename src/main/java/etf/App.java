package etf;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import java.security.Security;

public class App {
    public static void main(String[] args) {
        Security.addProvider(new BouncyCastleProvider());
        System.out.println("Bouncy Castle radi ✔");
    }
}
