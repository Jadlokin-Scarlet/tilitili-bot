package com.tilitili.bot.interceptor;

import org.springframework.stereotype.Component;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.Arrays;

@Component
public class RequestBodyFilter implements Filter {

	@Override
	public void destroy() {
	}

	@Override
	public void doFilter(ServletRequest request, ServletResponse response,
						 FilterChain chain) throws IOException, ServletException {
		HttpServletRequest httpRequest = (HttpServletRequest) request;
		if(Arrays.asList("POST", "PUT").contains(httpRequest.getMethod()) && isJsonRequest(httpRequest)) {
			CustomHttpRequestWrapper requestWrapper = new CustomHttpRequestWrapper(httpRequest);
			chain.doFilter(requestWrapper, response);
			return;
		}
		chain.doFilter(request, response);
	}

	private boolean isJsonRequest(HttpServletRequest request) {
		String contentType = request.getContentType();
		return contentType != null && contentType.startsWith("application/json");
	}

	@Override
	public void init(FilterConfig arg0) throws ServletException {
	}
}