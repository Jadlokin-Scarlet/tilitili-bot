package com.tilitili.bot.controller;

import com.tilitili.common.entity.view.BaseModel;
import com.tilitili.common.exception.AssertException;
import lombok.extern.slf4j.Slf4j;
import org.apache.catalina.connector.ClientAbortException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.net.URLEncoder;
import java.util.Optional;

@Slf4j
public class BaseController {

    @ExceptionHandler(AssertException.class)
    @ResponseBody
    public BaseModel<?> handleAssertError(Exception ex) {

        return new BaseModel<>(ex.getMessage());
    }

    @ExceptionHandler(Exception.class)
    @ResponseBody
    public BaseModel<?> handleError(HttpServletRequest req, Exception ex) {
        log.error("Request: " + req.getRequestURL(), ex);
        return new BaseModel<>("网络异常");
    }

    /**
     * 下载服务器已存在的文件,支持断点续传
     *
     * @param request
     *            请求对象
     * @param response
     *            响应对象
     * @param file
     *            文件路径(绝对)
     */
    public void download(HttpServletRequest request, HttpServletResponse response, File file) {
        log.debug("下载文件路径：" + file.getPath());

        try {
            // 设置响应报头
            long fSize = file.length();
            response.setContentType("application/x-download");
            // Content-Disposition: attachment; filename=WebGoat-OWASP_Developer-5.2.zip
            response.addHeader("Content-Disposition", "attachment; filename=" + URLEncoder.encode(file.getName(), "UTF-8"));
            // Accept-Ranges: bytes
            response.setHeader("Accept-Ranges", "bytes");
            long pos = 0; // pos开始读取位置; last最后读取位置; sum记录总共已经读取了多少字节
            long last = fSize - 1;
            long sum = 0;
            String range = request.getHeader("Range");
            if (null != range) {
                // 断点续传
                response.setStatus(HttpServletResponse.SC_PARTIAL_CONTENT);
                // 情景一：RANGE: bytes=2000070- 情景二：RANGE: bytes=2000070-2000970
                String numRang = range.replace("bytes=", "");
                String[] strRange = numRang.split("-");
                if (strRange.length == 2) {
                    pos = Optional.ofNullable(parseLongIfNumber(strRange[0].trim())).orElse(0L);
                    last = Optional.ofNullable(parseLongIfNumber(strRange[1].trim())).orElse(0L);
                } else {
                    pos = Optional.ofNullable(parseLongIfNumber(numRang.replace("-", "").trim())).orElse(0L);
                }
            }
            long rangLength = last - pos + 1;// 总共需要读取的字节
            // Content-Range: bytes 10-1033/304974592
            String contentRange = "bytes " + pos + "-" + last + "/" + fSize;
            response.setHeader("Content-Range", contentRange);
            // Content-Length: 1024
            response.addHeader("Content-Length", String.valueOf(rangLength));

            // 跳过已经下载的部分，进行后续下载
            try (OutputStream bufferOut = new BufferedOutputStream(response.getOutputStream())) {
                try (InputStream inputStream = new BufferedInputStream(new FileInputStream(file))){
                    long skip = inputStream.skip(pos);
                    if (pos != skip) {
                        log.error("断点查找失败 pos={} skip={}", pos, skip);
                    }
                    byte[] buffer = new byte[1024];
                    int length;
                    while (sum < rangLength) {
                        length = inputStream.read(buffer, 0, ((rangLength - sum) <= buffer.length ? ((int) (rangLength - sum)) : buffer.length));
                        sum = sum + length;
                        bufferOut.write(buffer, 0, length);
                    }
                }
            }
        } catch (ClientAbortException e) {
            // 浏览器点击取消
            log.warn("用户取消下载!");
        } catch (Throwable e) {
            log.error("下载文件失败....", e);
        }
    }

    private Long parseLongIfNumber(String str) {
        try {
            return Long.parseLong(str);
        } catch (NumberFormatException e) {
            log.error(str + " is not Number!");
            return null;
        }
    }
}
