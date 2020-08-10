package com.sue.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * @author sue
 * @date 2020/8/10 11:49
 */

@Controller
public class SSOController {
    @GetMapping("/hello")
    public Object hello(){
        return "sso";
    }
}
