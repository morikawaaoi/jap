package com.fujieid.jap.demo.controller;

import com.alibaba.fastjson.JSONObject;
import com.fujieid.jap.core.JapUser;
import com.fujieid.jap.core.JapUserService;
import com.fujieid.jap.core.result.JapResponse;
import com.fujieid.jap.demo.config.JapConfigContext;
import com.fujieid.jap.demo.util.ViewUtil;
import com.fujieid.jap.http.adapter.jakarta.JakartaRequestAdapter;
import com.fujieid.jap.http.adapter.jakarta.JakartaResponseAdapter;
import com.fujieid.jap.social.SocialConfig;
import com.fujieid.jap.social.SocialStrategy;
import me.zhyd.oauth.config.AuthConfig;
import me.zhyd.oauth.model.AuthToken;
import me.zhyd.oauth.model.AuthUser;
import me.zhyd.oauth.utils.UuidUtils;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.ModelAndView;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * 需要依赖 jap-social 模块
 *
 * @author yadong.zhang (yadong.zhang0415(a)gmail.com)
 * @version 1.0.0
 * @date 2021/1/12 14:07
 * @since 1.0.0
 */
@RestController
@RequestMapping("/social")
public class SocialController implements InitializingBean {

    @Resource(name = "social")
    private JapUserService japUserService;
    private SocialStrategy socialStrategy;

    @RequestMapping("/login/gitee")
    public ModelAndView renderAuth(HttpServletRequest request, HttpServletResponse response) {
        JapConfigContext.strategy = "social";
        SocialConfig config = new SocialConfig();
        // platform 参考 justauth#AuthDefaultSource
        // 如果包含通过 justauth 自定义的第三方平台，则该值为实现 AuthSource 后的 getName() 值
        //gitee
        config.setPlatform("gitee");
        config.setState(UuidUtils.getUUID());
        config.setJustAuthConfig(AuthConfig.builder()
                .clientId("d08dfa479bb5bb3e8ba88419ac80bcdc4f0b26a5165884b5d17a8ccece84e66a")
                .clientSecret("cc81ec922aabd37827a412f8a7c5a6e5aade572359ccaaab2469c86e98afa1fa")
                .redirectUri("http://sso.jap.com:8443/social/login/gitee")
                .build());
        //github
        /*config.setPlatform("github");
        config.setJustAuthConfig(AuthConfig.builder()
                .clientId("2f049372929ff04a26a8")
                .clientSecret("52ee879f70e05c7215a91032658ee54aa8a15d05")
                .redirectUri("http://sso.jap.com:8443/social/login/gitee")
                .build());*/
        //github
        //2f049372929ff04a26a8
        //52ee879f70e05c7215a91032658ee54aa8a15d05

        JapResponse japResponse = socialStrategy.authenticate(config, new JakartaRequestAdapter(request), new JakartaResponseAdapter(response));
        if (japResponse.isSuccess() && !japResponse.isRedirectUrl()) {
            JapUser japUser = (JapUser) japResponse.getData();
            AuthUser authUser = (AuthUser) japUser.getAdditional();
            AuthToken authToken = authUser.getToken();
            // 测试获取用户信息的接口
            try {
                JapResponse userInfoRes = socialStrategy.getUserInfo(config, authToken);
                System.out.println("通过 token 获取的用户信息：" + JSONObject.toJSONString(userInfoRes));
            } catch (Exception e) {
                System.err.println("通过 token 获取的用户信息出错：" + e.getMessage());
            }

            // 测试刷新令牌的接口
            try {
                JapResponse refreshRes = socialStrategy.refreshToken(config, authToken);
                System.out.println("refresh token ：" + JSONObject.toJSONString(refreshRes));
            } catch (Exception e) {
                System.err.println("refresh token 出错：" + e.getMessage());
            }

            // 测试回收令牌的接口
            try {
                JapResponse revokeRes = socialStrategy.revokeToken(config, authToken);
                System.out.println("revoke token：" + JSONObject.toJSONString(revokeRes));
            } catch (Exception e) {
                System.err.println("revoke token 出错：" + e.getMessage());
            }
        }
        return ViewUtil.getView(japResponse);
    }

    /**
     * 初始化 bean 时对 SimpleStrategy 进行初始化，适用于启用了 SSO 的情况，如果没有启用 SSO，则非强制使用该方式初始化
     *
     * @throws Exception
     */
    @Override
    public void afterPropertiesSet() throws Exception {
        socialStrategy = new SocialStrategy(japUserService, JapConfigContext.getConfig());

    }
}
