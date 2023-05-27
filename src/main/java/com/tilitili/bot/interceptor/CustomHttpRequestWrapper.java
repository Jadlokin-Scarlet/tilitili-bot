package com.tilitili.bot.interceptor;

import lombok.extern.slf4j.Slf4j;

import javax.servlet.ReadListener;
import javax.servlet.ServletInputStream;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import java.io.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
public class CustomHttpRequestWrapper extends HttpServletRequestWrapper {

	private final String body;

	public CustomHttpRequestWrapper(HttpServletRequest request) throws IOException {
		super(request);
		body = readInputStreamInStringFormat(request.getInputStream(), StandardCharsets.UTF_8);
		String requestURL = request.getRequestURL().toString();
		String queryString = request.getQueryString();
		String cookie = Arrays.stream(Optional.ofNullable(request.getCookies()).orElse(new Cookie[]{})).map(c -> String.format("%s=%s", c.getName(), c.getValue())).collect(Collectors.joining("; "));
		String message = String.format("%s?%s%nbody=%s%ncookie=%s", requestURL, queryString, body, cookie);
		log.info(message);
	}


	private String readInputStreamInStringFormat(InputStream stream, Charset charset) throws IOException {
		final int MAX_BODY_SIZE = 102400;
		final StringBuilder bodyStringBuilder = new StringBuilder();
		if (!stream.markSupported()) {
			stream = new BufferedInputStream(stream);
		}

		stream.mark(MAX_BODY_SIZE + 1);
		final byte[] entity = new byte[MAX_BODY_SIZE + 1];

		int bytesRead;
		while ((bytesRead = stream.read(entity)) != -1) {
			bodyStringBuilder.append(new String(entity, 0, bytesRead, charset));
		}
		stream.reset();

		return bodyStringBuilder.toString();
	}

	@Override
	public BufferedReader getReader() throws IOException {
		return new BufferedReader(new InputStreamReader(getInputStream()));
	}

	@Override
	public ServletInputStream getInputStream () throws IOException {
		final ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(body.getBytes());

		return new ServletInputStream() {
			private boolean finished = false;

			@Override
			public boolean isFinished() {
				return finished;
			}

			@Override
			public int available() throws IOException {
				return byteArrayInputStream.available();
			}

			@Override
			public void close() throws IOException {
				super.close();
				byteArrayInputStream.close();
			}

			@Override
			public boolean isReady() {
				return true;
			}

			@Override
			public void setReadListener(ReadListener readListener) {
				throw new UnsupportedOperationException();
			}

			public int read () throws IOException {
				int data = byteArrayInputStream.read();
				if (data == -1) {
					finished = true;
				}
				return data;
			}
		};
	}
}