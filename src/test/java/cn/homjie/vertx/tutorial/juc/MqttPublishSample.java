package cn.homjie.vertx.tutorial.juc;

import java.io.FileInputStream;
import java.security.KeyStore;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManagerFactory;

import org.apache.commons.codec.digest.HmacAlgorithms;
import org.apache.commons.codec.digest.HmacUtils;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

/**
 * @author jiehong.jh
 * @date 2018/11/29
 */
public class MqttPublishSample {

    public static void main(String[] args) throws InterruptedException {

        String topic = "/lora/gwmp/uplink/pk/dn";
        String content = "Message from MqttPublishSample";
        int qos = 2;
        String broker = "ssl://30.43.89.203:1883";
        MemoryPersistence persistence = new MemoryPersistence();

        long millis = System.currentTimeMillis();
        String client = "Publish";
        String clientId = client + "|securemode=2,signmethod=hmacmd5,timestamp=" + millis + "|";
        String deviceName = "dn";
        String productKey = "pk";
        String deviceSecret = "secret";
        String value = "clientId" + client
            + "deviceName" + deviceName
            + "productKey" + productKey
            + "timestamp" + millis;
        String hmac = new HmacUtils(HmacAlgorithms.HMAC_MD5, deviceSecret).hmacHex(value);

        try {
            MqttClient sampleClient = new MqttClient(broker, clientId, persistence);
            MqttConnectOptions connOpts = new MqttConnectOptions();
            connOpts.setUserName(deviceName + "&" + productKey);
            connOpts.setPassword(hmac.toCharArray());
            connOpts.setCleanSession(true);
            connOpts.setSocketFactory(socketFactory());
            System.out.println("Connecting to broker: " + broker);
            sampleClient.connect(connOpts);
            System.out.println("Connected");

            sampleClient.setCallback(new MqttCallbackImpl());

            System.out.println("Publishing message: " + content);
            MqttMessage message = new MqttMessage(content.getBytes());
            message.setQos(qos);
            sampleClient.publish(topic, message);
            System.out.println("Message published");

            //sampleClient.disconnect();
            //System.out.println("Disconnected");

            //System.exit(0);
            Thread.currentThread().join();
        } catch (Exception me) {
            //System.out.println("reason " + me.getReasonCode());
            System.out.println("msg " + me.getMessage());
            System.out.println("loc " + me.getLocalizedMessage());
            System.out.println("cause " + me.getCause());
            System.out.println("excep " + me);
            me.printStackTrace();
        }
    }

    private static SSLSocketFactory socketFactory() throws Exception {
        SSLContext ctx = SSLContext.getInstance("TLS");

        CertificateFactory cf = CertificateFactory.getInstance("X509");
        X509Certificate caCert = (X509Certificate)cf.generateCertificate(new FileInputStream("ca.crt"));

        KeyStore caKs = KeyStore.getInstance(KeyStore.getDefaultType());
        caKs.load(null, null);
        caKs.setCertificateEntry("ca-certificate", caCert);
        TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        tmf.init(caKs);

        ctx.init(null, tmf.getTrustManagers(), null);

        return ctx.getSocketFactory();
    }
}
