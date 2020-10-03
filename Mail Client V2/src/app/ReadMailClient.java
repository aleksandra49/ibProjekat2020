package app;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.spec.InvalidKeySpecException;
import java.util.ArrayList;
import java.util.List;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.xml.security.utils.JavaUtils;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.model.Message;

import certificat.KeyStoreReader;
import kripto.AsimmetricKeyDecryption;
import signature.VerifySignatureEnveloped;
import support.MailHelper;
import support.MailReader;
import util.Base64;
import util.GzipUtil;

public class ReadMailClient extends MailClient {

	public static long PAGE_SIZE = 3;
	public static boolean ONLY_FIRST_PAGE = true;
	
	private static final String KEY_FILE = "./data/session.key";
	private static final String IV1_FILE = "./data/iv1.bin";
	private static final String IV2_FILE = "./data/iv2.bin";
	
	private static final String userB_jks = "./data/userB.jks";
	private static final String userA_cer = "./data/userA.cer";
	private static final String key_alias = "userB";
	private static final String File_Path = "C:\\Users\\Gogi\\git\\ib-novi\\Mail Client V2\\data\\poslataPoruka.xml";
	private static final String Out_Path = "C:\\Users\\Gogi\\git\\ib-novi\\Mail Client V2\\data\\dekriptovanaPoruka.xml";
	private static final String Out_Path2 = "C:\\Users\\Gogi\\git\\ib-novi\\Mail Client V2\\data\\gmailDekriptovanaPoruka.xml";
	
	public static void main(String[] args) throws IOException, InvalidKeyException, NoSuchAlgorithmException, InvalidKeySpecException, IllegalBlockSizeException, BadPaddingException, MessagingException, NoSuchPaddingException, InvalidAlgorithmParameterException {
        // Build a new authorized API client service.
        Gmail service = getGmailService();
        ArrayList<MimeMessage> mimeMessages = new ArrayList<MimeMessage>();
        
        String user = "me";
        String query = "is:unread label:INBOX";
        
        List<Message> messages = MailReader.listMessagesMatchingQuery(service, user, query, PAGE_SIZE, ONLY_FIRST_PAGE);
        for(int i=0; i<messages.size(); i++) {
        	Message fullM = MailReader.getMessage(service, user, messages.get(i).getId());
        	
        	MimeMessage mimeMessage;
			try {
				
				mimeMessage = MailReader.getMimeMessage(service, user, fullM.getId());
				
				System.out.println("\n Message number " + i);
				System.out.println("From: " + mimeMessage.getHeader("From", null));
				System.out.println("Subject: " + mimeMessage.getSubject());
				System.out.println("Body: " + MailHelper.getText(mimeMessage));
				System.out.println("\n");
				
				mimeMessages.add(mimeMessage);
	        
			} catch (MessagingException e) {
				e.printStackTrace();
			}	
        }
        
        System.out.println("Select a message to decrypt:");
        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
	        
	    String answerStr = reader.readLine();
	    Integer answer = Integer.parseInt(answerStr);
	    
	        
	  
	    
	  //preuzimanje enkriptovane poruke
		MimeMessage chosenMessage = mimeMessages.get(answer);
		String mailBodyCSV=MailHelper.getText(chosenMessage);
	

		//dobavljanje privatnog kljuca korisnika B
		KeyStore keystore= KeyStoreReader.readKeyStore(userB_jks, "4567".toCharArray());
		Certificate certificate = KeyStoreReader.getCertificateFromKeyStore(keystore, key_alias);
		PrivateKey korisnikBPrivateKey  = KeyStoreReader.getPrivateKeyFromKeyStore(keystore, key_alias, "4567".toCharArray());
		
		Document doc = AsimmetricKeyDecryption.loadDocument(File_Path);
		doc = AsimmetricKeyDecryption.decrypt(doc, korisnikBPrivateKey);
		AsimmetricKeyDecryption.saveDocument(doc, Out_Path);
        System.out.println("File decrypted...");
		
        boolean result = VerifySignatureEnveloped.verifySignature(doc);
		System.out.println("Verified = " + result);
        
		
		String gmailMessage =MailHelper.getText(chosenMessage);
		

		
        
	}
	private static PublicKey getKorisnikApublicKey() throws KeyStoreException, NoSuchProviderException, 
	NoSuchAlgorithmException, CertificateException, FileNotFoundException, IOException {
		
		//kreiranje keystore
		KeyStore ksInstanca = KeyStore.getInstance("JKS", "SUN");
		File file = new File("./data/userB.jks");
		ksInstanca.load(new FileInputStream(file), "4567".toCharArray());
		//citanje iz keystore-a
		Certificate cer = ksInstanca.getCertificate("userA");
		return cer.getPublicKey();
	}
	
	private static Document toXmlDocument(String str) throws ParserConfigurationException, SAXException, IOException{
        
        DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder docBuilder = docBuilderFactory.newDocumentBuilder();
        Document document = docBuilder.parse(new InputSource(new StringReader(str)));
       
        return document;
   }
}
