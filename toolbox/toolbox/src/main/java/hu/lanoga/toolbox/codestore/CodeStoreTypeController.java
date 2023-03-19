package hu.lanoga.toolbox.codestore;

import java.util.List;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import hu.lanoga.toolbox.controller.DefaultCrudController;

@RequestMapping(value = "api/code-store-types")
@ConditionalOnMissingBean(name = "codeStoreTypeControllerOverrideBean")
@ConditionalOnProperty(name = "tools.code.store.type.controller.enabled", matchIfMissing = true)
@RestController
public class CodeStoreTypeController extends DefaultCrudController<CodeStoreType, CodeStoreTypeService> {

	@RequestMapping(value = "/find-all-expandable", method = RequestMethod.GET)
	public List<CodeStoreType> findAllExpandable() {
		return this.service.findAllExpandable();
	}

}
