/*
 * Copyright 2019-2021 VMware, Inc.
 * SPDX-License-Identifier: EPL-2.0
 */
package com.vmware.l10n.conf;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.web.servlet.config.annotation.ContentNegotiationConfigurer;
import org.springframework.web.servlet.config.annotation.DefaultServletHandlerConfigurer;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.PathMatchConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.util.UrlPathHelper;

import com.vmware.l10n.utils.WhiteListUtils;
import com.vmware.l10n.utils.WhiteListUtils.LocalWhitelistUtils;
import com.vmware.l10n.utils.WhiteListUtils.S3WhitelistUtils;
import com.vmware.vip.api.rest.l10n.L10nI18nAPI;

/**
 * Web Configuration
 */
@Configuration
@EnableWebMvc
public class WebConfiguration implements WebMvcConfigurer {
	private static Logger logger = LoggerFactory.getLogger(WebConfiguration.class);

	@Value("${csp.api.auth.enable:false}")
	private String cspAuthFlag;

	@Autowired
	private TokenService tokenService;

	@Autowired
	private WhiteListUtils whitelistUtils;

	@Bean
	@Profile("bundle")
	@Autowired
	public WhiteListUtils bundleWhitelistUtils(ApplicationContext ctx) {
		return new LocalWhitelistUtils();
	}

	@Bean
	@Profile("s3")
	@Autowired
	public WhiteListUtils s3WhitelistUtils(ApplicationContext ctx) {
		return new S3WhitelistUtils();
	}

	@Override
	public void configurePathMatch(PathMatchConfigurer configurer) {
		UrlPathHelper urlPathHelper = new UrlPathHelper();
		urlPathHelper.setUrlDecode(false);
		configurer.setUrlPathHelper(urlPathHelper);
	}

	@Override
	public void configureContentNegotiation(ContentNegotiationConfigurer configurer) {
		configurer.favorPathExtension(false);
	}

	@Override
	public void configureDefaultServletHandling(DefaultServletHandlerConfigurer configurer) {
		configurer.enable();
	}

	@Override
	public void addInterceptors(InterceptorRegistry registry) {
		// CSP authentication
		if (cspAuthFlag.equalsIgnoreCase("true")) {
			logger.info("add enable CSP authentication interceptor");
			registry.addInterceptor(new CspAuthInterceptor(tokenService))
			.addPathPatterns(L10nI18nAPI.BASE_COLLECT_SOURCE_PATH + "/api/v2/translation/**", L10nI18nAPI.BASE_COLLECT_SOURCE_PATH + "/api/v1/translation/**");
		}
		logger.info("add source collection validation interceptor");
		registry.addInterceptor(new CollectSourceValidationInterceptor(whitelistUtils.getWhiteList()))
		.addPathPatterns(L10nI18nAPI.BASE_COLLECT_SOURCE_PATH + "/api/v2/translation/**", L10nI18nAPI.BASE_COLLECT_SOURCE_PATH + "/api/v1/translation/**");
	}
}
