package com.douding.file.controller.admin;

import ch.qos.logback.core.util.FileUtil;
import com.douding.server.domain.Test;
import com.douding.server.dto.FileDto;
import com.douding.server.dto.ResponseDto;
import com.douding.server.enums.FileUseEnum;
import com.douding.server.service.FileService;
import com.douding.server.service.TestService;
import com.douding.server.util.Base64ToMultipartFile;
import com.douding.server.util.UuidUtil;
import com.mysql.cj.util.StringUtils;
import org.apache.tomcat.util.http.fileupload.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.util.FileCopyUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Date;
import java.util.List;

/*
    返回json 应用@RestController
    返回页面  用用@Controller
 */
@RequestMapping("/admin/file")
@RestController
public class UploadController implements InitializingBean {

    private static final Logger LOG = LoggerFactory.getLogger(UploadController.class);
    public  static final String BUSINESS_NAME ="文件上传";

    @Resource
    private TestService testService;

    @Value("${file.path}")
    private String FILE_PATH;

    @Value("${file.domain}")
    private String FILE_DOMAIN;

    @Resource
    private FileService fileService;

    @Override
    public void afterPropertiesSet() throws Exception {
        File course = new File(FILE_PATH + "course");
        File teacher = new File(FILE_PATH + "teacher");
        File segment = new File(FILE_PATH + "segment");
        FileUtils.forceMkdir(course);
        FileUtils.forceMkdir(teacher);
        FileUtils.forceMkdir(segment);
    }

    @RequestMapping("/upload")
    public ResponseDto<FileDto> upload(@RequestBody FileDto fileDto) throws Exception {
        ResponseDto<FileDto> responseDto = new ResponseDto<>();
        // 第一次上传
        if (fileService.findByKey(fileDto.getKey()) == null) {
            fileDto = createSegmentFile(fileDto);
            fileDto.setShardIndex(1);
        }
        // 存储分片
        saveSegment(fileDto);
        // 检测是否完成合成
        if (fileDto.getShardIndex().equals(fileDto.getShardTotal())) {
            // 合并分片
            merge(fileDto);
        }
        // 更新数据库
        fileService.save(fileDto);
        fileDto.setPath(FILE_DOMAIN + fileDto.getPath());
        responseDto.setContent(fileDto);
        return responseDto;
    }

    @GetMapping("/check/{key}")
    public ResponseDto<FileDto> check(@PathVariable String key) throws Exception {
        LOG.info("检查上传分片开始：{}", key);
        FileDto fileDto = fileService.findByKey(key);
        ResponseDto<FileDto> responseDto = new ResponseDto<>();
        if (fileDto != null) {
            fileDto.setPath(FILE_DOMAIN + fileDto.getPath());
            responseDto.setContent(fileDto);
        }
        LOG.info("上传分片结束：{}", key);
        return responseDto;
    }

    //合并分片
    public void merge(FileDto fileDto) throws Exception {
        LOG.info("合并分片开始");
        Integer shardTotal = fileDto.getShardTotal();
        File target = new File(FILE_PATH + "/segment/" + fileDto.getKey() + "/");
        List<FileInputStream> fisList = new ArrayList<>();
        try (FileOutputStream fos = new FileOutputStream(FILE_PATH + fileDto.getPath())) {
            for (int i = 1; i <= shardTotal; i++) {
                FileInputStream fis = new FileInputStream(target + "/" + i);
                fisList.add(fis);
            }
            byte[] bytes = new byte[1024 * 1024];
            int readCount;
            for (FileInputStream fis : fisList) {
                while ((readCount = fis.read(bytes)) != -1) {
                    fos.write(bytes, 0, readCount);
                }
            }
            fos.flush();
        } finally {
            for (FileInputStream fis : fisList) {
                if (fis != null) fis.close();
            }
        }
        LOG.info("清空分片");
        FileUtils.cleanDirectory(target);
        target.delete();
    }

    // 第一次出现的文件，把数据存到数据库中
    public FileDto createSegmentFile(FileDto fileDto){
        String path;
        // 文件路径
        if (fileDto.getUse().equals("T")) {
            path = "teacher/" + fileDto.getKey() + "." + fileDto.getSuffix();
        } else {
            path = "course/" + fileDto.getKey() + "." + fileDto.getSuffix();
        }
        fileDto.setPath(path);
        fileDto.setShardIndex(0);
        // 持久化
        fileService.save(fileDto);
        return fileDto;
    }

    // 存储分片到服务器
    public void saveSegment(FileDto fileDto) throws Exception {
        // 获取图片字节
        String base64Data =  fileDto.getShard().split(",")[1];
        Base64.Decoder decoder = Base64.getDecoder();
        byte[] segmentData = decoder.decode(base64Data);
        String path = FILE_PATH + "/segment/" + fileDto.getKey() + "/"
                + fileDto.getShardIndex();
        File dest = new File(path);
        // 创建父级目录
        if (!dest.getParentFile().exists()) {
            dest.getParentFile().mkdir();
        }
        // 储存分片
        FileOutputStream fos = new FileOutputStream(dest);
        FileCopyUtils.copy(segmentData, fos);
    }

}//end class
