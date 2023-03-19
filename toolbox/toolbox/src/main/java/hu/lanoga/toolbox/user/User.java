package hu.lanoga.toolbox.user;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;

import org.hibernate.validator.constraints.Length;
import org.json.JSONArray;
import org.json.JSONException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonRawValue;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.common.collect.ImmutableSet;
import com.teamunify.i18n.I;
import com.vaadin.ui.CheckBox;
import com.vaadin.ui.ComboBox;
import com.vaadin.ui.DateTimeField;
import com.vaadin.ui.Notification;
import com.vaadin.ui.TextArea;
import com.vaadin.ui.TextField;
import com.vaadin.ui.UI;

import hu.lanoga.toolbox.ToolboxSysKeys;
import hu.lanoga.toolbox.ToolboxSysKeys.CrudOperation;
import hu.lanoga.toolbox.codestore.CodeStoreItem;
import hu.lanoga.toolbox.codestore.CodeStoreItemJdbcRepository;
import hu.lanoga.toolbox.exception.ToolboxGeneralException;
import hu.lanoga.toolbox.export.ExporterIgnore;
import hu.lanoga.toolbox.export.ExporterOptions;
import hu.lanoga.toolbox.json.jackson.StringValueDeserializer;
import hu.lanoga.toolbox.repository.ToolboxPersistable;
import hu.lanoga.toolbox.repository.jdbc.Table;
import hu.lanoga.toolbox.repository.jdbc.View;
import hu.lanoga.toolbox.spring.ApplicationContextHelper;
import hu.lanoga.toolbox.spring.SecurityUtil;
import hu.lanoga.toolbox.spring.ToolboxUserDetails;
import hu.lanoga.toolbox.vaadin.component.JumpUrlCopyToClipboardButtonTextField;
import hu.lanoga.toolbox.vaadin.component.UserRoleField;
import hu.lanoga.toolbox.vaadin.component.crud.CreateOnlyCrudFormElement;
import hu.lanoga.toolbox.vaadin.component.crud.CrudFormElementCollection;
import hu.lanoga.toolbox.vaadin.component.crud.CrudGridColumn;
import hu.lanoga.toolbox.vaadin.component.crud.ViewOnlyCrudFormElement;
import hu.lanoga.toolbox.vaadin.util.UiHelper;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

/**
 * (Spring Security-hez is kell, UserDetails impl.)
 * 
 * @see SecurityUtil
 * @see hu.lanoga.toolbox.config.SecurityConfig
 */
@Slf4j
@Getter
@Setter
@Table(name = "auth_user")
@ExporterOptions(advancedPdfTemplateFile = "user-template.vm")
public class User implements ToolboxPersistable, ToolboxUserDetails {

	public static class VaadinForm implements CrudFormElementCollection<User> {

		@CreateOnlyCrudFormElement
		public TextField username = new TextField(I.trc("Caption", "Username"));
		public TextField email = new TextField(I.trc("Caption", "Email"));
		public TextField title = new TextField(I.trc("Caption", "Title"));
		public TextField givenName = new TextField(I.trc("Caption", "First name"));
		public TextField familyName = new TextField(I.trc("Caption", "Last name"));
		public TextField phoneNumber = new TextField(I.trc("Caption", "Phone number"));
		public TextField jobTitle = new TextField(I.trc("Caption", "Job title"));
		public TextArea note = new TextArea(I.trc("Caption", "Notes"));
		public CheckBox enabled2 = new CheckBox(I.trc("Caption", "Enabled"));
		
		public UserRoleField userRoles = new UserRoleField(I.trc("Caption", "User roles"));
	
		@ViewOnlyCrudFormElement
		public JumpUrlCopyToClipboardButtonTextField id = new JumpUrlCopyToClipboardButtonTextField(I.trc("Caption", "SysID"), "user-editor"); // TODO: tisztázni
		@ViewOnlyCrudFormElement
		public TextField gid = new TextField(I.trc("Caption", "GID"));
		@ViewOnlyCrudFormElement
		public ComboBox<Integer> createdBy = UiHelper.buildUserCombo(I.trc("Caption", "Record created by"), null, false, true);
		@ViewOnlyCrudFormElement
		public DateTimeField createdOn = new DateTimeField(I.trc("Caption", "Record created on"));
		@ViewOnlyCrudFormElement
		public ComboBox<Integer> modifiedBy = UiHelper.buildUserCombo(I.trc("Caption", "Record modified by (last)"), null, false, true);
		@ViewOnlyCrudFormElement
		public DateTimeField modifiedOn = new DateTimeField(I.trc("Caption", "Record modified on"));

		@Override
		public Void preBind(User modelObject, CrudOperation crudOperation) {
			
			this.userRoles.setUserAsIsNow(modelObject);
			
			return null;
		}
		
		@Override
		public Void preAction(final User modelObject, final CrudOperation crudOperation) {
						
			// ---
			
			final UserService userService = ApplicationContextHelper.getBean(UserService.class);
			modelObject.setUserRoles(userService.addAdditionalRoles2(modelObject)); // TODO: ez itt miért kell, ha magában a serviceban is van ilyen hívás

			// ---
			
			User oldState = null;
			if (modelObject.getId() != null) {
				oldState = userService.findOne(modelObject.getId());
			}

			if (oldState != null && !oldState.getUserRoles().equals(modelObject.getUserRoles())) {
				UI.getCurrent().showNotification(I.trc("Notification", "Note: after save here, the selected/target user needs a logout/login cycle for his/hers new/changed roles to take affect!"), Notification.TYPE_WARNING_MESSAGE);
			}

			return null;
		}

	}

	@ExporterIgnore
	private Integer tenantId;

	@CrudGridColumn(translationMsg = "SysID", columnExpandRatio = 0)
	private Integer id;
	
	@NotEmpty
	@Length(max = 100)
	@CrudGridColumn(translationMsg = "Username")
	private String username;

	@CrudGridColumn(translationMsg = "Other notes", allowHide = true, startHidden = true)
	@Length(max = 160)
	private String note;

	@Length(max = 50)
	@View
	private String title;

	@Length(max = 50)
	@View
	private String jobTitle;

	@ExporterIgnore
	private Integer superior;

	@View
	private java.sql.Date dateOfBirth;

	@Length(max = 100)
	@View
	@CrudGridColumn(translationMsg = "First name")
	private String givenName;

	@Length(max = 100)
	@View
	@CrudGridColumn(translationMsg = "Last name")
	private String familyName;

	@NotNull
	private Boolean enabled;

	@ExporterIgnore
	@JsonIgnore
	@View
	@NotNull
	private Boolean enabled2; // ez egy "hack", enabled mező nem ment Vaadin binding-gal, bekavart az, hogy 1) Boolean (nem primitív boolean) 2) ennek ellenére van isEnabled metódus... ez a Vaadin binder reflection részt megkavarta

	private Boolean anonymizedDeleted;

	@Length(max = 100)
	@NotEmpty
	@View
	@CrudGridColumn(translationMsg = "Email")
	private String email;

	@Pattern(regexp = "(^(\\++\\d*)*$)")
	@View
	@CrudGridColumn(translationMsg = "Phone number")
	private String phoneNumber;

	/**
	 * {@link ToolboxSysKeys.UserAuth#ROLE_TENANT_OVERSEER_STR} kapcsán megmutatja, hogy mely userek egymás "testvérei"/"avatarjai"...
	 * akiknél ugyanaz a parentId (plus maga a "parent" is) azokba lehet "át" lépni a jog megléte esetén
	 */
	private Integer parentId; // TODO: nevezzük át, nem kell avatar, legyen inkább csak overseerParentId

	@ExporterIgnore
	private Integer createdBy;

	@CrudGridColumn(translationMsg = "Record created on")
	private Timestamp createdOn;

	@ExporterIgnore
	private Integer modifiedBy;

	@CrudGridColumn(translationMsg = "Record modified on")
	private Timestamp modifiedOn;

	@View
	@CrudGridColumn(translationMsg = "Record created by", allowOrder = false)
	private String createdByCaption;

	@View
	@CrudGridColumn(translationMsg = "Record modified by", allowOrder = false)
	private String modifiedByCaption;

	@ExporterIgnore
	private Integer profileImg;

	@NotEmpty
	@JsonRawValue // a DB-ben JSON az érték, azért kell ez
	@JsonDeserialize(using = StringValueDeserializer.class) // a DB-ben JSON az érték, azért kell ez
	private String userRoles; // codeStore id JSON array...

	@ExporterIgnore
	private String additionalData1;

	@ExporterIgnore
	private String additionalData2;

	/**
	 * {@link GrantedAuthority} lista
	 */
	@JsonProperty // mivel a setteren van @JsonIgnore, de getter értéke kell JSON-ben is, ezért itt kell egy külön @JsonIgnore (és az authorities nem egy field)
	@Override
	public Collection<? extends GrantedAuthority> getAuthorities() {

		final List<SimpleGrantedAuthority> authorities = new ArrayList<>();

		try {

			final JSONArray jsonArray = new JSONArray(this.userRoles);

			for (int k = 0; k < jsonArray.length(); ++k) {

				// közvetlenül a CodeStoreItemJdbcRepository van meghívva a service helyett, mert itt kivételesen ki kell "kerülni" a @Secured annotációt

				final CodeStoreItem csi = ApplicationContextHelper.getBean(CodeStoreItemJdbcRepository.class).findOne(jsonArray.getInt(k));

				if (csi != null) {
					final String role = csi.getCommand();
					authorities.add(new SimpleGrantedAuthority(role));
				} else {
					log.warn("Missing ROLE CodeStoreItem (id: " + jsonArray.getInt(k) + ")!");
				}

			}

		} catch (final JSONException e) {
			throw new ToolboxGeneralException("JSON error!", e);
		}

		return ImmutableSet.copyOf(authorities);

	}

	@SuppressWarnings("unused")
	@JsonIgnore // kell a hu.lanoga.toolbox.json.jackson.JacksonHelper.deepCopy(T, Class<T>) miatt
	void setAuthorities(final Collection<? extends GrantedAuthority> authorities) {
		// no-po (szándékos)
	}

	@Override
	public boolean isAccountNonExpired() {
		return true;
	}

	@Override
	public boolean isAccountNonLocked() {
		return true;
	}

	@Override
	public boolean isCredentialsNonExpired() {
		return true;
	}

	@Override
	public boolean isEnabled() {
		if (this.enabled == null) {
			return false;
		}

		return this.enabled;
	}

	@JsonIgnore
	// @ExporterIgnore // csak field-re nézi az exporter (csak ezeket keresi ki reflection-nel), ha a jövőben változna, akkor itt is ki kell egészíteni
	@Override
	public String getPassword() {
		final UserPassword passwordModel = ApplicationContextHelper.getBean(UserPasswordJdbcRepository.class).findUserPassword(this.tenantId, this.id);
		return passwordModel.getPassword();
	}

	public void setEnabled(final Boolean enabled) {
		this.enabled = enabled;
		this.enabled2 = enabled;
	}

	public void setEnabled2(final Boolean enabled2) {
		this.enabled2 = enabled2;
		this.enabled = enabled2;
	}

	@Override
	public String toString() {
		final StringBuilder builder = new StringBuilder();
		builder.append("User [id=");
		builder.append(this.id);
		builder.append(", username=");
		builder.append(this.username);
		builder.append(", jobTitle=");
		builder.append(this.jobTitle);
		builder.append(", title=");
		builder.append(this.title);
		builder.append(", givenName=");
		builder.append(this.givenName);
		builder.append(", familyName=");
		builder.append(this.familyName);
		builder.append(", enabled=");
		builder.append(this.enabled);
		builder.append(", email=");
		builder.append(this.email);
		builder.append(", userRoles=");
		builder.append(this.userRoles);
		builder.append(", tenantId=");
		builder.append(this.tenantId);
		builder.append("]");
		return builder.toString();
	}

}
