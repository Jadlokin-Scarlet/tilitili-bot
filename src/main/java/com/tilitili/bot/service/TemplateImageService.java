package com.tilitili.bot.service;

import com.google.common.collect.ImmutableMap;
import com.tilitili.common.utils.OSSUtil;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import org.xhtmlrenderer.layout.SharedContext;
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

@Slf4j
@Service
public class TemplateImageService {
	private final DocumentBuilder builder;
	private final Configuration configuration;

//	private final Font font;

	@Autowired
	public TemplateImageService(Configuration configuration) throws ParserConfigurationException {
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		this.builder = factory.newDocumentBuilder();
		this.configuration = configuration;

//		try (InputStream resource = TemplateImageService.class.getResourceAsStream("/SimSun.ttf")) {
//			font = Font.createFont(Font.TRUETYPE_FONT, resource);
//		} catch (FontFormatException | IOException e) {
//			log.error("初始化字体文件异常", e);
//			throw new AssertException("初始化字体文件异常");
//		}
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

	public void turnImage(String template, Map<String,Object> params, File file) throws Exception {
		String html = getTemplate(template, params);
		try (ByteArrayInputStream bin = new ByteArrayInputStream(html.getBytes())) {
			Document document = builder.parse(bin);
			Java2DRenderer renderer = new Java2DRenderer(document, 600, 800);
//			renderer.setBufferedImageType(4);
//			Map<RenderingHints.Key, Object> map = new HashMap<>();//设置参数
//			map.put(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
//			map.put(RenderingHints.KEY_COLOR_RENDERING, RenderingHints.VALUE_COLOR_RENDER_QUALITY);
//			map.put(RenderingHints.KEY_DITHERING, RenderingHints.VALUE_DITHER_ENABLE);
//			map.put(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
//			map.put(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
//			map.put(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
//			map.put(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
//			map.put(RenderingHints.KEY_ALPHA_INTERPOLATION, RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY);
//			map.put(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
//			renderer.setRenderingHints(map);
			SharedContext sharedContext = renderer.getSharedContext();
//			sharedContext.setDotsPerPixel(1);
			sharedContext.getTextRenderer().setSmoothingThreshold(1);
//			sharedContext.setDPI(523);
//			AWTFontResolver fontResolver = (AWTFontResolver) sharedContext.getFontResolver();
//			fontResolver.setFontMapping("SimSun", font);

			BufferedImage img = renderer.getImage();
			ImageIO.write(img, "png", file);
		}
	}
}
