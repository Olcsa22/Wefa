package hu.lanoga.toolbox.email;

import java.util.List;

import hu.lanoga.toolbox.ToolboxSysKeys.EmailStatus;

/**
 * @see ToolboxEmail
 * @see EmailSender
 * @see EmailSenderScheduler
 */
public interface ToolboxEmailService {

	/**
	 * Az implementációban rögtön status-t is kell állítani {@link EmailStatus#PENDING}-re, azoknál (csak/pontosan azoknál), amik a listában is benne vannak!
	 * 
	 * @return
	 */
	List<ToolboxEmail> findAllSendableEmail();

	/**
	 * {@link EmailSendAttemptCountMistmatchException}-t kell dobni, ha az aktuális attempt szám 
	 * már nem egyezik a currentAttemptCount-tal (pl.: más párhuzamos szál megnövelte)
	 * 
	 * @param id
	 *            email id-ja
	 * @param currentAttamtCount
	 * 
	 * @throws EmailSendAttemptCountMistmatchException
	 * 
	 * @see ToolboxEmail#getId()
	 * @see ToolboxEmail#getAttempt()
	 */
	void incrementAttempt(int id, int currentAttemptCount) throws EmailSendAttemptCountMistmatchException;

	/**
	 * @param id
	 *            email id-ja
	 * @param status
	 * @param errorMessage
	 * 
	 * @see ToolboxEmail#getId()
	 * @see ToolboxEmail#getStatus()
	 */
	void updateStatus(int id, int status, String errorMessage);

}
