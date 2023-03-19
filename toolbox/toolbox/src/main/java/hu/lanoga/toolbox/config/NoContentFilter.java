package hu.lanoga.toolbox.config;

import java.io.IOException;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.http.HttpStatus;
import org.springframework.web.filter.OncePerRequestFilter;

// @Component
public class NoContentFilter extends OncePerRequestFilter {

	// TODO: ez mi? kell még? ki van kommentelve

	@Override
	protected void doFilterInternal(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse, FilterChain filterChain) throws ServletException, IOException {

		filterChain.doFilter(httpServletRequest, httpServletResponse);

		if (httpServletResponse.getContentType() == null || httpServletResponse.getContentType().equals("")) {
			httpServletResponse.setStatus(HttpStatus.NO_CONTENT.value());
		}

	}

}
