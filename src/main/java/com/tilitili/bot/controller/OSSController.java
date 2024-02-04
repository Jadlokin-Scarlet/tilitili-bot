package com.tilitili.bot.controller;

import com.tilitili.common.entity.view.BaseModel;
import com.tilitili.common.utils.Asserts;
import com.tilitili.common.utils.OSSUtil;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@Controller
@RequestMapping("/api/oss")
public class OSSController extends BaseController {
	@PostMapping("/uploadFileToOSS")
	@ResponseBody
	public BaseModel<String> uploadFileToOSS(MultipartFile file) throws IOException {
		Asserts.notNull(file, "参数异常");
		String filename = file.getOriginalFilename();
		String ossUrl;
		if (filename != null && filename.contains(".")) {
			String fileType = filename.substring(filename.indexOf(".") + 1);
			ossUrl = OSSUtil.uploadOSSBySteam(file.getInputStream(), fileType);
		} else {
			ossUrl = OSSUtil.uploadOSSBySteam(file.getInputStream());
		}
		Asserts.notBlank(ossUrl, "上传文件失败");
		return BaseModel.success(ossUrl);
	}
}
