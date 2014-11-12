package web_relays.research;

import gae_store_space.AppInstance;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.codehaus.jackson.map.ObjectMapper;

import web_relays.protocols.PathValue;

import com.google.common.base.Optional;


// CASE: на вебе есть список генераторов страницы, берем имя генератора, и для него возврящаем 
//   распределение. Если генератор не найден - данные на вебе устарели.
public class GetDistribution extends HttpServlet {
  private static final long serialVersionUID = 4122657047047348423L;
  
  private AppInstance app = AppInstance.getInstance(); 
  
	@Override
  public void doGet(
    HttpServletRequest request,
    HttpServletResponse response) throws ServletException, IOException
  {
		Optional<String> v = Optional.fromNullable(request.getParameter("arg0"));
		if (v.isPresent()) {
			PathValue path = new ObjectMapper().readValue(v.get(), PathValue.class);
	  	String r = new ObjectMapper().writeValueAsString(app.getDistribution(path));
	  	
	  	response.setContentType("text/html");
	  	response.setStatus(HttpServletResponse.SC_OK);
	    response.setCharacterEncoding("UTF-8");
	    response.getWriter().println(r);
    } else {
    	response.setContentType("text/html");
	  	response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
    }
  }
}
