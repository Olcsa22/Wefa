package hu.lanoga.toolbox.user;

import com.fasterxml.jackson.annotation.JsonRawValue;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import hu.lanoga.toolbox.json.jackson.StringValueDeserializer;
import org.hibernate.validator.constraints.Length;
import org.hibernate.validator.constraints.NotEmpty;

import com.fasterxml.jackson.annotation.JsonIgnore;

import hu.lanoga.toolbox.export.ExporterIgnore;
import hu.lanoga.toolbox.repository.ToolboxPersistable;
import hu.lanoga.toolbox.repository.jdbc.Table;
import hu.lanoga.toolbox.twofactorcredential.TwoFactorCredentialJdbcRepository;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Table(name = "auth_user_password")
public class UserPassword  implements ToolboxPersistable {

	@ExporterIgnore
	private Integer tenantId;

	private Integer id;

	@NotEmpty
	private Integer userId;
	
	@NotEmpty
	@Length(max = 100)
	@JsonIgnore
	@ExporterIgnore
	private String password;

	/**
	 * @see TwoFactorCredentialJdbcRepository#saveUserCredentials(String, String, int, java.util.List)
	 */
	private Integer twoFactorType;

	/**
	 * @see TwoFactorCredentialJdbcRepository#saveUserCredentials(String, String, int, java.util.List)
	 */
	private Boolean twoFactorEnabled;

	/**
	 * @see TwoFactorCredentialJdbcRepository#saveUserCredentials(String, String, int, java.util.List)
	 */
	@ExporterIgnore
	@JsonRawValue
	@JsonDeserialize(using = StringValueDeserializer.class)
	private String twoFactorValue;

}
