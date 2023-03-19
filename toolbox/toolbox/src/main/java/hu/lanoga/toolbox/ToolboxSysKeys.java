package hu.lanoga.toolbox;

import java.io.File;
import java.util.HashSet;
import java.util.Set;

import hu.lanoga.toolbox.db.DbInitHelper;
import hu.lanoga.toolbox.email.EmailService;
import hu.lanoga.toolbox.file.FileDescriptor;
import hu.lanoga.toolbox.file.FileStoreService;
import hu.lanoga.toolbox.file.remote.RemoteFile;
import hu.lanoga.toolbox.filter.input.angular.AngularPageRequest;
import hu.lanoga.toolbox.filter.internal.BasePageRequest;
import hu.lanoga.toolbox.repository.jdbc.DefaultJdbcRepository;
import hu.lanoga.toolbox.repository.jdbc.JdbcRepositoryManager;
import hu.lanoga.toolbox.session.LcuHelperSessionBean;
import hu.lanoga.toolbox.spring.SecurityUtil;
import hu.lanoga.toolbox.tenant.TenantKeyValueSettingsService;
import hu.lanoga.toolbox.user.UserKeyValueSettingsService;

/**
 * <pre>
 * 
 * számozás: 
 * 
 * 1) CODE_STORE_TYPE_ID értéke, ToolboxSysKeys: 1-től indul, egyesével nő
 * 2) osztályon belüli számozás, ToolboxSysKeys: CODE_STORE_TYPE_ID + "" + sorszám, két számjegyre paddingolva, átalában kettővel ugrik (tehát 02, 04, 06) (tehát pl.: ha 1 CODE_STORE_TYPE_ID, akkor 100, 102...)
 * 3) osztályon belüli számozás, SysKeys a projektben, ha a ToolboxSyskeys valamely osztálya van kiterjesztve: alapvetően a 2-es pontban leírt módon folytatás, viszont az első elem 50 (tehát pl.: ha 1 CODE_STORE_TYPE_ID, akkor 150, 152...)
 * 4) CODE_STORE_TYPE_ID értéke, SysKeys a projektben: 50-től indul, egyesével nő
 * 5) osztályon belüli számozás, SysKeys a projektben: CODE_STORE_TYPE_ID + "" + sorszám, három számjegyre paddingolva, átalában kettővel ugrik (tehát 02, 04, 06) (tehát pl.: ha 50 CODE_STORE_TYPE_ID, akkor 50000, 50002...)
 * 
 * </pre>
 */
public class ToolboxSysKeys {

	/**
	 * a rekord enabled/disabled állításának fajtája
	 * 
	 * @see DefaultJdbcRepository
	 * @see JdbcRepositoryManager
	 */
	public enum JdbcDisableMode {

		/**
		 * enabled mező 
		 */
		ENABLED_COLUMN,

		/**
		 * disabled mező ("fordított")
		 */
		DISABLED_COLUMN;

	}

	public enum JmsDestinationMode {

		GLOBAL, TENANT, USER, VAADIN_UI, HTTP_SESSION

	}

	public enum JmsMsgType {

		FILE_CART_MSG, TENANT_OVERSEER_MSG

	}

	/**
	 * repository réteg tenant kezelés módja 
	 * 
	 * (fontos, hogy "kézi"/"maszek" repository metódusoknál nem automatikus az alakalmazása, 
	 * kézi {@link JdbcRepositoryManager#fillVariables(String, boolean)} hívás vagy más kézi megoldás kell!) 
	 * 
	 * (fontos, hogy ez csak a legalsó szintre vonatkozik, service szinten user jogra is kell limitálni, 
	 * az egy teljesen független dolog (Secured annotáció használata kell így is stb.)!) 
	 * 
	 * @see DefaultJdbcRepository
	 * @see JdbcRepositoryManager
	 */
	public enum RepositoryTenantMode {

		/**
		 * alap mód, belépett user tenant id-ja számít, 
		 * ThreadLocal változóval van opcionális lehetőség override-ra
		 * 
		 * @see SecurityUtil#getLoggedInUserTenantId() 
		 * @see JdbcRepositoryManager#setTlTenantId(int)
		 */
		DEFAULT,

		/**
		 * semmilyen tenant kezelés nincs, ThreadLocal változóval sincs lehetőség megadásra (nem veszi figyelembe)... 
		 */
		NO_TENANT
	

	}

	public class ValidationConstants {

		public static final String PHONE_NUMBER_REGEX = "(^(\\++\\d*)*$)";

	}

	public enum CrudOperation {

		READ, ADD, UPDATE, DELETE;

	}

	/**
	 * (https://www.postgresql.org/docs/9.6/static/sql-insert.html)
	 * 
	 * @see DefaultJdbcRepository
	 */
	public enum JdbcInsertConflictMode {

		/**
		 * postgres: "For ON CONFLICT DO UPDATE, a conflict_target must be provided."
		 */
		ON_CONFLICT_DO_UPDATE,

		/**
		 * postgres: "For ON CONFLICT DO NOTHING, it is optional to specify a conflict_target; when omitted, conflicts with all usable constraints (and unique indexes) are handled."
		 */
		ON_CONFLICT_DO_NOTHING,

		/**
		 * default működés, exception-t dob (unique constraint violation...)
		 */
		DEFAULT

	}

	/**
	 * @see BasePageRequest
	 */
	public enum PageRequestType {

		ANGULAR;

	}

	/**
	 * @see BasePageRequest
	 */
	public enum SearchCriteriaLogicalOperation {

		AND, OR;

	}

	/**
	 * @see BasePageRequest
	 */
	public enum SearchCriteriaOperation {

		/**
		 * pontos (String-nél case sensitive) egyezés
		 */
		EQ,

		/**
		 * not equals
		 */
		NE,

		/**
		 * case insensitive, '%value%'...
		 */
		LIKE,

		/**
		 * case insensitive, 'value%'...
		 */
		LIKE_END,
		
		/**
		 * case insensitive, '%value%'...
		 */
		NOT_LIKE,

		/**
		 * case insensitive, 'value%'...
		 */
		NOT_LIKE_END,

		SMALLER_THAN,

		SMALLER_THAN_EQUALS,

		BIGGER_THAN,

		BIGGER_THAN_EQUALS,

		/**
		 * zárt intervallum (tehát a határok is benne)
		 */
		BETWEEN,

		/**
		 * Lásd SQL IN {@link Set}-tel kombinálva működik. 
		 * Javaból pl. {@link HashSet}. 
		 * 
		 * {@link AngularPageRequest} esetén vesszővel elválasztott számokból álló String, 
		 * pl.: "12,14", értsd itt csak az Integer fajta műküdik (Long sem)). 
		 */
		IN,

		IS_NULL,

		IS_NOT_NULL,

		JSON_CONTAINS
	}

	public enum DocxExportEnginePdfMode {

		WITH_SOFFICE;

	}

	/**
	 * @see DbInitHelper
	 */
	public enum DbInitMode {

		/**
		 * nem csinál semmi (NOOP)
		 */
		SKIP,

		/**
		 * rogton a target-database-hez probal kapcsolodni (toolbox.bootstrap.datasource.url + toolbox.bootstrap.datasource.target-database), ujraprobalkozik/var a connection-re...
		 */
		WAIT_FOR_TARGET_DATABASE,

		/**
		 * megprobalja letrehozni a target-database-t (ha az meg nem letezik), ujraprobalkozik/var a connection-re
		 */
		WAIT_FOR_ROOT_AND_CREATE_TARGET_IF_NOT_EXISTS,

		/**
		 * (veszelyes!) ha mar letezik a target-database, akkor eldobja (DROP DATABASE) es ujra letrehozza, ujraprobalkozik/var a connection-re
		 */
		WAIT_FOR_ROOT_AND_DROP_CREATE_TARGET;

	}

	public enum CacheEngine {

		EHCACHE_MEMORY, EHCACHE_MEMORY_AND_DISK

	}

	/**
	 * @see SecurityUtil
	 */
	public class UserAuth {

		// 1:admin és 1:remote userek alap jelszava: raGeDREJaspew6

		protected UserAuth() {
			//
		}

		public static final int CODE_STORE_TYPE_ID = 1;
		
		// ----------------------------------
		
		/**
		 * "commont-tenant" a neve
		 */
		public static final int COMMON_TENANT_ID = 1;
		
		/**
		 * "lcu-tenant" a neve
		 */
		public static final int LCU_TENANT_ID = 2;
		
		/**
		 * dev módban, unit tesztekhez stb. létrehozott teszt tenant neve... 
		 * 11-es tenantId... 
		 */
		public static final String TEST_TENANT_NAME = "test-tenant";

		// ----------------------------------

		public static final String DEFAULT_ADMIN_USERNAME = "admin";

		/**
		 * common-tenant/admin user
		 * 
		 * @see #COMMON_TENANT_ID
		 */
		public static final int SUPER_ADMIN_USER_ID = 1;

		/**
		 * @see #COMMON_TENANT_ID
		 */
		public static final int SYSTEM_USER_ID = 2;
		
		/**
		 * @see #LCU_TENANT_ID
		 */
		public static final int LCU_SYSTEM_USER_ID = 5;

		/**
		 * @see #COMMON_TENANT_ID
		 */
		public static final int ANONYMOUS_USER_ID = 3;

		/**
		 * @see #COMMON_TENANT_ID
		 */
		public static final String SYSTEM_USERNAME = "system";
		
		/**
		 * @see #COMMON_TENANT_ID
		 */
		public static final String ANONYMOUS_USERNAME = "anonymous";
		
		/**
		 * @see #COMMON_TENANT_ID
		 */
		public static final String SUPER_REMOTE_USERNAME = "remote";
		
		/**
		 * a {@link #SYSTEM_USERNAME} "testvére", de LCU tenant-ban van és LCU célokra szolgál, 
		 * ad-hoc (regisztráció nélküli) LCU (=vásárló)
		 * 
		 * @see #LCU_TENANT_ID
		 * @see LcuHelperSessionBean
		 * @see SecurityUtil#setSystemUserLcu()
		 */
		public static final String LCU_SYSTEM_USERNAME = "lcu-system";

		// ----------------------------------

		/**
		 * anonymous user-nek ez a joga van csak
		 */
		public static final String ROLE_ANONYMOUS_STR = "ROLE_ANONYMOUS";
		public static final int ROLE_ANONYMOUS_CS_ID = 106;

		/**
		 * limited (customer stb.) role (alkalmazásfüggő, hogy pontosan mire használjuk) 
		 */
		public static final String ROLE_LCU_STR = "ROLE_LCU";

		/**
		 * limited (customer stb.) role (alkalmazásfüggő, hogy pontosan mire használjuk) 
		 */
		public static final int ROLE_LCU_CS_ID = 110;

		public static final String ROLE_USER_STR = "ROLE_USER";
		public static final int ROLE_USER_CS_ID = 100;

		public static final String ROLE_ADMIN_STR = "ROLE_ADMIN";
		public static final int ROLE_ADMIN_CS_ID = 102;

		public static final String ROLE_SUPER_ADMIN_STR = "ROLE_SUPER_ADMIN";
		public static final int ROLE_SUPER_ADMIN_CS_ID = 104;

		public static final String ROLE_REMOTE_STR = "ROLE_REMOTE";
		public static final int ROLE_REMOTE_CS_ID = 112;

		/**
		 * hasonló a ROLE_REMOTE-hoz, de olyan API hívásokhoz, ahol több tenant, globális beállítások stb. is érintett...
		 * (tehát inkább belső Lanoga util eszközök stb. kapcsán)
		 * 
		 * @see #COMMON_TENANT_ID
		 */
		public static final String ROLE_SUPER_REMOTE_STR = "ROLE_SUPER_REMOTE";
		
		/**
		 * @see #COMMON_TENANT_ID
		 */
		public static final int ROLE_SUPER_REMOTE_CS_ID = 114;

		/**
		 * ilyen jog megléte esetén más userekbe "át" léphet (más tenant userébe!) 
		 * (nem akárhová, lásd még AUTH_USER tábla)
		 */
		public static final String ROLE_TENANT_OVERSEER_STR = "ROLE_TENANT_OVERSEER";
		public static final int ROLE_TENANT_OVERSEER_CS_ID = 116;

		/**
		 * a már meglévő TENANT_OVERSEER jog mellé "SUPER" változat, 
		 * ha meg van adva, akkor automatikusan létrehoz egy user-t abba a tenant-ba amibe át akar lépni, ha még nem létezik, 
		 * tehát ebből kifolyólag minden tenant-ba át tud lépni (ezért "super")
		 * 
		 * @see #COMMON_TENANT_ID
		 */
		public static final String ROLE_SUPER_TENANT_OVERSEER_STR = "ROLE_SUPER_TENANT_OVERSEER";
		
		/**
		 * @see #COMMON_TENANT_ID
		 */
		public static final int ROLE_SUPER_TENANT_OVERSEER_CS_ID = 118;


		/**
		 * @see #ROLE_SUPER_ADMIN_STR
		 * 		ennél gyengébb, de a super felületet használja stb.
		 * @see #COMMON_TENANT_ID
		 */
		public static final String ROLE_SUPER_USER_STR = "ROLE_SUPER_USER";
		
		/**
		 * @see #COMMON_TENANT_ID
		 */
		public static final int ROLE_SUPER_USER_CS_ID = 120;		

	}

	public class AuthTokenType {

		protected AuthTokenType() {
			//
		}

		public static final int CODE_STORE_TYPE_ID = 2;

		public static final int USER_REGISTRATION = 200;
		public static final int FORGOTTEN_PASSWORD = 202;

	}

	public class AggregateSqlFunctionNameType {

		protected AggregateSqlFunctionNameType() {
			//
		}

		public static final String SUM = "SUM";
		public static final String AVG = "AVG";
		public static final String MIN = "MIN";
		public static final String MAX = "MAX";

	}

	public class SearchTargetFieldNameType {

		protected SearchTargetFieldNameType() {
			//
		}

		public static final String JSONARR = "jsonarr";

	}

	public final class FileDescriptorChildType {

		private FileDescriptorChildType() {
			//
		}

		public static final int CODE_STORE_TYPE_ID = 20;

		public static final int VARIANT_2048_2048_PROGR_JPG = 2000;

		/**
		 * valójában 3072*3072 befoglaló méretű és ez is progressive jpg lesz (hibás a konstans neve, a feature megy)
		 */
		public static final int VARIANT_3096_3096_JPG = 2002; // TODO: "valójában 3072*3072 befoglaló méretű és ez is progressive jpg lesz"... átnevezni itt is
		public static final int VARIANT_PDF = 2004;

	}

	/**
	 * TEMPORARY vagy TO_BE_DELETED status esetén a létrehozástól számított 24 óra után 
	 * automatikusan (amikor a karbantartó job épp fut) törlésre kerül a fájl
	 * 
	 * @see FileStoreService
	 */
	public final class FileDescriptorStatus {

		private FileDescriptorStatus() {
			//
		}

		public static final int CODE_STORE_TYPE_ID = 3;

		public static final int TEMPORARY = 304;
		public static final int NORMAL = 306;
		public static final int TO_BE_DELETED = 310;
		public static final int DELETED = 312;

	}

	public final class FileDescriptorLocationType {

		private FileDescriptorLocationType() {
			//
		}

		public static final int CODE_STORE_TYPE_ID = 6;

		public static final int PROTECTED_FOLDER = 600;
		public static final int PUBLIC_FOLDER_FOR_CDN = 620;

	}

	public final class FileDescriptorSecurityType {

		private FileDescriptorSecurityType() {
			//
		}

		public static final int CODE_STORE_TYPE_ID = 7;

		/**
		 * admin, vagy a fájl létrehozója írhatja (módosíthatja) és olvashatja
		 */
		public static final int ADMIN_OR_CREATOR = 700;

		/**
		 * akármelyik belépett, legalább ROLE_USER jogú user olvashatja, de csak az admin vagy a fájl létrehozója módosíthatja...  
		 * jelenleg a módosítás csere/törlés lehet (érdemi update nem értelmezet, nincs itt fájl tartalom editor)
		 * 
		 * @see FileOperationAccessTypeIntent
		 */
		public static final int READ_AUTHENTICATED_USER_MODIFY_ADMIN_OR_CREATOR = 702;

		/**
		 * akármelyik belépet, legalább ROLE_USER jogú user írhatja (módosíthatja) és olvashatja is; 
		 * fontos: plusz most már a fájl létrehozója is (akkor is, ha az LCU)
		 */
		public static final int AUTHENTICATED_USER = 704;

		public static final int SUPER_ADMIN_ONLY = 706;

		public static final int SYSTEM_ONLY = 708;

	}
	
	/**
	 * fontos, hogy a Toolbox kód nem korlátoz/véd teljesen... 
	 * a {@link FileStoreService} osztályban több helyen van check, 
	 * de abból adódóan, hogy a {@link File} is hozzáférhető "partizán"/"hazug" módon lehet írni/törölni is, 
	 * akkor is, ha read intent-tel jutottál hozzá a {@link FileDescriptor}-hoz... 
	 * 
	 * (lényeg, hogy részben a projekt fejlesztő felelősség, hogy saját kódjában 
	 * ő is tartsa magát az átala megadott intent-he...) 
	 * 
	 * (a child (konvertált, kis méretű kép, előnézet stb.) fájlokra általában lazább szabályozás vontakozik!)
	 */
	public enum FileOperationAccessTypeIntent {
		
		// TODO: meg lehetne ezt is csinálni CodeStore-ra, de nem égető (felületen nem látszik, csak kód "közi" dolog)
		
		READ_ONLY_INTENT, 
		
		/**
		 * nem csak write, hanem törlés stb. is (minden, ami nem read-only dolog)... 
		 */
		CHANGE_INTENT 
		
	}

	/**
	 * @see EmailService
	 */
	public class EmailStatus {

		protected EmailStatus() {
			//
		}

		public static final int CODE_STORE_TYPE_ID = 4;

		public static final int CREATED = 400;
		public static final int PENDING = 402;
		public static final int ERROR = 404;
		public static final int SENT = 410;

		/**
		 * piszkozat, vagy egyéb félkész, még nem küldendő email
		 */
		public static final int PLANNED = 420;

	}

	/**
	 * @see EmailService
	 */
	public class EmailTemplateType {

		protected EmailTemplateType() {
			//
		}

		public static final int CODE_STORE_TYPE_ID = 5;

		public static final int GENERATED_PASSWORD = 500;

		/**
		 * informál új user regisztárálásáról (abban az esetben, ha kell kézi enabled-re állítás)
		 */
		public static final int NEW_USER_REGISTRATION = 502;

		/**
		 * informál új user regisztárálásáról (abban az esetben, ha nem kell jóváhagyás)
		 */
		public static final int NEW_USER_REGISTERED = 504;

		public static final int FORGOTTEN_PASSWORD = 506;

		public static final int QUICK_CONTACT_NOTIFICATION = 508;

	}

	public class ContactAddressType {

		protected ContactAddressType() {
			//
		}

		public static final int CODE_STORE_TYPE_ID = 8;

		public static final int HOME_EMAIL = 800;
		public static final int WORK_EMAIL = 802;
		public static final int PRIVATE_PHONE = 804;
		public static final int SKYPE = 806;
		public static final int SOCIAL_MEDIA = 808;

	}

	public class GeoAddressType {

		protected GeoAddressType() {
			//
		}

		public static final int CODE_STORE_TYPE_ID = 9;

		public static final int HOME_ADDRESS = 900;
		public static final int WORK_ADDRESS = 902;
		public static final int COMPANY_HEADQUARTERS = 904;
		public static final int COMPANY_SITE = 906;

	}

	public class GeoAreaTypes {

		protected GeoAreaTypes() {
			//
		}

		public static final int CODE_STORE_TYPE_ID = 10;

		public static final int COUNTRY = 10000;
		public static final int CITY = 10002;
		public static final int STREET = 10004;
	}

	public class ExportType {

		protected ExportType() {
			//
		}

		public static final int CODE_STORE_TYPE_ID = 11;

		public static final int XLSX = 11000;
		public static final int PDF = 11002;
	}

	/**
	 * @see RemoteFile
	 */
	public final class RemoteProviderStatus { // jobb lenne RemoteFileStatus néven

		private RemoteProviderStatus() {
			//
		}

		public static final int CODE_STORE_TYPE_ID = 12;

		public static final int PENDING = 1200;
		public static final int UPLOADED = 1202;
		public static final int FAILED = 1204;
	}

	/**
	 * @see RemoteFile
	 */
	public final class RemoteProviderPriority {

		private RemoteProviderPriority() {
			//
		}

		public static final int CODE_STORE_TYPE_ID = 13;

		// fontos, hogy növekvő sorrendben legyen (értsd a számérték szerint is)

		public static final int LOW = 1300;
		public static final int MEDIUM = 1302;
		public static final int HIGH = 1304;
		public static final int VERY_HIGH = 1306;

	}

	/**
	 * @see RemoteFile
	 */
	public final class RemoteProviderType {

		private RemoteProviderType() {
			//
		}

		public static final int CODE_STORE_TYPE_ID = 14;

		public static final int GOOGLE_DRIVE = 1400;
		public static final int AMAZON_S3 = 1402;

	}
	
	public class TwoFactorAuthentication {

		protected TwoFactorAuthentication() {
			//
		}

		public static final int CODE_STORE_TYPE_ID = 15;

		public static final int GOOGLE = 1500;

	}

	public final class PaymentOperationType {

		private PaymentOperationType() {
			//
		}

		public static final int CODE_STORE_TYPE_ID = 16;

		public static final int AUTH = 1600;
		public static final int PURCHASE = 1602;
		public static final int CAPTURE = 1604;
		public static final int REFUND = 1606;
		public static final int VOID = 1608;
		public static final int WITHDRAW = 1610;

	}

	public final class PaymentTransactionStatus {

		private PaymentTransactionStatus() {
			//
		}

		public static final int CODE_STORE_TYPE_ID = 17;

		public static final int GATEWAY_INIT = 1704;

		// --- 1720 felett van a PENDING jellegű (amikor már túl vagyunk a provider API híváson)

		public static final int PENDING = 1720;

		// --- 1730 felett "végső" status

		/**
		 * készpénzes vagy más IRL fizetés esetén bekerült a tranzakció a db-be sikeresen, 
		 * érdemben fizetettre később kell a backoffice felületen állítani (kézzel)
		 */
		public static final int RECEIVED = 1730;

		public static final int FAILED = 1740; // TODO: legyen egy másik failed is, FAILED_OR_UNKNOWN néven

		/**
		 * vásárló megszakította a fizetést
		 */
		public static final int USER_CANCELED = 1742;

		public static final int SUCCESS = 1750;

	}

	public final class PaymentProvider {

		private PaymentProvider() {
			//
		}

		public static final int CODE_STORE_TYPE_ID = 18;

		public static final int SIMPLEPAY2 = 1814;

		public static final int BARION = 1820;

		public static final int PAY_PAL = 1830;

		public static final int CIB = 1835;

		/**
		 * készpénz vagy más helyszíni fizetés (pl. utalvány)
		 */
		public static final int IRL = 1840;
		
		public static final int PAYU = 1850;
		public static final int PAYU_APPLE_PAY = 1851;
		public static final int PAYU_GOOGLE_PAY = 1852;
		
		/**
		 * valamilyen kártyás vagy más online payment, 
		 * ez egy furca placeholder érték olyan esetekre, amikor valamilyen rekordba már be kell írni,
		 * de még nem tudjuk pontosan, hogy a vásárló melyik opciót váalsztja (CIB, SimplePay stb. közül)
		 */
		public static final int SOME_KIND_OF_ONLINE_PAYMENT = 1880;
		
		/**
		 * valamilyen utalásos fizetés
		 */
		public static final int SOME_KIND_OF_BANK_TRANSFER_PAYMENT = 1890;

	}
	
//	public final class PaymentTransactionStatusCheckFrequency {
//		
//		// TODO: in the DB, but not implmented, not used
//		
//		private PaymentTransactionStatusCheckFrequency() {
//			//
//		}
//	
//		public static final int CODE_STORE_TYPE_ID = 19;
//		
//		// ...
//		
//	}
	
	public final class ChatTargetType {

		private ChatTargetType() {
			//
		}

		public static final int CODE_STORE_TYPE_ID = 21;

		/**
		 * Egy ROLE (amit authentikációnal is használunk, pl. ROLE_ADMIN)
		 */
		public static final int AUTH_ROLE = 2108;

		/**
		 * Egy konkrét user (userId)
		 */
		public static final int AUTH_USER = 2110;

	}

	/**
	 * @see TenantKeyValueSettingsService
	 */
	public class TenantKeyValueSettings {

		protected TenantKeyValueSettings() {
			//
		}

		public static final String PREFERRED_CURRENCY = "preferred_currency";
		public static final String PREFERRED_LOCALE = "preferred_locale";

		/**
		 * "CET" stb.
		 */
		public static final String PREFERRED_TIME_ZONE = "preferred_time_zone";

		/**
		 * "metric" stb.
		 */
		public static final String PREFERRED_MEASUREMENT_SYSTEM = "preferred_measurement_system";

		public static final String PREFERRED_THEME = "preferred_theme";

		public static final String QUICK_CONTACT_RECEIVERS = "quick_contact_receivers";

		/**
		 * email beállítások
		 */
		public static final String TOOLS_MAIL_SENDER_HOST = "tools.mail.sender.host";
		public static final String TOOLS_MAIL_SENDER_PORT = "tools.mail.sender.port";
		public static final String TOOLS_MAIL_SENDER_USERNAME = "tools.mail.sender.username";
		public static final String TOOLS_MAIL_SENDER_FROM_ADDRESS = "tools.mail.sender.fromAddress";
		public static final String TOOLS_MAIL_SENDER_PASSWORD = "tools.mail.sender.password";
		public static final String TOOLS_MAIL_SENDER_SSL_ON_CONNECT = "tools.mail.sender.ssl-on-connect";

		/**
		 * amazon S3 beállítások
		 */
		public static final String TOOLS_AMAZON_S3_REGION_NAME = "tools.amazon.s3.region-name";
		public static final String TOOLS_AMAZON_S3_BUCKET_NAME = "tools.amazon.s3.bucket-name";
		public static final String TOOLS_AMAZON_S3_ACCESS_KEY = "tools.amazon.s3.access-key";
		public static final String TOOLS_AMAZON_S3_SECRET_KEY = "tools.amazon.s3.secret-key";

	}

	/**
	 * @see UserKeyValueSettingsService
	 */
	public class UserKeyValueSettings {

		protected UserKeyValueSettings() {
			//
		}

		public static final String PREFERRED_CURRENCY = "preferred_currency";
		public static final String PREFERRED_LOCALE = "preferred_locale";

		/**
		 * "CET" stb.
		 */
		public static final String PREFERRED_TIME_ZONE = "preferred_time_zone";

		/**
		 * "metric" stb.
		 */
		public static final String PREFERRED_MEASUREMENT_SYSTEM = "preferred_measurement_system";

		public static final String PREFERRED_THEME = "preferred_theme";

	}

}
