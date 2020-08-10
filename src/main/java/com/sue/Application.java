package com.sue;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import tk.mybatis.spring.annotation.MapperScan;

/**
 * @author sue
 * @date 2020/8/10 11:48
 */

@SpringBootApplication
@MapperScan(basePackages = "com.sue.mapper")
@ComponentScan(basePackages = {"com.sue","org.n3r.idworker"})
public class Application {
    public static void main(String[] args){
        SpringApplication.run(Application.class,args);
    }
}
