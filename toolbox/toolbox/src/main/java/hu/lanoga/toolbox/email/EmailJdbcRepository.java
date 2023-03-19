package hu.lanoga.toolbox.email;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.stereotype.Repository;

import hu.lanoga.toolbox.ToolboxSysKeys;
import hu.lanoga.toolbox.ToolboxSysKeys.EmailStatus;
import hu.lanoga.toolbox.repository.jdbc.DefaultJdbcRepository;

@ConditionalOnMissingBean(name = "emailJdbcRepositoryOverrideBean")
@Repository
public class EmailJdbcRepository extends DefaultJdbcRepository<Email> {
	
	@Value("${tools.mail.sender.scheduler.resend-attempt:0}")
	private int resendAttempt;

	public List<Email> findAllSendableEmail() {
		
		// a végén a ToolboxSysKeys.EmailStatus.PENDING áramszünet stb. esetére kell, ha valamelyik "örökké" PENDING maradna // TODO: tisztázni (itt figyelni kellene a job lefutás időpontjára is) (ez uitóbbi esetleg eltárolni és ahhoz viszonyítani) (megbesz)
		
		return namedParameterJdbcTemplate.query(fillVariables("SELECT * FROM email WHERE #tenantCondition AND (status = '" + ToolboxSysKeys.EmailStatus.CREATED  + "' OR (status = '" + ToolboxSysKeys.EmailStatus.ERROR + "' AND  attempt <= '" + resendAttempt + "') OR (status = '" + ToolboxSysKeys.EmailStatus.PENDING + "' AND  modified_on <= NOW() - interval '1' hour)) ORDER BY id ASC"), newRowMapperInstance());
	}

	public void updateSendableEmailStatusToPending(final int lastId) {

		final SqlParameterSource namedParameters = new MapSqlParameterSource()
				.addValue("fromStatus", ToolboxSysKeys.EmailStatus.CREATED)
				.addValue("toStatus", ToolboxSysKeys.EmailStatus.PENDING)
				.addValue("lastId", lastId);

		namedParameterJdbcTemplate.update(fillVariables("UPDATE email SET status = :toStatus, modified_on = NOW() WHERE #tenantCondition AND id < :lastId AND status = :fromStatus"), namedParameters);

	}

	public void incrementAttempt(final int id, final int currentAttemptCount) throws EmailSendAttemptCountMistmatchException {

		final SqlParameterSource namedParameters = new MapSqlParameterSource().addValue("id", id).addValue("attempt", currentAttemptCount);
		int updated = namedParameterJdbcTemplate.update(fillVariables("UPDATE email SET attempt = attempt + 1, modified_on = NOW() WHERE #tenantCondition AND id = :id AND attempt = :attempt"), namedParameters);

		if (updated != 1) {
			throw new EmailSendAttemptCountMistmatchException("email id: " + id + ", expected current attempt count: " + currentAttemptCount); // TODO: ezt a service rétegbe kell, itt legyen más ex típus
		}
	}

	/**
	 * @param id email id
	 * @param status
	 * @param errorMessage
	 * 
	 * @see EmailStatus
	 */
	public void updateStatus(final int id, int status, String errorMessage) {

		final SqlParameterSource namedParameters = new MapSqlParameterSource()
				.addValue("id", id)
				.addValue("status", status)
				.addValue("errorMessage", errorMessage);

		namedParameterJdbcTemplate.update(fillVariables("UPDATE email SET status = :status, error_message = :errorMessage, modified_on = NOW() WHERE #tenantCondition AND id = :id"), namedParameters);

	}
}
