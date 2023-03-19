package hu.lanoga.toolbox.controller;

import org.apache.commons.lang3.StringUtils;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

import hu.lanoga.toolbox.util.BrandUtil;

/**
 * átirányítások...
 * 
 * (Spring "redirect:" működése: will respond with a 302 and the new URL in the Location header; the browser/client will then make another request to the new URL) 
 * (Spring "forward:" működése: happens entirely on a server side; the Servlet container forwards the same request to the target URL; the URL won’t change in the browser)
 */
@Controller // fontos, hogy sima Controller, nem RestController...
@ConditionalOnMissingBean(name = "redirectControllerOverrideBean")
public class RedirectController {

	@RequestMapping({
			"/public/login/"
	})
	public String loginWithEndingSlash() {

		// példák még (értsd redirectControllerOverrideBean-ben használd ezt, vagy hasonlót szükség esetén)
		// return "forward:/admin/index.html"; // ez kell akkor, ha az admin rész a /admin alatt van és Angular
		// return "forward:/admin/v/ui"; // ez kell akkor, ha az admin rész a /admin alatt van és Vaadin

		return "redirect:/public/login"; // ez kell akkor, ha nincs külön admin oldal
	}

	@RequestMapping({
			"/admin", "/admin/"
	})
	public String admin() {

		// példák még (értsd redirectControllerOverrideBean-ben használd ezt, vagy hasonlót szükség esetén)
		// return "forward:/admin/index.html"; // ez kell akkor, ha az admin rész a /admin alatt van és Angular
		// return "forward:/admin/v/ui"; // ez kell akkor, ha az admin rész a /admin alatt van és Vaadin

		// TODO: zavaros, túl sok dolog van, összevetni a AuthenticationSuccessHandler-ben lévő dolgokkal is
		
		return "redirect:/"; // ez kell akkor, ha nincs külön admin oldal
	}

	@RequestMapping("/b/assets/manifest.json")
	public String manifest() {

		String brand = BrandUtil.getBrand(false, true);

		if (StringUtils.isBlank(brand) || brand.equalsIgnoreCase("default")) {
			return "forward:/default/assets/manifest.json";
		} else {
			return "forward:/" + brand + "/assets/manifest.json";
		}
	}
	
	@RequestMapping({
			"/super-admin", "/super-admin/"
	})
	public String superAdmin() {
		return "redirect:/super-admin/v/main";
	}

	@RequestMapping({
			"/super-user", "/super-user/"
	})
	public String superUser() {
		return "redirect:/super-admin/v/main"; // alap esetben nincs külön super-user
	}

	@RequestMapping({
			"/sw.js"
	})
	public String serviceWorker() {
		return "forward:/default/assets/sw.js";
	}

}
