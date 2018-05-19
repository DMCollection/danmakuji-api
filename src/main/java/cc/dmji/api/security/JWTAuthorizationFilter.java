package cc.dmji.api.security;

import cc.dmji.api.Constants.SecurityConstants;
import cc.dmji.api.common.ResultCode;
import cc.dmji.api.utils.JwtTokenUtils;
import io.jsonwebtoken.ExpiredJwtException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Collections;

import static cc.dmji.api.Constants.SecurityConstants.*;

/**
 * Created by echisan on 2018/5/18
 */
public class JWTAuthorizationFilter extends BasicAuthenticationFilter {

    private static final Logger logger = LoggerFactory.getLogger(JWTAuthorizationFilter.class);

    public JWTAuthorizationFilter(AuthenticationManager authenticationManager) {
        super(authenticationManager);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws IOException, ServletException {
        String h = request.getHeader("Authorization");

        if (h == null || !h.startsWith(TOKEN_PREFIX)) {
            chain.doFilter(request, response);
            return;
        }

        UsernamePasswordAuthenticationToken authenticationToken = getAuthentication(request,response);
        SecurityContextHolder.getContext().setAuthentication(authenticationToken);
        chain.doFilter(request, response);
    }

    private UsernamePasswordAuthenticationToken getAuthentication(HttpServletRequest request,HttpServletResponse response) {
        String token = request.getHeader(TOKEN_HEADER_AUTHORIZATION);
        if (token != null) {
            String realToken = token.replace(TOKEN_PREFIX, "");
            JwtTokenUtils jwtTokenUtils = new JwtTokenUtils();
            try {
                if (jwtTokenUtils.validateToken(realToken)) {
                    JwtTokenUtils.Payload payload = jwtTokenUtils.getPayload(realToken);
                    logger.debug("username is {}", payload.getUsername());
                    return new UsernamePasswordAuthenticationToken(
                            payload.getUsername(),
                            null,
                            Collections.singleton(
                                    new SimpleGrantedAuthority(payload.getRole())
                            )
                    );
                }
            } catch (ExpiredJwtException e) {
                logger.info("该token已经过时");
                response.setHeader(SecurityConstants.TOKEN_RESULT_CODE_HEADER, String.valueOf(ResultCode.USER_EXPIRATION.getCode()));
            } catch (Exception e){
                logger.info("其他错误");
                response.setHeader(SecurityConstants.TOKEN_RESULT_CODE_HEADER, String.valueOf(ResultCode.PERMISSION_DENY.getCode()));
            }
            return null;
        }
        return null;
    }
}
