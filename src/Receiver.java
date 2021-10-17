import java.io.*;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.spec.RSAPublicKeySpec;
import java.util.*;
import java.security.*;
import javax.crypto.*;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public class Receiver {
    private static int BUFFER_SIZE = 32*1024;
    static String IV = "AAAAAAAAAAAAAAAA";
    public static void main(String[] args) throws Exception {
        //create a symmetric key from symmetric.key file for AES algorithm
        File symmetricKeyFile = new File("symmetric.key");
        Scanner sc = new Scanner(symmetricKeyFile);
        String sKey = sc.nextLine();
        byte[] byteKey = new byte[128];
        SecretKey secretKey = new SecretKeySpec(sKey.getBytes(StandardCharsets.UTF_8), "AES");

        Scanner scan = new Scanner(System.in);
        String fileName;
        System.out.println("Please enter name of the message file");
        fileName = scan.nextLine();

        // Calculate AES decryption of C with Kxy
        FileInputStream cipherText = new FileInputStream("message.aescipher");
        BufferedInputStream encryptedBuff = new BufferedInputStream(cipherText);
        FileOutputStream decryptedAES = new FileOutputStream("message.ds-msg2");
        byte[] availableInBuff = new byte[encryptedBuff.available()];
        encryptedBuff.read(availableInBuff);
        decryptedAES.write(AESdecrypt(availableInBuff,secretKey));
        //byte[] pieces = new byte[64];
/*        int bytesInStream;
        while ((bytesInStream = cipherText.read(pieces)) != -1) {
            if (bytesInStream == 64) {
                //encryptedBuff.read(pieces,0, pieces.length);
                decryptedAES.write(AESdecrypt(pieces,secretKey));
            }
            if (bytesInStream < 64 && bytesInStream > 0) {
                byte[] newByte = new byte[bytesInStream];
                //encryptedBuff.read(newByte,0,newByte.length);
                newByte = cipherText.readAllBytes();
                decryptedAES.write(AESdecrypt(newByte,secretKey));
            }
        }*/


        byte[] shaDecrypted = new byte[128];
        FileInputStream decryptedFile = new FileInputStream("message.ds-msg");
        BufferedInputStream newBuff = new BufferedInputStream(decryptedFile);
        newBuff.read(shaDecrypted,0,shaDecrypted.length);
        FileOutputStream newOut = new FileOutputStream(fileName,true);
        newOut.write(shaDecrypted);
        newOut.write(newBuff.readAllBytes());
        // Read first 128 bytes from message.ds-msg for digital signature and copy message M

        //Calculate RSA decryption using Kx+ and save into file named message.dd and output into console in hexadecimal
        FileOutputStream decryptRSAForSha = new FileOutputStream("message.dd");
        String decryptedSHA = RSAdecrypt(shaDecrypted);
        decryptRSAForSha.write(decryptedSHA.getBytes(StandardCharsets.UTF_8));
        System.out.println("Decrypted SHA256 in hex: ");

        for(int k = 0, j=0; k < shaDecrypted.length; k++, j++){
            System.out.format("%2X ", shaDecrypted[k]);

            if(j >= 15){
                System.out.println("");
                j = 0;
            }
        }

        // Read message M piece by piece; calculate SHA256 of all of M; output to console in hexadecimal bytes
        String sha256Value = SHA256Convert(fileName);
        byte[] sha256B = new byte[sha256Value.length()];
        sha256B = sha256Value.getBytes(StandardCharsets.UTF_8);
        System.out.println("Decrypted SHA256 for entire message in hex: ");

        for(int k = 0, j=0; k < sha256B.length; k++, j++){
            System.out.format("%2X ", sha256B[k]);

            if(j >= 15){
                System.out.println("");
                j = 0;
            }
        }
        // Compare with SHA256 digital digest; return if true or not

    }
    public static byte[] AESdecrypt (byte[] message,SecretKey key) throws Exception {
        Cipher cipher = Cipher.getInstance("AES/CBC/NoPadding");
        cipher.init(cipher.DECRYPT_MODE, key,new IvParameterSpec(IV.getBytes("UTF-8")));
        byte[] decrypted = cipher.doFinal(message);
        return decrypted;
    }

    public static String RSAdecrypt (byte[] message) throws Exception {
        FileInputStream keyFile = new FileInputStream("XPublic.key");
        ObjectInputStream publicKeyObject = new ObjectInputStream(keyFile);
        BigInteger modulus = (BigInteger) publicKeyObject.readObject();
        BigInteger exponent = (BigInteger) publicKeyObject.readObject();
        publicKeyObject.close();

        //Create the public key spec
        RSAPublicKeySpec publicSpec = new RSAPublicKeySpec(modulus, exponent);

        //create a key factory
        KeyFactory factory = KeyFactory.getInstance("RSA");

        //create the RSA public key
        PublicKey publicKeyX = factory.generatePublic(publicSpec);

        Cipher cipher = Cipher.getInstance("RSA/ECB/NoPadding");
        cipher.init(Cipher.DECRYPT_MODE, publicKeyX);
        //byte plaintext = cipher.
        return new String(cipher.doFinal(message));
    }
    public static String SHA256Convert(String messageInput) throws NoSuchAlgorithmException, IOException {
        BufferedInputStream file = new BufferedInputStream(new FileInputStream(messageInput));
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        DigestInputStream in = new DigestInputStream(file,md);
        int i;
        byte[] buffer = new byte[BUFFER_SIZE];
        do{
            i = in.read(buffer,0,BUFFER_SIZE);
        } while(i == BUFFER_SIZE);
        md = in.getMessageDigest();
        in.close();

        byte[] hash = md.digest();

        System.out.println("/n digital digest hash value: ");

        for(int k = 0, j=0; k < hash.length; k++, j++){
            System.out.format("%2X ", hash[k]);

            if(j >= 15){
                System.out.println("");
                j = 0;
            }
        }

        // We might have to put the try/catch block HERE!!!!!!!!

        return new String (hash);
    } // This is used to convert message to SHA256


    public static byte[] hexToByte (String hexString){
        byte[] val = new byte[hexString.length() / 2];

        for (int i = 0; i < val.length; i++) {
            int index = i * 2;
            int j = Integer.parseInt(hexString.substring(index, index + 2), 16);
            val[i] = (byte) j;
        }
        return val;
    }
}
