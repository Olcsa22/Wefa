package hu.lanoga.toolbox.email;

/**
 * @see ToolboxEmailService
 * @see EmailSender
 * @see EmailSenderScheduler
 */
public interface ToolboxEmail {

	Integer getId();

	String getFromEmail();
	String getToEmail();
	String getSubject();
	String getBody();

	String getFileIds();

	/**
	 * false estén HTML a body...
	 * 
	 * @return
	 */
	Boolean getIsPlainText();

	Integer getStatus();
	
	Integer getAttempt();
}
