package hu.lanoga.toolbox.twofactorcredential;

import hu.lanoga.toolbox.repository.ToolboxPersistable;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TwoFactorCredential implements ToolboxPersistable {

	private Integer tenantId;

	private Integer id;

	private String username;

	private String secretCode;

	private Integer validationCode;

	private String scratchCodes;

	private Integer createdBy;

	private java.sql.Timestamp createdOn;

	private Integer modifiedBy;

	private java.sql.Timestamp modifiedOn;
}
