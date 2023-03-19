package hu.lanoga.wefa;

import hu.lanoga.toolbox.ToolboxSysKeys;

public class SysKeys extends ToolboxSysKeys {

	public class UserAuth extends ToolboxSysKeys.UserAuth {

		public static final String ROLE_CLERK_STR = "ROLE_CLERK";
		public static final int ROLE_CLERK_CS_ID = 152;

		public static final String ROLE_APPROVER_STR = "ROLE_APPROVER";
		public static final int ROLE_APPROVER_CS_ID = 150;

	}

	public class ProccessJsonObjectTypes {

		public static final String PROC_DEF_DATA_MODEL = "proc-def-data-model";
		public static final String PROC_DEF_FORM_MODEL = "proc-def-form-model";

		public static final String PROC_DEF_STEP_DATA_MODEL = "proc-def-step-data-model";
		public static final String PROC_DEF_STEP_FORM_MODEL = "proc-def-step-form-model";

		public static final String PROC_DEF_PUBLIC_START_DATA_MODEL = "proc-def-public-start-data-model";
		public static final String PROC_DEF_PUBLIC_START_FORM_MODEL = "proc-def-public-start-form-model";

	}

	public class EmailTemplateType extends ToolboxSysKeys.EmailTemplateType {

		/**
		 * értesítés általános célra
		 */
		public static final int EMAIL_NOTIF = 550;
		
		/**
		 * értesítés ügyfeleknek (1)
		 */
		public static final int CUSTOMER_EMAIL_NOTIF_1 = 552;

	}

	public static final String STALE_DATA_CHECK_TS_VAR_MAP_KEY = "staleDataCheckTs";

	/**
	 * Activi assigne prefix
	 */
	public static final String USER_ROLE_TAG = "ROLE:";

}
