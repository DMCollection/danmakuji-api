package cc.dmji.api.web.controller;

import cc.dmji.api.annotation.ValidUserSelf;
import cc.dmji.api.common.Result;
import cc.dmji.api.common.ResultCode;
import cc.dmji.api.constants.RedisKey;
import cc.dmji.api.constants.SecurityConstants;
import cc.dmji.api.entity.User;
import cc.dmji.api.enums.Role;
import cc.dmji.api.enums.UserStatus;
import cc.dmji.api.service.RedisTokenService;
import cc.dmji.api.service.UserService;
import cc.dmji.api.utils.DmjiUtils;
import org.apache.catalina.servlet4preview.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

/**
 * Created by echisan on 2018/5/18
 */
@RestController
@RequestMapping("/auth")
public class AuthController extends BaseController {

    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);

    @Autowired
    private UserService userService;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Autowired
    private RedisTokenService redisTokenService;

    @PostMapping("/register")
    public ResponseEntity<Result> createAuthenticationToken(
            @RequestBody User user) {

        String nick = user.getNick();
        String password = user.getPwd();
        String email = user.getEmail();

        if (!DmjiUtils.validUsername(nick)) {
            return getResponseEntity(HttpStatus.OK, getErrorResult(ResultCode.PARAM_IS_INVALID, "账号格式不符合要求"));
        } else {
            User dbUser = userService.getUserByNick(nick);
            if (dbUser == null) {
                return getResponseEntity(HttpStatus.OK, getErrorResult(ResultCode.PARAM_IS_INVALID, "该用户已存在"));
            }
        }
        if (!DmjiUtils.validPassword(password)) {
            return getResponseEntity(HttpStatus.OK, getErrorResult(ResultCode.PARAM_IS_INVALID, "密码格式不符合要求"));
        }

        if (!DmjiUtils.validEmail(email)) {
            return getResponseEntity(HttpStatus.OK, getErrorResult(ResultCode.PARAM_IS_INVALID, "邮箱格式不正确"));
        } else {
            User dbUser = userService.getUserByEmail(email);
            if (dbUser == null) {
                return getResponseEntity(HttpStatus.OK, getErrorResult(ResultCode.PARAM_IS_INVALID, "该邮箱地址已被使用"));
            }
        }

        // 验证通过
        User newUser = new User();
        newUser.setNick(nick);
        newUser.setEmail(email);
        newUser.setEmailVerified(UserStatus.EMAIL_UN_VERIFY.getStatus());
        newUser.setPwd(password);
        newUser.setRole(Role.USER.getName());
        newUser.setIsLock(UserStatus.UN_LOCK.getStatus());

        try {
            User user1 = userService.insertUser(user);
            user1.setPwd("不让看");
            return new ResponseEntity<Result>(
                    getSuccessResult(user1, "注册成功"),
                    HttpStatus.OK
            );
        } catch (Exception e) {
            logger.info("数据库出现偏差，原因：{}", e.getMessage());
            return new ResponseEntity<>(
                    getErrorResult(ResultCode.SYSTEM_INTERNAL_ERROR),
                    HttpStatus.INTERNAL_SERVER_ERROR
            );
        }
    }

    @GetMapping("/logout")
    public ResponseEntity<Result> logout(HttpServletRequest request) {
        String header = request.getHeader(SecurityConstants.TOKEN_HEADER_AUTHORIZATION);
        String token = header.replace(SecurityConstants.TOKEN_PREFIX, "");
        if (redisTokenService.hasToken(token)){
            Long tokenIndex = redisTokenService.invalidToken(token);
            logger.info("登出成功, 该tokenIndex为：{}",tokenIndex);
            return getResponseEntity(HttpStatus.OK, getSuccessResult("登出成功"));
        }else {
            return getResponseEntity(HttpStatus.BAD_REQUEST,getErrorResult(ResultCode.PARAM_IS_INVALID));
        }
    }

    @GetMapping("/verify/uid/{uid}/key/{key}")
    public ResponseEntity<Result> verifyEmail(@PathVariable("uid") String uid,
                                              @PathVariable("key") String uuid) {

        String key = RedisKey.VERIFY_EMAIL_KEY + uid;
        String redisUUID = stringRedisTemplate.opsForValue().get(key);
        if (StringUtils.isEmpty(redisUUID)) {
            return getResponseEntity(HttpStatus.OK, getErrorResult(ResultCode.DATA_EXPIRATION, "该链接已失效"));
        }
        if (uuid.equalsIgnoreCase(redisUUID)) {
            User user = userService.getUserById(uid);
            user.setEmailVerified(UserStatus.EMAIL_VERIFY.getStatus());
            stringRedisTemplate.delete(key);
            return getResponseEntity(HttpStatus.OK, getSuccessResult("邮箱验证成功!"));
        } else {
            return getResponseEntity(HttpStatus.OK, getErrorResult(ResultCode.DATA_IS_WRONG));
        }
    }
}
