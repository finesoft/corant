/*
 * Copyright (c) 2013-2018, Bingo.Chen (finesoft@gmail.com).
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.corant;

import static org.corant.shared.util.MapUtils.getMapString;
import java.io.IOException;
import java.sql.SQLException;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.inject.Named;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.sql.DataSource;
import javax.transaction.Transactional;
import org.apache.commons.dbutils.QueryRunner;
import org.apache.commons.dbutils.handlers.MapListHandler;
import org.corant.kernel.bootstrap.DirectRunner;

/**
 * corant-asosat-ddd
 *
 * @author bingo 下午8:18:27
 *
 */
@ApplicationScoped
@WebServlet(urlPatterns = "/test")
public class TestServlet extends HttpServlet {

  private static final long serialVersionUID = 8174294172579816895L;

  @Inject
  @Named("dmmsRwDs")
  DataSource ds;

  public static void main(String... strings) throws Exception {
    new DirectRunner(null).run();
  }

  @Transactional
  @Override
  protected void service(HttpServletRequest req, HttpServletResponse resp)
      throws ServletException, IOException {
    StringBuilder sb = new StringBuilder("<table>");
    try {
      new QueryRunner(ds).query("SELECT * FROM CT_DMMS_INDU", new MapListHandler()).forEach(m -> {
        sb.append("<tr><td>").append(getMapString(m, "name")).append("</td></tr>");
      });
    } catch (SQLException e) {
      e.printStackTrace();
    }
    sb.append("</table>");
    resp.getWriter().write(sb.toString());
  }

}
