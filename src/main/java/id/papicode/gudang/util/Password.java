package id.papicode.gudang.util;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

/** Hash password PBKDF2-HMAC-SHA256 dengan salt acak. Format: pbkdf2$iterasi$salt$hash (Base64). */
public final class Password {
    private static final int ITERASI = 120_000;
    private static final SecureRandom RNG = new SecureRandom();

    private Password() { }

    public static String hash(String pw) {
        byte[] salt = new byte[16];
        RNG.nextBytes(salt);
        byte[] h = pbkdf2(pw.toCharArray(), salt, ITERASI);
        Base64.Encoder b = Base64.getEncoder();
        return "pbkdf2$" + ITERASI + "$" + b.encodeToString(salt) + "$" + b.encodeToString(h);
    }

    public static boolean cocok(String pw, String simpanan) {
        if (pw == null || simpanan == null) return false;
        String[] p = simpanan.split("\\$");
        if (p.length != 4 || !p[0].equals("pbkdf2")) return false;
        Base64.Decoder d = Base64.getDecoder();
        byte[] salt = d.decode(p[2]);
        byte[] h = d.decode(p[3]);
        byte[] uji = pbkdf2(pw.toCharArray(), salt, Integer.parseInt(p[1]));
        return MessageDigest.isEqual(h, uji); // waktu konstan
    }

    private static byte[] pbkdf2(char[] pw, byte[] salt, int iter) {
        try {
            PBEKeySpec spec = new PBEKeySpec(pw, salt, iter, 256);
            return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
