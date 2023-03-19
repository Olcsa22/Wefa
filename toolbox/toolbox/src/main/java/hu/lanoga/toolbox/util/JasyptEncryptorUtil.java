//package hu.lanoga.toolbox.util;
//
//import org.jasypt.util.text.BasicTextEncryptor;
//
//import hu.lanoga.toolbox.spring.ApplicationContextHelper;
//
//public class JasyptEncryptorUtil {
//
//	// TODO: unit teszt
//
//	/**
//	 * jasypt.encryptor.password property-vel...
//	 * 
//	 * @param textToEncrypt
//	 * @return
//	 */
//	public static String jasyptTextEncryptWithMainPassword(final String textToEncrypt) {
//		return jasyptTextEncrypt(textToEncrypt, ApplicationContextHelper.getConfigProperty("jasypt.encryptor.password", String.class));
//	}
//
//	/**
//	 * jasypt.encryptor.password property-vel...
//	 * 
//	 * @param textToDecrypt
//	 * @return
//	 */
//	public static String jasyptTextDecrypt(final String textToDecrypt) {
//		return jasyptTextDecrypt(textToDecrypt, ApplicationContextHelper.getConfigProperty("jasypt.encryptor.password", String.class));
//	}
//
//	/**
//	 * Jasypt segítségével kódol egy adott szöveget.
//	 * A textToEncrypt paraméter a kódolni kívánt szöveg, a password paraméter pedig a jelszó.
//	 *
//	 * @param textToEncrypt
//	 * @param password
//	 * @return
//	 */
//	public static String jasyptTextEncrypt(final String textToEncrypt, final String password) {
//		final BasicTextEncryptor bte = new BasicTextEncryptor();
//		bte.setPassword(password);
//		return bte.encrypt(textToEncrypt);
//	}
//
//	/**
//	 * Jasypttel kódolt szöveg visszafejtése.
//	 * A textToDecrypt paraméter a visszafejteni kívánt kódolt szöveg, a password paraméter pedig a jelszó.
//	 *
//	 * @param textToDecrypt
//	 * @param password
//	 * @return
//	 */
//	public static String jasyptTextDecrypt(final String textToDecrypt, final String password) {
//		final BasicTextEncryptor bte = new BasicTextEncryptor();
//		bte.setPassword(password);
//		return bte.decrypt(textToDecrypt);
//	}
//}
