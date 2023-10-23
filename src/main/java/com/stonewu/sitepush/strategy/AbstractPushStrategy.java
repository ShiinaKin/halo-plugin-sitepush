package com.stonewu.sitepush.strategy;

import cn.hutool.http.HttpResponse;
import com.stonewu.sitepush.DefaultSettingFetcher;
import com.stonewu.sitepush.setting.PushSettingProvider;
import com.stonewu.sitepush.utils.AuthProxyHttpRequestSender;
import com.stonewu.sitepush.utils.DefaultHttpRequestSender;
import com.stonewu.sitepush.utils.HttpRequestSender;
import com.stonewu.sitepush.utils.ProxyHttpRequestSender;
import java.net.InetSocketAddress;
import java.net.Proxy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;

/**
 * @author Erzbir
 * @Date 2023/10/23
 */
@Slf4j
public abstract class AbstractPushStrategy implements PushStrategy {
    protected final DefaultSettingFetcher settingFetcher;

    protected HttpRequestSender httpRequestSender;

    public AbstractPushStrategy(DefaultSettingFetcher settingFetcher) {
        this.settingFetcher = settingFetcher;
    }

    @Override
    public int push(String siteUrl, String key, String pageLink) {
        PushSettingProvider settingProvider = getSettingProvider();
        updateHttpRequestSender(settingProvider);
        String token = settingProvider.getAccess();
        if (settingProvider.isEnable() && StringUtils.hasText(token)) {
            HttpResponse response;
            try {
                response = request(siteUrl, pageLink, settingProvider);
            } catch (Exception e) {
                log.info("Push exception: {}", e.getMessage());
                return 0;
            }
            String body = response.body();
            log.info("Pushing to {} Result: {}", getPushType(), body);
            boolean ok = response.isOk();
            response.close();
            return ok ? 1 : 0;
        }
        return -1;

    }

    /**
     * 通过设置更新请求策略
     */
    protected void updateHttpRequestSender(PushSettingProvider settingProvider) {
        if (settingProvider.isUseProxy()) {
            Proxy proxy = new Proxy(settingProvider.getProxyType(), new InetSocketAddress(
                settingProvider.getProxyAddress(), settingProvider.getProxyPort()));
            if (settingProvider.proxyAuthEnable()) {
                httpRequestSender =
                    new AuthProxyHttpRequestSender(settingProvider.getProxyUsername(),
                        settingProvider.getProxyPassword(), proxy);
            } else {
                httpRequestSender = new ProxyHttpRequestSender(proxy);
            }
        } else {
            httpRequestSender = new DefaultHttpRequestSender();
        }
    }

    protected abstract PushSettingProvider getSettingProvider();

    protected abstract HttpResponse request(String siteUrl, String pageLink,
        PushSettingProvider settingProvider) throws Exception;
}
