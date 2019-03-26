package com.datapath.web.mappers;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;

@Slf4j
public class GeneralBeanMapper {

    public static Object map(Object source, Class targetClass) {
        try {
            Object targetObj = targetClass.newInstance();
            BeanUtils.copyProperties(source, targetObj);
            return targetObj;
        } catch (InstantiationException | IllegalAccessException e) {
            log.error("Cant't create instance of class {}", targetClass.getSimpleName());
            log.error(e.getMessage(), e);
        }

        return null;
    }
}
