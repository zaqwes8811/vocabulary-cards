package web_relays;

import gae_store_space.AppInstance;

import java.io.IOException;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.codehaus.jackson.map.ObjectMapper;

import pipeline.TextPipeline;

import web_relays.protocols.PageSummaryValue;
// 
public class GetterUserInformation extends HttpServlet {
	/**
	 * 
	 */
  private static final long serialVersionUID = 5249671566813631715L;
  
	private AppInstance app = AppInstance.getInstance(); 

	@Override
  public void doGet(
    HttpServletRequest request,
    HttpServletResponse response) throws ServletException, IOException
  {

    response.setContentType("text/html");
    response.setStatus(HttpServletResponse.SC_OK);
    
    List<PageSummaryValue> v = app.getUserInformation(TextPipeline.defaultUserId);
    String json = new ObjectMapper().writeValueAsString(v);

    response.setCharacterEncoding("UTF-8");
    response.getWriter().println(json);
  }
}
