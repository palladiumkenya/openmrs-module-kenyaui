/**
 * The contents of this file are subject to the OpenMRS Public License
 * Version 1.0 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://license.openmrs.org
 *
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See the
 * License for the specific language governing rights and limitations
 * under the License.
 *
 * Copyright (C) OpenMRS, LLC.  All Rights Reserved.
 */

package org.openmrs.module.kenyaui.interceptor;

import org.openmrs.api.APIAuthenticationException;
import org.openmrs.api.context.Context;
import org.openmrs.module.appframework.AppDescriptor;
import org.openmrs.module.appframework.api.AppFrameworkService;
import org.openmrs.module.kenyaui.annotation.AppPage;
import org.openmrs.module.kenyaui.annotation.PublicAction;
import org.openmrs.module.kenyaui.annotation.PublicPage;
import org.openmrs.module.kenyaui.annotation.SharedPage;
import org.openmrs.ui.framework.fragment.FragmentActionRequest;
import org.openmrs.ui.framework.interceptor.FragmentActionInterceptor;
import org.openmrs.ui.framework.interceptor.PageRequestInterceptor;
import org.openmrs.ui.framework.page.PageContext;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;

/**
 * Permissions checking interceptor for all UI framework page and action requests
 */
@Component
public class AppSecurityInterceptor implements PageRequestInterceptor, FragmentActionInterceptor {

	/**
	 * @see PageRequestInterceptor#beforeHandleRequest(org.openmrs.ui.framework.page.PageContext)
	 */
	@Override
	public void beforeHandleRequest(PageContext context) {
		Class<?> controllerClazz = context.getController().getClass();

		PublicPage publicPage = controllerClazz.getAnnotation(PublicPage.class);
		AppPage appPage = controllerClazz.getAnnotation(AppPage.class);
		SharedPage sharedPage = controllerClazz.getAnnotation(SharedPage.class);

		if (countNonNull(publicPage, appPage, sharedPage) > 1) {
			throw new RuntimeException("Page controller should have only one of the @OpenPage, @AppPage and @SharedPage annotations");
		}

		// Start by checking if a login is required
		if (publicPage == null && !Context.isAuthenticated()) {
			throw new APIAuthenticationException("Login is required");
		}

		String requestAppId = null;

		// Set the current request app based on @AppPage or @SharedPage
		if (appPage != null) {
			requestAppId = appPage.value();
		}
		else if (sharedPage != null) {
			List<String> allowedAppIds = Arrays.asList(sharedPage.value());

			// Read app id from request
			requestAppId = (String) context.getRequest().getAttribute("appId");

			if (requestAppId == null) {
				throw new RuntimeException("Shared page controller requires the appId request parameter");
			}
			else if (!allowedAppIds.contains(requestAppId)) {
				throw new RuntimeException("Shared page accessed with invalid appId: " + requestAppId);
			}
		}

		setRequestApp(context, requestAppId);
	}

	/**
	 * @see FragmentActionInterceptor#beforeHandleRequest(org.openmrs.ui.framework.fragment.FragmentActionRequest, java.lang.reflect.Method)
	 */
	@Override
	public void beforeHandleRequest(FragmentActionRequest request, Method controllerMethod) {
		PublicAction publicAction = controllerMethod.getAnnotation(PublicAction.class);

		if (publicAction == null && !Context.isAuthenticated()) {
			throw new APIAuthenticationException("Login is required");
		}
	}

	/**
	 * Sets the app associated with the given request
	 * @param pageContext the page context
	 * @param appId the app id (may be null)
	 */
	public static void setRequestApp(PageContext pageContext, String appId) {
		AppDescriptor app = null;

		if (appId != null) {
			app = Context.getService(AppFrameworkService.class).getAppById(appId);

			if (app == null) {
				throw new RuntimeException("No such app with appId " + appId);
			}

			// Check logged in user has require privilege for this app
			if (!Context.hasPrivilege(app.getRequiredPrivilegeName())) {
				throw new APIAuthenticationException("Insufficient privileges for app");
			}
		}

		// Important to add these attributes even if they're null
		pageContext.getRequest().getRequest().setAttribute("currentApp", app);
		pageContext.getModel().addAttribute("currentApp", app);
	}

	/**
	 * Counts the number of non-null arguments
	 * @param args the arguments
	 * @return the number which are non-null
	 */
	private static int countNonNull(Object... args) {
		int count = 0;
		for (Object arg : args) {
			if (arg != null) {
				++count;
			}
		}
		return count;
	}
}