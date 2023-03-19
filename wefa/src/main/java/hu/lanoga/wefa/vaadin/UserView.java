package hu.lanoga.wefa.vaadin;

import com.vaadin.navigator.View;
import com.vaadin.navigator.ViewChangeListener;

import hu.lanoga.toolbox.spring.SecurityUtil;
import hu.lanoga.toolbox.vaadin.component.UserComponent;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class UserView extends UserComponent implements View {

	public UserView() {
		super(!SecurityUtil.hasAdminRole());
	}

	@Override
	public void enter(final ViewChangeListener.ViewChangeEvent event) {
		this.initLayout();

		// -----------------------------

		// régi, egyszerű Groovy modell/form próba
		// try {
		//
		// final StringBuilder sbGroovyScript1 = new StringBuilder();
		//
		// sbGroovyScript1.append("import hu.lanoga.toolbox.repository.ToolboxPersistable;\r\n");
		// sbGroovyScript1.append("public class Sample implements ToolboxPersistable {\r\n");
		// sbGroovyScript1.append("private Integer id; private String name; private Boolean enabled;\r\n");
		// sbGroovyScript1.append("public Integer getId() { return id; }\r\n");
		// sbGroovyScript1.append("public void setId(Integer id) { this.id = id; }\r\n");
		// sbGroovyScript1.append("public String getName() { return name; }\r\n");
		// sbGroovyScript1.append("public void setName(String name) { this.name = name; }\r\n");
		// sbGroovyScript1.append("public Boolean getEnabled() { return enabled; }\r\n");
		// sbGroovyScript1.append("public void setEnabled(Boolean enabled) { this.enabled = enabled; }\r\n");
		// sbGroovyScript1.append("}\r\n");
		//
		// final Class domainClass = ToolboxGroovyHelper.buildClass(sbGroovyScript1.toString());
		//
		// // ---
		//
		// final StringBuilder sbGroovyScript2 = new StringBuilder();
		// sbGroovyScript2.append("import com.vaadin.ui.*;\r\n");
		// sbGroovyScript2.append("import hu.lanoga.toolbox.vaadin.component.crud.ViewOnlyCrudFormElement;\r\n");
		// sbGroovyScript2.append("import com.teamunify.i18n.I;\r\n");
		// sbGroovyScript2.append("import hu.lanoga.toolbox.vaadin.component.crud.CrudFormElementCollection;\r\n");
		// sbGroovyScript2.append("public class SampleVaadinForm implements CrudFormElementCollection {\r\n");
		// sbGroovyScript2.append("public TextField name = new TextField(I.trc('Caption', 'Név'));\r\n");
		// sbGroovyScript2.append("@ViewOnlyCrudFormElement\r\n");
		// sbGroovyScript2.append("public CheckBox enabled = new CheckBox(I.trc('Caption', 'Engedélyezett'));\r\n");
		// sbGroovyScript2.append("}\r\n");
		//
		// // ---
		//
		// MultiFormLayoutCrudFormComponent crudFormComponent = new MultiFormLayoutCrudFormComponent(() -> {
		// return ToolboxGroovyHelper.buildClassInstance(sbGroovyScript2.toString());
		// }, null);
		// crudFormComponent.setDomainObject((ToolboxPersistable) domainClass.newInstance());
		// crudFormComponent.setCrudOperation(CrudOperation.UPDATE);
		// crudFormComponent.setCrudAction(v -> {
		// try {
		// System.out.println("save..." + domainClass.getMethod("getName").invoke(v));
		// System.out.println("save..." + JacksonHelper.toJson(v));
		//
		// } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException | NoSuchMethodException | SecurityException e) {
		// log.error("x------------", e);
		// }
		// });
		//
		// this.addComponent(crudFormComponent);
		// crudFormComponent.init();
		//
		// } catch (Exception e) {
		// log.error("y------------", e);
		// }

	}

}