package com.inmobi.grill.server.ui;

import com.inmobi.grill.api.APIResult;
import com.inmobi.grill.api.GrillSessionHandle;
import com.inmobi.grill.api.query.QueryHandle;
import com.inmobi.grill.server.api.GrillConfConstants;
import org.apache.hadoop.hive.conf.HiveConf;
import org.glassfish.jersey.media.multipart.FormDataBodyPart;
import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.glassfish.jersey.media.multipart.FormDataMultiPart;
import org.glassfish.jersey.media.multipart.FormDataParam;

import javax.ws.rs.*;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.*;
import java.util.List;

/**
 * This resource transfers requests from the browser to Grill REST api.
 * We use Jersey client to make requests to Grill server so that server modes, sessions etc are followed
 */
@Path("/ui")
public class UIHandler {

  private Client client = ClientBuilder.newClient();
  private HiveConf conf = new HiveConf();

  private WebTarget grillTarget() {
    return client.target(conf.get(GrillConfConstants.GRILL_SERVER_BASE_URL,
      GrillConfConstants.DEFAULT_GRILL_SERVER_BASE_URL));
  }

  private WebTarget sessionTarget() {
    return grillTarget().path("session");
  }

  private WebTarget queryTarget() {
    return grillTarget().path("queryapi");
  }

  @Path("/login")
  @POST
  public Response login(@FormDataParam("username") String username,
                        @FormDataParam("password") String password) {
    FormDataMultiPart mp = new FormDataMultiPart();

    mp.bodyPart(new FormDataBodyPart(FormDataContentDisposition.name("username").build(),
      username));
    mp.bodyPart(new FormDataBodyPart(FormDataContentDisposition.name("password").build(),
      password));

    GrillSessionHandle handle = sessionTarget().request().post(
      Entity.entity(mp, MediaType.MULTIPART_FORM_DATA_TYPE), GrillSessionHandle.class);
    return Response.seeOther(UriBuilder.fromPath("/index.html").build())
                    .cookie(new NewCookie("grillSession", handle.toString(),
                      null, null, null, 0, false))
                    .cookie(new NewCookie("grillUser", username, null, null, null, 0, false))
                    .build();
  }


  @Path("/logout")
  @POST
  public Response logout(@CookieParam("grillSession") String grillSession) {
    GrillSessionHandle handle = GrillSessionHandle.valueOf(grillSession);
    sessionTarget().queryParam("sessionid", handle).request().delete(APIResult.class);
    return Response.seeOther(UriBuilder.fromPath("/ui/login").build())
      .cookie(new NewCookie("grillSession", "__invalid__", null, null, null, 0, false)).build();
  }

  // Submit query
  @Path("newquery")
  @POST
  public QueryHandle newQuery(@FormDataParam("query") String query,
                              @CookieParam("grillSession") String grillSession) {

    FormDataMultiPart mp = new FormDataMultiPart();
    mp.bodyPart(new FormDataBodyPart(FormDataContentDisposition.name("sessionid").build(),
      GrillSessionHandle.valueOf(grillSession), MediaType.APPLICATION_XML_TYPE));
    mp.bodyPart(new FormDataBodyPart(FormDataContentDisposition.name("query").build(), query));
    mp.bodyPart(new FormDataBodyPart(FormDataContentDisposition.name(
      "operation").build(), "execute"));

    return queryTarget().path("queries").request().post(
      Entity.entity(mp, MediaType.MULTIPART_FORM_DATA_TYPE), QueryHandle.class);
  }

  // Cancel query
  @Path("cancelquery/{queryHandle}")
  @DELETE
  public Response cancelQuery(@CookieParam("grillSession") String grillSession,
                          @PathParam("queryHandle") String queryHandle) {
    APIResult result = queryTarget().path("query").path(queryHandle)
      .queryParam("sessionid", GrillSessionHandle.valueOf(grillSession)).request().delete(APIResult.class);
    return Response.ok().build();
  }


  // Get query list
  @Path("queries")
  @GET
  @Produces({MediaType.APPLICATION_JSON})
  public List<QueryHandle> getQueryList(@CookieParam("grillSession") String grillSession,
                                        @CookieParam("grillUser") String grillUser) {
    return queryTarget().path("queries")
      .queryParam("sessionid", GrillSessionHandle.valueOf(grillSession))
      .queryParam("user", grillUser)
      .request(MediaType.APPLICATION_XML)
      .get(new GenericType<List<QueryHandle>>() {});
  }


}
