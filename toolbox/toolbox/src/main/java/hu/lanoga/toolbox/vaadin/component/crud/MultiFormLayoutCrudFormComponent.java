package hu.lanoga.toolbox.vaadin.component.crud;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.apache.commons.lang3.StringUtils;
import org.flywaydb.core.internal.util.Pair;

import com.google.common.collect.Lists;
import com.teamunify.i18n.I;
import com.vaadin.shared.ui.MarginInfo;
import com.vaadin.ui.AbstractLayout;
import com.vaadin.ui.Button;
import com.vaadin.ui.FormLayout;
import com.vaadin.ui.TabSheet;
import com.vaadin.ui.UI;
import com.vaadin.ui.VerticalLayout;

import hu.lanoga.toolbox.ToolboxSysKeys;
import hu.lanoga.toolbox.repository.ToolboxPersistable;
import hu.lanoga.toolbox.vaadin.AbstractToolboxUI;
import hu.lanoga.toolbox.vaadin.util.UiHelper;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@SuppressWarnings("rawtypes")
@Getter
@Setter
@Slf4j
public class MultiFormLayoutCrudFormComponent<D extends ToolboxPersistable, C extends CrudFormElementCollection> extends VerticalLayout implements CrudFormElementCollectionCrudComponent<D, C> {

	private D domainObject;
	private ToolboxSysKeys.CrudOperation crudOperation;
	private Consumer<D> crudAction;

	private Supplier<C> crudFormElementCollectionSupplier;

	private Button btnMain;

	private List<String> tabCaptions;

	private Map<ToolboxSysKeys.CrudOperation, String> notifMap = new HashMap<>();

	private Consumer<String> glitchCallbackConsumer;
	private int allowedTabNumbers = 15;

	private TabSheet ts;

	public MultiFormLayoutCrudFormComponent() {
		super();
	}

	/**
	 * @param crudFormElementCollectionSupplier
	 * @param tabCaptions
	 * 		null esetén default feliratok
	 */
	public MultiFormLayoutCrudFormComponent(final Supplier<C> crudFormElementCollectionSupplier, final List<String> tabCaptions) {
		this();

		this.crudFormElementCollectionSupplier = crudFormElementCollectionSupplier;

		this.tabCaptions = tabCaptions;
	}

	/**
	 * @param crudFormElementCollectionSupplier
	 * @param tabCaptions
	 * 		null esetén default feliratok
	 * @param notifMap
	 */
	public MultiFormLayoutCrudFormComponent(final Supplier<C> crudFormElementCollectionSupplier, final List<String> tabCaptions, final Map<ToolboxSysKeys.CrudOperation, String> notifMap) {
		this();

		this.crudFormElementCollectionSupplier = crudFormElementCollectionSupplier;

		this.tabCaptions = tabCaptions;

		this.notifMap = notifMap;
	}

	@Override
	public void init() {

		this.setMargin(new MarginInfo(true, true, false, true));

		// ---

		this.ts = new TabSheet();
		this.ts.addSelectedTabChangeListener(x -> UiHelper.centerParentWindow(this));
		this.addComponent(this.ts);

		final List<AbstractLayout> flList = new ArrayList<>();
		for (int i = 0; i < allowedTabNumbers; ++i) {
			flList.add(new FormLayout());
		}

		// ---

		final Pair<Button, C> pair = this.initInner(this.crudOperation, flList, this.crudFormElementCollectionSupplier, this.domainObject, this.crudAction, this.notifMap);
		this.btnMain = pair.getLeft();
		C crudFormElementCollection = pair.getRight();
		
		// ---
		
		try {
			
			if (crudFormElementCollection.getClass().isAnnotationPresent(MultiFormLayoutCrudFormComponentTabNames.class)) {
				
				MultiFormLayoutCrudFormComponentTabNames m = crudFormElementCollection.getClass().getAnnotation(MultiFormLayoutCrudFormComponentTabNames.class);
				this.tabCaptions = Lists.newArrayList(m.value().split(";"));
				
			}
			
		} catch (Exception e) {
			log.warn("MultiFormLayoutCrudFormComponentTabNames annotation processing failed", e);
		}
		
		
		// ---
		
		int nn = 0;

		for (int i = 0; i < allowedTabNumbers; ++i) {
			final FormLayout fl = (FormLayout) flList.get(i);
			if (fl.getComponentCount() > 0) {
				nn++;
			}
		}
		
		for (int i = 0; i < allowedTabNumbers; ++i) {

			final FormLayout fl = (FormLayout) flList.get(i);

			if (fl.getComponentCount() > 0) {

				fl.setMargin(new MarginInfo(true, false, true, false));
				
				String strCap = null;
				
				if (this.tabCaptions != null && (i <= tabCaptions.size() - 1) && StringUtils.isNotBlank(this.tabCaptions.get(i))) {
					strCap = this.tabCaptions.get(i);
				} else {
					if (i == 0) {
						strCap = I.trc("Tab", "Basic data");
					} else {
						strCap = I.trc("Tab", "Additional data");
						if (nn > 2) {
							strCap +=  " " + i;
						}
					}
				}

				if (!strCap.equals("<hidden>")) { // TODO: tisztázni
					this.ts.addTab(fl, strCap);
				}

			}

		}

		if (this.btnMain != null) {
			this.addComponent(this.btnMain);
		}

		// ---

		UiHelper.centerParentWindow(this);

		this.glitchCallbackConsumer = c -> UiHelper.centerParentWindow(this);
		((AbstractToolboxUI) UI.getCurrent()).emptyCallbackViaAtuiCallback(new WeakReference<>(this.glitchCallbackConsumer));
	}

	/**
	 * lehet null is (ha nincs gomb)
	 */
	@Override
	public Button getMainButton() {
		return this.btnMain;
	}

}
