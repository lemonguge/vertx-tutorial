package cn.homjie.vertx.tutorial.juc;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
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
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

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
        //secretKeyStore();
        //pairKeyStore();
        //certificateKeyStore();

        //generateKeyPair();
        //generateKey();

        keyPairSave();
    }

    private static void keyPairSave() throws Exception {
        KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
        kpg.initialize(2048);
        KeyPair kp = kpg.generateKeyPair();
        Key pvt = kp.getPrivate();
        Key pub = kp.getPublic();

        // Private key format: PKCS#8, Public key format: X.509
        System.out.println("Private key format: " + pvt.getFormat() + ", Public key format: " + pub.getFormat());

        Base64.Encoder encoder = Base64.getEncoder();
        try (FileWriter out = new FileWriter("mqtt.key")) {
            out.write(encoder.encodeToString(pvt.getEncoded()));
        }
        try (FileWriter out = new FileWriter("mqtt.pub")) {
            out.write(encoder.encodeToString(pub.getEncoded()));
        }

        Base64.Decoder decoder = Base64.getDecoder();
        KeyFactory kf = KeyFactory.getInstance("RSA");

        Path path = Paths.get("mqtt.key");
        byte[] bytes = decoder.decode(Files.readAllLines(path).get(0));
        PKCS8EncodedKeySpec pkcs8EncodedKeySpec = new PKCS8EncodedKeySpec(bytes);
        System.out.println(kf.generatePrivate(pkcs8EncodedKeySpec).equals(pvt));

        path = Paths.get("mqtt.pub");
        bytes = decoder.decode(Files.readAllLines(path).get(0));
        X509EncodedKeySpec x509EncodedKeySpec = new X509EncodedKeySpec(bytes);
        System.out.println(kf.generatePublic(x509EncodedKeySpec).equals(pub));
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
            # See https://mosquitto.org/man/mosquitto-tls-7.html
            # Generate a certificate authority certificate and key.
            openssl req -new -x509 -days 365 -extensions v3_ca -keyout ca.key -out ca.crt
            
            # Generate a server key.
            openssl genrsa -des3 -out server.key 2048
            # Generate a server key without encryption.
            openssl genrsa -out server.key 2048
            # Generate a certificate signing request to send to the CA.
            openssl req -out server.csr -key server.key -new
            # Send the CSR to the CA, or sign it with your CA key:
            openssl x509 -req -in server.csr -CA ca.crt -CAkey ca.key -CAcreateserial -out server.crt -days 365
            
            # Generate a client key.
            openssl genrsa -des3 -out client.key 2048
            # Generate a certificate signing request to send to the CA.
            openssl req -out client.csr -key client.key -new
            # Send the CSR to the CA, or sign it with your CA key:
            # openssl x509 -req -in client.csr -CA ca.crt -CAkey ca.key -CAcreateserial -out client.crt -days <duration>
            ------
            # Generate CA private key format PKCS1
            openssl genrsa -out mqtt.key 2048
            # private key convert PKCS8
            openssl pkcs8 -topk8 -inform pem -in mqtt.key -outform pem -nocrypt -out mqtt.pem
            # Generate CSR
            openssl req -new -key mqtt.key -out mqtt.csr
            # Generate Self Signed certificate（CA 根证书）
            openssl x509 -req -days 365 -in mqtt.csr -signkey mqtt.key -out mqtt.crt
            ------
            # PKCS#12，由X.509证书和对应的私钥组成
            openssl pkcs12 -export -in mqtt.crt -inkey mqtt.key -out mqtt.p12
            # 将p12文件转换为jks
            keytool -importkeystore -srckeystore mqtt.p12 -srcstoretype pkcs12 -destkeystore mqtt.jks
            # 将证书转换为jks
            keytool -import -trustcacerts -file ca.crt -keystore truststore.jks
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

    private static void generateKeyPair() throws NoSuchAlgorithmException, InvalidKeySpecException {
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

    private static void generateKey() throws NoSuchAlgorithmException, InvalidKeySpecException {
        KeyGenerator kg = KeyGenerator.getInstance("DES");
        SecretKey key = kg.generateKey();
        System.out.println(kg.getProvider());
        System.out.println(kg.getAlgorithm());
        SecretKeyFactory skf = SecretKeyFactory.getInstance("DES");
        DESKeySpec desKS = (DESKeySpec)skf.getKeySpec(key, DESKeySpec.class);
        System.out.println("\tDES key bytes size:" + desKS.getKey().length);
    }
}
