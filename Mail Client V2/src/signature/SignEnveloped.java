package signature;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.Security;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.apache.xml.security.signature.XMLSignature;
import org.apache.xml.security.transforms.Transforms;
import org.apache.xml.security.utils.Constants;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.w3c.dom.Document;
import org.w3c.dom.Element;


public class SignEnveloped {
	
	private static final String IN_FILE = "./data/univerzitet.xml";
	private static final String OUT_FILE = "./data/univerzitet_signed1.xml";
	private static final String KEY_STORE_FILE = "./data/primer.jks";
	
  static {
  	//staticka inicijalizacija
      Security.addProvider(new BouncyCastleProvider());
      org.apache.xml.security.Init.init();
  }
	
	public void testIt() {
		//ucitava se dokument
		Document doc = loadDocument(IN_FILE);
		
		//ucitava privatni kljuc koji ce biti iskoriscen za potpisivanje dokumenta
		PrivateKey pk = readPrivateKey(KEY_STORE_FILE,"nesto","nesto");
		
		//ucitava sertifikat
		Certificate cert = readCertificate(KEY_STORE_FILE,"nesto","nesto");
		
		//potpisuje
		System.out.println("Signing....");
		doc = signDocument(doc, pk, cert);
		
		//snima se dokument
		saveDocument(doc, OUT_FILE);
		System.out.println("Signing of document done");
	}
	
	/**
	 * Kreira DOM od XML dokumenta
	 */
	private Document loadDocument(String file) {
		try {
			DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
			dbf.setNamespaceAware(true);
			DocumentBuilder db = dbf.newDocumentBuilder();
			Document document = db.parse(new File(file));

			return document;
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		} 
	}
	
	/**
	 * Snima DOM u XML fajl 
	 */
	public static void saveDocument(Document doc, String fileName) {
		try {
			File outFile = new File(fileName);
			FileOutputStream f = new FileOutputStream(outFile);

			TransformerFactory factory = TransformerFactory.newInstance();
			Transformer transformer = factory.newTransformer();
			
			DOMSource source = new DOMSource(doc);
			StreamResult result = new StreamResult(f);
			
			transformer.transform(source, result);

			f.close();

		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Ucitava sertifikat is KS fajla
	 * alias primer
	 */
	
	public static Certificate readCertificate(String putanjaDoKeyStora, String password, String alias) {
		try {
			//kreiramo instancu KeyStore
			KeyStore ks = KeyStore.getInstance("JKS", "SUN");
			
			//ucitavamo podatke
			BufferedInputStream in = new BufferedInputStream(new FileInputStream(putanjaDoKeyStora));
			ks.load(in, password.toCharArray());
			
			if(ks.isKeyEntry(alias)) {
				Certificate cert = ks.getCertificate(alias);
				return cert;
				
			}
			else
				return null;
			
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		} 
	}
	/**
	 * Ucitava privatni kljuc is KS fajla
	 * alias primer
	 */
	public static PrivateKey readPrivateKey(String putanjaDoKeyStora, String password, String alias) {
		try {
			//kreiramo instancu KeyStore
			KeyStore ks = KeyStore.getInstance("JKS", "SUN");
			
			//ucitavamo podatke
			BufferedInputStream in = new BufferedInputStream(new FileInputStream(putanjaDoKeyStora));
			ks.load(in, password.toCharArray());
			
			if(ks.isKeyEntry(alias)) {
				PrivateKey pk = (PrivateKey) ks.getKey(alias, password.toCharArray());
				return pk;
			}
			else
				return null;
			
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}
	
	public static Document signDocument(Document doc, PrivateKey privateKey, Certificate cert) {
      
      try {
			Element rootEl = doc.getDocumentElement();
			
			//kreira se signature objekat
			XMLSignature sig = new XMLSignature(doc, null, XMLSignature.ALGO_ID_SIGNATURE_RSA_SHA1);
			
			//kreiraju se transformacije nad dokumentom
			Transforms transforms = new Transforms(doc);
			    
			//iz potpisa uklanja Signature element
			//Ovo je potrebno za enveloped tip po specifikaciji
			transforms.addTransform(Transforms.TRANSFORM_ENVELOPED_SIGNATURE);
			
			//normalizacija
			transforms.addTransform(Transforms.TRANSFORM_C14N_WITH_COMMENTS);
			    
			//potpisuje se citav dokument (URI "")
			sig.addDocument("", transforms, Constants.ALGO_ID_DIGEST_SHA1);
			    
			//U KeyInfo se postavalja Javni kljuc samostalno i citav sertifikat
			sig.addKeyInfo(cert.getPublicKey());
			sig.addKeyInfo((X509Certificate) cert);
			    
			//poptis je child root elementa
			rootEl.appendChild(sig.getElement());
			
			//potpisivanje
			sig.sign(privateKey);
			
			return doc;
			
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}
	
	public static void main(String[] args) {
		SignEnveloped sign = new SignEnveloped();
		sign.testIt();
	}

}
