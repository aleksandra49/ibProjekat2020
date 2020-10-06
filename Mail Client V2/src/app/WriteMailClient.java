package app;


import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.Certificate;
import java.util.Base64.Encoder;

import org.apache.xml.security.keys.KeyInfo;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.mail.internet.MimeMessage;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.apache.xml.security.encryption.EncryptedData;
import org.apache.xml.security.encryption.EncryptedKey;
import org.apache.xml.security.encryption.XMLCipher;
import org.apache.xml.security.utils.JavaUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.google.api.services.gmail.Gmail;

import certificat.KeyStoreReader;
import util.Base64;
import util.GzipUtil;
import util.IVHelper;
import kripto.AsimmetricKeyEncryption;
import signature.SignEnveloped;
import support.MailHelper;
import support.MailWritter;

public class WriteMailClient extends MailClient {

	private static final String KEY_FILE = "./data/session.key";
	private static final String IV1_FILE = "./data/iv1.bin";
	private static final String IV2_FILE = "./data/iv2.bin";
	private static final String userA_jks = "./data/userA.jks";
	private static final String userB_jks = "./data/userB.jks";
	private static final String userA_cer = "./data/userA.cer";
	
	public static void main(String[] args) {
		
        try {
        	Gmail service = getGmailService();
            
        	System.out.println("Insert a reciever:");
            BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
            String reciever = reader.readLine();
        	
            System.out.println("Insert a subject:");
            String subject = reader.readLine();
            
            
            System.out.println("Insert body:");
            String body = reader.readLine();
            
            
            DocumentBuilderFactory documentFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder documentBuilder = documentFactory.newDocumentBuilder();
            Document document = documentBuilder.newDocument();
            Element root = document.createElement("Mail");
            document.appendChild(root);
            
            Element subjectElement = document.createElement("subject");
            subjectElement.appendChild(document.createTextNode(subject));
            Element bodyElement = document.createElement("body");
            bodyElement.appendChild(document.createTextNode(body));
            root.appendChild(subjectElement);
            root.appendChild(bodyElement);
            
            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer();
            
           
            PrivateKey pk = SignEnveloped.readPrivateKey(userA_jks, "1234", "usera");
    		Certificate cert = SignEnveloped.readCertificate(userA_jks, "1234", "usera");
    		System.out.println("cert : "+cert);
    		System.out.println("private key: " + pk);
    		
    		
    		SignEnveloped.saveDocument(document, "C:\\Users\\Gogi\\git\\ib-novi\\Mail Client V2\\data\\obicnaPoruka.xml");
    		SignEnveloped.signDocument(document, pk, cert);
    		SignEnveloped.saveDocument(document, "C:\\Users\\Gogi\\git\\ib-novi\\Mail Client V2\\data\\potpisanaPoruka.xml");
    		
    		//dobavljanje javnog kljuca userB
    		KeyStore keyStoreUserB = KeyStoreReader.readKeyStore(userB_jks, "4567".toCharArray());
    		Certificate certUserB = KeyStoreReader.getCertificateFromKeyStore(keyStoreUserB, "userb");
    		PublicKey javniKorisnikBKljuc = KeyStoreReader.getPublicKeyFromCertificate(certUserB);
    		
    		
    		//Enkripcija 
    		try {
    			SecretKey secretKey = AsimmetricKeyEncryption.generateDataEncryptionKey();

    			XMLCipher xmlCipher = XMLCipher.getInstance(XMLCipher.TRIPLEDES);
    			
    			xmlCipher.init(XMLCipher.ENCRYPT_MODE, secretKey);

    			XMLCipher keyCipher = XMLCipher.getInstance(XMLCipher.RSA_v1dot5);
    			
    			keyCipher.init(XMLCipher.WRAP_MODE, javniKorisnikBKljuc);
    			
    			EncryptedKey encryptedKey = keyCipher.encryptKey(document, secretKey);
    			
    			EncryptedData encryptedData = xmlCipher.getEncryptedData();
    			
    			KeyInfo keyInfo = new KeyInfo(document);
    			
    			keyInfo.addKeyName("Kriptovani tajni kljuc");
    			
    			keyInfo.add(encryptedKey);
    			
    			encryptedData.setKeyInfo(keyInfo);

    			xmlCipher.doFinal(document, document.getDocumentElement(), true); // 


    		} catch (Exception e) {
    			e.printStackTrace();
    		} 
    		
    		DOMSource domSource = new DOMSource(document);
            StreamResult streamResult = new StreamResult(new File("C:\\Users\\Gogi\\git\\ib-novi\\Mail Client V2\\data\\poslataPoruka.xml"));
            
            transformer.transform(domSource, streamResult);
            String textXml = toStringMethod(document);
            Encoder encoder = java.util.Base64.getEncoder();
            String encodedString = encoder.encodeToString(textXml.getBytes());
            //Compression
            String compressedSubject = Base64.encodeToString(GzipUtil.compress(subject));
            String compressedBody = Base64.encodeToString(GzipUtil.compress(body));
            
            //Key generation
            KeyGenerator keyGen = KeyGenerator.getInstance("AES"); 
			SecretKey secretKey = keyGen.generateKey();
			Cipher aesCipherEnc = Cipher.getInstance("AES/CBC/PKCS5Padding");
			
			//inicijalizacija za sifrovanje 
			IvParameterSpec ivParameterSpec1 = IVHelper.createIV();
			aesCipherEnc.init(Cipher.ENCRYPT_MODE, secretKey, ivParameterSpec1);
			
			
			//sifrovanje
			byte[] ciphertext = aesCipherEnc.doFinal(compressedBody.getBytes());
			String ciphertextStr = Base64.encodeToString(ciphertext);
			System.out.println("Kriptovan tekst: " + ciphertextStr);
			
			
			//inicijalizacija za sifrovanje 
			IvParameterSpec ivParameterSpec2 = IVHelper.createIV();
			aesCipherEnc.init(Cipher.ENCRYPT_MODE, secretKey, ivParameterSpec2);
			
			byte[] ciphersubject = aesCipherEnc.doFinal(compressedSubject.getBytes());
			String ciphersubjectStr = Base64.encodeToString(ciphersubject);
			System.out.println("Kriptovan subject: " + ciphersubjectStr);
			
			
			//snimaju se bajtovi kljuca i IV.
			JavaUtils.writeBytesToFilename(KEY_FILE, secretKey.getEncoded());
			JavaUtils.writeBytesToFilename(IV1_FILE, ivParameterSpec1.getIV());
			JavaUtils.writeBytesToFilename(IV2_FILE, ivParameterSpec2.getIV());
			
        	MimeMessage mimeMessage = MailHelper.createMimeMessage(reciever, ciphersubjectStr, textXml);
        	MailWritter.sendMessage(service, "me", mimeMessage);
        	
        }catch (Exception e) {
        	e.printStackTrace();
		}
	}
	public static String toStringMethod(Document doc) {
        try {
            StringWriter sw = new StringWriter();
            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer();
            transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "no");
            transformer.setOutputProperty(OutputKeys.METHOD, "xml");
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");

            transformer.transform(new DOMSource(doc), new StreamResult(sw));
            return sw.toString();
        } catch (Exception ex) {
            throw new RuntimeException();
        }
    }
}
