package hu.lanoga.toolbox.auth.token;

import java.sql.Timestamp;

import hu.lanoga.toolbox.export.ExporterIgnore;
import hu.lanoga.toolbox.repository.ToolboxPersistable;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AuthToken implements ToolboxPersistable {

	@ExporterIgnore
	private Integer tenantId;
	
	private Integer id;

	/**
	 * @see hu.lanoga.toolbox.ToolboxSysKeys.AuthTokenType
	 */
	private Integer tokenType;

	private String token;

	private Timestamp validUntil;
	
	/**
	 * tokenType-tól függ, hogy mit tárolunk itt (pl. userId)
	 */
	private Integer resourceId1;
	
	/**
	 * tokenType-tól függ, hogy mit tárolunk itt (pl. userId)
	 */
	private Integer resourceId2;

	private String note1;
	private String note2;
	private String note3;
		
	private Integer createdBy;
	private Timestamp createdOn;
	private Integer modifiedBy;
	private Timestamp modifiedOn;
}
