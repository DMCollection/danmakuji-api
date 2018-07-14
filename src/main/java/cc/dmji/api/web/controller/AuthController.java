package cc.dmji.api.web.controller;

import cc.dmji.api.annotation.UserLog;
import cc.dmji.api.common.Result;
import cc.dmji.api.common.ResultCode;
import cc.dmji.api.constants.RedisKey;
import cc.dmji.api.constants.SecurityConstants;
import cc.dmji.api.entity.User;
import cc.dmji.api.enums.UserStatus;
import cc.dmji.api.service.MailService;
import cc.dmji.api.service.RedisTokenService;
import cc.dmji.api.service.UserService;
import cc.dmji.api.utils.GeneralUtils;
import cc.dmji.api.utils.JwtTokenUtils;
import org.apache.catalina.servlet4preview.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.mail.MessagingException;
import java.util.concurrent.TimeUnit;

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

    @Autowired
    private MailService mailService;

    @Autowired
    private JwtTokenUtils jwtTokenUtils;

    @GetMapping("/logout")
    @UserLog("登出")
    public ResponseEntity<Result> logout(HttpServletRequest request) {
        String header = request.getHeader(SecurityConstants.TOKEN_HEADER_AUTHORIZATION);
        if (!StringUtils.isEmpty(header)){
            String token = header.replace(SecurityConstants.TOKEN_PREFIX, "");
            if (redisTokenService.hasToken(token)) {
                Long tokenIndex = redisTokenService.invalidToken(token);
                redisTokenService.deleteUserLock(jwtTokenUtils.getUid(token));
                logger.info("登出成功, 该tokenIndex为：{}", tokenIndex);
                return getResponseEntity(HttpStatus.OK, getSuccessResult("登出成功"));
            } else {
                return getResponseEntity(HttpStatus.BAD_REQUEST, getErrorResult(ResultCode.PARAM_IS_INVALID));
            }
        }else {
            return getResponseEntity(HttpStatus.BAD_REQUEST, getErrorResult(ResultCode.PARAM_IS_INVALID));
        }
    }

    @GetMapping("/verify/uid/{uid}/key/{key}")
    @UserLog("邮箱验证")
    public ResponseEntity<Result> verifyEmail(@PathVariable("uid") String uid,
                                              @PathVariable("key") String uuid) {

        String key = RedisKey.VERIFY_EMAIL_KEY + uid;
        String redisUUID = stringRedisTemplate.opsForValue().get(key);

        logger.debug("redis UUID : {} , param UUID : {}", redisUUID, uuid);
        if (StringUtils.isEmpty(redisUUID)) {
            return getResponseEntity(HttpStatus.OK, getErrorResult(ResultCode.DATA_EXPIRATION, "该链接已失效!邮箱验证失败!"));
        }
        if (uuid.equalsIgnoreCase(redisUUID)) {
            User user = userService.getUserById(uid);
            user.setEmailVerified(UserStatus.EMAIL_VERIFY.getStatus());
            userService.updateUser(user);
            stringRedisTemplate.delete(key);
            return getResponseEntity(HttpStatus.OK, getSuccessResult("邮箱验证成功!"));
        } else {
            return getResponseEntity(HttpStatus.OK, getSuccessResult("邮箱验证失败!"));
        }
    }

    @GetMapping("/emailReVerify")
    @UserLog("发送邮箱认证请求")
    public ResponseEntity<Result> sendReVerifyEmail(HttpServletRequest request) {

        String userId = getUidFromToken(request);
        if (userId == null) {
            return getResponseEntity(HttpStatus.FORBIDDEN, getErrorResult(ResultCode.PERMISSION_DENY, "用户尚未登录，不能申请邮箱认证"));
        }

        if (stringRedisTemplate.hasKey(RedisKey.RE_VERIFY_EMAIL_LIMIT_ + userId)) {
            Long expire = stringRedisTemplate.getExpire(RedisKey.RE_VERIFY_EMAIL_LIMIT_ + userId, TimeUnit.SECONDS);
            return getResponseEntity(HttpStatus.OK, getErrorResult(ResultCode.PERMISSION_DENY, "请求太频繁，请于" + expire + "秒后再试"));
        }

        User user = userService.getUserById(userId);
        String key = RedisKey.VERIFY_EMAIL_KEY + userId;
        String uuid = GeneralUtils.getUUID();


        try {
            mailService.sendVerifyEmail(user.getEmail(), userId, uuid);
            stringRedisTemplate.opsForValue().set(key, uuid, 20L, TimeUnit.MINUTES);
            stringRedisTemplate.opsForValue().set(RedisKey.RE_VERIFY_EMAIL_LIMIT_ + userId, uuid, 60L, TimeUnit.SECONDS);
        } catch (MessagingException e) {
            e.printStackTrace();
            return getResponseEntity(HttpStatus.INTERNAL_SERVER_ERROR, getErrorResult(ResultCode.SYSTEM_INTERNAL_ERROR, "验证邮箱发送失败，请稍候再试"));
        }

        return getResponseEntity(HttpStatus.OK, getSuccessResult());
    }
}
