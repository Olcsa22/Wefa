package hu.lanoga.toolbox.codestore;

import java.util.List;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import hu.lanoga.toolbox.controller.DefaultCrudController;

@RequestMapping(value = "api/code-store-items")
@ConditionalOnMissingBean(name = "codeStoreItemControllerOverrideBean")
@ConditionalOnProperty(name = "tools.code.store.item.controller.enabled", matchIfMissing = true)
@RestController
public class CodeStoreItemController extends DefaultCrudController<CodeStoreItem, CodeStoreItemService> {

	@RequestMapping(value = "/type/{typeId}", method = RequestMethod.GET)
	public List<CodeStoreItem> findAllByType(@PathVariable final Integer typeId) {
		return this.service.findAllByType(typeId);
	}

}
