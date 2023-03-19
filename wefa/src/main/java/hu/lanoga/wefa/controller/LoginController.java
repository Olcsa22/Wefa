package hu.lanoga.wefa.controller;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.web.bind.annotation.RestController;

@RestController("loginControllerOverrideBean")
public class LoginController extends hu.lanoga.toolbox.auth.LoginController {

	@Override
	public String login(HttpServletRequest request, HttpServletResponse response) {
		
		response.addHeader("X-Frame-Options", "deny"); // TODO: SecurityConfig-ban zavaros, ezért itt letiltva // TODO: miért is?
		
		return super.login(request, response);
	}

	
	
}
