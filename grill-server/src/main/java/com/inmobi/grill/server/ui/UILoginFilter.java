package com.inmobi.grill.server.ui;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.core.Cookie;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import java.io.IOException;
import java.util.Map;

public class UILoginFilter implements ContainerRequestFilter {
  public static final Log LOG = LogFactory.getLog(UILoginFilter.class);
  public static final String SESSION_COOKIE = "grillSession";

  @Override
  public void filter(ContainerRequestContext reqCtx) throws IOException {
    Map<String, Cookie> cookies = reqCtx.getCookies();
    if (!cookies.containsKey(SESSION_COOKIE)) {
      LOG.info("Session cookie not set");
      reqCtx.abortWith(Response.seeOther(UriBuilder.fromPath("/ui/login").build()).build());
    }
  }
}
