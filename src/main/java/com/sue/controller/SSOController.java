package com.sue.controller;

import com.sue.pojo.Users;
import com.sue.pojo.vo.UsersVO;
import com.sue.service.mallservice.UserService;
import com.sue.utils.JsonUtils;
import com.sue.utils.MD5Utils;
import com.sue.utils.RedisOperator;
import org.apache.commons.lang3.StringUtils;
import org.apache.ibatis.annotations.Param;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.UUID;

/**
 * @author sue
 * @date 2020/8/10 11:49
 */

@Controller
public class SSOController {


    @Autowired
    private UserService userService;

    @Autowired
    private RedisOperator redisOperator;

    public static final String REDIS_USER_TOKEN = "redis_user_token";
    public static final String REDIS_USER_TICKET = "redis_user_ticket";
    public static final String REDIS_TMP_TICKET = "redis_tmp_ticket";
    public static final String COOKIE_USER_TICKET = "cookie_user_ticket";


    @GetMapping("/login")
    public String login(String returnUrl, Model model, HttpServletRequest request, HttpServletResponse response){
        model.addAttribute("returnUrl",request);

        //用户从未登录过，第一次进入则跳转到CAS的统一登录界面
        return "login";
    }


    /**
     * CAS的统一登录接口
     *  目的:
     *      1.登陆后创建全局会话 -> uniqueToken
     *      2.创建用户全局门票，用以表示在CAS端是否登录 -> userTIcket
     *      3.创建用户的临时门票，用于回跳回传 -> tmpTicket
     * @param username
     * @param password
     * @param returnUrl
     * @param model
     * @param request
     * @param response
     * @return
     */
    @PostMapping("/doLogin")
    public String doLogin(
            String username,
            String password,
            String returnUrl,Model model,
            HttpServletRequest request,
            HttpServletResponse response
    ){
        model.addAttribute("returnUrl",returnUrl);

        // 0. 判断用户名和密码必须不为空
        if (StringUtils.isBlank(username) ||
                StringUtils.isBlank(password)) {
            model.addAttribute("errmsg", "用户名或密码不能为空");
            return "login";
        }

        // 1. 实现登录
        Users userResult = userService.queryUserForLogin(username,
                MD5Utils.getMD5Str(password));
        if (userResult == null) {
            model.addAttribute("errmsg", "用户名或密码不正确");
            return "login";
        }

        // 2. 实现用户的redis会话
        String uniqueToken = UUID.randomUUID().toString().trim();
        UsersVO usersVO = new UsersVO();
        BeanUtils.copyProperties(userResult, usersVO);
        usersVO.setUserUniqueToken(uniqueToken);
        redisOperator.set(REDIS_USER_TOKEN + ":" + userResult.getId(),
                JsonUtils.objectToJson(usersVO));

        //3.生成ticket门票，全局门票，代表用户在cas端登陆过
        String userTicket = UUID.randomUUID().toString().trim();

        //3.1 用户全局门票需要放入CAS端Cookie中
        this.setCookie(COOKIE_USER_TICKET,userTicket,response);

        //4.userTicket关联用户Id并且放入到Redis中，代表这个用户可以在各个系统访问
        redisOperator.set(REDIS_USER_TICKET+":"+userTicket,userResult.getId());


        //5.生成临时票据，回跳到调用端网站，是由CAS端所签发的一个一次性的临时ticket
        String tmpTicket = createTmpTicket();


        /**
         * userTicket 用于表示用户在CAS端的一个登陆状态：已经登陆
         * tmpTicket 用于颁发给用户j进行一次性的验证票据 有时效性
         *
         */


        return "redirect:"+returnUrl+"?tmpTicket="+tmpTicket;
    }


    /**
     * 创建临时票据
     * @return
     */
    private String createTmpTicket(){
        String tmpTicket = UUID.randomUUID().toString().trim();
        redisOperator.set(REDIS_TMP_TICKET+":"+tmpTicket,MD5Utils.getMD5Str(tmpTicket),600);
        return tmpTicket;
    }

    private void setCookie(String key,String val,HttpServletResponse response){
        Cookie cookie = new Cookie(key,val);
        cookie.setDomain("sso.com");
        cookie.setPath("/");
        response.addCookie(cookie);
    }
}
