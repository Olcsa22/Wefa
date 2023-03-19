package hu.lanoga.toolbox.payment;

import org.hibernate.validator.constraints.Length;

import com.teamunify.i18n.I;
import com.vaadin.ui.ComboBox;
import com.vaadin.ui.DateTimeField;
import com.vaadin.ui.TextArea;
import com.vaadin.ui.TextField;

import hu.lanoga.toolbox.export.ExporterIgnore;
import hu.lanoga.toolbox.repository.ToolboxPersistable;
import hu.lanoga.toolbox.repository.jdbc.View;
import hu.lanoga.toolbox.vaadin.component.crud.CrudFormElementCollection;
import hu.lanoga.toolbox.vaadin.component.crud.CrudGridColumn;
import hu.lanoga.toolbox.vaadin.component.crud.SecondaryCrudFormElement;
import hu.lanoga.toolbox.vaadin.component.crud.ViewOnlyCrudFormElement;
import hu.lanoga.toolbox.vaadin.util.UiHelper;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PaymentTransactionRawLog implements ToolboxPersistable {

	public static class VaadinForm implements CrudFormElementCollection<PaymentTransactionRawLog> {

		public TextField paymentTransactionId = new TextField(I.trc("Caption", "Payment transaction record"));
		public TextArea logData = new TextArea(I.trc("Caption", "LOG data"));

		@SecondaryCrudFormElement
		@ViewOnlyCrudFormElement
		public TextField id = new TextField(I.trc("Caption", "SysID"));

		@SecondaryCrudFormElement
		@ViewOnlyCrudFormElement
		public ComboBox<Integer> createdBy = UiHelper.buildUserCombo(I.trc("Caption", "Record created by"), null, false, true);

		@SecondaryCrudFormElement
		@ViewOnlyCrudFormElement
		public DateTimeField createdOn = new DateTimeField(I.trc("Caption", "Record created on"));

	}

	public PaymentTransactionRawLog() {
		//
	}
	
	public PaymentTransactionRawLog(Integer paymentTransactionId, String logData) {
		super();
		this.paymentTransactionId = paymentTransactionId;
		this.logData = logData;
	}

	private Integer id;

	@ExporterIgnore
	private Integer tenantId;

	@CrudGridColumn(translationMsg = "Payment transaction record", startHidden = true)
	private Integer paymentTransactionId;

	@Length(max = 2000)
	private String logData;

	@View
	@CrudGridColumn(translationMsg = "LOG data (short)")
	private String logDataShort;
	
	private Integer createdBy;

	@CrudGridColumn(translationMsg = "Record created on")
	private java.sql.Timestamp createdOn;

	private Integer modifiedBy;

	private java.sql.Timestamp modifiedOn;

}
