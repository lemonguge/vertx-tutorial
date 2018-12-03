package cn.homjie.vertx.tutorial.juc;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.security.Key;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.SecureRandom;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.security.spec.DSAPrivateKeySpec;
import java.security.spec.InvalidKeySpecException;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.DESKeySpec;

/**
 * @author jiehong.jh
 * @date 2018/12/3
 */
public class KeyStoreMain {

    public static void main(String[] args) throws Exception {
        secretKeyStore();
        //pairKeyStore();
        //certificateKeyStore();

        //generateKeyPair();
        //generateKey();
    }

    private static void secretKeyStore() throws Exception {
        // JKS，只能保存私钥
        KeyStore ks = KeyStore.getInstance("PKCS12");

        char[] pwdArray = "pwd123!".toCharArray();
        char[] protect = "kspp543@".toCharArray();
        // 初始化
        ks.load(null, pwdArray);

        KeyGenerator keyGenerator = KeyGenerator.getInstance("AES");
        SecureRandom secureRandom = new SecureRandom();
        int keyBitSize = 256;
        keyGenerator.init(keyBitSize, secureRandom);
        SecretKey secretKey = keyGenerator.generateKey();

        KeyStore.SecretKeyEntry secret = new KeyStore.SecretKeyEntry(secretKey);
        KeyStore.ProtectionParameter password = new KeyStore.PasswordProtection(protect);
        ks.setEntry("db-encryption-secret", secret, password);

        String fileName = "secretKey.pkcs12";
        try (FileOutputStream fos = new FileOutputStream(fileName)) {
            ks.store(fos, pwdArray);
        }

        ks.load(new FileInputStream(fileName), pwdArray);

        Key key = ks.getKey("db-encryption-secret", protect);
        System.out.println(key.equals(secretKey));
    }

    private static void pairKeyStore() throws Exception {
        // JKS，只能保存私钥
        KeyStore ks = KeyStore.getInstance("JKS");

        char[] pwdArray = "pwd123!".toCharArray();
        char[] protect = "kspp543@".toCharArray();
        // 初始化
        ks.load(null, pwdArray);

        KeyPairGenerator kpg = KeyPairGenerator.getInstance("DSA");
        kpg.initialize(512);
        KeyPair kp = kpg.generateKeyPair();
        PrivateKey privateKey = kp.getPrivate();
        /*
            Generate CA private key
            openssl genrsa -out mqtt.key 2048
            # Generate CSR
            openssl req -new -key mqtt.key -out mqtt.csr
            # Generate Self Signed certificate（CA 根证书）
            openssl x509 -req -days 365 -in mqtt.csr -signkey mqtt.key -out mqtt.crt
         */
        CertificateFactory cf = CertificateFactory.getInstance("X509");
        Certificate certificate = cf.generateCertificate(new FileInputStream("mqtt.crt"));

        ks.setKeyEntry("sso-signing-key", privateKey, protect, new Certificate[] {certificate});

        String fileName = "pairKey.jks";
        try (FileOutputStream fos = new FileOutputStream(fileName)) {
            ks.store(fos, pwdArray);
        }

        ks.load(new FileInputStream(fileName), pwdArray);

        Key ssoSigningKey = ks.getKey("sso-signing-key", protect);
        System.out.println(ssoSigningKey.equals(privateKey));
    }

    private static void certificateKeyStore() throws Exception {
        // JKS，只能保存私钥
        KeyStore ks = KeyStore.getInstance("JKS");

        char[] pwdArray = "pwd123!".toCharArray();
        char[] protect = "kspp543@".toCharArray();
        // 初始化
        ks.load(null, pwdArray);

        CertificateFactory cf = CertificateFactory.getInstance("X509");
        Certificate certificate = cf.generateCertificate(new FileInputStream("mqtt.crt"));

        ks.setCertificateEntry("mqtt.iot", certificate);

        String fileName = "certificate.jks";
        try (FileOutputStream fos = new FileOutputStream(fileName)) {
            ks.store(fos, pwdArray);
        }

        ks.load(new FileInputStream(fileName), pwdArray);
        Certificate cert = ks.getCertificate("mqtt.iot");
        System.out.println(cert);
    }

    public static void generateKeyPair() throws NoSuchAlgorithmException, InvalidKeySpecException {
        KeyPairGenerator kpg = KeyPairGenerator.getInstance("DSA");
        kpg.initialize(512);
        KeyPair kp = kpg.generateKeyPair();
        System.out.println(kpg.getProvider());
        System.out.println(kpg.getAlgorithm());
        KeyFactory kf = KeyFactory.getInstance("DSA");
        DSAPrivateKeySpec dsaPKS = kf.getKeySpec(kp.getPrivate(), DSAPrivateKeySpec.class);
        System.out.println("\tDSA param G:" + dsaPKS.getG());
        System.out.println("\tDSA param P:" + dsaPKS.getP());
        System.out.println("\tDSA param Q:" + dsaPKS.getQ());
        System.out.println("\tDSA param X:" + dsaPKS.getX());
    }

    public static void generateKey() throws NoSuchAlgorithmException, InvalidKeySpecException {
        KeyGenerator kg = KeyGenerator.getInstance("DES");
        SecretKey key = kg.generateKey();
        System.out.println(kg.getProvider());
        System.out.println(kg.getAlgorithm());
        SecretKeyFactory skf = SecretKeyFactory.getInstance("DES");
        DESKeySpec desKS = (DESKeySpec)skf.getKeySpec(key, DESKeySpec.class);
        System.out.println("\tDES key bytes size:" + desKS.getKey().length);
    }
}