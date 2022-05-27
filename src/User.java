import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.util.Arrays;
import java.util.Base64;
import java.util.Random;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

public class User {
    private static final String SALT = "EsameLaboratorioReti2022";
    private static final int ITERATION = 1000;
    private static final int KEY_LENGHT = 256;

    private String nickname;
    private String password;
    String[] tags;

    public User(String nickname, String password, String tag) {

    }

    private String encryptedPassword (String clearPassword){
        String encriptedPassword =;

        return encriptedPassword;
    }

}
