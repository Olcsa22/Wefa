package hu.lanoga.toolbox.user;

import java.sql.Timestamp;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.Pattern;

import org.hibernate.validator.constraints.Length;

import hu.lanoga.toolbox.ToolboxSysKeys;
import hu.lanoga.toolbox.export.ExporterIgnore;
import hu.lanoga.toolbox.repository.ToolboxPersistable;
import hu.lanoga.toolbox.repository.jdbc.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Table(name = "auth_user_info")
public class UserInfo implements ToolboxPersistable {

	@ExporterIgnore
	private Integer tenantId;

	@ExporterIgnore
	private Integer id;

	@ExporterIgnore
	@NotEmpty
	private Integer userId;

	@Length(max = 50)
	private String title;

	@Length(max = 50)
	private String jobTitle;

	private java.sql.Date dateOfBirth;

	@Length(max = 100)
	private String givenName;

	@Length(max = 100)
	private String familyName;

	@Length(max = 100)
	@NotEmpty
	private String email;

	@Pattern(regexp=ToolboxSysKeys.ValidationConstants.PHONE_NUMBER_REGEX)
	private String phoneNumber;

	@ExporterIgnore
	private Integer createdBy;

	private Timestamp createdOn;

	@ExporterIgnore
	private Integer modifiedBy;

	private Timestamp modifiedOn;

}
