package org.thoughtcrime.securesms.util;

import org.signal.core.util.logging.Log;

import javax.crypto.Cipher;
import java.security.Key;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import android.util.Base64;

public class RSAUtils {
  private static final String TAG = "RSAUtils";
  public static KeyPair generate() {
    try {
      KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
      keyGen.initialize(1024);
      return keyGen.generateKeyPair();
    } catch (NoSuchAlgorithmException e) {
      Log.e(TAG,"generate failed !", e);
      throw new RuntimeException(e);
    }
  }

  public static String keyToBase64(Key publicKey) {
    return Base64.encodeToString(publicKey.getEncoded(),Base64.DEFAULT);
  }

  public static String encrypt(String plainText, PublicKey publicKey) {
    try{
      Cipher encryptCipher = Cipher.getInstance("RSA/ECB/OAEPWithSHA-256AndMGF1Padding");
      encryptCipher.init(Cipher.ENCRYPT_MODE, publicKey);
      return Base64.encodeToString(encryptCipher.doFinal(plainText.getBytes()),Base64.DEFAULT);
    }catch (Throwable e){
      Log.e(TAG,"encrypt failed !", e);
    }
    return "";
  }

  public static String decrypt(String base64CipherText, PrivateKey privateKey){
    try{
      byte[] cipherText = Base64.decode(base64CipherText,Base64.DEFAULT);
      Cipher decryptCipher = Cipher.getInstance("RSA/ECB/OAEPWithSHA-256AndMGF1Padding");
      decryptCipher.init(Cipher.DECRYPT_MODE, privateKey);
      return new String(decryptCipher.doFinal(cipherText));
    }catch (Throwable e){
      Log.e(TAG,"decrypt failed !", e);
    }
    return "";
  }

  public static PublicKey getPublicKeyFromBase64(String base64PublicKey) {
    byte[]             decodedKey = Base64.decode(base64PublicKey,Base64.DEFAULT);
    X509EncodedKeySpec keySpec    = new X509EncodedKeySpec(decodedKey);
    try {
      KeyFactory keyFactory = KeyFactory.getInstance("RSA");
      return keyFactory.generatePublic(keySpec);
    } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
      throw new RuntimeException(e);
    }
  }

}
