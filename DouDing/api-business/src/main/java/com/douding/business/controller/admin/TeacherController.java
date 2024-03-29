package com.douding.business.controller.admin;




import com.douding.server.dto.TeacherDto;
import com.douding.server.dto.PageDto;
import com.douding.server.dto.ResponseDto;
import com.douding.server.service.TeacherService;
import com.douding.server.util.ValidatorUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;


@RestController
@RequestMapping("/admin/teacher")
public class TeacherController {

    private static final Logger LOG = LoggerFactory.getLogger(TeacherController.class);
    //给了日志用的
    public  static final String BUSINESS_NAME ="讲师";

    @Resource
    private TeacherService teacherService;

    @RequestMapping("/list")
    public ResponseDto<PageDto<TeacherDto>> list(PageDto<TeacherDto> pageDto){
        ResponseDto<PageDto<TeacherDto>> responseDto = new ResponseDto<>();
        teacherService.list(pageDto);
        responseDto.setContent(pageDto);
        return responseDto;
    }

    @PostMapping("/save")
    public ResponseDto<TeacherDto> save(@RequestBody TeacherDto teacherDto){
        ResponseDto<TeacherDto> responseDto = new ResponseDto<>();
        teacherService.save(teacherDto);
        responseDto.setContent(teacherDto);
        return responseDto;
    }
    
    @DeleteMapping("/delete/{id}")
    public ResponseDto<TeacherDto> delete(@PathVariable String id){
        ResponseDto<TeacherDto> responseDto = new ResponseDto<>();
        teacherService.delete(id);
        return responseDto;
    }

    @RequestMapping("/all")
    public ResponseDto<List<TeacherDto>> all(){
        ResponseDto<List<TeacherDto>> responseDto = new ResponseDto<>();
        List<TeacherDto> teacherDtoList = teacherService.all();
        responseDto.setContent(teacherDtoList);
        return responseDto;
    }

}//end class