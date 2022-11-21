package com.tilitili.bot.service;

import com.google.common.collect.ImmutableMap;
import com.tilitili.common.utils.OSSUtil;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import org.xhtmlrenderer.swing.Java2DRenderer;

import javax.imageio.ImageIO;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

@Service
public class TemplateImageService {
	private final DocumentBuilder builder;
	private final Configuration configuration;

	@Autowired
	public TemplateImageService(Configuration configuration) throws ParserConfigurationException {
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		builder = factory.newDocumentBuilder();
		this.configuration = configuration;
	}

	public String getLongStringImage(String text) throws Exception {
		Path tempFile = Files.createTempFile("longStringImage", ".png");
		turnImage("index.ftl", ImmutableMap.of("msg", text), tempFile.toFile());
		String imageUrl = OSSUtil.uploadOSSByFileWithType(tempFile.toFile(), "png");
		Files.delete(tempFile);
		return imageUrl;
	}

	private String getTemplate(String template, Map<String,Object> param) throws IOException, TemplateException {
		Map<String,Object> newParam = new HashMap<>();
		for (Map.Entry<String, Object> entry : param.entrySet()) {
			String key = entry.getKey();
			Object value = entry.getValue();
			newParam.put(key, value);
			if (value instanceof String) {
				String valueStr = (String) value;
				if (valueStr.contains("\n")) {
					newParam.put(key, valueStr.replaceAll("\n", "<br/>"));
				}
			}
		}

		Template temp = configuration.getTemplate(template);
		try (StringWriter stringWriter = new StringWriter()) {
			temp.process(newParam, stringWriter);
			return stringWriter.getBuffer().toString();
		}
	}

	public void turnImage(String template, Map<String,Object> map, File file) throws Exception {
		String html = getTemplate(template, map);
		try (ByteArrayInputStream bin = new ByteArrayInputStream(html.getBytes())) {
			Document document = builder.parse(bin);
			Java2DRenderer renderer = new Java2DRenderer(document, 600, 800);
			BufferedImage img = renderer.getImage();
			ImageIO.write(img, "png", file);
		}
	}

}
