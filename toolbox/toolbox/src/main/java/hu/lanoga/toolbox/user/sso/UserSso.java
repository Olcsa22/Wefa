package hu.lanoga.toolbox.user.sso;

import hu.lanoga.toolbox.repository.ToolboxPersistable;
import hu.lanoga.toolbox.repository.jdbc.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Table(name = "auth_user_sso")
public class UserSso implements ToolboxPersistable {
	
	private Integer tenantId;

	private Integer id;

	private Integer userId;

	private String ssoId;

	private String ssoType;

}
