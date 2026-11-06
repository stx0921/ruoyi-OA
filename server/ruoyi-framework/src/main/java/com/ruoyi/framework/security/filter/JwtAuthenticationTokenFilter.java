package com.ruoyi.framework.security.filter;

import java.io.IOException;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import com.ruoyi.common.core.domain.model.LoginUser;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.framework.web.service.TokenService;

/**
 * token过滤器 验证token有效性
 * 
 * @author ruoyi
 */
@Component
public class JwtAuthenticationTokenFilter extends OncePerRequestFilter
{
    @Autowired
    private TokenService tokenService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException
    {
        HttpServletRequest authRequest = request;
        // 工单附件由 <img>/<a> 直接访问，浏览器的图片/链接请求无法携带 Authorization 头：
        // 仅对该 GET 预览接口回退读取前端登录 Cookie（RuoYiOA-Token），不扩大到其它接口。
        if ("GET".equalsIgnoreCase(request.getMethod())
                && request.getRequestURI().contains("/oa/feedback/attachment/")
                && StringUtils.isEmpty(request.getHeader("Authorization")))
        {
            Cookie[] cookies = request.getCookies();
            if (cookies != null)
            {
                for (Cookie cookie : cookies)
                {
                    if ("RuoYiOA-Token".equals(cookie.getName()) && StringUtils.isNotEmpty(cookie.getValue()))
                    {
                        final String token = cookie.getValue();
                        authRequest = new HttpServletRequestWrapper(request)
                        {
                            @Override
                            public String getHeader(String name)
                            {
                                if ("Authorization".equalsIgnoreCase(name))
                                {
                                    return "Bearer " + token;
                                }
                                return super.getHeader(name);
                            }
                        };
                        break;
                    }
                }
            }
        }
        LoginUser loginUser = tokenService.getLoginUser(authRequest);
        if (StringUtils.isNotNull(loginUser) && StringUtils.isNull(SecurityUtils.getAuthentication()))
        {
            tokenService.verifyToken(loginUser);
            UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(loginUser, null, loginUser.getAuthorities());
            authenticationToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(authenticationToken);
        }
        chain.doFilter(request, response);
    }
}
